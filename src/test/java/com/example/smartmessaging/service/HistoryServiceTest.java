package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryAttemptFlowResponseDTO;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.mapper.HistoryMapper;
import com.example.smartmessaging.service.impl.HistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoryServiceTest {
    private HistoryMapper historyMapper;
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyMapper = mock(HistoryMapper.class);
        historyService = new HistoryServiceImpl(historyMapper);
    }

    @Test
    void search_condition_is_normalized_and_channels_and_tags_are_attached() {
        HistorySearchRequestDTO condition = searchCondition(
                "  이벤트  ",
                "invalid-sort",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 2),
                Arrays.asList(1L, 2L, 1L, null),
                null
        );
        HistorySearchRequestDTO normalized = condition.normalized();
        HistoryListResponseDTO history = history(11L);
        HistoryChannelResponseDTO channel = new HistoryChannelResponseDTO(11L, "SMS");
        HistoryTagResponseDTO tag = new HistoryTagResponseDTO(11L, "이벤트");

        when(historyMapper.countHistories(normalized)).thenReturn(1L);
        when(historyMapper.findHistories(normalized)).thenReturn(List.of(history));
        when(historyMapper.findChannelsByHistoryIds(List.of(11L))).thenReturn(List.of(channel));
        when(historyMapper.findTagsByHistoryIds(List.of(11L))).thenReturn(List.of(tag));

        PageResponseDTO<HistoryListResponseDTO> result = historyService.getHistories(condition);

        assertThat(normalized.keyword()).isEqualTo("이벤트");
        assertThat(normalized.sort()).isEqualTo("latest");
        assertThat(normalized.startAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(normalized.endAtExclusive()).isEqualTo(LocalDateTime.of(2026, 7, 3, 0, 0));
        assertThat(normalized.tagIds()).containsExactly(1L, 2L);
        assertThat(normalized.tagCount()).isEqualTo(2);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).channels()).containsExactly("SMS");
        assertThat(result.content().get(0).tags()).containsExactly("이벤트");
        assertThat(result.content().get(0).displayTags()).containsExactly("이벤트");
        assertThat(result.content().get(0).hiddenTagCount()).isZero();
    }

    @Test
    void empty_result_does_not_load_detail_batches() {
        HistorySearchRequestDTO condition = searchCondition(null, null, null, null, List.of(), null);
        HistorySearchRequestDTO normalized = condition.normalized();
        when(historyMapper.countHistories(normalized)).thenReturn(0L);

        PageResponseDTO<HistoryListResponseDTO> result = historyService.getHistories(condition);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.totalPages()).isZero();
        verify(historyMapper).countHistories(normalized);
    }

    @Test
    void page_over_total_pages_is_adjusted_to_last_page() {
        HistorySearchRequestDTO condition = searchCondition(null, null, null, null, List.of(), 99);
        HistorySearchRequestDTO firstNormalized = condition.normalized();
        HistorySearchRequestDTO lastPageCondition = firstNormalized.withPage(3);
        when(historyMapper.countHistories(firstNormalized)).thenReturn(21L);
        when(historyMapper.findHistories(lastPageCondition)).thenReturn(List.of());

        PageResponseDTO<HistoryListResponseDTO> result = historyService.getHistories(condition);

        assertThat(result.currentPage()).isEqualTo(3);
        assertThat(lastPageCondition.offset()).isEqualTo(20);
    }

    @Test
    void status_options_match_database_check_constraint_values() {
        assertThat(historyService.getStatusOptions())
                .extracting("value", "label")
                .containsExactly(
                        tuple("SCHEDULED", "예약"),
                        tuple("SENDING", "전송중"),
                        tuple("SENT", "완료"),
                        tuple("FAILED", "실패")
                );
    }

    @Test
    void detail_result_attaches_channels_tags_and_attempt_flows() {
        HistoryDetailResponseDTO detail = detail(11L);
        HistoryChannelResponseDTO channel = new HistoryChannelResponseDTO(11L, "SMS");
        HistoryTagResponseDTO tag = new HistoryTagResponseDTO(11L, "이벤트");
        HistoryAttemptFlowResponseDTO attempt = new HistoryAttemptFlowResponseDTO(1, "SMS", 10, 8, 2);

        when(historyMapper.findHistoryDetailById(11L)).thenReturn(detail);
        when(historyMapper.findChannelsByHistoryIds(List.of(11L))).thenReturn(List.of(channel));
        when(historyMapper.findTagsByHistoryIds(List.of(11L))).thenReturn(List.of(tag));
        when(historyMapper.findAttemptFlowsByHistoryId(11L)).thenReturn(List.of(attempt));

        HistoryDetailResponseDTO result = historyService.getHistoryDetail(11L);

        assertThat(result.channels()).containsExactly("SMS");
        assertThat(result.tags()).containsExactly("이벤트");
        assertThat(result.attemptFlows()).containsExactly(attempt);
    }

    @Test
    void deleted_or_missing_send_history_cannot_be_loaded() {
        when(historyMapper.findHistoryDetailById(999L)).thenReturn(null);

        assertThatThrownBy(() -> historyService.getHistoryDetail(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SEND_HISTORY_NOT_FOUND);
    }

    private HistorySearchRequestDTO searchCondition(
            String keyword,
            String sort,
            LocalDate startDate,
            LocalDate endDate,
            List<Long> tagIds,
            Integer page
    ) {
        return new HistorySearchRequestDTO(
                keyword,
                null,
                null,
                null,
                tagIds,
                startDate,
                endDate,
                sort,
                page
        );
    }

    private HistoryListResponseDTO history(Long id) {
        return new HistoryListResponseDTO(id, null, null, null, null, null, null, null, null, null);
    }

    private HistoryDetailResponseDTO detail(Long id) {
        return new HistoryDetailResponseDTO(id, null, null, null, null, null, null, null, null, null, null, null);
    }
}
