package com.example.smartmessaging.dto.type;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryTypeTest {

    @Test
    void 정렬_요청값을_enum으로_변환한다() {
        assertThat(HistorySortType.fromValueOrDefault("latest")).isEqualTo(HistorySortType.LATEST);
        assertThat(HistorySortType.fromValueOrDefault("oldest")).isEqualTo(HistorySortType.OLDEST);
        assertThat(HistorySortType.fromValueOrDefault("mostSent")).isEqualTo(HistorySortType.MOST_SENT);
        assertThat(HistorySortType.fromValueOrDefault("highestSuccessRate"))
                .isEqualTo(HistorySortType.HIGHEST_SUCCESS_RATE);
    }

    @Test
    void null이거나_허용되지_않은_정렬값은_최신순으로_변환한다() {
        assertThat(HistorySortType.fromValueOrDefault(null)).isEqualTo(HistorySortType.LATEST);
        assertThat(HistorySortType.fromValueOrDefault("invalid-sort")).isEqualTo(HistorySortType.LATEST);
    }

    @Test
    void 전송기록_상태는_DB_CHECK_제약조건의_값과_일치한다() {
        assertThat(Arrays.stream(SendHistoryStatus.values()).map(SendHistoryStatus::getValue))
                .containsExactly("SCHEDULED", "SENDING", "SENT", "FAILED");
    }

    @Test
    void 전송기록_상태별_라벨과_CSS_클래스를_반환한다() {
        assertThat(SendHistoryStatus.labelOf("SCHEDULED")).isEqualTo("예약");
        assertThat(SendHistoryStatus.labelOf("SENDING")).isEqualTo("전송중");
        assertThat(SendHistoryStatus.labelOf("SENT")).isEqualTo("완료");
        assertThat(SendHistoryStatus.labelOf("FAILED")).isEqualTo("실패");
        assertThat(SendHistoryStatus.styleClassOf("SENT")).isEqualTo("history-status--completed");
    }

    @Test
    void 알_수_없는_상태는_원본_라벨과_기본_CSS_클래스를_사용한다() {
        assertThat(SendHistoryStatus.labelOf(null)).isEqualTo("-");
        assertThat(SendHistoryStatus.labelOf("UNKNOWN")).isEqualTo("UNKNOWN");
        assertThat(SendHistoryStatus.styleClassOf("UNKNOWN"))
                .isEqualTo(SendHistoryStatus.DEFAULT_STYLE_CLASS);
    }
}
