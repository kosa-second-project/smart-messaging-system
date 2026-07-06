package com.example.smartmessaging.dto.response;

import com.example.smartmessaging.dto.vo.SendResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DevTestSendResponse {
    private Long customerId;
    private String customerName;
    private String email;
    private String phone;
    private String smsActionUrl;
    private String emailActionUrl;
    private String kakaoActionUrl;
    private SendResult smsResult;
    private SendResult emailResult;
    private SendResult kakaoResult;
}
