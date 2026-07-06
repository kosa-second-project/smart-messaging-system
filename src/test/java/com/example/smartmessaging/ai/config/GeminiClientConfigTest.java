package com.example.smartmessaging.ai.config;

import com.google.genai.Client;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GeminiClientConfig.class)
            .withPropertyValues("spring.ai.google.genai.api-key=test-key");

    @Test
    void Gemini_HTTP_요청_timeout은_30초다() {
        assertThat(GeminiClientConfig.requestHttpOptions().timeout())
                .contains(GeminiClientConfig.REQUEST_TIMEOUT_MS);
        assertThat(GeminiClientConfig.REQUEST_TIMEOUT_MS).isEqualTo(30_000);
    }

    @Test
    void 기존_Gemini_API_key_설정으로_Client_빈을_생성한다() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Client.class);
        });
    }
}
