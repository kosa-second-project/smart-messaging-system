package com.example.smartmessaging.dto.model;

import com.example.smartmessaging.dto.vo.SendRecipientCandidateVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class RecipientSendPlan {
    private Long customerId;
    private Long firstChannelId;
    private BigDecimal estimatedCost;
    private List<String> fallbackSequence;
    private SendRecipientCandidateVO recipient;
}
