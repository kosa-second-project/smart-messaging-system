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
public class MessageStatVO extends BaseVO {
    private Long id;
    private Integer totalSendCount;
    private Integer totalSuccessCount;
    private Integer totalFailCount;
    private BigDecimal billingCost;
    private BigDecimal maxCost;
    private LocalDate date;
}
