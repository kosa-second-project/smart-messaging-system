package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerStatVO extends BaseVO {
    private Long id;
    private LocalDate date;
    private Integer totalCustomerCount;
    private Integer normalCustomerCount;
    private Integer newCustomerCount;
    private Integer dormantCustomerCount;
    private Integer smsConsentCount;
    private Integer kakaoConsentCount;
    private Integer emailConsentCount;
    private Integer joinedCustomerCount;
}
