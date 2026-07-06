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
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignCommandConsumer {

    private static final int CHUNK_SIZE = 1000;

    private final CampaignDraftService draftService;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final RecipientChannelResolver recipientChannelResolver;
    private final ShortUrlService shortUrlService;
    private final MessageQueuePublisher messageQueuePublisher;

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

            List<Long> customerIds = draftService.getDraftCustomerIds(command.getUserId(), command.getDraftId());
            if (customerIds == null || customerIds.isEmpty()) {
                throw new IllegalStateException("Draft recipient list is empty or expired.");
            }

            List<ChannelVO> activeChannels = channelService.getActiveChannels();
            List<SendRecipientCandidateVO> recipients = findRecipientCandidates(customerIds);
            List<String> priorities = restorePriorities(command.getRoutingChannelIds(), activeChannels);
            List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);

            int targetCount = plans == null ? 0 : plans.size();
            sendPreparationMapper.updateHistoryTotalTargetCount(command.getSendHistoryId(), targetCount);

            if (targetCount == 0) {
                sendPreparationMapper.updateHistoryStatus(command.getSendHistoryId(), "SENT");
                draftService.deleteDraft(command.getUserId(), command.getDraftId());
                rabbitChannel.basicAck(deliveryTag, false);
                log.info("[CampaignCommandConsumer] no sendable targets sendHistoryId={}", command.getSendHistoryId());
                return;
            }

            int published = 0;
            for (RecipientSendPlan plan : plans) {
                SendTargetVO target = SendTargetVO.builder()
                        .sendHistoryId(command.getSendHistoryId())
                        .customerId(plan.getCustomerId())
                        .finalChannelId(plan.getFirstChannelId())
                        .status("PENDING")
                        .cost(plan.getEstimatedCost())
                        .userUuid(plan.getRecipient().getKakaoUserKey())
                        .build();
                target.setCreatedBy(command.getUserId());
                sendPreparationMapper.insertSendTarget(target);

                if (!isScheduled) {
                    messageQueuePublisher.publish(buildTask(command, target, plan));
                    published++;
                }
            }

            if (!isScheduled) {
                sendPreparationMapper.updateHistoryStatus(command.getSendHistoryId(), "SENDING");
            }
            draftService.deleteDraft(command.getUserId(), command.getDraftId());

            log.info("[CampaignCommandConsumer] done sendHistoryId={}, targets={}, published={}",
                    command.getSendHistoryId(), targetCount, published);
            rabbitChannel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[CampaignCommandConsumer] failed sendHistoryId={}, error={}", command.getSendHistoryId(), e.getMessage(), e);
            sendPreparationMapper.updateHistoryStatus(command.getSendHistoryId(), "FAILED");
            rabbitChannel.basicNack(deliveryTag, false, false);
        }
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

    private MessageTaskDto buildTask(CampaignCommandQueueDto command, SendTargetVO target, RecipientSendPlan plan) {
        String actionUrl = null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(command.getContent());
        if (matcher.find()) {
            actionUrl = shortUrlService.createTrackedUrl(target.getId(), matcher.group().trim(), ShortUrlPurpose.CLICK);
        }

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
                .userId(command.getUserId())
                .phoneNumber(plan.getRecipient().getPhone())
                .email(plan.getRecipient().getEmail())
                .kakaoUserKey(plan.getRecipient().getKakaoUserKey())
                .title(command.getTitle())
                .content(command.getContent())
                .purpose(command.getPurpose())
                .actionButtonName("Detail")
                .actionUrl(actionUrl)
                .unsubscribeUrl(unsubscribeUrl)
                .fallbackSequence(plan.getFallbackSequence())
                .currentStep(0)
                .build();
    }

    private boolean shouldCreateUnsubscribeUrl(CampaignCommandQueueDto command, RecipientSendPlan plan) {
        if (command.getPurpose() == null || !"AD".equals(command.getPurpose().trim().toUpperCase(Locale.ROOT))) {
            return false;
        }
        return plan.getFallbackSequence() != null
                && plan.getFallbackSequence().stream()
                .map(this::normalizeChannelType)
                .anyMatch(channel -> "SMS".equals(channel) || "LMS".equals(channel));
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
