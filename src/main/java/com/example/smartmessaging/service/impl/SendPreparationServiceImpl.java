package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.queue.CampaignCommandQueueDto;
import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendHistoryRoutingVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.service.CampaignDraftService;
import com.example.smartmessaging.service.ChannelService;
import com.example.smartmessaging.service.SendPreparationService;
import com.example.smartmessaging.service.TokenCryptoService;
import com.example.smartmessaging.service.repository.SendPreparationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SendPreparationServiceImpl implements SendPreparationService {

    private final CampaignDraftService draftService;
    private final ChannelService channelService;
    private final SendPreparationMapper sendPreparationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TokenCryptoService tokenCryptoService;

    @Override
    @Transactional
    public SendPrepareResponseDTO prepare(Long userId, SendPrepareRequestDTO request, String kakaoAccessToken) {
        validate(request);

        List<Long> customerIds = draftService.getDraftCustomerIds(userId, request.getDraftId());
        if (customerIds.isEmpty()) {
            throw new BusinessException(ErrorCode.DRAFT_NOT_FOUND);
        }

        List<ChannelVO> activeChannels = channelService.getActiveChannels();
        List<ChannelVO> routingChannels = orderRoutingChannels(activeChannels, request.getPriorities());
        if (routingChannels.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        LocalDateTime scheduledTime = request.getScheduledAt();
        boolean isScheduled = scheduledTime != null;
        if (scheduledTime == null) {
            scheduledTime = LocalDateTime.now();
        }

        String normalizedPurpose = normalizePurpose(request.getPurpose());
        if ("AD".equalsIgnoreCase(normalizedPurpose) || "ADVERTISEMENT".equalsIgnoreCase(normalizedPurpose)) {
            int hour = scheduledTime.getHour();
            if (hour >= 21 || hour < 8) {
                isScheduled = true;
                if (hour >= 21) {
                    scheduledTime = scheduledTime.plusDays(1).withHour(8).withMinute(0).withSecond(0).withNano(0);
                } else {
                    scheduledTime = scheduledTime.withHour(8).withMinute(0).withSecond(0).withNano(0);
                }
            }
        }

        SendHistoryVO history = SendHistoryVO.builder()
                .templateId(request.getTemplateId())
                .userId(userId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .purpose(normalizedPurpose)
                .status(isScheduled ? "SCHEDULED" : "SENDING")
                .totalTargetCount(customerIds.size())
                .successCount(0)
                .failCount(0)
                .estimatedCost(BigDecimal.ZERO)
                .estimatedSaving(BigDecimal.ZERO)
                .actualCost(BigDecimal.ZERO)
                .kakaoAccessTokenEnc(tokenCryptoService.encrypt(kakaoAccessToken))
                .scheduledAt(scheduledTime)
                .build();
        sendPreparationMapper.insertSendHistory(history);

        for (int i = 0; i < routingChannels.size(); i++) {
            SendHistoryRoutingVO routing = SendHistoryRoutingVO.builder()
                    .sendHistoryId(history.getId())
                    .channelId(routingChannels.get(i).getId())
                    .priorityOrder(i + 1)
                    .build();
            sendPreparationMapper.insertSendHistoryRouting(routing);
        }

        CampaignCommandQueueDto command = CampaignCommandQueueDto.builder()
                .sendHistoryId(history.getId())
                .draftId(request.getDraftId())
                .userId(userId)
                .title(request.getTitle())
                .content(request.getContent())
                .purpose(normalizedPurpose)
                .linkButtonName(normalizeNullable(request.getLinkButtonName()))
                .linkUrl(normalizeNullable(request.getLinkUrl()))
                .linkPurpose(normalizeLinkPurpose(request.getLinkPurpose()))
                .templateId(request.getTemplateId())
                .routingChannelIds(routingChannels.stream().map(ChannelVO::getId).toList())
                .kakaoAccessToken(kakaoAccessToken)
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CAMP_COMMAND_EXCHANGE,
                RabbitMQConfig.CAMP_COMMAND_ROUTING_KEY,
                command
        );

        return SendPrepareResponseDTO.builder()
                .sendHistoryId(history.getId())
                .totalRequestedCount(customerIds.size())
                .preparedTargetCount(0)
                .excludedTargetCount(0)
                .publishedMessageCount(1)
                .estimatedCost(BigDecimal.ZERO)
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
        if (!isBlank(request.getLinkUrl())) {
            validateHttpUrl(request.getLinkUrl());
        }
    }

    private void validateHttpUrl(String url) {
        String scheme;
        try {
            scheme = UriComponentsBuilder.fromUriString(url.trim()).build().getScheme();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
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

    private String normalizeLinkPurpose(String linkPurpose) {
        if (isBlank(linkPurpose)) {
            return "CLICK";
        }
        String normalized = linkPurpose.trim().toUpperCase(Locale.ROOT);
        return "PURCHASE".equals(normalized) ? "PURCHASE" : "CLICK";
    }

    private String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
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
}
