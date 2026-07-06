package com.example.smartmessaging.dto.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SendRecipientCandidateVO {
    private Long customerId;
    private String phone;
    private String email;
    private Long channelId;
    private String channelType;
    private BigDecimal costPerMsg;
    private Integer maxLength;
    private Boolean consented;
}
