package com.example.smartmessaging.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmsMessageTypeResolverTest {

    @Test
    void 문자_본문_조립은_행동링크와_수신거부링크를_같은_형식으로_추가한다() {
        String text = SmsMessageTypeResolver.buildMessageText(
                "쿠폰이 도착했습니다.",
                "AD",
                "쿠폰 보기",
                "http://localhost:8080/r/abc",
                "http://localhost:8080/u/def"
        );

        assertThat(text).isEqualTo("""
                (광고) 쿠폰이 도착했습니다.

                쿠폰 보기
                http://localhost:8080/r/abc

                수신거부:
                http://localhost:8080/u/def""");
    }

    @Test
    void 제목이_있거나_본문_바이트가_SMS_한도를_넘으면_LMS로_판별한다() {
        assertThat(SmsMessageTypeResolver.resolve("SMS", "제목", "짧은 본문")).isEqualTo("LMS");
        assertThat(SmsMessageTypeResolver.resolve("SMS", null, "가".repeat(46))).isEqualTo("LMS");
        assertThat(SmsMessageTypeResolver.resolve("SMS", null, "short")).isEqualTo("SMS");
    }
}
