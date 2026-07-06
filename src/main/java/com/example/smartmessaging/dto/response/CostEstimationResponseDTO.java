package com.example.smartmessaging.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CostEstimationResponseDTO {
    private Integer totalTargetCount;
    private Integer sendableTargetCount;
    private Integer skippedTargetCount;
    private BigDecimal estimatedCost;
    private Boolean advertisingRestricted;
    private LocalDateTime advertisingSendAt;
    private List<ChannelEstimate> channels;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelEstimate {
        private Long channelId;
        private String channelType;
        private Integer targetCount;
        private BigDecimal estimatedCost;
    }
}
