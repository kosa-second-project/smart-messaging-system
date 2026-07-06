package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;
import com.example.smartmessaging.dto.type.ShortUrlPurpose;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.dto.vo.SendTargetVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.mapper.SendPreparationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendPreparationService {

    private static final int CHUNK_SIZE = 1000;

    private final CampaignDraftService draftService;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final RecipientChannelResolver recipientChannelResolver;
    private final MessageQueuePublisher messageQueuePublisher;
    private final ShortUrlService shortUrlService;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Transactional
    public SendPrepareResponseDTO prepare(Long userId, SendPrepareRequestDTO request) {
        validate(request);

        List<Long> customerIds = draftService.getDraftCustomerIds(userId, request.getDraftId());
        if (customerIds.isEmpty()) {
            throw new BusinessException(ErrorCode.DRAFT_NOT_FOUND);
        }

        List<ChannelVO> activeChannels = channelService.getActiveChannels();
        List<SendRecipientCandidateVO> recipients = findRecipientCandidates(customerIds);
        List<RecipientSendPlan> plans = recipientChannelResolver.resolve(
                recipients,
                activeChannels,
                request.getPriorities()
        );

        if (plans.isEmpty()) {
            throw new BusinessException("발송 가능한 수신자가 없습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }

        BigDecimal estimatedCost = plans.stream()
                .map(RecipientSendPlan::getEstimatedCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. 발송 예약 시각 결정 및 법적 규제 시간대(야간 21시~08시 광고 금지) 보정
        java.time.LocalDateTime scheduledTime = request.getScheduledAt();
        boolean isScheduled = (scheduledTime != null);
        
        if (scheduledTime == null) {
            scheduledTime = java.time.LocalDateTime.now();
        }

        // 목적이 광고성("AD" 또는 "ADVERTISEMENT")인 경우 야간 발송 제한 적용
        String normalizedPurpose = normalizePurpose(request.getPurpose());
        if ("AD".equalsIgnoreCase(normalizedPurpose) || "ADVERTISEMENT".equalsIgnoreCase(normalizedPurpose)) {
            int hour = scheduledTime.getHour();
            if (hour >= 21 || hour < 8) {
                isScheduled = true; // 야간 차단 시간대에 걸리면 강제 예약 상태로 보정
                if (hour >= 21) {
                    scheduledTime = scheduledTime.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
                } else {
                    scheduledTime = scheduledTime.withHour(8).withMinute(0).withSecond(0).withNano(0);
                }
            }
        }

        String historyStatus = isScheduled ? "SCHEDULED" : "SENDING";

        SendHistoryVO history = SendHistoryVO.builder()
                .templateId(request.getTemplateId())
                .userId(userId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .purpose(normalizedPurpose)
                .status(historyStatus)
                .totalTargetCount(plans.size())
                .successCount(0)
                .failCount(0)
                .estimatedCost(estimatedCost)
                .estimatedSaving(BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .scheduledAt(scheduledTime)
                .build();
        sendPreparationMapper.insertSendHistory(history);

        List<ChannelVO> routingChannels = orderRoutingChannels(activeChannels, request.getPriorities());
        for (int i = 0; i < routingChannels.size(); i++) {
            SendHistoryRoutingVO routing = SendHistoryRoutingVO.builder()
                    .sendHistoryId(history.getId())
                    .channelId(routingChannels.get(i).getId())
                    .priorityOrder(i + 1)
                    .build();
            sendPreparationMapper.insertSendHistoryRouting(routing);
        }

        // 4. 비동기 백그라운드 처리를 위한 초경량 캠페인 명령 생성 및 Enqueue (예약 건도 즉시 대상자 적재를 위해 무조건 발행)
        com.example.smartmessaging.dto.queue.CampaignCommandQueueDto command = com.example.smartmessaging.dto.queue.CampaignCommandQueueDto.builder()
                .sendHistoryId(history.getId())
                .draftId(request.getDraftId())
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(request.getPurpose())
                .templateId(request.getTemplateId())
                .routingChannelIds(routingChannels.stream().map(ChannelVO::getId).toList())
                .build();

        rabbitTemplate.convertAndSend(
                com.example.smartmessaging.config.RabbitMQConfig.CAMP_COMMAND_EXCHANGE,
                com.example.smartmessaging.config.RabbitMQConfig.CAMP_COMMAND_ROUTING_KEY,
                command
        );

        return SendPrepareResponseDTO.builder()
                .sendHistoryId(history.getId())
                .totalRequestedCount(customerIds.size())
                .preparedTargetCount(plans.size())
                .excludedTargetCount(customerIds.size() - plans.size())
                .publishedMessageCount(plans.size())
                .estimatedCost(estimatedCost)
                .build();
    }

    private void validate(SendPrepareRequestDTO request) {
        if (request == null
                || isBlank(request.getDraftId())
                || isBlank(request.getTitle())
                || isBlank(request.getContent())
                || isBlank(request.getPurpose())
                || request.getPriorities() == null
                || request.getPriorities().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (request.getScheduledAt() != null && request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<SendRecipientCandidateVO> findRecipientCandidates(List<Long> customerIds) {
        if (customerIds.size() <= CHUNK_SIZE) {
            return sendPreparationMapper.findRecipientCandidatesByCustomerIds(customerIds);
        }

        java.util.ArrayList<SendRecipientCandidateVO> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < customerIds.size(); i += CHUNK_SIZE) {
            List<Long> chunk = customerIds.subList(i, Math.min(customerIds.size(), i + CHUNK_SIZE));
            candidates.addAll(sendPreparationMapper.findRecipientCandidatesByCustomerIds(chunk));
        }
        return candidates;
    }

    private MessageTaskDto buildTask(SendHistoryVO history, SendTargetVO target, RecipientSendPlan plan, SendPrepareRequestDTO request) {
        String actionUrl = createActionUrl(target.getId(), request);
        String unsubscribeUrl = shouldCreateUnsubscribeUrl(history, plan)
                ? shortUrlService.createTrackedUrl(target.getId(), null, ShortUrlPurpose.UNSUBSCRIBE)
                : null;

        return MessageTaskDto.builder()
                .messageId("send-" + history.getId() + "-" + target.getId())
                .sendHistoryId(history.getId())
                .sendTargetId(target.getId())
                .templateId(history.getTemplateId())
                .campaignId(history.getId())
                .customerId(plan.getCustomerId())
                .phoneNumber(plan.getRecipient().getPhone())
                .email(plan.getRecipient().getEmail())
                .kakaoUserKey(plan.getRecipient().getKakaoUserKey())
                .title(history.getTitle())
                .content(history.getContent())
                .purpose(history.getPurpose())
                .actionButtonName(resolveLinkButtonName(request.getLinkButtonName()))
                .actionUrl(actionUrl)
                .unsubscribeUrl(unsubscribeUrl)
                .fallbackSequence(plan.getFallbackSequence())
                .currentStep(0)
                .build();
    }

    private List<ChannelVO> orderRoutingChannels(List<ChannelVO> activeChannels, List<String> requestedPriority) {
        Map<String, ChannelVO> uniqueChannels = new LinkedHashMap<>();
        for (ChannelVO channel : activeChannels) {
            uniqueChannels.putIfAbsent(normalizeChannelType(channel.getChannelType()), channel);
        }

        List<String> normalizedPriority = requestedPriority.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeChannelType)
                .distinct()
                .toList();

        return uniqueChannels.values().stream()
                .filter(channel -> normalizedPriority.contains(normalizeChannelType(channel.getChannelType())))
                .sorted(Comparator
                        .comparingInt((ChannelVO channel) -> normalizedPriority.indexOf(normalizeChannelType(channel.getChannelType())))
                        .thenComparing(channel -> channel.getCostPerMsg() != null ? channel.getCostPerMsg() : BigDecimal.ZERO))
                .toList();
    }

    private String normalizePurpose(String purpose) {
        String normalized = purpose.trim().toUpperCase(Locale.ROOT);
        if ("INFORMATIONAL".equals(normalized)) {
            return "INFO";
        }
        return normalized;
    }

    private String createActionUrl(Long sendTargetId, SendPrepareRequestDTO request) {
        if (isBlank(request.getLinkUrl())) {
            return null;
        }
        ShortUrlPurpose purpose = ShortUrlPurpose.from(request.getLinkPurpose());
        if (purpose == ShortUrlPurpose.UNSUBSCRIBE) {
            purpose = ShortUrlPurpose.CLICK;
        }
        return shortUrlService.createTrackedUrl(sendTargetId, request.getLinkUrl().trim(), purpose);
    }

    private boolean shouldCreateUnsubscribeUrl(SendHistoryVO history, RecipientSendPlan plan) {
        if (!"AD".equals(normalizePurpose(history.getPurpose()))) {
            return false;
        }
        return plan.getFallbackSequence() != null
                && plan.getFallbackSequence().stream().map(this::normalizeChannelType).anyMatch(channel -> "SMS".equals(channel) || "LMS".equals(channel));
    }

    private String resolveLinkButtonName(String linkButtonName) {
        if (isBlank(linkButtonName)) {
            return "자세히 보기";
        }
        return linkButtonName.trim();
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String createTrackingUserUuid() {
        return UUID.randomUUID().toString();
    }
}
