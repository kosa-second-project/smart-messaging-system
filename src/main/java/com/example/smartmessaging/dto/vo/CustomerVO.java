package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVO extends BaseVO {
    private Long id;
    private String phone;
    private String name;
    private String email;
    private String customerType;
    private Boolean isRealCustomer;
    private Boolean isAdBlocked;
    private LocalDateTime lastActiveAt;
    private LocalDateTime joinedAt;
    private LocalDate birthDate;    // DB: BIRTH_DATE DATE
    private String gender;           // DB: GENDER CHAR(1) - 'M', 'F' 등
}
