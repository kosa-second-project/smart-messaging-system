package com.example.smartmessaging.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "profanity-filter")
public class ProfanityFilterProperties {

    // 설정 파일에 timeout이 없더라도 외부 API가 무기한 대기하지 않도록 기본값을 둔다.
    private String baseUrl = "https://api.kr-filter.com";
    private String apiKey;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(5);
}
