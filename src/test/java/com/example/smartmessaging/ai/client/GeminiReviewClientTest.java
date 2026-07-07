package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.dto.response.LlmReviewResponse;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class GeminiReviewClientTest {

    private final GeminiReviewClient client = new GeminiReviewClient(
            (ChatClient) null,
            new ObjectMapper()
    );

    @Test
    void 코드블록과_설명이_있는_첫_JSON_객체를_파싱한다() {
        LlmReviewResponse response = client.parseResponse("""
                검사 결과입니다.
                ```json
                {
                  "reviewedExistingIssues": [],
                  "newIssues": [{
                    "ruleId": "CLARITY_ISSUE",
                    "riskLevel": "LOW",
                    "field": "content",
                    "message": "표현이 모호합니다."
                  }],
                  "suggestedRewrite": "조건을 구체적으로 확인해 주세요."
                }
                ```
                뒤의 객체는 무시합니다. {"ignored":true}
                """);

        assertThat(response.getNewIssues()).hasSize(1);
        assertThat(response.getNewIssues().get(0).getRuleId()).isEqualTo("CLARITY_ISSUE");
        assertThat(response.getSuggestedRewrite()).isEqualTo("조건을 구체적으로 확인해 주세요.");
    }

    @Test
    void 배열이_누락된_JSON도_파싱하고_서비스에서_빈_배열로_처리할_수_있다() {
        LlmReviewResponse response = client.parseResponse("{\"suggestedRewrite\":null}");

        assertThat(response.getReviewedExistingIssues()).isNull();
        assertThat(response.getNewIssues()).isNull();
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
    void Gemini_호출_실패는_외부_API_예외로_변환한다() {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> client.review("system", "user"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR)
                );
    }
}
