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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledMessageQueuePublisher {

    private final SendQueueRepository sendQueueRepository;
    private final RecipientChannelResolver recipientChannelResolver;
    private final MessageQueuePublisher messageQueuePublisher;

    @Scheduled(fixedDelayString = "${messaging.queue.scheduled-scan-delay-ms:60000}")
    @Transactional
    public void enqueueDueScheduledTargets() {
        List<SendTargetVO> dueTargets = sendQueueRepository.findDueScheduledTargets();
        for (SendTargetVO target : dueTargets) {
            enqueueTarget(target);
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
            sendQueueRepository.refreshHistoryCounters(history.getId(), history.getUserId());
            return;
        }

        sendQueueRepository.updateSendTargetStatus(target.getId(), "SENDING", plan.getFirstChannelId(), BigDecimal.ZERO, history.getUserId());
        sendQueueRepository.updateSendHistoryStatus(history.getId(), "SENDING", history.getUserId());
        messageQueuePublisher.publishMessageSendAfterCommit(MessageQueueDto.builder()
                .sendHistoryId(history.getId())
                .sendTargetId(target.getId())
                .customerId(target.getCustomerId())
                .userId(history.getUserId())
                .title(history.getTitle())
                .content(history.getContent())
                .advertising("AD".equalsIgnoreCase(history.getPurpose()))
                .channelSequence(plan.getChannelSequence())
                .build());
    }
}
