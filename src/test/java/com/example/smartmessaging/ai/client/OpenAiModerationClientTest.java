package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.config.OpenAiModerationProperties;
import com.example.smartmessaging.ai.dto.response.OpenAiModerationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.API_ERROR;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.DISABLED;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.MISSING_API_KEY;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.PARSING_ERROR;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 실제 OpenAI API를 호출하지 않고 요청 형식과 응답 매핑, 안전한 실패 유형 변환을 검증한다.
 */
class OpenAiModerationClientTest {

    private OpenAiModerationProperties properties;
    private MockRestServiceServer server;
    private OpenAiModerationClient client;

    @BeforeEach
    void setUp() {
        properties = new OpenAiModerationProperties();
        properties.setBaseUrl("https://openai.test/");
        properties.setApiKey("test-openai-key");
        properties.setModel("omni-moderation-latest");
        properties.setEnabled(true);

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new OpenAiModerationClient(restTemplate, properties);
    }

    @Test
    void Bearer_인증과_model_input으로_Moderation_API를_호출한다() {
        server.expect(requestTo("https://openai.test/v1/moderations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-openai-key"))
                .andExpect(content().json("""
                        {
                          "model": "omni-moderation-latest",
                          "input": "검사할 본문"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "modr-test",
                          "model": "omni-moderation-latest",
                          "results": [{
                            "flagged": true,
                            "categories": {
                              "harassment": true,
                              "harassment/threatening": false,
                              "future-category": true
                            },
                            "category_scores": {
                              "harassment": 0.9,
                              "harassment/threatening": 0.1,
                              "future-category": 0.7
                            },
                            "category_applied_input_types": {
                              "harassment": ["text"]
                            }
                          }],
                          "future_response_field": "ignored"
                        }
                        """, MediaType.APPLICATION_JSON));

        OpenAiModerationResponse response = client.moderate("검사할 본문");

        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getFlagged()).isTrue();
        assertThat(response.getResults().get(0).getCategories().getHarassment()).isTrue();
        assertThat(response.getResults().get(0).getCategoryScores().getHarassment()).isEqualTo(0.9);
        server.verify();
    }

    @Test
    void API_key가_없으면_외부_API를_호출하지_않는다() {
        properties.setApiKey("  ");

        assertFailureType(MISSING_API_KEY, () -> client.moderate("검사할 본문"));
        server.verify();
    }

    @Test
    void 비활성화되면_외부_API를_호출하지_않는다() {
        properties.setEnabled(false);

        assertFailureType(DISABLED, () -> client.moderate("검사할 본문"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 503})
    void 인증_rate_limit_서버_오류를_API_ERROR로_변환한다(int statusCode) {
        server.expect(requestTo("https://openai.test/v1/moderations"))
                .andRespond(withStatus(HttpStatusCode.valueOf(statusCode)));

        assertFailureType(API_ERROR, () -> client.moderate("검사할 본문"));
        server.verify();
    }

    @Test
    void API_오류의_상태와_요청_ID_구조화된_원인을_보존한다() {
        server.expect(requestTo("https://openai.test/v1/moderations"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-request-id", "req-test-123")
                        .body("""
                                {
                                  "error": {
                                    "message": "로그에 남기지 않을 원문 메시지",
                                    "type": "invalid_request_error",
                                    "code": "invalid_api_key"
                                  }
                                }
                                """));

        assertThatThrownBy(() -> client.moderate("검사할 본문"))
                .isInstanceOf(OpenAiModerationException.class)
                .satisfies(throwable -> {
                    OpenAiModerationException exception = (OpenAiModerationException) throwable;
                    assertThat(exception.getFailureType()).isEqualTo(API_ERROR);
                    assertThat(exception.getHttpStatus()).isEqualTo(401);
                    assertThat(exception.getRequestId()).isEqualTo("req-test-123");
                    assertThat(exception.getErrorType()).isEqualTo("invalid_request_error");
                    assertThat(exception.getErrorCode()).isEqualTo("invalid_api_key");
                    assertThat(exception.getMessage()).doesNotContain("원문 메시지");
                });
        server.verify();
    }

    @Test
    void timeout을_TIMEOUT으로_변환한다() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OpenAiModerationClient timeoutClient = new OpenAiModerationClient(restTemplate, properties);
        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(OpenAiModerationResponse.class)
        )).thenThrow(new ResourceAccessException("timeout"));

        assertFailureType(TIMEOUT, () -> timeoutClient.moderate("검사할 본문"));
    }

    @Test
    void 응답을_파싱할_수_없으면_PARSING_ERROR로_변환한다() {
        server.expect(requestTo("https://openai.test/v1/moderations"))
                .andRespond(withSuccess("{invalid-json", MediaType.APPLICATION_JSON));

        assertFailureType(PARSING_ERROR, () -> client.moderate("검사할 본문"));
        server.verify();
    }

    private void assertFailureType(
            OpenAiModerationException.FailureType expected,
            org.assertj.core.api.ThrowableAssert.ThrowingCallable callable
    ) {
        assertThatThrownBy(callable)
                .isInstanceOf(OpenAiModerationException.class)
                .extracting(exception -> ((OpenAiModerationException) exception).getFailureType())
                .isEqualTo(expected);
    }
}
