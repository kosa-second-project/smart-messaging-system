package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.GeminiReviewClient;
import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.response.LlmReviewResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService.RagPromptContext;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService.RagReferenceLog;
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
        ValidationIssueResponseDTO serverIssue = issue(
                "UNSUPPORTED_VARIABLE",
                IssueSource.SERVER_RULE,
                IssueSeverity.HIGH
        );
        LlmReviewResponseDTO response = new LlmReviewResponseDTO(List.of(), List.of(), null);
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
                .contains("UNSUPPORTED_VARIABLE")
                .contains("SERVER_RULE");
    }

    @Test
    void 신규_이슈는_허용_규칙과_기본_severity로_보정하고_중복을_제거한다() {
        LlmReviewResponseDTO response = new LlmReviewResponseDTO(null, List.of(
                newIssue("OVERSTATED_BENEFIT", "LOW", "content", "역대급", "과장 표현입니다."),
                newIssue("OVERSTATED_BENEFIT", "LOW", "content", "역대급", "중복입니다."),
                newIssue("CLARITY_ISSUE", "HIGH", "invalid", null, "표현이 모호합니다."),
                newIssue("UNKNOWN_RULE", "HIGH", "content", null, "알 수 없는 규칙입니다.")
        ), null);
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(request(), List.of());

        assertThat(result.newIssues()).hasSize(2);
        ValidationIssueResponseDTO overstated = result.newIssues().get(0);
        assertThat(overstated.source()).isEqualTo(IssueSource.LLM_REVIEW);
        assertThat(overstated.severity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(overstated.status()).isEqualTo(ReviewStatus.WARNING);
        ValidationIssueResponseDTO clarity = result.newIssues().get(1);
        assertThat(clarity.severity()).isEqualTo(IssueSeverity.MEDIUM);
        assertThat(clarity.field()).isEqualTo("content");
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
        LlmReviewResponseDTO response = new LlmReviewResponseDTO(null, List.of(
                newIssue(ruleId, "INVALID", "title", null, "문맥 검사 이슈입니다.")
        ), null);
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(request(), List.of());

        assertThat(result.newIssues()).singleElement().satisfies(issue -> {
            assertThat(issue.ruleId()).isEqualTo(ruleId);
            assertThat(issue.severity()).isEqualTo(expectedSeverity);
            assertThat(issue.status()).isEqualTo(ValidationIssueResponseDTO.statusOf(expectedSeverity));
            assertThat(issue.field()).isEqualTo("title");
        });
    }

    @Test
    void 광고_표기와_수신거부_관련_LLM_ruleId는_허용_규칙에서_제외된다() {
        LlmReviewResponseDTO response = new LlmReviewResponseDTO(null, List.of(
                newIssue("UNSUBSCRIBE_MISSING", "MEDIUM", "content", "수신거부", "수신거부가 없습니다."),
                newIssue("AD_LABEL_MISSING", "MEDIUM", "content", "(광고)", "광고 표기가 없습니다."),
                newIssue("CLARITY_ISSUE", "LOW", "content", "혜택", "혜택 조건이 모호합니다.")
        ), null);
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(request(), List.of());

        assertThat(result.newIssues())
                .extracting(ValidationIssueResponseDTO::ruleId)
                .containsExactly("CLARITY_ISSUE");
    }

    @Test
    void 오탐_가능성은_외부_검사만_한_단계_완화하고_서버_룰은_변경하지_않는다() {
        ValidationIssueResponseDTO serverIssue = issue(
                "UNSUPPORTED_VARIABLE",
                IssueSource.SERVER_RULE,
                IssueSeverity.HIGH
        );
        ValidationIssueResponseDTO profanityIssue = issue(
                "PROFANITY_DETECTED",
                IssueSource.PROFANITY_FILTER,
                IssueSeverity.HIGH
        );
        ValidationIssueResponseDTO moderationIssue = issue(
                "AI_SAFETY_DETECTED",
                IssueSource.OPENAI_MODERATION,
                IssueSeverity.MEDIUM
        );

        LlmReviewResponseDTO response = new LlmReviewResponseDTO(List.of(
                reviewed("UNSUPPORTED_VARIABLE", "SERVER_RULE", "POSSIBLE_FALSE_POSITIVE"),
                reviewed("PROFANITY_DETECTED", "PROFANITY_FILTER", "POSSIBLE_FALSE_POSITIVE"),
                reviewed("AI_SAFETY_DETECTED", "OPENAI_MODERATION", "POSSIBLE_FALSE_POSITIVE")
        ), null, null);
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(
                request(),
                List.of(serverIssue, profanityIssue, moderationIssue)
        );

        // SERVER_RULE은 HIGH 유지, 필터/Moderation만 POSSIBLE_FALSE_POSITIVE일 때 한 단계 완화된다.
        assertThat(result.existingIssues())
                .extracting(ValidationIssueResponseDTO::severity)
                .containsExactly(IssueSeverity.HIGH, IssueSeverity.MEDIUM, IssueSeverity.LOW);
        assertThat(result.existingIssues().get(0).detail()).isEmpty();
        assertThat(result.existingIssues().get(1).detail())
                .contains("llmReviewResult=POSSIBLE_FALSE_POSITIVE", "llmReason=문맥 재검토 결과입니다.");
        assertThat(result.existingIssues().get(1).suggestion()).isEqualTo("중립적인 표현을 사용하세요.");
    }

    @Test
    void CONFIRMED와_NEEDS_REVIEW는_기존_severity를_유지하고_수정안을_반환한다() {
        ValidationIssueResponseDTO profanityIssue = issue(
                "PROFANITY_DETECTED",
                IssueSource.PROFANITY_FILTER,
                IssueSeverity.HIGH
        );
        ValidationIssueResponseDTO moderationIssue = issue(
                "AI_SAFETY_DETECTED",
                IssueSource.OPENAI_MODERATION,
                IssueSeverity.MEDIUM
        );
        LlmReviewResponseDTO response = new LlmReviewResponseDTO(List.of(
                reviewed("PROFANITY_DETECTED", "PROFANITY_FILTER", "CONFIRMED"),
                reviewed("AI_SAFETY_DETECTED", "OPENAI_MODERATION", "NEEDS_REVIEW")
        ), null, "더 중립적인 안내 문구입니다.");
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(
                request(),
                List.of(profanityIssue, moderationIssue)
        );

        assertThat(result.existingIssues())
                .extracting(ValidationIssueResponseDTO::severity)
                .containsExactly(IssueSeverity.HIGH, IssueSeverity.MEDIUM);
        assertThat(result.suggestedRewrite()).isEqualTo("더 중립적인 안내 문구입니다.");
    }

    @Test
    void RAG_context_isIncludedInLlmUserPrompt() {
        RagPromptContextService ragPromptContextService = mock(RagPromptContextService.class);
        LlmReviewService serviceWithRag = new LlmReviewService(
                geminiReviewClient,
                new ObjectMapper(),
                ragPromptContextService
        );
        // RAG는 LLM user prompt의 참고자료로만 들어가고, 별도 응답 DTO로 사용자에게 노출하지 않는다.
        when(ragPromptContextService.buildReviewPromptContext(request()))
                .thenReturn(new RagPromptContext(
                        "\n[RAG Reference Materials]\n- brand tone reference\n",
                        List.of(new RagReferenceLog(
                                "hmall-campaign-001",
                                0.82,
                                "campaign_copy",
                                List.of("coupon"),
                                List.of("tone_reference"),
                                List.of("100%"),
                                "Hmall coupon reference preview"
                        ))
                ));
        when(geminiReviewClient.review(anyString(), anyString()))
                .thenReturn(new LlmReviewResponseDTO(List.of(), List.of(), null));

        serviceWithRag.review(request(), List.of());

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(geminiReviewClient).review(anyString(), userPrompt.capture());
        assertThat(userPrompt.getValue())
                .contains("\"ragContext\"")
                .contains("[RAG Reference Materials]")
                .contains("brand tone reference");
    }

    @Test
    void review_system_prompt_says_ad_label_and_unsubscribe_are_not_review_targets() {
        assertThat(LlmReviewService.SYSTEM_PROMPT)
                .contains("광고 표기 '(광고)'와 수신거부 문구의 존재 여부 또는 누락 여부는 검사하지 마십시오")
                .contains("실제 발송 보조 표기는 화면/발송 단계에서 별도로 처리됩니다");
    }

    @Test
    void presentation_related_llm_rule_ids_are_ignored() {
        LlmReviewResponseDTO response = new LlmReviewResponseDTO(null, List.of(
                newIssue("UNSUBSCRIBE_MISSING", "MEDIUM", "content", "수신거부", "수신거부가 없습니다."),
                newIssue("AD_LABEL_MISSING", "MEDIUM", "content", "(광고)", "광고 표기가 없습니다."),
                newIssue("CLARITY_ISSUE", "LOW", "content", "혜택", "혜택 조건이 모호합니다.")
        ), null);
        when(geminiReviewClient.review(anyString(), anyString())).thenReturn(response);

        LlmReviewService.ReviewResult result = llmReviewService.review(request(), List.of());

        assertThat(result.newIssues())
                .extracting(ValidationIssueResponseDTO::ruleId)
                .containsExactly("CLARITY_ISSUE");
    }

    private AiReviewRequestDTO request() {
        return new AiReviewRequestDTO(
                AiContextType.MESSAGE_SEND,
                MessageType.INFO,
                List.of(ChannelType.LMS),
                List.of("NEW"),
                "혜택 안내",
                "역대급 혜택을 확인하세요.",
                List.of(),
                null,
                null,
                null
        );
    }

    private ValidationIssueResponseDTO issue(String ruleId, IssueSource source, IssueSeverity severity) {
        return new ValidationIssueResponseDTO(
                ruleId,
                source,
                severity,
                ValidationIssueResponseDTO.statusOf(severity),
                "content",
                "기존 검사 이슈입니다.",
                "대상 표현",
                "기존 제안",
                List.of()
        );
    }

    private LlmReviewResponseDTO.NewIssue newIssue(
            String ruleId,
            String riskLevel,
            String field,
            String targetText,
            String message
    ) {
        return new LlmReviewResponseDTO.NewIssue(
                ruleId,
                riskLevel,
                field,
                targetText,
                message,
                "수정 제안"
        );
    }

    private LlmReviewResponseDTO.ReviewedExistingIssue reviewed(
            String ruleId,
            String source,
            String result
    ) {
        return new LlmReviewResponseDTO.ReviewedExistingIssue(
                ruleId,
                source,
                result,
                "문맥 재검토 결과입니다.",
                "중립적인 표현을 사용하세요."
        );
    }
}
