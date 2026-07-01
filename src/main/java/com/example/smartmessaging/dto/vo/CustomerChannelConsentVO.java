package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerChannelConsentVO extends BaseVO {
    private BigDecimal id;
    private Boolean isConsented;
    private BigDecimal customerId;
    private BigDecimal channelId;
}
