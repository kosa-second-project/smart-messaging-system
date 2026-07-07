package com.example.smartmessaging.dto.response;

import com.example.smartmessaging.dto.type.SendHistoryStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record HistoryDetailResponseDTO(
        Long sendHistoryId,
        String title,
        String content,
        LocalDateTime scheduledAt,
        String purpose,
        String status,
        Integer totalTargetCount,
        Integer successCount,
        Integer failCount,
        BigDecimal actualCost,
        BigDecimal estimatedSaving,
        BigDecimal successRate,
        List<String> channels,
        List<String> tags,
        List<HistoryAttemptFlowResponseDTO> attemptFlows,
        Integer retryableFailCount,
        Integer unretryableFailCount
) {
    public HistoryDetailResponseDTO(
            Long sendHistoryId,
            String title,
            String content,
            LocalDateTime scheduledAt,
            String purpose,
            String status,
            Integer totalTargetCount,
            Integer successCount,
            Integer failCount,
            BigDecimal actualCost,
            BigDecimal estimatedSaving,
            BigDecimal successRate
    ) {
        this(
                sendHistoryId,
                title,
                content,
                scheduledAt,
                purpose,
                status,
                totalTargetCount,
                successCount,
                failCount,
                actualCost,
                estimatedSaving,
                successRate,
                List.of(),
                List.of(),
                List.of(),
                0,
                0
        );
    }

    public HistoryDetailResponseDTO withChannels(List<String> channels) {
        return new HistoryDetailResponseDTO(
                sendHistoryId,
                title,
                content,
                scheduledAt,
                purpose,
                status,
                totalTargetCount,
                successCount,
                failCount,
                actualCost,
                estimatedSaving,
                successRate,
                copyOrEmpty(channels),
                tags,
                attemptFlows,
                retryableFailCount,
                unretryableFailCount
        );
    }

    public HistoryDetailResponseDTO withTags(List<String> tags) {
        return new HistoryDetailResponseDTO(
                sendHistoryId,
                title,
                content,
                scheduledAt,
                purpose,
                status,
                totalTargetCount,
                successCount,
                failCount,
                actualCost,
                estimatedSaving,
                successRate,
                channels,
                copyOrEmpty(tags),
                attemptFlows,
                retryableFailCount,
                unretryableFailCount
        );
    }

    public HistoryDetailResponseDTO withAttemptFlows(List<HistoryAttemptFlowResponseDTO> attemptFlows) {
        return new HistoryDetailResponseDTO(
                sendHistoryId,
                title,
                content,
                scheduledAt,
                purpose,
                status,
                totalTargetCount,
                successCount,
                failCount,
                actualCost,
                estimatedSaving,
                successRate,
                channels,
                tags,
                attemptFlows == null ? List.of() : List.copyOf(attemptFlows),
                retryableFailCount,
                unretryableFailCount
        );
    }

    public HistoryDetailResponseDTO withRetryAvailability(Integer retryableFailCount, Integer unretryableFailCount) {
        return new HistoryDetailResponseDTO(
                sendHistoryId,
                title,
                content,
                scheduledAt,
                purpose,
                status,
                totalTargetCount,
                successCount,
                failCount,
                actualCost,
                estimatedSaving,
                successRate,
                channels,
                tags,
                attemptFlows,
                retryableFailCount == null ? 0 : retryableFailCount,
                unretryableFailCount == null ? 0 : unretryableFailCount
        );
    }

    public BigDecimal getDisplaySuccessRate() {
        return successRate == null
                ? BigDecimal.ZERO.setScale(1)
                : successRate.setScale(1, RoundingMode.HALF_UP);
    }

    public String getPurposeLabel() {
        return HistoryListResponseDTO.purposeLabelOf(purpose);
    }

    public String getStatusLabel() {
        return SendHistoryStatus.labelOf(status);
    }

    private static List<String> copyOrEmpty(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}