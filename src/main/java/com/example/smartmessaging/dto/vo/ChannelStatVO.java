package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelStatVO extends BaseVO {
    private BigDecimal id;
    private BigDecimal clickTargetCount;
    private BigDecimal clickCount;
    private BigDecimal conversionTargetCount;
    private BigDecimal conversionCount;
    private BigDecimal consentTargetCount;
    private BigDecimal consentCount;
    private LocalDate date;
    private BigDecimal channelId;
}
