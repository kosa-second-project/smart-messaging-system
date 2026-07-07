package com.example.smartmessaging.dto.response;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryDetailResponseDTOTest {

    @Test
    void display_success_rate_is_rounded_to_one_decimal_place() {
        HistoryDetailResponseDTO response = response(new BigDecimal("99.95"), null);

        assertThat(response.getDisplaySuccessRate()).isEqualByComparingTo("100.0");
    }

    @Test
    void null_success_rate_is_displayed_as_zero() {
        HistoryDetailResponseDTO response = response(null, null);

        assertThat(response.getDisplaySuccessRate()).isEqualByComparingTo("0.0");
    }

    @Test
    void detail_tags_are_kept_without_display_limit() {
        HistoryDetailResponseDTO response = response(null, null)
                .withTags(List.of("태그1", "태그2", "태그3", "태그4"));

        assertThat(response.tags()).containsExactly("태그1", "태그2", "태그3", "태그4");
    }

    @Test
    void detail_status_label_uses_enum_mapping() {
        HistoryDetailResponseDTO response = response(null, "FAILED");

        assertThat(response.getStatusLabel()).isEqualTo("실패");
    }

    private HistoryDetailResponseDTO response(BigDecimal successRate, String status) {
        return new HistoryDetailResponseDTO(
                null,
                null,
                null,
                null,
                null,
                status,
                null,
                null,
                null,
                null,
                null,
                successRate
        );
    }
}
