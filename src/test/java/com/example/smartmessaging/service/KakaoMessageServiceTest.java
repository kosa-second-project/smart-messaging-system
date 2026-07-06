package com.example.smartmessaging.service;

import com.example.smartmessaging.exception.KakaoApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.example.smartmessaging.service.impl.KakaoMessageServiceImpl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoMessageServiceTest {

    private final KakaoMessageService kakaoMessageService = new KakaoMessageServiceImpl(
            new RestTemplateBuilder(),
            new ObjectMapper()
    );

    @Test
    void 카카오_메시지는_링크URL이_없으면_서비스_기본URL로_대체하지_않는다() {
        assertThatThrownBy(() -> kakaoMessageService.sendMemoMessage(
                "token",
                "제목",
                "내용",
                "자세히 보기",
                null
        )).isInstanceOf(KakaoApiException.class)
                .hasMessageContaining("링크 URL이 없습니다");
    }
}
