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
public class MessageStatByDegreeVO extends BaseVO {
    private BigDecimal id;
    private BigDecimal degree;
    private BigDecimal sendCount;
    private BigDecimal successCount;
    private LocalDate date;
    private BigDecimal cost;
    private BigDecimal channelId;
}
