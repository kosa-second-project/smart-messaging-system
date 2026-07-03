package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RuleValidationServiceTest {

    private final RuleValidationService ruleValidationService = new RuleValidationService();

    @Test
    void 광고성_메시지에_필수_표기가_없으면_FAIL이다() {
        AiReviewRequest request = request(
                MessageType.AD,
                "#{고객명}님, 오늘만 20% 쿠폰 혜택을 확인해보세요.",
                List.of("#{고객명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("MISSING_AD_PREFIX", "MISSING_OPT_OUT");
        assertThat(response.isNeedsHumanReview()).isTrue();
    }

    @Test
    void 정보성_메시지에_광고_키워드가_있으면_WARNING이다() {
        AiReviewRequest request = request(
                MessageType.INFO,
                "#{고객명}님, 20% 쿠폰 할인 혜택을 확인해보세요.",
                List.of("#{고객명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.WARNING);
        assertThat(ruleIds(response)).containsExactly("AD_SUSPECTED");
        assertThat(response.getSummary()).isEqualTo("검사 결과 주의가 필요한 항목이 있습니다.");
    }

    @Test
    void 잘못된_변수_형식은_FAIL이다() {
        AiReviewRequest request = request(
                MessageType.INFO,
                "{고객명}님, 배송이 완료되었습니다.",
                List.of("#{고객명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("INVALID_VARIABLE_FORMAT");
        assertThat(response.getIssues().get(0).getTargetText()).isEqualTo("{고객명}");
    }

    @Test
    void availableVariables에_없는_변수는_FAIL이다() {
        AiReviewRequest request = request(
                MessageType.AD,
                "(광고) #{고객명}님, #{쿠폰명} 확인하세요. 무료수신거부 080-000-0000",
                List.of("#{고객명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
        assertThat(response.getIssues().get(0).getTargetText()).isEqualTo("#{쿠폰명}");
    }

    @Test
    void 허용된_임시_변수는_PASS이다() {
        AiReviewRequest request = request(
                MessageType.AD,
                "(광고) #{고객명}님, #{쿠폰명} 확인하세요. 무료수신거부 080-000-0000",
                List.of("#{고객명}", "#{쿠폰명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.getIssues()).isEmpty();
        assertThat(response.getSummary()).isEqualTo("검사 결과 문제가 발견되지 않았습니다.");
    }

    @Test
    void 프로젝트에_정의되지_않은_변수는_availableVariables에_있어도_FAIL이다() {
        AiReviewRequest request = request(
                MessageType.AD,
                "(광고) #{고객명}님, #{만료일} 확인하세요. 무료수신거부 080-000-0000",
                List.of("#{고객명}", "#{만료일}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
        assertThat(response.getIssues().get(0).getTargetText()).isEqualTo("#{만료일}");
    }

    @Test
    void 개인정보_패턴을_각각_검출한다() {
        AiReviewRequest request = request(
                MessageType.INFO,
                "연락처 010-1234-5678, 이메일 test@example.com, 주민번호 900101-1234567",
                List.of()
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly(
                "PERSONAL_PHONE_NUMBER",
                "PERSONAL_EMAIL",
                "PERSONAL_RRN"
        );
    }

    @Test
    void 정상_광고성_메시지는_PASS이다() {
        AiReviewRequest request = request(
                MessageType.AD,
                " (광고) #{고객명}님, 여름 쿠폰 혜택을 확인해보세요. 무료수신거부 080-000-0000 ",
                List.of("#{고객명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.isNeedsHumanReview()).isFalse();
        assertThat(response.getSuggestedRewrite()).isNull();
    }

    @Test
    void 정상_정보성_메시지는_PASS이다() {
        AiReviewRequest request = request(
                MessageType.INFO,
                "#{고객명}님, 주문하신 상품이 출고되었습니다.",
                List.of("#{고객명}")
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.getIssues()).isEmpty();
    }

    @Test
    void 사용_가능한_변수_목록이_비어_있으면_본문의_변수를_허용하지_않는다() {
        AiReviewRequest request = request(
                MessageType.INFO,
                "#{고객명}님, 주문하신 상품이 출고되었습니다.",
                null
        );

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
    }

    @Test
    void 검사_요청이_null이면_커스텀_예외가_발생한다() {
        assertInvalidRequest(null, "검사 요청은 필수입니다.");
    }

    @Test
    void 메시지_유형이_null이면_커스텀_예외가_발생한다() {
        AiReviewRequest request = request(MessageType.INFO, "배송이 완료되었습니다.", List.of());
        request.setMessageType(null);

        assertInvalidRequest(request, "메시지 유형은 필수입니다.");
    }

    @Test
    void 본문이_null이면_커스텀_예외가_발생한다() {
        AiReviewRequest request = request(MessageType.INFO, null, List.of());

        assertInvalidRequest(request, "검사할 메시지 내용은 필수입니다.");
    }

    @Test
    void 본문이_blank이면_커스텀_예외가_발생한다() {
        AiReviewRequest request = request(MessageType.INFO, "   \n\t", List.of());

        assertInvalidRequest(request, "검사할 메시지 내용은 필수입니다.");
    }

    @Test
    void templateId가_null이어도_정상_요청은_PASS이다() {
        AiReviewRequest request = request(
                MessageType.INFO,
                "#{고객명}님, 주문하신 상품이 출고되었습니다.",
                List.of("#{고객명}")
        );
        request.setTemplateId(null);

        AiReviewResponse response = ruleValidationService.review(request);

        assertThat(response.getStatus()).isEqualTo(ReviewStatus.PASS);
    }

    private AiReviewRequest request(MessageType messageType, String content, List<String> availableVariables) {
        AiReviewRequest request = new AiReviewRequest();
        request.setContextType(AiContextType.MESSAGE_SEND);
        request.setMessageType(messageType);
        request.setChannels(List.of(ChannelType.SMS, ChannelType.LMS));
        request.setCustomerTags(List.of());
        request.setTitle("안내");
        request.setContent(content);
        request.setAvailableVariables(availableVariables);
        return request;
    }

    private void assertInvalidRequest(AiReviewRequest request, String message) {
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> ruleValidationService.review(request))
                .withMessage(message)
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_REVIEW_INVALID_REQUEST)
                );
    }

    private List<String> ruleIds(AiReviewResponse response) {
        return response.getIssues().stream()
                .map(ValidationIssue::getRuleId)
                .toList();
    }
}
