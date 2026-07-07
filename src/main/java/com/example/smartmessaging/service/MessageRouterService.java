package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.SendResult;

/**
 * 메시지 발송 라우터 서비스 인터페이스
 */
public interface MessageRouterService {
    SendResult send(MessageTaskDto task);
}
