package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.service.repository.HistoryMapper;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignCommandConsumer {

    private static final int CHUNK_SIZE = 1000;
    private static final int TARGET_INSERT_CHUNK_SIZE = 500;

    private final CampaignDraftService draftService;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final HistoryMapper historyMapper;
    private final RecipientChannelResolver recipientChannelResolver;
    private final ShortUrlService shortUrlService;
    private final MessageQueuePublisher messageQueuePublisher;
    private final SqlSessionFactory sqlSessionFactory;
    private final PlatformTransactionManager transactionManager;

    @RabbitListener(queues = RabbitMQConfig.CAMP_COMMAND_QUEUE)
    @Transactional
    public void consumeCampaignCommand(CampaignCommandQueueDto command, Channel rabbitChannel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("[CampaignCommandConsumer] start sendHistoryId={}, draftId={}", command.getSendHistoryId(), command.getDraftId());

        try {
            SendHistoryVO campaign = sendPreparationMapper.selectSendHistoryById(command.getSendHistoryId());
            boolean isScheduled = campaign != null && "SCHEDULED".equals(campaign.getStatus());
            if (!isScheduled) {
                sendPreparationMapper.updateHistoryStatus(command.getSendHistoryId(), "SENDING");
            }

            long totalCount = draftService.getTotalCount(command.getUserId(), command.getDraftId());
            if (totalCount == 0) {
                throw new IllegalStateException("Draft recipient list is empty or expired.");
            }

            List<ChannelVO> activeChannels = channelService.getActiveChannels();
            List<String> priorities = restorePriorities(command.getRoutingChannelIds(), activeChannels);

            // 임시 저장 대상자 수로 우선 총 대상자 수 설정
            sendPreparationMapper.updateHistoryTotalTargetCount(command.getSendHistoryId(), (int) totalCount);

            int pageSize = 1000;
            int totalPages = (int) Math.ceil((double) totalCount / pageSize);
            int published = 0;
            int actualTargetCount = 0;

            for (int page = 1; page <= totalPages; page++) {
                List<Long> chunkIds = draftService.getPagedIds(command.getUserId(), command.getDraftId(), page, pageSize);
                if (chunkIds.isEmpty()) {
                    continue;
                }
                List<SendRecipientCandidateVO> recipients = findRecipientCandidates(chunkIds);
                List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);
                if (plans != null && !plans.isEmpty()) {
                    actualTargetCount += plans.size();
                    published += persistTargetsAndPublish(command, plans, isScheduled);
                }
            }

            // 실제 필터링 등을 거쳐 결정된 최종 발송 대상자 수로 최종 업데이트
            sendPreparationMapper.updateHistoryTotalTargetCount(command.getSendHistoryId(), actualTargetCount);

            if (actualTargetCount == 0) {
                historyMapper.finalizeSendHistory(command.getSendHistoryId(), "SENT");
                draftService.deleteDraft(command.getUserId(), command.getDraftId());
                rabbitChannel.basicAck(deliveryTag, false);
                log.info("[CampaignCommandConsumer] no sendable targets sendHistoryId={}", command.getSendHistoryId());
                return;
            }

            draftService.deleteDraft(command.getUserId(), command.getDraftId());

            log.info("[CampaignCommandConsumer] done sendHistoryId={}, targets={}, published={}",
                    command.getSendHistoryId(), actualTargetCount, published);
            rabbitChannel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[CampaignCommandConsumer] failed sendHistoryId={}, error={}", command.getSendHistoryId(), e.getMessage(), e);
            sendPreparationMapper.updateHistoryStatus(command.getSendHistoryId(), "FAILED");
            rabbitChannel.basicNack(deliveryTag, false, false);
        }
    }

    private int persistTargetsAndPublish(CampaignCommandQueueDto command, List<RecipientSendPlan> plans, boolean isScheduled) {
        int published = 0;
        for (int i = 0; i < plans.size(); i += TARGET_INSERT_CHUNK_SIZE) {
            List<RecipientSendPlan> planChunk = plans.subList(i, Math.min(plans.size(), i + TARGET_INSERT_CHUNK_SIZE));
            List<SendTargetVO> targetChunk = new ArrayList<>(planChunk.size());
            List<String> userUuids = new ArrayList<>(planChunk.size());

            for (RecipientSendPlan plan : planChunk) {
                SendTargetVO target = buildTarget(command, plan);
                targetChunk.add(target);
                userUuids.add(target.getUserUuid());
            }

            insertSendTargetsInBatch(targetChunk);
            Map<String, SendTargetVO> persistedTargets = findPersistedTargetsByUserUuid(userUuids);

            for (int j = 0; j < planChunk.size(); j++) {
                RecipientSendPlan plan = planChunk.get(j);
                SendTargetVO target = persistedTargets.get(targetChunk.get(j).getUserUuid());
                if (target == null || target.getId() == null) {
                    throw new IllegalStateException("Inserted send_target not found. userUuid=" + targetChunk.get(j).getUserUuid());
                }

                String actionUrl = createActionUrl(command, target, plan);
                if (!isScheduled) {
                    messageQueuePublisher.publish(buildTask(command, target, plan, actionUrl));
                    published++;
                }
            }
        }
        return published;
    }

    private void insertSendTargetsInBatch(List<SendTargetVO> targets) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> {
            SqlSessionTemplate batchSession = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
            SendPreparationMapper batchMapper = batchSession.getMapper(SendPreparationMapper.class);
            for (SendTargetVO target : targets) {
                batchMapper.insertSendTarget(target);
            }
            batchSession.flushStatements();
        });
    }
    private SendTargetVO buildTarget(CampaignCommandQueueDto command, RecipientSendPlan plan) {
        SendTargetVO target = SendTargetVO.builder()
                .sendHistoryId(command.getSendHistoryId())
                .customerId(plan.getCustomerId())
                .finalChannelId(plan.getFirstChannelId())
                .status("PENDING")
                .cost(plan.getMaxAvailableCost())
                .userUuid(createTrackingUserUuid())
                .build();
        target.setCreatedBy(command.getUserId());
        target.setUpdatedBy(command.getUserId());
        return target;
    }

    private Map<String, SendTargetVO> findPersistedTargetsByUserUuid(List<String> userUuids) {
        Map<String, SendTargetVO> result = new HashMap<>();
        for (SendTargetVO target : sendPreparationMapper.findSendTargetsByUserUuids(userUuids)) {
            result.put(target.getUserUuid(), target);
        }
        return result;
    }

    private List<SendRecipientCandidateVO> findRecipientCandidates(List<Long> customerIds) {
        if (customerIds.size() <= CHUNK_SIZE) {
            return sendPreparationMapper.findRecipientCandidatesByCustomerIds(customerIds);
        }

        List<SendRecipientCandidateVO> candidates = new ArrayList<>();
        for (int i = 0; i < customerIds.size(); i += CHUNK_SIZE) {
            List<Long> chunk = customerIds.subList(i, Math.min(customerIds.size(), i + CHUNK_SIZE));
            candidates.addAll(sendPreparationMapper.findRecipientCandidatesByCustomerIds(chunk));
        }
        return candidates;
    }

    private List<String> restorePriorities(List<Long> routingChannelIds, List<ChannelVO> activeChannels) {
        if (routingChannelIds == null) {
            return List.of();
        }
        return routingChannelIds.stream()
                .map(id -> activeChannels.stream().filter(channel -> channel.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(ChannelVO::getChannelType)
                .toList();
    }

    private MessageTaskDto buildTask(CampaignCommandQueueDto command, SendTargetVO target, RecipientSendPlan plan, String actionUrl) {

        String unsubscribeUrl = shouldCreateUnsubscribeUrl(command, plan)
                ? shortUrlService.createTrackedUrl(target.getId(), null, ShortUrlPurpose.UNSUBSCRIBE)
                : null;

        return MessageTaskDto.builder()
                .messageId("send-" + command.getSendHistoryId() + "-" + target.getId())
                .sendHistoryId(command.getSendHistoryId())
                .sendTargetId(target.getId())
                .templateId(command.getTemplateId())
                .campaignId(command.getSendHistoryId())
                .customerId(plan.getCustomerId())
                .isRealCustomer(plan.getRecipient().getIsRealCustomer())
                .customerName(plan.getRecipient().getCustomerName())
                .userId(command.getUserId())
                .phoneNumber(plan.getRecipient().getPhone())
                .email(plan.getRecipient().getEmail())
                .kakaoUserKey(plan.getRecipient().getKakaoUserKey())
                .kakaoAccessToken(command.getKakaoAccessToken())
                .title(command.getTitle())
                .content(command.getContent())
                .purpose(command.getPurpose())
                .actionButtonName(actionUrl == null ? null : command.getLinkButtonName())
                .actionUrl(actionUrl)
                .unsubscribeUrl(unsubscribeUrl)
                .fallbackSequence(plan.getFallbackSequence())
                .currentStep(0)
                .build();
    }

    private String createActionUrl(CampaignCommandQueueDto command, SendTargetVO target, RecipientSendPlan plan) {
        if (command.getLinkUrl() == null || command.getLinkUrl().isBlank() || !hasSendableChannel(plan)) {
            return null;
        }
        return shortUrlService.createTrackedUrl(
                target.getId(),
                command.getLinkUrl(),
                ShortUrlPurpose.from(command.getLinkPurpose())
        );
    }

    private boolean hasSendableChannel(RecipientSendPlan plan) {
        return plan.getFallbackSequence() != null && !plan.getFallbackSequence().isEmpty();
    }

    private boolean hasSmsOrLms(RecipientSendPlan plan) {
        if (plan.getFallbackSequence() == null || plan.getFallbackSequence().isEmpty()) {
            return false;
        }
        String firstChannel = normalizeChannelType(plan.getFallbackSequence().get(0));
        return "SMS".equals(firstChannel) || "LMS".equals(firstChannel);
    }

    private boolean shouldCreateUnsubscribeUrl(CampaignCommandQueueDto command, RecipientSendPlan plan) {
        if (command.getPurpose() == null || !"AD".equals(command.getPurpose().trim().toUpperCase(Locale.ROOT))) {
            return false;
        }
        return hasSmsOrLms(plan);
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

    private String createTrackingUserUuid() {
        return UUID.randomUUID().toString();
    }
}
