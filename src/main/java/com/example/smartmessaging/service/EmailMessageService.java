package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 이메일 발송 서비스 인터페이스
 */
public interface EmailMessageService {
    
    record EmailCheck(@Email @NotBlank String email) {}

    SendResult sendEmail(String email, String title, String content);
}
