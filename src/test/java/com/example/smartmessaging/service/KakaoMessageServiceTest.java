package com.example.smartmessaging.service;

import com.example.smartmessaging.service.impl.KakaoMessageServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoMessageServiceTest {

    private final KakaoMessageService kakaoMessageService = new KakaoMessageServiceImpl(
            new RestTemplateBuilder(),
            new ObjectMapper(),
            "http://localhost:8080"
    );

    @Test
    void sendFeedMessageReturnsFalseWhenReceiversAreEmpty() {
        boolean result = kakaoMessageService.sendFeedMessage(
                "token",
                List.of(),
                "title",
                "content",
                "button",
                null
        );

        assertThat(result).isFalse();
    }
}