package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.OpenAiModerationClient;
import com.example.smartmessaging.ai.client.OpenAiModerationException;
import com.example.smartmessaging.ai.dto.response.OpenAiModerationResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OpenAiModerationClient를 mock 처리해 외부 호출 없이 경고 이슈 변환과 장애 복구를 검증한다.
 */
@ExtendWith(OutputCaptureExtension.class)
class OpenAiModerationValidationServiceTest {

    private OpenAiModerationClient client;
    private OpenAiModerationValidationService service;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiModerationClient.class);
        service = new OpenAiModerationValidationService(client);
    }

    @Test
    void flagged가_false이면_issue를_추가하지_않는다() {
        when(client.moderate("정상 본문")).thenReturn(response(false, null));

        assertThat(service.validate("정상 본문")).isEmpty();
    }

    @Test
    void flagged가_true이면_true인_category만_경고_issue에_추가한다() {
        OpenAiModerationResponseDTO.Categories categories = new OpenAiModerationResponseDTO.Categories(true, null, false, null, null, null, null, null, null, null, null, null, true);
        when(client.moderate("검사 본문")).thenReturn(response(true, categories));

        List<ValidationIssueResponseDTO> issues = service.validate("검사 본문");

        assertThat(issues).hasSize(1);
        ValidationIssueResponseDTO issue = issues.get(0);
        assertThat(issue.ruleId()).isEqualTo("AI_SAFETY_DETECTED");
        assertThat(issue.severity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(issue.targetText()).isEqualTo("harassment, violence/graphic");
        assertThat(issue.detail()).containsExactly("harassment", "violence/graphic");
    }

    @ParameterizedTest
    @EnumSource(OpenAiModerationException.FailureType.class)
    void 외부_검사_실패를_MODERATION_UNAVAILABLE_issue로_변환한다(
            OpenAiModerationException.FailureType failureType
    ) {
        when(client.moderate("검사 본문"))
                .thenThrow(new OpenAiModerationException(failureType));

        List<ValidationIssueResponseDTO> issues = service.validate("검사 본문");

        assertThat(issues).hasSize(1);
        ValidationIssueResponseDTO issue = issues.get(0);
        assertThat(issue.ruleId()).isEqualTo("MODERATION_UNAVAILABLE");
        assertThat(issue.severity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(issue.targetText()).isNull();
        assertThat(issue.detail()).containsExactly(failureType.name());
    }

    @Test
    void results가_비어_있으면_PARSING_ERROR_경고로_복구한다() {
        OpenAiModerationResponseDTO response = new OpenAiModerationResponseDTO(null, null, List.of());
        when(client.moderate("검사 본문")).thenReturn(response);

        ValidationIssueResponseDTO issue = service.validate("검사 본문").get(0);

        assertThat(issue.ruleId()).isEqualTo("MODERATION_UNAVAILABLE");
        assertThat(issue.detail()).containsExactly("PARSING_ERROR");
    }

    @Test
    void 예상하지_못한_예외는_원인과_스택_트레이스를_한_번만_기록한다(CapturedOutput output) {
        when(client.moderate("검사 본문"))
                .thenThrow(new IllegalStateException("unexpected failure"));

        ValidationIssueResponseDTO issue = service.validate("검사 본문").get(0);

        assertThat(issue.ruleId()).isEqualTo("MODERATION_UNAVAILABLE");
        assertThat(issue.detail()).containsExactly("API_ERROR");
        assertThat(output.getOut())
                .containsOnlyOnce("OpenAI Moderation 검사 중 예상하지 못한 오류가 발생했습니다.")
                .contains("exceptionType=IllegalStateException")
                .contains("unexpected failure")
                .doesNotContain("OpenAI Moderation 검사를 완료하지 못했습니다.");
    }

    private OpenAiModerationResponseDTO response(
            boolean flagged,
            OpenAiModerationResponseDTO.Categories categories
    ) {
        OpenAiModerationResponseDTO.Result result = new OpenAiModerationResponseDTO.Result(flagged, categories, null);
        return new OpenAiModerationResponseDTO(null, null, List.of(result));
    }
}
