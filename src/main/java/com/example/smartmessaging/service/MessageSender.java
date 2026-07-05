package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.queue.MessageQueueDto;

public interface MessageSender {
    /**
     * 대기열에서 꺼낸 메시지 일감을 외부 채널 API를 모사하여 가상 발송 처리합니다.
     */
    void send(MessageQueueDto message);
}
