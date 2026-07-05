package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 욕설 검사 서비스를 stub 처리해 실제 외부 API 호출 없이 서버 룰과의 병합 결과를 검증한다.
 * 실제 API key를 사용하는 연동 확인은 구현 완료 후 Postman 또는 로컬 실행으로 별도 진행한다.
 */
class AiReviewServiceTest {

    private ProfanityValidationService profanityValidationService;
    private OpenAiModerationValidationService openAiModerationValidationService;
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        profanityValidationService = mock(ProfanityValidationService.class);
        openAiModerationValidationService = mock(OpenAiModerationValidationService.class);
        when(profanityValidationService.validate(anyString())).thenReturn(List.of());
        when(openAiModerationValidationService.validate(anyString())).thenReturn(List.of());
        aiReviewService = new AiReviewService(
                new RuleValidationService(),
                profanityValidationService,
                openAiModerationValidationService
        );
    }

    @Test
    void 욕설이_없으면_기존_서버_룰_결과만_반환한다() {
        AiReviewRequest request = request(MessageType.INFO, "배송이 완료되었습니다.");
        when(profanityValidationService.validate(request.getContent())).thenReturn(List.of());

        AiReviewResponse response = aiReviewService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.getIssues()).isEmpty();
        assertThat(response.isNeedsHumanReview()).isFalse();
    }

    @Test
    void 욕설_issue를_서버_룰_issue와_병합하고_FAIL로_결정한다() {
        AiReviewRequest request = request(MessageType.INFO, "문의는 test@example.com으로 주세요.");
        ValidationIssue profanityIssue = new ValidationIssue(
                "PROFANITY_DETECTED",
                IssueSeverity.HIGH,
                "본문에 부적절한 표현이 포함되어 있습니다.",
                "부적절어",
                "부적절한 표현을 제거하거나 완화된 표현으로 수정하세요."
        );
        when(profanityValidationService.validate(request.getContent()))
                .thenReturn(List.of(profanityIssue));

        AiReviewResponse response = aiReviewService.review(request);

        assertThat(response.getIssues())
                .extracting(ValidationIssue::getRuleId)
                .containsExactly("PERSONAL_EMAIL", "PROFANITY_DETECTED");
        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(response.getSummary()).isEqualTo("검사 결과 수정이 필요한 항목이 있습니다.");
        assertThat(response.isNeedsHumanReview()).isTrue();
    }

    @Test
    void filtered_응답과_관계없이_요청_원문을_변경하지_않는다() {
        AiReviewRequest request = request(MessageType.INFO, "사용자가 작성한 원문");
        when(profanityValidationService.validate(request.getContent())).thenReturn(List.of());

        aiReviewService.review(request);

        assertThat(request.getContent()).isEqualTo("사용자가 작성한 원문");
        verify(profanityValidationService).validate("사용자가 작성한 원문");
    }

    @Test
    void 외부_검사_결과가_없으면_기존_FAIL_결과를_그대로_유지한다() {
        AiReviewRequest request = request(MessageType.AD, "광고 안내");
        when(profanityValidationService.validate(request.getContent())).thenReturn(List.of());

        AiReviewResponse response = aiReviewService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(response.getIssues())
                .extracting(ValidationIssue::getRuleId)
                .containsExactly("MISSING_AD_PREFIX", "MISSING_OPT_OUT");
        assertThat(response.isNeedsHumanReview()).isTrue();
    }

    @Test
    void Moderation_경고를_병합하고_WARNING으로_결정한다() {
        AiReviewRequest request = request(MessageType.INFO, "공격적으로 해석될 수 있는 본문");
        ValidationIssue moderationIssue = new ValidationIssue(
                "AI_SAFETY_DETECTED",
                IssueSeverity.MEDIUM,
                "OpenAI Moderation 검사에서 유해 가능 표현이 감지되었습니다.",
                "harassment",
                "유해하거나 공격적으로 해석될 수 있는 표현을 완화해 주세요.",
                List.of("harassment")
        );
        when(openAiModerationValidationService.validate(request.getContent()))
                .thenReturn(List.of(moderationIssue));

        AiReviewResponse response = aiReviewService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.WARNING);
        assertThat(response.getIssues())
                .extracting(ValidationIssue::getRuleId)
                .containsExactly("AI_SAFETY_DETECTED");
        assertThat(response.isNeedsHumanReview()).isFalse();
    }

    @Test
    void 욕설_검사_다음에_Moderation_검사를_실행한다() {
        AiReviewRequest request = request(MessageType.INFO, "검사 순서 확인");

        aiReviewService.review(request);

        org.mockito.InOrder order = inOrder(
                profanityValidationService,
                openAiModerationValidationService
        );
        order.verify(profanityValidationService).validate(request.getContent());
        order.verify(openAiModerationValidationService).validate(request.getContent());
    }

    private AiReviewRequest request(MessageType messageType, String content) {
        AiReviewRequest request = new AiReviewRequest();
        request.setContextType(AiContextType.MESSAGE_SEND);
        request.setMessageType(messageType);
        request.setChannels(List.of(ChannelType.SMS));
        request.setCustomerTags(List.of());
        request.setTitle("안내");
        request.setContent(content);
        request.setAvailableVariables(List.of());
        return request;
    }
}
