package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.model.RecipientSendPlan;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import com.example.smartmessaging.service.RecipientChannelResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 수신 대상 채널 결정 및 단가 계산 서비스 구현체
 */
@Component
public class RecipientChannelResolverImpl implements RecipientChannelResolver {

    @Override
    public List<RecipientSendPlan> resolve(
            List<SendRecipientCandidateVO> recipients,
            List<ChannelVO> activeChannels,
            List<String> requestedPriority
    ) {
        if (recipients == null || recipients.isEmpty() || activeChannels == null || activeChannels.isEmpty()) {
            return List.of();
        }

        List<ChannelVO> orderedChannels = orderChannels(activeChannels, requestedPriority);
        List<RecipientSendPlan> plans = new ArrayList<>();

        for (SendRecipientCandidateVO recipient : recipients) {
            List<ChannelVO> availableChannels = orderedChannels.stream()
                    .filter(channel -> isRecipientAvailable(recipient, normalizeChannelType(channel.getChannelType())))
                    .toList();

            if (availableChannels.isEmpty()) {
                continue;
            }

            ChannelVO firstChannel = availableChannels.get(0);
            BigDecimal maxAvailableCost = availableChannels.stream()
                    .map(channel -> channel.getCostPerMsg() != null ? channel.getCostPerMsg() : BigDecimal.ZERO)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            plans.add(RecipientSendPlan.builder()
                    .customerId(recipient.getCustomerId())
                    .firstChannelId(firstChannel.getId())
                    .estimatedCost(firstChannel.getCostPerMsg() != null ? firstChannel.getCostPerMsg() : BigDecimal.ZERO)
                    .maxAvailableCost(maxAvailableCost)
                    .fallbackSequence(availableChannels.stream()
                            .map(channel -> normalizeChannelType(channel.getChannelType()))
                            .toList())
                    .recipient(recipient)
                    .build());
        }

        return plans;
    }

    private List<ChannelVO> orderChannels(List<ChannelVO> activeChannels, List<String> requestedPriority) {
        Map<String, ChannelVO> uniqueChannels = new LinkedHashMap<>();
        for (ChannelVO channel : activeChannels) {
            uniqueChannels.putIfAbsent(normalizeChannelType(channel.getChannelType()), channel);
        }

        List<String> normalizedPriority = requestedPriority == null ? List.of() : requestedPriority.stream()
                .filter(Objects::nonNull)
                .map(this::normalizeChannelType)
                .distinct()
                .toList();

        return uniqueChannels.values().stream()
                .sorted(Comparator
                        .comparingInt((ChannelVO channel) -> {
                            int index = normalizedPriority.indexOf(normalizeChannelType(channel.getChannelType()));
                            return index >= 0 ? index : Integer.MAX_VALUE;
                        })
                        .thenComparing(channel -> channel.getCostPerMsg() != null ? channel.getCostPerMsg() : BigDecimal.ZERO))
                .toList();
    }

    private boolean isRecipientAvailable(SendRecipientCandidateVO recipient, String channelType) {
        return switch (channelType) {
            case "KAKAO" -> isConsented(recipient.getKakaoConsent()) && hasText(recipient.getPhone());
            case "EMAIL" -> isConsented(recipient.getEmailConsent()) && hasText(recipient.getEmail());
            case "SMS", "LMS" -> isConsented(recipient.getSmsConsent()) && hasText(recipient.getPhone());
            default -> false;
        };
    }

    private boolean isConsented(Integer consent) {
        return consent != null && consent == 1;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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
