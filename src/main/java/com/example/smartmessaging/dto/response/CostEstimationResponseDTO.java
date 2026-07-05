package com.example.smartmessaging.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostEstimationResponseDTO {
    private BigDecimal totalEstimatedCost;
    private Map<String, Integer> channelDistribution; // e.g., {"EMAIL": 10, "SMS": 20}
    private int totalValidRecipients;
}
