package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.GeminiReviewClient;
import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.LlmReviewResponse;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmReviewServiceTest {

    private GeminiReviewClient geminiReviewClient;
    private LlmReviewService llmReviewService;

    @BeforeEach
    void setUp() {
        geminiReviewClient = mock(GeminiReviewClient.class);
        llmReviewService = new LlmReviewService(geminiReviewClient, new ObjectMapper());
    }

    @Test
    void 기존_이슈를_JSON으로_전달하고_빈_LLM_결과를_그대로_병합한다() {
        ValidationIssue serverIssue = issue(
                "MISSING_OPT_OUT",
                IssueSource.SERVER_RULE,
                IssueSeverity.HIGH
        );
        LlmReviewResponse response = new LlmReviewResponse();
        response.setReviewedExistingIssues(List.of());
        response.setNewIssues(List.of());
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(
                request(),
                List.of(serverIssue)
        );

        assertThat(result.existingIssues()).containsExactly(serverIssue);
        assertThat(result.newIssues()).isEmpty();
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(geminiReviewClient).review(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue())
                .contains("\"existingIssues\"")
                .contains("MISSING_OPT_OUT")
                .contains("SERVER_RULE");
    }

    @Test
    void 신규_이슈는_허용_규칙과_기본_severity로_보정하고_중복을_제거한다() {
        LlmReviewResponse response = new LlmReviewResponse();
        response.setNewIssues(List.of(
                newIssue("OVERSTATED_BENEFIT", "LOW", "content", "역대급", "과장 표현입니다."),
                newIssue("OVERSTATED_BENEFIT", "LOW", "content", "역대급", "중복입니다."),
                newIssue("CLARITY_ISSUE", "HIGH", "invalid", null, "표현이 모호합니다."),
                newIssue("UNKNOWN_RULE", "HIGH", "content", null, "알 수 없는 규칙입니다.")
        ));
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(request(), List.of());

        assertThat(result.newIssues()).hasSize(2);
        ValidationIssue overstated = result.newIssues().get(0);
        assertThat(overstated.getSource()).isEqualTo(IssueSource.LLM_REVIEW);
        assertThat(overstated.getSeverity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(overstated.getStatus()).isEqualTo(ReviewStatus.WARNING);
        ValidationIssue clarity = result.newIssues().get(1);
        assertThat(clarity.getSeverity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(clarity.getField()).isEqualTo("content");
    }

    @ParameterizedTest
    @CsvSource({
            "BRAND_TONE_MISMATCH, LOW",
            "OVERSTATED_BENEFIT, MEDIUM",
            "INFO_MESSAGE_PROMOTIONAL, MEDIUM",
            "SENSITIVE_EXPRESSION_RISK, MEDIUM",
            "CHANNEL_FIT_WARNING, LOW",
            "CLARITY_ISSUE, LOW"
    })
    void 허용된_여섯_가지_LLM_규칙을_기본_severity로_변환한다(
            String ruleId,
            IssueSeverity expectedSeverity
    ) {
        LlmReviewResponse response = new LlmReviewResponse();
        response.setNewIssues(List.of(
                newIssue(ruleId, "INVALID", "title", null, "문맥 검사 이슈입니다.")
        ));
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(request(), List.of());

        assertThat(result.newIssues()).singleElement().satisfies(issue -> {
            assertThat(issue.getRuleId()).isEqualTo(ruleId);
            assertThat(issue.getSeverity()).isEqualTo(expectedSeverity);
            assertThat(issue.getStatus()).isEqualTo(ValidationIssue.statusOf(expectedSeverity));
            assertThat(issue.getField()).isEqualTo("title");
        });
    }

    @Test
    void 오탐_가능성은_외부_검사만_한_단계_완화하고_서버_룰은_변경하지_않는다() {
        ValidationIssue serverIssue = issue(
                "MISSING_AD_PREFIX",
                IssueSource.SERVER_RULE,
                IssueSeverity.HIGH
        );
        ValidationIssue profanityIssue = issue(
                "PROFANITY_DETECTED",
                IssueSource.PROFANITY_FILTER,
                IssueSeverity.HIGH
        );
        ValidationIssue moderationIssue = issue(
                "AI_SAFETY_DETECTED",
                IssueSource.OPENAI_MODERATION,
                IssueSeverity.MEDIUM
        );

        LlmReviewResponse response = new LlmReviewResponse();
        response.setReviewedExistingIssues(List.of(
                reviewed("MISSING_AD_PREFIX", "SERVER_RULE", "POSSIBLE_FALSE_POSITIVE"),
                reviewed("PROFANITY_DETECTED", "PROFANITY_FILTER", "POSSIBLE_FALSE_POSITIVE"),
                reviewed("AI_SAFETY_DETECTED", "OPENAI_MODERATION", "POSSIBLE_FALSE_POSITIVE")
        ));
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(
                request(),
                List.of(serverIssue, profanityIssue, moderationIssue)
        );

        assertThat(result.existingIssues())
                .extracting(ValidationIssue::getSeverity)
                .containsExactly(IssueSeverity.HIGH, IssueSeverity.MEDIUM, IssueSeverity.LOW);
        assertThat(result.existingIssues().get(0).getDetail()).isEmpty();
        assertThat(result.existingIssues().get(1).getDetail())
                .contains("llmReviewResult=POSSIBLE_FALSE_POSITIVE", "llmReason=문맥 재검토 결과입니다.");
        assertThat(result.existingIssues().get(1).getSuggestion()).isEqualTo("중립적인 표현을 사용하세요.");
    }

    @Test
    void CONFIRMED와_NEEDS_REVIEW는_기존_severity를_유지하고_수정안을_반환한다() {
        ValidationIssue profanityIssue = issue(
                "PROFANITY_DETECTED",
                IssueSource.PROFANITY_FILTER,
                IssueSeverity.HIGH
        );
        ValidationIssue moderationIssue = issue(
                "AI_SAFETY_DETECTED",
                IssueSource.OPENAI_MODERATION,
                IssueSeverity.MEDIUM
        );
        LlmReviewResponse response = new LlmReviewResponse();
        response.setReviewedExistingIssues(List.of(
                reviewed("PROFANITY_DETECTED", "PROFANITY_FILTER", "CONFIRMED"),
                reviewed("AI_SAFETY_DETECTED", "OPENAI_MODERATION", "NEEDS_REVIEW")
        ));
        response.setSuggestedRewrite("더 중립적인 안내 문구입니다.");
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(
                request(),
                List.of(profanityIssue, moderationIssue)
        );

        assertThat(result.existingIssues())
                .extracting(ValidationIssue::getSeverity)
                .containsExactly(IssueSeverity.HIGH, IssueSeverity.MEDIUM);
        assertThat(result.suggestedRewrite()).isEqualTo("더 중립적인 안내 문구입니다.");
    }

    private AiReviewRequest request() {
        AiReviewRequest request = new AiReviewRequest();
        request.setContextType(AiContextType.MESSAGE_SEND);
        request.setMessageType(MessageType.INFO);
        request.setChannels(List.of(ChannelType.LMS));
        request.setCustomerTags(List.of("NEW"));
        request.setTitle("혜택 안내");
        request.setContent("역대급 혜택을 확인하세요.");
        request.setAvailableVariables(List.of());
        return request;
    }

    private ValidationIssue issue(String ruleId, IssueSource source, IssueSeverity severity) {
        return new ValidationIssue(
                ruleId,
                source,
                severity,
                ValidationIssue.statusOf(severity),
                "content",
                "기존 검사 이슈입니다.",
                "대상 표현",
                "기존 제안",
                List.of()
        );
    }

    private LlmReviewResponse.NewIssue newIssue(
            String ruleId,
            String riskLevel,
            String field,
            String targetText,
            String message
    ) {
        LlmReviewResponse.NewIssue issue = new LlmReviewResponse.NewIssue();
        issue.setRuleId(ruleId);
        issue.setRiskLevel(riskLevel);
        issue.setField(field);
        issue.setTargetText(targetText);
        issue.setMessage(message);
        issue.setSuggestion("수정 제안");
        return issue;
    }

    private LlmReviewResponse.ReviewedExistingIssue reviewed(
            String ruleId,
            String source,
            String result
    ) {
        LlmReviewResponse.ReviewedExistingIssue review = new LlmReviewResponse.ReviewedExistingIssue();
        review.setRuleId(ruleId);
        review.setSource(source);
        review.setReviewResult(result);
        review.setReason("문맥 재검토 결과입니다.");
        review.setSuggestion("중립적인 표현을 사용하세요.");
        return review;
    }
}
