package com.example.smartmessaging.dto.response;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryDetailResponseDTOTest {

    @Test
    void 최종도달률은_소수점_첫째자리까지_반올림한다() {
        HistoryDetailResponseDTO response = new HistoryDetailResponseDTO();
        response.setSuccessRate(new BigDecimal("99.95"));

        assertThat(response.getDisplaySuccessRate()).isEqualByComparingTo("100.0");
    }

    @Test
    void 최종도달률이_없으면_영점으로_표시한다() {
        HistoryDetailResponseDTO response = new HistoryDetailResponseDTO();

        assertThat(response.getDisplaySuccessRate()).isEqualByComparingTo("0.0");
    }

    @Test
    void 상세태그는_개수제한없이_모두_유지한다() {
        HistoryDetailResponseDTO response = new HistoryDetailResponseDTO();
        response.setTags(List.of("태그1", "태그2", "태그3", "태그4"));

        assertThat(response.getTags()).containsExactly("태그1", "태그2", "태그3", "태그4");
    }

    @Test
    void 상세응답도_enum_기준의_전송상태_라벨을_사용한다() {
        HistoryDetailResponseDTO response = new HistoryDetailResponseDTO();
        response.setStatus("FAILED");

        assertThat(response.getStatusLabel()).isEqualTo("실패");
    }
}
