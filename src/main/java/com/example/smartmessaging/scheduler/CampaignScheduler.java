package com.example.smartmessaging.scheduler;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.RecipientChannelResolver;
import com.example.smartmessaging.service.ShortUrlService;
import com.example.smartmessaging.service.queue.MessageQueuePublisher;
import com.example.smartmessaging.service.repository.HistoryMapper;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignScheduler {

    private static final int CHUNK_SIZE = 1000;

    private final SendPreparationMapper sendPreparationMapper;
    private final HistoryMapper historyMapper;
    private final ChannelService channelService;
    private final RecipientChannelResolver recipientChannelResolver;
    private final ShortUrlService shortUrlService;
    private final MessageQueuePublisher messageQueuePublisher;

    @Scheduled(cron = "*/30 * * * * *")
    @Transactional
    public void processReservedCampaigns() {
        List<SendHistoryVO> campaigns = sendPreparationMapper.findPendingReservedCampaigns(LocalDateTime.now());
        if (campaigns.isEmpty()) {
            return;
        }

        for (SendHistoryVO campaign : campaigns) {
            try {
                sendPreparationMapper.updateHistoryStatus(campaign.getId(), "SENDING");

                List<SendTargetVO> targets = sendPreparationMapper.selectPendingTargetsByHistoryId(campaign.getId());
                if (targets.isEmpty()) {
                    sendPreparationMapper.updateHistoryStatus(campaign.getId(), "SENT");
                    continue;
                }

                List<ChannelVO> activeChannels = channelService.getActiveChannels();
                List<Long> routingChannelIds = sendPreparationMapper.findRoutingChannelIdsByHistoryId(campaign.getId());
                List<String> priorities = restorePriorities(routingChannelIds, activeChannels);
                List<Long> customerIds = targets.stream().map(SendTargetVO::getCustomerId).toList();
                List<SendRecipientCandidateVO> recipients = findRecipientCandidates(customerIds);
                List<RecipientSendPlan> plans = recipientChannelResolver.resolve(recipients, activeChannels, priorities);
                if (plans == null) {
                    plans = List.of();
                }

                Map<Long, SendTargetVO> targetByCustomerId = targets.stream()
                        .collect(Collectors.toMap(SendTargetVO::getCustomerId, Function.identity(), (left, right) -> left));
                Map<Long, RecipientSendPlan> planByCustomerId = plans.stream()
                        .collect(Collectors.toMap(RecipientSendPlan::getCustomerId, Function.identity(), (left, right) -> left));

                int published = 0;
                for (SendTargetVO target : targets) {
                    RecipientSendPlan plan = planByCustomerId.get(target.getCustomerId());
                    if (plan == null) {
                        historyMapper.updateSendTargetStatus(target.getId(), "SKIPPED");
                        continue;
                    }
                    SendTargetVO persistedTarget = targetByCustomerId.get(plan.getCustomerId());
                    messageQueuePublisher.publish(buildTask(campaign, persistedTarget, plan));
                    published++;
                }

                if (published == 0) {
                    sendPreparationMapper.updateHistoryStatus(campaign.getId(), "SENT");
                }
                log.info("[CampaignScheduler] reserved campaign published sendHistoryId={}, published={}", campaign.getId(), published);
            } catch (Exception e) {
                log.error("[CampaignScheduler] reserved campaign failed sendHistoryId={}", campaign.getId(), e);
                sendPreparationMapper.updateHistoryStatus(campaign.getId(), "SCHEDULED");
            }
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

    private MessageTaskDto buildTask(SendHistoryVO campaign, SendTargetVO target, RecipientSendPlan plan) {
        String actionUrl = null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(campaign.getContent());
        if (matcher.find()) {
            actionUrl = shortUrlService.createTrackedUrl(target.getId(), matcher.group().trim(), ShortUrlPurpose.CLICK);
        }

        String unsubscribeUrl = shouldCreateUnsubscribeUrl(campaign, plan)
                ? shortUrlService.createTrackedUrl(target.getId(), null, ShortUrlPurpose.UNSUBSCRIBE)
                : null;

        return MessageTaskDto.builder()
                .messageId("send-" + campaign.getId() + "-" + target.getId())
                .sendHistoryId(campaign.getId())
                .sendTargetId(target.getId())
                .templateId(campaign.getTemplateId())
                .campaignId(campaign.getId())
                .customerId(plan.getCustomerId())
                .isRealCustomer(plan.getRecipient().getIsRealCustomer())
                .userId(campaign.getUserId())
                .phoneNumber(plan.getRecipient().getPhone())
                .email(plan.getRecipient().getEmail())
                .kakaoUserKey(plan.getRecipient().getKakaoUserKey())
                .title(campaign.getTitle())
                .content(campaign.getContent())
                .purpose(campaign.getPurpose())
                .actionButtonName("Detail")
                .actionUrl(actionUrl)
                .unsubscribeUrl(unsubscribeUrl)
                .fallbackSequence(plan.getFallbackSequence())
                .currentStep(0)
                .build();
    }

    private boolean shouldCreateUnsubscribeUrl(SendHistoryVO campaign, RecipientSendPlan plan) {
        if (campaign.getPurpose() == null || !"AD".equals(campaign.getPurpose().trim().toUpperCase(Locale.ROOT))) {
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
