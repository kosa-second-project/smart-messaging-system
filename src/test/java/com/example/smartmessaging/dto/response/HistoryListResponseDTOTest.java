package com.example.smartmessaging.dto.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryListResponseDTOTest {

    @Test
    void empty_tags_have_no_display_tags_or_hidden_count() {
        HistoryListResponseDTO response = response().withTags(List.of());

        assertThat(response.displayTags()).isEmpty();
        assertThat(response.hiddenTagCount()).isZero();
    }

    @Test
    void up_to_three_tags_are_displayed() {
        HistoryListResponseDTO response = response().withTags(List.of("10대", "30대", "sms 동의"));

        assertThat(response.displayTags()).containsExactly("10대", "30대", "sms 동의");
        assertThat(response.hiddenTagCount()).isZero();
    }

    @Test
    void more_than_three_tags_calculate_hidden_count() {
        HistoryListResponseDTO response = response().withTags(List.of("10대", "30대", "sms 동의", "휴면", "일반", "이메일 동의", "신규"));

        assertThat(response.displayTags()).containsExactly("10대", "30대", "sms 동의");
        assertThat(response.hiddenTagCount()).isEqualTo(4);
    }

    @Test
    void status_label_and_css_class_use_enum_mapping() {
        HistoryListResponseDTO response = new HistoryListResponseDTO(
                null, null, null, null, null, null, null, null, null, "SENT   "
        );

        assertThat(response.getStatusLabel()).isEqualTo("완료");
        assertThat(response.getStatusStyleClass()).isEqualTo("history-status--completed");
    }

    private HistoryListResponseDTO response() {
        return new HistoryListResponseDTO(
                null, null, null, null, null, null, null, null, null, null
        );
    }
}
