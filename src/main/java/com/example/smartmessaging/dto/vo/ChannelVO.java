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
public class ChannelVO extends BaseVO {
    private BigDecimal id;
    private String channelType;
    private BigDecimal costPerMsg;
    private BigDecimal maxLength;
    private Boolean isActive;
}
