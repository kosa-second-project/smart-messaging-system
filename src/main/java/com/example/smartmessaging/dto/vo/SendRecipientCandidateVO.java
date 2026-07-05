package com.example.smartmessaging.dto.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendRecipientCandidateVO {
    private Long customerId;
    private String phone;
    private String email;
    private String kakaoUserKey;
    private Integer kakaoConsent;
    private Integer emailConsent;
    private Integer smsConsent;
}
