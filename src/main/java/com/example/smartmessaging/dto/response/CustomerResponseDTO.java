package com.example.smartmessaging.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class CustomerResponseDTO {
    
    private Long customerId;
    private String name;
    private String phone;
    private String customerType;     // 일반, 신규, 휴면
    private String joinedAt;         // 가입일 (yyyy-MM-dd)
    private String lastSend;         // 최근 발송 일시 (yyyy-MM-dd HH:mm)
    
    // 수신동의 플래그 4종
    private Boolean smsConsent;
    private Boolean kakaoConsent;
    private Boolean rcsConsent;
    private Boolean emailConsent;
    
    // 태그 목록
    private List<String> tags;
}
