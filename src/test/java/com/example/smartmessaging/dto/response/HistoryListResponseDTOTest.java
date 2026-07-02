package com.example.smartmessaging.dto.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryListResponseDTOTest {

    @Test
    void 태그가_없으면_표시태그와_숨김개수가_비어있다() {
        HistoryListResponseDTO response = new HistoryListResponseDTO();
        response.setTags(List.of());

        assertThat(response.getDisplayTags()).isEmpty();
        assertThat(response.getHiddenTagCount()).isZero();
    }

    @Test
    void 태그가_세개이하면_모두_표시한다() {
        HistoryListResponseDTO response = new HistoryListResponseDTO();
        response.setTags(List.of("10대", "30대", "sms 동의"));

        assertThat(response.getDisplayTags()).containsExactly("10대", "30대", "sms 동의");
        assertThat(response.getHiddenTagCount()).isZero();
    }

    @Test
    void 태그가_네개이상이면_세개만_표시하고_나머지개수를_계산한다() {
        HistoryListResponseDTO response = new HistoryListResponseDTO();
        response.setTags(List.of("10대", "30대", "sms 동의", "휴면", "일반", "이메일 동의", "신규"));

        assertThat(response.getDisplayTags()).containsExactly("10대", "30대", "sms 동의");
        assertThat(response.getHiddenTagCount()).isEqualTo(4);
    }
}
