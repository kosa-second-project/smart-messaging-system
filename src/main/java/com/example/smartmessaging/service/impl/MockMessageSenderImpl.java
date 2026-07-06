package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.mapper.HistoryMapper;
import com.example.smartmessaging.service.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모킹 메시지 발송 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockMessageSenderImpl implements MessageSender {

    private final HistoryMapper historyMapper;

    @Override
    @Transactional
    public void send(MessageQueueDto message) {
        String channel = message.getChannelType() != null ? message.getChannelType().toUpperCase() : "SMS";
        String recipient = message.getRecipientNo();
        String title = message.getTitle() != null ? message.getTitle() : "제목 없음";
        
        switch (channel) {
            case "KAKAO":
                log.info("💛 [카카오 알림톡 가상 발송] 수신인: {}, 템플릿: '{}', 내용: '{}'", 
                        recipient, title, truncateContent(message.getContent()));
                break;
            case "EMAIL":
                log.info("✉️ [이메일 가상 발송] 수신처: {}, 제목: '{}', 내용 요약: '{}'", 
                        recipient, title, truncateContent(message.getContent()));
                break;
            case "SMS":
            default:
                log.info("💬 [SMS 가상 발송] 수신번호: {}, 광고여부: {}, 내용 요약: '{}'", 
                        recipient, title, truncateContent(message.getContent()));
                break;
        }

        try {
            long latency = (long) (Math.random() * 40) + 10;
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (message.getSendTargetId() != null) {
            historyMapper.updateSendTargetStatus(message.getSendTargetId(), "SUCCEEDED");
        }
        if (message.getSendHistoryId() != null) {
            historyMapper.incrementSuccessCount(message.getSendHistoryId());
            historyMapper.updateHistoryStatus(message.getSendHistoryId(), "COMPLETED");
        }
    }

    private String truncateContent(String content) {
        if (content == null) return "";
        String clean = content.replace("\n", " ").trim();
        return clean.length() > 30 ? clean.substring(0, 27) + "..." : clean;
    }
}
