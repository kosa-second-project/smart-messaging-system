package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.ProfanityFilterClient;
import com.example.smartmessaging.ai.dto.response.ProfanityFilterResponse;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ProfanityFilterClient를 mock 처리해 외부 API 호출 없이 응답 판정과 예외 복구 정책을 검증한다.
 * 실제 API key를 사용하는 연동 확인은 구현 완료 후 Postman 또는 로컬 실행으로 별도 진행한다.
 */
class ProfanityValidationServiceTest {

    private ProfanityFilterClient profanityFilterClient;
    private ProfanityValidationService profanityValidationService;

    @BeforeEach
    void setUp() {
        profanityFilterClient = mock(ProfanityFilterClient.class);
        profanityValidationService = new ProfanityValidationService(profanityFilterClient);
    }

    @Test
    void 정상_응답의_detected가_비어_있으면_issue를_추가하지_않는다() {
        when(profanityFilterClient.filter("정상 본문"))
                .thenReturn(Optional.of(response(2000, List.of(), "정상 본문")));

        assertThat(profanityValidationService.validate("정상 본문")).isEmpty();
    }

    @Test
    void 감지된_표현을_PROFANITY_DETECTED_issue로_변환한다() {
        when(profanityFilterClient.filter("원문은 유지됩니다"))
                .thenReturn(Optional.of(response(
                        2000,
                        List.of(detected("나쁜말"), detected("부적절어"), detected("나쁜말")),
                        "***은 유지되지 않습니다"
                )));

        List<ValidationIssue> issues = profanityValidationService.validate("원문은 유지됩니다");

        assertThat(issues).hasSize(1);
        ValidationIssue issue = issues.get(0);
        assertThat(issue.getRuleId()).isEqualTo("PROFANITY_DETECTED");
        assertThat(issue.getSeverity()).isEqualTo(IssueSeverity.HIGH);
        assertThat(issue.getMessage()).isEqualTo("본문에 부적절한 표현이 포함되어 있습니다.");
        assertThat(issue.getTargetText()).isEqualTo("나쁜말, 부적절어");
        assertThat(issue.getSuggestion()).isEqualTo("부적절한 표현을 제거하거나 완화된 표현으로 수정하세요.");
    }

    @Test
    void status_code가_정상_코드가_아니면_issue를_추가하지_않는다() {
        when(profanityFilterClient.filter("검사 본문"))
                .thenReturn(Optional.of(response(4010, List.of(detected("나쁜말")), null)));

        assertThat(profanityValidationService.validate("검사 본문")).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("externalApiFailures")
    void 외부_API_실패는_issue로_변환하지_않는다(RuntimeException exception) {
        when(profanityFilterClient.filter("검사 본문")).thenThrow(exception);

        assertThat(profanityValidationService.validate("검사 본문")).isEmpty();
    }

    private static Stream<RuntimeException> externalApiFailures() {
        return Stream.of(
                new ResourceAccessException("timeout"),
                HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ),
                HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ),
                HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Service Unavailable",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ),
                new RestClientException("response parse failure")
        );
    }

    private ProfanityFilterResponse response(
            int statusCode,
            List<ProfanityFilterResponse.DetectedWord> detected,
            String filtered
    ) {
        ProfanityFilterResponse.Status status = new ProfanityFilterResponse.Status();
        status.setCode(statusCode);

        ProfanityFilterResponse response = new ProfanityFilterResponse();
        response.setStatus(status);
        response.setDetected(detected);
        response.setFiltered(filtered);
        return response;
    }

    private ProfanityFilterResponse.DetectedWord detected(String word) {
        ProfanityFilterResponse.DetectedWord detectedWord = new ProfanityFilterResponse.DetectedWord();
        detectedWord.setFilteredWord(word);
        return detectedWord;
    }
}
