package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendAttemptVO;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.MessageRouterService;
import com.example.smartmessaging.service.repository.HistoryMapper;
import com.example.smartmessaging.util.SmsMessageTypeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMessageSenderImpl implements MessageSender {

    private final HistoryMapper historyMapper;
    private final ChannelService channelService;
    private final MessageRouterService messageRouterService;

    @Override
    @Transactional
    public void send(MessageTaskDto task) {
        historyMapper.updateSendTargetStatus(task.getSendTargetId(), "SENDING");

        List<String> sequence = task.getFallbackSequence();
        if (sequence == null || sequence.isEmpty()) {
            markFailed(task, 1, null, "EMPTY-SEQUENCE");
            return;
        }

        for (int step = Math.max(0, task.getCurrentStep()); step < sequence.size(); step++) {
            String channelType = normalizeChannelType(sequence.get(step));
            Long channelId = findChannelId(channelType);
            if (channelId == null) {
                saveAttempt(task, step + 1, null, false, "UNSUPPORTED-" + channelType);
                continue;
            }

            if (!isRealCustomer(task)) {
                String actualChannelType = resolveActualChannelType(task, channelType);
                Long actualChannelId = findChannelId(actualChannelType);
                if (actualChannelId == null) {
                    saveAttempt(task, step + 1, null, false, "UNSUPPORTED-" + actualChannelType);
                    continue;
                }
                saveMockSuccess(task, step + 1, actualChannelId, actualChannelType);
                return;
            }

            SendResult result = sendReal(task, step, channelType);
            String resultChannelType = normalizeChannelType(result.getChannel());
            if (resultChannelType.isBlank()) {
                resultChannelType = channelType;
            }
            Long resultChannelId = findChannelId(resultChannelType);
            if (resultChannelId == null) {
                resultChannelId = channelId;
            }

            saveAttempt(
                    task,
                    step + 1,
                    resultChannelId,
                    result.isSuccess(),
                    result.isSuccess() ? "REAL-" + resultChannelType : result.getErrorCode()
            );

            if (result.isSuccess()) {
                historyMapper.updateSendTargetSuccess(task.getSendTargetId(), resultChannelId);
                historyMapper.incrementSuccessCount(task.getSendHistoryId());
                historyMapper.incrementActualCostByChannel(task.getSendHistoryId(), channelId);
                completeHistoryIfFinished(task.getSendHistoryId());
                return;
            }

            log.warn("[CustomerMessageSender] real send failed. fallback continues. messageId={}, targetId={}, channel={}, error={}",
                    task.getMessageId(), task.getSendTargetId(), resultChannelType, result.getErrorCode());
        }

        historyMapper.updateSendTargetStatus(task.getSendTargetId(), "FAILED");
        historyMapper.incrementFailCount(task.getSendHistoryId());
        completeHistoryIfFinished(task.getSendHistoryId());
    }

    private SendResult sendReal(MessageTaskDto task, int step, String channelType) {
        task.setCurrentStep(step);
        log.info("[CustomerMessageSender] real send messageId={}, targetId={}, customerId={}, channel={}",
                task.getMessageId(), task.getSendTargetId(), task.getCustomerId(), channelType);
        return messageRouterService.send(task);
    }

    private void saveMockSuccess(MessageTaskDto task, int attemptOrder, Long channelId, String channelType) {
        log.info("[CustomerMessageSender] mock send messageId={}, targetId={}, customerId={}, channel={}, recipient={}",
                task.getMessageId(), task.getSendTargetId(), task.getCustomerId(), channelType, selectRecipient(task, channelType));

        saveAttempt(task, attemptOrder, channelId, true, "MOCK-" + task.getMessageId());
        historyMapper.updateSendTargetSuccess(task.getSendTargetId(), channelId);
        historyMapper.incrementSuccessCount(task.getSendHistoryId());
        completeHistoryIfFinished(task.getSendHistoryId());
    }

    private void markFailed(MessageTaskDto task, int attemptOrder, Long channelId, String reason) {
        saveAttempt(task, attemptOrder, channelId, false, reason);
        historyMapper.updateSendTargetStatus(task.getSendTargetId(), "FAILED");
        historyMapper.incrementFailCount(task.getSendHistoryId());
        completeHistoryIfFinished(task.getSendHistoryId());
    }

    private void saveAttempt(MessageTaskDto task, int attemptOrder, Long channelId, boolean success, String messageIdOrReason) {
        Long auditUserId = task.getUserId() != null ? task.getUserId() : 1L;
        int nextAttemptOrder = historyMapper.findNextAttemptOrder(task.getSendTargetId());
        SendAttemptVO attempt = SendAttemptVO.builder()
                .sendTargetId(task.getSendTargetId())
                .attemptOrder(nextAttemptOrder)
                .channelId(channelId)
                .isSucceeded(success)
                .solapiMessageId(messageIdOrReason)
                .build();
        attempt.setCreatedBy(auditUserId);
        attempt.setUpdatedBy(auditUserId);
        historyMapper.insertSendAttempt(attempt);
    }

    private void completeHistoryIfFinished(Long sendHistoryId) {
        if (historyMapper.countUnfinishedTargets(sendHistoryId) == 0) {
            historyMapper.finalizeSendHistory(sendHistoryId, "SENT");
        }
    }

    private String resolveActualChannelType(MessageTaskDto task, String channelType) {
        String normalized = normalizeChannelType(channelType);
        if ("SMS".equals(normalized) || "LMS".equals(normalized)) {
            return SmsMessageTypeResolver.resolve(
                    normalized,
                    personalize(task.getTitle(), task.getCustomerName()),
                    SmsMessageTypeResolver.buildMessageText(
                            personalize(task.getContent(), task.getCustomerName()),
                            task.getPurpose(),
                            task.getActionButtonName(),
                            task.getActionUrl(),
                            task.getUnsubscribeUrl()
                    )
            );
        }
        return normalized;
    }

    private String personalize(String text, String customerName) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String name = customerName == null || customerName.isBlank() ? "\uACE0\uAC1D" : customerName.trim();
        return text
                .replace("#{\uACE0\uAC1D\uBA85}", name)
                .replace("#{\uC774\uB984}", name);
    }

    private Long findChannelId(String channelType) {
        return channelService.getActiveChannels().stream()
                .filter(channel -> normalizeChannelType(channel.getChannelType()).equals(channelType))
                .map(ChannelVO::getId)
                .findFirst()
                .orElse(null);
    }

    private String selectRecipient(MessageTaskDto task, String channelType) {
        if ("EMAIL".equals(channelType)) {
            return task.getEmail();
        }
        if ("KAKAO".equals(channelType)) {
            return task.getKakaoUserKey() != null ? task.getKakaoUserKey() : task.getPhoneNumber();
        }
        return task.getPhoneNumber();
    }

    private boolean isRealCustomer(MessageTaskDto task) {
        return !Boolean.FALSE.equals(task.getIsRealCustomer());
    }

    private String normalizeChannelType(String channelType) {
        if (channelType == null) {
            return "";
        }
        String normalized = channelType.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("KAKAO")) {
            return "KAKAO";
        }
        return normalized;
    }
}
