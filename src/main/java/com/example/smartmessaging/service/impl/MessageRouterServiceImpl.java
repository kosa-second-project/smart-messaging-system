package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.MessageTaskDto;
import com.example.smartmessaging.dto.vo.SendResult;
import com.example.smartmessaging.service.EmailMessageService;
import com.example.smartmessaging.service.KakaoMessageService;
import com.example.smartmessaging.service.MessageRouterService;
import com.example.smartmessaging.service.SmsMessageService;
import com.example.smartmessaging.util.SmsMessageTypeResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 메시지 발송 라우터 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRouterServiceImpl implements MessageRouterService {

    private final KakaoMessageService kakaoMessageService;
    private final SmsMessageService smsMessageService;
    private final EmailMessageService emailMessageService;

    @Override
    public SendResult send(MessageTaskDto task) {
        if (task.getFallbackSequence() == null || task.getFallbackSequence().isEmpty()) {
            return SendResult.fail("NONE", "EMPTY_SEQUENCE", "발송 가능한 채널 시퀀스가 비어있습니다.");
        }

        if (task.getCurrentStep() < 0 || task.getCurrentStep() >= task.getFallbackSequence().size()) {
            return SendResult.fail("NONE", "INVALID_STEP", "잘못된 시도 단계(Step) 번호입니다.");
        }

        String currentChannel = task.getFallbackSequence().get(task.getCurrentStep());
        String personalizedTitle = personalize(task.getTitle(), task.getCustomerName());
        String personalizedContent = personalize(task.getContent(), task.getCustomerName());
        log.info("[MessageRouter] Routing messageId={} to channel={}, step={}/{}",
                task.getMessageId(), currentChannel, task.getCurrentStep() + 1, task.getFallbackSequence().size());

        try {
            switch (currentChannel.toUpperCase()) {
                case "KAKAO":
                    if (task.getKakaoAccessToken() == null || task.getKakaoAccessToken().isBlank()) {
                        return SendResult.fail("KAKAO", "KAKAO_TOKEN_MISSING", "카카오 access token이 없어 친구 발송을 진행할 수 없습니다.");
                    }
                    boolean kakaoSuccess = kakaoMessageService.sendFeedMessageToAll(
                            task.getKakaoAccessToken(),
                            personalizedTitle,
                            personalizedContent,
                            task.getActionButtonName(),
                            task.getActionUrl()
                    );
                    if (kakaoSuccess) {
                        return SendResult.success("KAKAO");
                    } else {
                        return SendResult.fail("KAKAO", "KAKAO_SEND_FAIL", "카카오 친구 메시지 발송 실패");
                    }
                case "SMS":
                case "LMS":
                    String resolvedMessageType = SmsMessageTypeResolver.resolve(
                            currentChannel,
                            personalizedTitle,
                            SmsMessageTypeResolver.buildMessageText(
                                    personalizedContent,
                                    task.getPurpose(),
                                    task.getActionButtonName(),
                                    task.getActionUrl(),
                                    task.getUnsubscribeUrl()
                            )
                    );
                    return smsMessageService.sendTextMessage(
                            task.getPhoneNumber(),
                            personalizedTitle,
                            personalizedContent,
                            resolvedMessageType,
                            task.getPurpose(),
                            task.getActionButtonName(),
                            task.getActionUrl(),
                            task.getUnsubscribeUrl()
                    );
                case "EMAIL":
                    return emailMessageService.sendEmail(
                            task.getEmail(),
                            personalizedTitle,
                            personalizedContent,
                            task.getActionButtonName(),
                            task.getActionUrl()
                    );
                default:
                    return SendResult.fail(currentChannel, "UNSUPPORTED_CHANNEL", "지원하지 않는 채널입니다: " + currentChannel);
            }
        } catch (Exception e) {
            log.error("[MessageRouter] Exception occurred sending via channel={}", currentChannel, e);
            return SendResult.fail(currentChannel, "SYSTEM_ERROR", "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요.");
        }
    }

    private String personalize(String text, String customerName) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String name = customerName == null || customerName.isBlank() ? "\uACE0\uAC1D" : customerName.trim();
        return text
                .replace("#{\uACE0\uAC1D\uBA85}", name)
                .replace("#{\uC774\uB984}", name);
    }
}
