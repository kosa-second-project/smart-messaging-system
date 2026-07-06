package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;

/**
 * SMS/LMS 문자 발송 서비스 인터페이스
 */
public interface SmsMessageService {

    SendResult sendSms(String phoneNumber, String content);

    SendResult sendTextMessage(String phoneNumber, String title, String content, String channelType);

    SendResult sendTextMessage(
            String phoneNumber,
            String title,
            String content,
            String channelType,
            String purpose,
            String actionButtonName,
            String actionUrl,
            String unsubscribeUrl
    );
}
