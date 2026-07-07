package com.example.smartmessaging.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryRetryFailedResponseDTO {
    private Long sendHistoryId;
    private int retryTargetCount;
    private int publishedCount;
    private int skippedCount;
    private String message;
}
