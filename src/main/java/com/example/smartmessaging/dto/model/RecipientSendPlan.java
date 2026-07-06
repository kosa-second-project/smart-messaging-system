package com.example.smartmessaging.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientSendPlan {
    private Long customerId;
    private List<Long> channelSequence;
    private Long firstChannelId;
    private String firstChannelType;
    private BigDecimal estimatedCost;
    private boolean sendable;
    private String skipReason;
}
