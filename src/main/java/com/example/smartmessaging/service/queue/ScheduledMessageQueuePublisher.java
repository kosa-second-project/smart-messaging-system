package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.model.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.repository.SendQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledMessageQueuePublisher {

    private final SendQueueRepository sendQueueRepository;
    private final RecipientChannelResolver recipientChannelResolver;
    private final MessageQueuePublisher messageQueuePublisher;
    private final QueueFailureHandler queueFailureHandler;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "${messaging.queue.scheduled-scan-delay-ms:60000}")
    public void enqueueDueScheduledTargets() {
        List<SendTargetVO> dueTargets = sendQueueRepository.findDueScheduledTargets();
        for (SendTargetVO target : dueTargets) {
            try {
                transactionTemplate.executeWithoutResult(status -> enqueueTarget(target));
            } catch (Exception e) {
                queueFailureHandler.markTargetFailed(
                        target.getId(),
                        target.getSendHistoryId(),
                        null,
                        "SCHEDULED_TARGET_ENQUEUE_FAILED");
                log.error("Failed to enqueue scheduled target. targetId={}", target.getId(), e);
            }
        }
    }

    private void enqueueTarget(SendTargetVO target) {
        SendHistoryVO history = sendQueueRepository.findSendHistoryById(target.getSendHistoryId());
        if (history == null) {
            return;
        }

        List<Long> priorityChannelIds = sendQueueRepository.findRoutingByHistoryId(history.getId()).stream()
                .map(SendHistoryRoutingVO::getChannelId)
                .toList();
        List<SendRecipientCandidateVO> candidates = sendQueueRepository.findRecipientCandidatesByCustomerIds(List.of(target.getCustomerId()));
        RecipientSendPlan plan = recipientChannelResolver.resolve(
                List.of(target.getCustomerId()),
                candidates,
                priorityChannelIds,
                history.getContent()).get(0);

        if (!plan.isSendable()) {
            sendQueueRepository.updateSendTargetStatus(target.getId(), "SKIPPED", null, BigDecimal.ZERO, history.getUserId());
            refreshAndComplete(history.getId(), history.getUserId());
            return;
        }

        sendQueueRepository.updateSendTargetStatus(target.getId(), "SENDING", plan.getFirstChannelId(), BigDecimal.ZERO, history.getUserId());
        sendQueueRepository.updateSendHistoryStatus(history.getId(), "SENDING", history.getUserId());
        MessageQueueDto message = MessageQueueDto.builder()
                .sendHistoryId(history.getId())
                .sendTargetId(target.getId())
                .customerId(target.getCustomerId())
                .userId(history.getUserId())
                .title(history.getTitle())
                .content(history.getContent())
                .linkUrl(history.getLinkUrl())
                .advertising("AD".equalsIgnoreCase(history.getPurpose()))
                .channelSequence(plan.getChannelSequence())
                .build();
        messageQueuePublisher.publishMessageSendAfterCommit(
                message,
                e -> queueFailureHandler.markMessageFailed(message, "SCHEDULED_MESSAGE_PUBLISH_FAILED"));
    }

    private void refreshAndComplete(Long sendHistoryId, Long userId) {
        sendQueueRepository.refreshHistoryCounters(sendHistoryId, userId);
        if (sendQueueRepository.countUnfinishedTargets(sendHistoryId) == 0) {
            sendQueueRepository.markHistoryTerminal(sendHistoryId, userId);
        }
    }
}
