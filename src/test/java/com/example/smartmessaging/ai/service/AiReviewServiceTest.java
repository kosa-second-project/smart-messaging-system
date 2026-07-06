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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiReviewServiceTest {

    private ProfanityValidationService profanityValidationService;
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        profanityValidationService = mock(ProfanityValidationService.class);
        aiReviewService = new AiReviewService(new RuleValidationService(), profanityValidationService);
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
