package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.DevTestSendRequest;
import com.example.smartmessaging.dto.response.DevTestSendResponse;

/**
 * 개발/테스트 메시지 발송 서비스 인터페이스
 */
public interface DevTestMessageService {
    DevTestSendResponse sendToMe(Long userId, String userName, String kakaoAccessToken, DevTestSendRequest request);
}
