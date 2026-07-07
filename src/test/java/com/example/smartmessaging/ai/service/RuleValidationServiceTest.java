package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.response.AiReviewResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleValidationServiceTest {

    private final RuleValidationService ruleValidationService = new RuleValidationService();

    @Test
    void ad_message_without_required_ad_phrases_passes_server_rule_review() {
        AiReviewRequestDTO request = request(
                MessageType.AD,
                "#{고객명}님, 오늘만 20% 쿠폰 혜택을 확인해보세요.",
                List.of("#{고객명}")
        );

        AiReviewResponseDTO response = ruleValidationService.review(request);

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
        assertThat(response.needsHumanReview()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "구매하신 상품이 출고되었습니다.",
            "오늘만 할인 혜택을 확인하세요."
    })
    void info_message_promotional_context_is_not_decided_by_server_rules(String content) {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, content, List.of())
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
    }

    @Test
    void invalid_variable_format_fails() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, "{고객명}님, 배송이 완료되었습니다.", List.of("#{고객명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("INVALID_VARIABLE_FORMAT");
        assertThat(response.issues().get(0).targetText()).isEqualTo("{고객명}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{고객명}", "${주문번호}", "#쿠폰명", "[고객명]"})
    void alternate_variable_notation_fails(String content) {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, content, List.of("#{고객명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("INVALID_VARIABLE_FORMAT");
        assertThat(response.issues().get(0).targetText()).isEqualTo(content);
    }

    @ParameterizedTest
    @ValueSource(strings = {"#{고객명}", "#{주문번호}", "#{쿠폰명}"})
    void supported_project_variables_pass_when_allowed(String variable) {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, variable, List.of(variable))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"#{고객명", "#{}", "#{고객명 }"})
    void incomplete_or_invalid_variable_format_fails(String content) {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, content, List.of("#{고객명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("INVALID_VARIABLE_FORMAT");
        assertThat(response.issues().get(0).targetText()).isEqualTo(content);
    }

    @Test
    void ordinary_hashtag_and_brackets_are_not_variable_errors() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, "#여름세일 [이벤트] 안내", List.of())
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"#{이벤트명}", "#{만료일}"})
    void unknown_variables_fail_even_with_valid_format(String variable) {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, variable, List.of(variable))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
        assertThat(response.issues().get(0).targetText()).isEqualTo(variable);
    }

    @Test
    void variable_not_in_available_variables_fails() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.AD, "#{고객명}님, #{쿠폰명} 확인하세요.", List.of("#{고객명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
        assertThat(response.issues().get(0).targetText()).isEqualTo("#{쿠폰명}");
    }

    @Test
    void allowed_displayed_variable_passes() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.AD, "#{고객명}님, #{쿠폰명} 확인하세요.", List.of("#{고객명}", "#{쿠폰명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
    }

    @Test
    void project_undefined_variable_fails_even_if_available_variables_contains_it() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.AD, "#{고객명}님, #{만료일} 확인하세요.", List.of("#{고객명}", "#{만료일}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
        assertThat(response.issues().get(0).targetText()).isEqualTo("#{만료일}");
    }

    @Test
    void personal_information_patterns_are_detected() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(
                        MessageType.INFO,
                        "연락처 010-1234-5678, 이메일 test@example.com, 주민번호 900101-1234567",
                        List.of()
                )
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly(
                "PERSONAL_PHONE_NUMBER",
                "PERSONAL_EMAIL",
                "PERSONAL_RRN"
        );
    }

    @Test
    void normal_ad_message_passes_without_suggested_rewrite() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.AD, " #{고객명}님, 여름 쿠폰 혜택을 확인해보세요. ", List.of("#{고객명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.needsHumanReview()).isFalse();
        assertThat(response.suggestedRewrite()).isNull();
    }

    @Test
    void normal_info_message_passes() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, "#{고객명}님, 주문하신 상품이 출고되었습니다.", List.of("#{고객명}"))
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.PASS);
        assertThat(response.issues()).isEmpty();
    }

    @Test
    void empty_available_variables_do_not_allow_variables_in_content() {
        AiReviewResponseDTO response = ruleValidationService.review(
                request(MessageType.INFO, "#{고객명}님, 주문하신 상품이 출고되었습니다.", List.of())
        );

        assertThat(response.status()).isEqualTo(ReviewStatus.FAIL);
        assertThat(ruleIds(response)).containsExactly("UNSUPPORTED_VARIABLE");
    }

    private AiReviewRequestDTO request(MessageType messageType, String content, List<String> availableVariables) {
        return new AiReviewRequestDTO(
                AiContextType.MESSAGE_SEND,
                messageType,
                List.of(ChannelType.LMS),
                List.of(),
                "제목",
                content,
                availableVariables,
                null,
                null,
                null
        );
    }

    private List<String> ruleIds(AiReviewResponseDTO response) {
        return response.issues().stream()
                .map(ValidationIssueResponseDTO::ruleId)
                .toList();
    }
}
