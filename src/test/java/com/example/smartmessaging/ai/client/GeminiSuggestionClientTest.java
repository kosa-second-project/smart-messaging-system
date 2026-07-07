package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.dto.response.AiSuggestionResponseDTO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GeminiSuggestionClientTest {

    private final GeminiSuggestionClient client = new GeminiSuggestionClient(
            (ChatClient) null,
            new ObjectMapper()
    );

    @Test
    void 코드블록과_설명이_있는_JSON을_파싱한다() {
        AiSuggestionResponseDTO response = client.parseResponse("""
                아래는 추천 결과입니다.
                ```json
                {"suggestions":[{"title":"안내","content":"본문"}]}
                ```
                이상입니다.
                """);

        assertThat(response.suggestions()).hasSize(1);
        assertThat(response.suggestions().get(0).title()).isEqualTo("안내");
        assertThat(response.suggestions().get(0).content()).isEqualTo("본문");
    }

    @Test
    void 파싱할_수_없는_응답은_외부_API_예외로_변환한다() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> client.parseResponse("올바른 JSON이 아닙니다."))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR)
                );
    }

    @Test
    void suggestions_배열이_없으면_빈_리스트로_보정한다() {
        AiSuggestionResponseDTO response = client.parseResponse("{\"result\":[]}");

        assertThat(response.suggestions()).isEmpty();
    }
}
