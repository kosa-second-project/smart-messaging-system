package com.example.smartmessaging.dto.response;

import com.example.smartmessaging.dto.type.SendHistoryStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record HistoryListResponseDTO(
        Long id,
        LocalDateTime scheduledAt,
        String title,
        String purpose,
        Integer totalTargetCount,
        Integer successCount,
        Integer failCount,
        BigDecimal successRate,
        BigDecimal estimatedSaving,
        String status,
        List<String> channels,
        List<String> tags,
        List<String> displayTags,
        int hiddenTagCount
) {
    public HistoryListResponseDTO(
            Long id,
            LocalDateTime scheduledAt,
            String title,
            String purpose,
            Integer totalTargetCount,
            Integer successCount,
            Integer failCount,
            BigDecimal successRate,
            BigDecimal estimatedSaving,
            String status
    ) {
        this(
                id,
                scheduledAt,
                title,
                purpose,
                totalTargetCount,
                successCount,
                failCount,
                successRate,
                estimatedSaving,
                status,
                List.of(),
                List.of(),
                List.of(),
                0
        );
    }

    public HistoryListResponseDTO withChannels(List<String> channels) {
        return new HistoryListResponseDTO(
                id,
                scheduledAt,
                title,
                purpose,
                totalTargetCount,
                successCount,
                failCount,
                successRate,
                estimatedSaving,
                status,
                copyOrEmpty(channels),
                tags,
                displayTags,
                hiddenTagCount
        );
    }

    public HistoryListResponseDTO withTags(List<String> tags) {
        List<String> copiedTags = copyOrEmpty(tags);
        List<String> displayTags = copiedTags.stream().limit(3).toList();
        return new HistoryListResponseDTO(
                id,
                scheduledAt,
                title,
                purpose,
                totalTargetCount,
                successCount,
                failCount,
                successRate,
                estimatedSaving,
                status,
                channels,
                copiedTags,
                displayTags,
                Math.max(copiedTags.size() - displayTags.size(), 0)
        );
    }

    public BigDecimal getDisplaySuccessRate() {
        return successRate == null
                ? BigDecimal.ZERO.setScale(1)
                : successRate.setScale(1, RoundingMode.HALF_UP);
    }

    public String getPurposeLabel() {
        return purposeLabelOf(purpose);
    }

    public static String purposeLabelOf(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return "-";
        }
        return switch (purpose.trim().toUpperCase(Locale.ROOT)) {
            case "AD" -> "광고성";
            case "INFO" -> "정보성";
            default -> purpose;
        };
    }

    public String getStatusLabel() {
        return statusLabelOf(status);
    }

    public String getStatusStyleClass() {
        return SendHistoryStatus.styleClassOf(status);
    }

    public static String statusLabelOf(String status) {
        return SendHistoryStatus.labelOf(status);
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
