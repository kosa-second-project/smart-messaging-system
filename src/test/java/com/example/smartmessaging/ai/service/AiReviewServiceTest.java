package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.response.AiReviewResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 욕설 검사 서비스를 stub 처리해 실제 외부 API 호출 없이 서버 룰과의 병합 결과를 검증한다.
 * 실제 API key를 사용하는 연동 확인은 구현 완료 후 Postman 또는 로컬 실행으로 별도 진행한다.
 */
class AiReviewServiceTest {

    private ProfanityValidationService profanityValidationService;
    private OpenAiModerationValidationService openAiModerationValidationService;
    private LlmReviewService llmReviewService;
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        profanityValidationService = mock(ProfanityValidationService.class);
        openAiModerationValidationService = mock(OpenAiModerationValidationService.class);
        llmReviewService = mock(LlmReviewService.class);
        when(profanityValidationService.validate(anyString())).thenReturn(List.of());
        when(openAiModerationValidationService.validate(anyString())).thenReturn(List.of());
        when(llmReviewService.review(any(), anyList())).thenAnswer(invocation ->
                new LlmReviewService.ReviewResult(invocation.getArgument(1), List.of(), null));
        aiReviewService = new AiReviewService(
                new RuleValidationService(),
                profanityValidationService,
                openAiModerationValidationService,
                llmReviewService
        );
    }

    @Test
    void 욕설이_없으면_기존_서버_룰_결과만_반환한다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "배송이 완료되었습니다.");
        when(profanityValidationService.validate(request.content())).thenReturn(List.of());

        AiReviewResponseDTO response = aiReviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
        assertThat(response.needsHumanReview()).isFalse();
    }

    @Test
    void 욕설_issue를_서버_룰_issue와_병합하고_FAIL로_결정한다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "문의는 test@example.com으로 주세요.");
        ValidationIssueResponseDTO profanityIssue = new ValidationIssueResponseDTO(
                "PROFANITY_DETECTED",
                IssueSeverity.HIGH,
                "본문에 부적절한 표현이 포함되어 있습니다.",
                "부적절어",
                "부적절한 표현을 제거하거나 완화된 표현으로 수정하세요."
        );
        when(profanityValidationService.validate(request.content()))
                .thenReturn(List.of(profanityIssue));

        AiReviewResponseDTO response = aiReviewService.review(request);

        assertThat(response.issues())
                .extracting(ValidationIssueResponseDTO::ruleId)
                .containsExactly("PERSONAL_EMAIL", "PROFANITY_DETECTED");
        assertThat(response.issues())
                .extracting(ValidationIssueResponseDTO::source)
                .containsExactly(IssueSource.SERVER_RULE, IssueSource.PROFANITY_FILTER);
        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(response.summary()).isEqualTo("검사 결과 수정이 필요한 항목이 있습니다.");
        assertThat(response.needsHumanReview()).isTrue();
    }

    @Test
    void filtered_응답과_관계없이_요청_원문을_변경하지_않는다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "사용자가 작성한 원문");
        when(profanityValidationService.validate(request.content())).thenReturn(List.of());

        aiReviewService.review(request);

        assertThat(request.content()).isEqualTo("사용자가 작성한 원문");
        verify(profanityValidationService).validate("사용자가 작성한 원문");
    }

    @Test
    void 외부_검사_결과가_없으면_기존_FAIL_결과를_그대로_유지한다() {
        AiReviewRequestDTO request = request(MessageType.AD, "광고 안내");
        when(profanityValidationService.validate(request.content())).thenReturn(List.of());

        AiReviewResponseDTO response = aiReviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
        assertThat(response.needsHumanReview()).isFalse();
    }

    @Test
    void Moderation_경고를_병합하고_WARNING으로_결정한다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "공격적으로 해석될 수 있는 본문");
        ValidationIssueResponseDTO moderationIssue = new ValidationIssueResponseDTO(
                "AI_SAFETY_DETECTED",
                IssueSeverity.MEDIUM,
                "OpenAI Moderation 검사에서 유해 가능 표현이 감지되었습니다.",
                "harassment",
                "유해하거나 공격적으로 해석될 수 있는 표현을 완화해 주세요.",
                List.of("harassment")
        );
        when(openAiModerationValidationService.validate(request.content()))
                .thenReturn(List.of(moderationIssue));

        AiReviewResponseDTO response = aiReviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.WARNING);
        assertThat(response.issues())
                .extracting(ValidationIssueResponseDTO::ruleId)
                .containsExactly("AI_SAFETY_DETECTED");
        assertThat(response.needsHumanReview()).isFalse();
    }

    @Test
    void 욕설_검사_다음에_Moderation_검사를_실행한다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "검사 순서 확인");

        aiReviewService.review(request);

        org.mockito.InOrder order = inOrder(
                profanityValidationService,
                openAiModerationValidationService,
                llmReviewService
        );
        order.verify(profanityValidationService).validate(request.content());
        order.verify(openAiModerationValidationService).validate(request.content());
        order.verify(llmReviewService).review(any(), anyList());
    }

    @Test
    void LLM_신규_이슈와_수정안을_최종_응답에_병합한다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "역대급 혜택을 확인하세요.");
        ValidationIssueResponseDTO llmIssue = new ValidationIssueResponseDTO(
                "OVERSTATED_BENEFIT",
                IssueSource.LLM_REVIEW,
                IssueSeverity.MEDIUM,
                ReviewStatus.WARNING,
                "content",
                "과장된 혜택 표현입니다.",
                "역대급 혜택",
                "혜택 조건을 구체적으로 안내해 주세요.",
                List.of()
        );
        when(llmReviewService.review(any(), anyList())).thenAnswer(invocation ->
                new LlmReviewService.ReviewResult(
                        invocation.getArgument(1),
                        List.of(llmIssue),
                        "혜택 조건을 확인해보세요."
                ));

        AiReviewResponseDTO response = aiReviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.WARNING);
        assertThat(response.issues()).containsExactly(llmIssue);
        assertThat(response.suggestedRewrite()).isEqualTo("혜택 조건을 확인해보세요.");
    }

    @Test
    void LLM_호출이_실패하면_기존_결과와_가용성_이슈를_반환한다() {
        AiReviewRequestDTO request = request(MessageType.AD, "광고 안내");
        when(llmReviewService.review(any(), anyList()))
                .thenThrow(new IllegalStateException("LLM unavailable"));

        AiReviewResponseDTO response = aiReviewService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.NOTICE);
        assertThat(response.issues())
                .extracting(ValidationIssueResponseDTO::ruleId)
                .containsExactly("LLM_REVIEW_UNAVAILABLE");
        ValidationIssueResponseDTO unavailable = response.issues().get(0);
        assertThat(unavailable.source()).isEqualTo(IssueSource.LLM_REVIEW);
        assertThat(unavailable.severity()).isEqualTo(IssueSeverity.LOW);
        assertThat(unavailable.status()).isEqualTo(ReviewStatus.NOTICE);
        assertThat(unavailable.field()).isNull();
    }

    @Test
    void 필수값이_없으면_외부_검사를_호출하지_않는다() {
        AiReviewRequestDTO request = request(MessageType.INFO, "배송이 완료되었습니다.");
        request = new AiReviewRequestDTO(
                request.contextType(),
                request.messageType(),
                request.channels(),
                request.customerTags(),
                "  ",
                request.content(),
                request.availableVariables(),
                request.category(),
                request.templateId(),
                request.userId()
        );

        AiReviewRequestDTO invalidRequest = request;

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> aiReviewService.review(invalidRequest));

        verifyNoInteractions(
                profanityValidationService,
                openAiModerationValidationService,
                llmReviewService
        );
    }

    private AiReviewRequestDTO request(MessageType messageType, String content) {
        return new AiReviewRequestDTO(
                AiContextType.MESSAGE_SEND,
                messageType,
                List.of(ChannelType.SMS),
                List.of(),
                "안내",
                content,
                List.of(),
                null,
                null,
                null
        );
    }
}
