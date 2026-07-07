package com.example.smartmessaging.dto.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistorySearchRequestDTOTest {

    @Test
    void null_sort_is_normalized_to_latest() {
        HistorySearchRequestDTO request = request(null, null).normalized();

        assertThat(request.sort()).isEqualTo("latest");
    }

    @Test
    void invalid_sort_is_normalized_to_latest() {
        HistorySearchRequestDTO request = request(null, "invalid-sort").normalized();

        assertThat(request.sort()).isEqualTo("latest");
    }

    @Test
    void keyword_is_trimmed_and_wrapped_as_like_pattern() {
        HistorySearchRequestDTO request = request(" 휴면 고객 ", null).normalized();

        assertThat(request.keyword()).isEqualTo("휴면 고객");
        assertThat(request.keywordLikePattern()).isEqualTo("%휴면 고객%");
    }

    @Test
    void keyword_like_pattern_escapes_special_characters() {
        HistorySearchRequestDTO request = request("50%_할인!", null).normalized();

        assertThat(request.keyword()).isEqualTo("50%_할인!");
        assertThat(request.keywordLikePattern()).isEqualTo("%50!%!_할인!!%");
    }

    @Test
    void mybatis_compatible_getters_return_calculated_values() {
        HistorySearchRequestDTO request = new HistorySearchRequestDTO(
                "이벤트",
                null,
                null,
                null,
                List.of(1L, 2L),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                "latest",
                2
        );

        assertThat(request.getStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(request.getEndAtExclusive()).isEqualTo(LocalDateTime.of(2026, 7, 3, 0, 0));
        assertThat(request.getOffset()).isEqualTo(10);
        assertThat(request.getPageSize()).isEqualTo(10);
        assertThat(request.getTagCount()).isEqualTo(2);
        assertThat(request.getKeywordLikePattern()).isEqualTo("%이벤트%");
    }

    private HistorySearchRequestDTO request(String keyword, String sort) {
        return new HistorySearchRequestDTO(
                keyword,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                sort,
                null
        );
    }
}
