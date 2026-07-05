package com.example.smartmessaging.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SendPrepareResponseDTO {
    private Long sendHistoryId;
    private int totalRequestedCount;
    private int preparedTargetCount;
    private int excludedTargetCount;
    private int publishedMessageCount;
    private BigDecimal estimatedCost;
}
