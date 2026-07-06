package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.model.MessageSendContext;
import com.example.smartmessaging.dto.model.MessageSendResult;
import com.example.smartmessaging.dto.model.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.dto.vo.SendAttemptVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.repository.SendQueueRepository;
import com.example.smartmessaging.service.sender.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final SendQueueRepository sendQueueRepository;
    private final MessageSender messageSender;
    private final RecipientChannelResolver recipientChannelResolver;

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_SEND_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    @Transactional
    public void consume(MessageQueueDto message) {
        try {
            sendMessage(message);
        } catch (Exception e) {
            Long targetId = message == null ? null : message.getSendTargetId();
            log.error("Message send failed by system exception. targetId={}", targetId, e);
            throw new AmqpRejectAndDontRequeueException("Message send failed by system exception", e);
        }
    }

    private void sendMessage(MessageQueueDto message) {
        SendTargetVO target = sendQueueRepository.findSendTargetById(message.getSendTargetId());
        if (target == null) {
            throw new IllegalStateException("Send target not found: " + message.getSendTargetId());
        }

        sendQueueRepository.updateSendTargetStatus(target.getId(), "SENDING", target.getFinalChannelId(), BigDecimal.ZERO, message.getUserId());

        List<SendRecipientCandidateVO> candidates = sendQueueRepository.findRecipientCandidatesByCustomerIds(List.of(message.getCustomerId()));
        Map<Long, SendRecipientCandidateVO> byChannelId = candidates.stream()
                .collect(Collectors.toMap(SendRecipientCandidateVO::getChannelId, Function.identity(), (left, right) -> left));

        int attemptOrder = 1;
        for (Long channelId : message.getChannelSequence()) {
            SendRecipientCandidateVO candidate = byChannelId.get(channelId);
            MessageSendResult result = tryChannel(message, candidate);
            recordAttempt(message, channelId, attemptOrder, result);

            if (result.isSucceeded()) {
                BigDecimal cost = candidate == null || candidate.getCostPerMsg() == null ? BigDecimal.ZERO : candidate.getCostPerMsg();
                sendQueueRepository.updateSendTargetStatus(target.getId(), "SUCCEEDED", channelId, cost, message.getUserId());
                refreshAndComplete(message.getSendHistoryId(), message.getUserId());
                return;
            }
            attemptOrder++;
        }

        sendQueueRepository.updateSendTargetStatus(target.getId(), "FAILED", target.getFinalChannelId(), BigDecimal.ZERO, message.getUserId());
        refreshAndComplete(message.getSendHistoryId(), message.getUserId());
    }

    private MessageSendResult tryChannel(MessageQueueDto message, SendRecipientCandidateVO candidate) {
        if (candidate == null) {
            return MessageSendResult.failure("CHANNEL_CANDIDATE_NOT_FOUND");
        }
        String recipientValue = recipientChannelResolver.recipientValue(candidate);
        return messageSender.send(MessageSendContext.builder()
                .sendHistoryId(message.getSendHistoryId())
                .sendTargetId(message.getSendTargetId())
                .customerId(message.getCustomerId())
                .channelId(candidate.getChannelId())
                .channelType(candidate.getChannelType())
                .recipientValue(recipientValue)
                .title(message.getTitle())
                .content(message.getContent())
                .linkUrl(message.getLinkUrl())
                .advertising(Boolean.TRUE.equals(message.getAdvertising()))
                .build());
    }

    private void recordAttempt(MessageQueueDto message, Long channelId, int attemptOrder, MessageSendResult result) {
        SendAttemptVO attempt = SendAttemptVO.builder()
                .sendTargetId(message.getSendTargetId())
                .attemptOrder(attemptOrder)
                .channelId(channelId)
                .isSucceeded(result.isSucceeded())
                .solapiMessageId(result.getProviderMessageId())
                .build();
        attempt.setCreatedBy(message.getUserId());
        attempt.setUpdatedBy(message.getUserId());
        sendQueueRepository.insertSendAttempt(attempt);
    }

    private void refreshAndComplete(Long sendHistoryId, Long userId) {
        sendQueueRepository.refreshHistoryCounters(sendHistoryId, userId);
        if (sendQueueRepository.countUnfinishedTargets(sendHistoryId) == 0) {
            sendQueueRepository.markHistoryTerminal(sendHistoryId, userId);
        }
    }
}
