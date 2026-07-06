package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.vo.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class KakaoQueueMessageService {

    private final boolean dummyEnabled;

    public KakaoQueueMessageService(@Value("${messaging.kakao.dummy-enabled:false}") boolean dummyEnabled) {
        this.dummyEnabled = dummyEnabled;
    }

    public SendResult sendKakao(String recipientKey, String title, String content, String linkUrl) {
        if (recipientKey == null || recipientKey.isBlank()) {
            return SendResult.fail("KAKAO", "MISSING_KAKAO_RECIPIENT", "Kakao recipient key is missing.");
        }
        if (content == null || content.isBlank()) {
            return SendResult.fail("KAKAO", "INVALID_CONTENT", "Kakao message content is required.");
        }
        if (!dummyEnabled) {
            return SendResult.fail("KAKAO", "KAKAO_PROVIDER_DISABLED", "Kakao provider is not configured for queued campaign sends.");
        }

        log.info("Dummy Kakao send accepted. recipientPresent={}, titlePresent={}, linkUrlPresent={}",
                true,
                title != null && !title.isBlank(),
                linkUrl != null && !linkUrl.isBlank());
        return SendResult.success("KAKAO", "KAKAO-MOCK-" + UUID.randomUUID());
    }
}
