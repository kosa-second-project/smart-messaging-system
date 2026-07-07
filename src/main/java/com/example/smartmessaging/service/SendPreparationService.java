package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.SendPrepareRequestDTO;
import com.example.smartmessaging.dto.response.SendPrepareResponseDTO;

/**
 * 발송 준비 서비스 인터페이스
 */
public interface SendPreparationService {
    SendPrepareResponseDTO prepare(Long userId, SendPrepareRequestDTO request, String kakaoAccessToken);
}
