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
public class CustomerStatVO extends BaseVO {
    private BigDecimal id;
    private LocalDate date;
    private BigDecimal totalCustomerCount;
    private BigDecimal normalCustomerCount;
    private BigDecimal newCustomerCount;
    private BigDecimal dormantCustomerCount;
    private BigDecimal smsConsentCount;
    private BigDecimal kakaoConsentCount;
    private BigDecimal emailConsentCount;
    private BigDecimal joinedCustomerCount;
}
