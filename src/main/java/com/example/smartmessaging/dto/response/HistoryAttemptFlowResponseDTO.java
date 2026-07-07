package com.example.smartmessaging.dto.response;

public record HistoryAttemptFlowResponseDTO(
        Integer attemptOrder,
        String channelName,
        Integer requestCount,
        Integer successCount,
        Integer failCount
) {
}
