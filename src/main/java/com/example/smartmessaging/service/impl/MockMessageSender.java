package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.queue.MessageQueueDto;
import com.example.smartmessaging.mapper.HistoryMapper;
import com.example.smartmessaging.service.MessageSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockMessageSender implements MessageSender {

    private final HistoryMapper historyMapper;

    @Override
    @Transactional
    public void send(MessageQueueDto message) {
        // 1. 채널별 가상 발송 로그 출력
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

        // 2. 가상 발송 지연시간 모사 (실제 네트워크 전송 시간 흉내 - 10ms ~ 50ms 랜덤)
        try {
            long latency = (long) (Math.random() * 40) + 10;
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 3. DB (send_target) 발송 완료 상태 업데이트 및 마스터(send_history) 성공 카운트 및 상태 연동
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
