package com.example.smartmessaging.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendRealRecipientWhitelistVO extends BaseVO {
    private Long id;
    private String phone;
    private String name;
    private String email;
    private String customerType;
    private Boolean isAdBlocked;
    private LocalDateTime lastActiveAt;
    private LocalDateTime joinedAt;
    private LocalDate birthDate;
    private String gender;
    private String description;
    private Boolean isActive;
}
