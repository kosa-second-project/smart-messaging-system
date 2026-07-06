package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.model.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.repository.SendQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignCommandConsumer {

    private static final int ORACLE_IN_CHUNK_SIZE = 900;

    private final CampaignDraftService draftService;
    private final SendQueueRepository sendQueueRepository;
    private final RecipientChannelResolver recipientChannelResolver;
    private final MessageQueuePublisher messageQueuePublisher;
    private final QueueFailureHandler queueFailureHandler;
    private final TransactionTemplate transactionTemplate;

    @RabbitListener(queues = RabbitMQConfig.CAMPAIGN_COMMAND_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    public void consume(CampaignCommandQueueDto command) {
        try {
            transactionTemplate.executeWithoutResult(status -> prepareCampaign(command));
        } catch (Exception e) {
            Long historyId = command == null ? null : command.getSendHistoryId();
            Long userId = command == null ? null : command.getUserId();
            queueFailureHandler.markCampaignFailed(historyId, userId, "CAMPAIGN_COMMAND_CONSUME_FAILED");
            log.error("Campaign command failed. historyId={}", historyId, e);
            throw new AmqpRejectAndDontRequeueException("Campaign command failed", e);
        }
    }

    private void prepareCampaign(CampaignCommandQueueDto command) {
        sendQueueRepository.updateSendHistoryStatus(command.getSendHistoryId(), "PREPARING", command.getUserId());

        List<Long> customerIds = draftService.getAllIds(command.getUserId(), command.getDraftId());
        List<Long> priorityChannelIds = sendQueueRepository.findRoutingByHistoryId(command.getSendHistoryId()).stream()
                .map(SendHistoryRoutingVO::getChannelId)
                .toList();
        List<SendRecipientCandidateVO> candidates = findCandidates(customerIds);
        List<RecipientSendPlan> plans = recipientChannelResolver.resolve(
                customerIds,
                candidates,
                priorityChannelIds,
                command.getContent());

        boolean delayed = command.getScheduledAt() != null && command.getScheduledAt().isAfter(LocalDateTime.now().plusSeconds(1));
        String preparedStatus = delayed ? "SCHEDULED" : "SENDING";

        for (RecipientSendPlan plan : plans) {
            SendTargetVO target = SendTargetVO.builder()
                    .sendHistoryId(command.getSendHistoryId())
                    .customerId(plan.getCustomerId())
                    .finalChannelId(plan.getFirstChannelId())
                    .status(plan.isSendable() ? "PENDING" : "SKIPPED")
                    .cost(BigDecimal.ZERO)
                    .build();
            target.setCreatedBy(command.getUserId());
            target.setUpdatedBy(command.getUserId());
            sendQueueRepository.insertSendTarget(target);

            if (plan.isSendable() && !delayed) {
                MessageQueueDto message = MessageQueueDto.builder()
                        .sendHistoryId(command.getSendHistoryId())
                        .sendTargetId(target.getId())
                        .customerId(plan.getCustomerId())
                        .userId(command.getUserId())
                        .title(command.getTitle())
                        .content(command.getContent())
                        .linkUrl(command.getLinkUrl())
                        .advertising(Boolean.TRUE.equals(command.getAdvertising()))
                        .channelSequence(plan.getChannelSequence())
                        .build();
                messageQueuePublisher.publishMessageSendAfterCommit(
                        message,
                        e -> queueFailureHandler.markMessageFailed(message, "MESSAGE_SEND_PUBLISH_FAILED"));
            }
        }

        sendQueueRepository.refreshHistoryCounters(command.getSendHistoryId(), command.getUserId());
        sendQueueRepository.updateSendHistoryStatus(command.getSendHistoryId(), preparedStatus, command.getUserId());
        completeIfTerminal(command.getSendHistoryId(), command.getUserId());
    }

    private List<SendRecipientCandidateVO> findCandidates(List<Long> customerIds) {
        if (customerIds == null || customerIds.isEmpty()) {
            return List.of();
        }
        List<SendRecipientCandidateVO> result = new ArrayList<>();
        for (int start = 0; start < customerIds.size(); start += ORACLE_IN_CHUNK_SIZE) {
            int end = Math.min(start + ORACLE_IN_CHUNK_SIZE, customerIds.size());
            result.addAll(sendQueueRepository.findRecipientCandidatesByCustomerIds(customerIds.subList(start, end)));
        }
        return result;
    }

    private void completeIfTerminal(Long sendHistoryId, Long userId) {
        if (sendQueueRepository.countUnfinishedTargets(sendHistoryId) == 0) {
            sendQueueRepository.markHistoryTerminal(sendHistoryId, userId);
        }
    }
}
