package com.example.smartmessaging.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "openai.moderation")
public class OpenAiModerationProperties {

    private String baseUrl = "https://api.openai.com";
    private String apiKey;
    private String model = "omni-moderation-latest";
    private boolean enabled = true;

    // 외부 안전성 검사가 지연되더라도 전체 AI 리뷰 요청이 무기한 대기하지 않도록 제한한다.
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(5);
}
