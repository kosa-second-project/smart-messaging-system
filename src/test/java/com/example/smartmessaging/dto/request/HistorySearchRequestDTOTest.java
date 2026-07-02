package com.example.smartmessaging.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HistorySearchRequestDTOTest {

    @Test
    void 정렬값이_null이면_최신순으로_정규화한다() {
        HistorySearchRequestDTO request = new HistorySearchRequestDTO();
        request.setSort(null);

        request.normalize();

        assertThat(request.getSort()).isEqualTo("latest");
    }

    @Test
    void 허용되지_않은_정렬값이면_최신순으로_정규화한다() {
        HistorySearchRequestDTO request = new HistorySearchRequestDTO();
        request.setSort("invalid-sort");

        request.normalize();

        assertThat(request.getSort()).isEqualTo("latest");
    }

    @Test
    void 일반_검색어는_앞뒤에_LIKE_와일드카드만_추가한다() {
        HistorySearchRequestDTO request = new HistorySearchRequestDTO();
        request.setKeyword(" 휴면 고객 ");

        request.normalize();

        assertThat(request.getKeyword()).isEqualTo("휴면 고객");
        assertThat(request.getKeywordLikePattern()).isEqualTo("%휴면 고객%");
    }

    @Test
    void LIKE_특수문자는_리터럴로_검색하도록_이스케이프한다() {
        HistorySearchRequestDTO request = new HistorySearchRequestDTO();
        request.setKeyword("50%_할인!");

        request.normalize();

        assertThat(request.getKeyword()).isEqualTo("50%_할인!");
        assertThat(request.getKeywordLikePattern()).isEqualTo("%50!%!_할인!!%");
    }
}
