package com.example.smartmessaging.ai.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

@Configuration
@EnableConfigurationProperties(GoogleGenAiConnectionProperties.class)
public class GeminiClientConfig {

    static final int REQUEST_TIMEOUT_MS = 30_000;

    /**
     * Spring AI가 사용하는 저수준 Gemini Client에 실제 HTTP call timeout을 적용한다.
     * API key는 기존 Spring 설정에서만 읽으며 로그나 별도 설정 객체에 복사하지 않는다.
     */
    @Bean
    public Client googleGenAiClient(GoogleGenAiConnectionProperties properties) {
        Assert.hasText(properties.getApiKey(), "Google GenAI API key must be configured.");
        return Client.builder()
                .apiKey(properties.getApiKey())
                .httpOptions(requestHttpOptions())
                .build();
    }

    static HttpOptions requestHttpOptions() {
        return HttpOptions.builder()
                .timeout(REQUEST_TIMEOUT_MS)
                .build();
    }
}
