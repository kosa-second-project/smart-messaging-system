package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.util.SmsMessageTypeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRouterService {

    private final KakaoMessageService kakaoMessageService;
    private final SmsMessageService smsMessageService;
    private final EmailMessageService emailMessageService;

    /**
     * MQ Consumer가 큐에서 데이터를 꺼내 호출할 발송 라우팅 메소드
     */
    public SendResult send(MessageTaskDto task) {
        if (task.getFallbackSequence() == null || task.getFallbackSequence().isEmpty()) {
            return SendResult.fail("NONE", "EMPTY_SEQUENCE", "발송 가능한 채널 시퀀스가 비어있습니다.");
        }
        
        if (task.getCurrentStep() < 0 || task.getCurrentStep() >= task.getFallbackSequence().size()) {
            return SendResult.fail("NONE", "INVALID_STEP", "잘못된 시도 단계(Step) 번호입니다.");
        }

        String currentChannel = task.getFallbackSequence().get(task.getCurrentStep());
        log.info("[MessageRouter] Routing messageId={} to channel={}, step={}/{}", 
                task.getMessageId(), currentChannel, task.getCurrentStep() + 1, task.getFallbackSequence().size());

        try {
            switch (currentChannel.toUpperCase()) {
                case "KAKAO":
                    // TODO: 나중에 실제 카카오 사용자 연동 시 토큰 관리 필요
                    // 현재는 테스트용 memo(나에게 보내기)를 호출하여 뼈대 연동 확인
                    boolean kakaoSuccess = kakaoMessageService.sendMemoMessage(
                            "DUMMY_TOKEN",
                            task.getTitle(),
                            task.getContent(),
                            task.getActionButtonName(),
                            task.getActionUrl()
                    );
                    if (kakaoSuccess) {
                        return SendResult.success("KAKAO");
                    } else {
                        return SendResult.fail("KAKAO", "KAKAO_SEND_FAIL", "카카오 메시지 발송 실패");
                    }
                case "SMS":
                case "LMS":
                    String resolvedMessageType = SmsMessageTypeResolver.resolve(
                            currentChannel,
                            task.getTitle(),
                            SmsMessageTypeResolver.buildMessageText(
                                    task.getContent(),
                                    task.getPurpose(),
                                    task.getActionButtonName(),
                                    task.getActionUrl(),
                                    task.getUnsubscribeUrl()
                            )
                    );
                    return smsMessageService.sendTextMessage(
                            task.getPhoneNumber(),
                            task.getTitle(),
                            task.getContent(),
                            resolvedMessageType,
                            task.getPurpose(),
                            task.getActionButtonName(),
                            task.getActionUrl(),
                            task.getUnsubscribeUrl()
                    );
                case "EMAIL":
                    return emailMessageService.sendEmail(task.getEmail(), task.getTitle(), task.getContent());
                default:
                    return SendResult.fail(currentChannel, "UNSUPPORTED_CHANNEL", "지원하지 않는 채널입니다: " + currentChannel);
            }
        } catch (Exception e) {
            log.error("[MessageRouter] Exception occurred sending via channel={}", currentChannel, e);
            return SendResult.fail(currentChannel, "SYSTEM_ERROR", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.");
        }
    }
}
