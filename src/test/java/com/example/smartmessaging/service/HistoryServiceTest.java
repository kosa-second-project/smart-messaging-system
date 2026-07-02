package com.example.smartmessaging.service;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryChannelResponseDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryTagResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.mapper.HistoryMapper;
import com.example.smartmessaging.service.impl.HistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void 조회조건을_정규화하고_채널과_태그를_배치결과로_조립한다() {
        HistorySearchRequestDTO condition = new HistorySearchRequestDTO();
        condition.setKeyword("  이벤트  ");
        condition.setSort("invalid-sort");
        condition.setStartDate(LocalDate.of(2026, 7, 1));
        condition.setEndDate(LocalDate.of(2026, 7, 2));
        condition.setTagIds(new java.util.ArrayList<>(java.util.Arrays.asList(1L, 2L, 1L, null)));

        HistoryListResponseDTO history = new HistoryListResponseDTO();
        history.setId(11L);
        HistoryChannelResponseDTO channel = new HistoryChannelResponseDTO();
        channel.setSendHistoryId(11L);
        channel.setChannelName("SMS");
        HistoryTagResponseDTO tag = new HistoryTagResponseDTO();
        tag.setSendHistoryId(11L);
        tag.setTagName("이벤트");

        when(historyMapper.countHistories(condition)).thenReturn(1L);
        when(historyMapper.findHistories(condition)).thenReturn(List.of(history));
        when(historyMapper.findChannelsByHistoryIds(List.of(11L))).thenReturn(List.of(channel));
        when(historyMapper.findTagsByHistoryIds(List.of(11L))).thenReturn(List.of(tag));

        PageResponseDTO<HistoryListResponseDTO> result = historyService.getHistories(condition);

        assertThat(condition.getKeyword()).isEqualTo("이벤트");
        assertThat(condition.getSort()).isEqualTo("latest");
        assertThat(condition.getStartAt()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(condition.getEndAtExclusive()).isEqualTo(LocalDateTime.of(2026, 7, 3, 0, 0));
        assertThat(condition.getTagIds()).containsExactly(1L, 2L);
        assertThat(condition.getTagCount()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getChannels()).containsExactly("SMS");
        assertThat(result.getContent().get(0).getTags()).containsExactly("이벤트");
        assertThat(result.getContent().get(0).getDisplayTags()).containsExactly("이벤트");
        assertThat(result.getContent().get(0).getHiddenTagCount()).isZero();
    }

    @Test
    void 데이터가_없으면_상세배치조회를_실행하지_않는다() {
        HistorySearchRequestDTO condition = new HistorySearchRequestDTO();
        when(historyMapper.countHistories(condition)).thenReturn(0L);

        PageResponseDTO<HistoryListResponseDTO> result = historyService.getHistories(condition);

        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalPages()).isZero();
        verify(historyMapper).countHistories(condition);
    }

    @Test
    void 요청페이지가_마지막페이지보다_크면_마지막페이지로_보정한다() {
        HistorySearchRequestDTO condition = new HistorySearchRequestDTO();
        condition.setPage(99);
        when(historyMapper.countHistories(condition)).thenReturn(21L);
        when(historyMapper.findHistories(condition)).thenReturn(List.of());

        PageResponseDTO<HistoryListResponseDTO> result = historyService.getHistories(condition);

        assertThat(condition.getPage()).isEqualTo(3);
        assertThat(result.getCurrentPage()).isEqualTo(3);
        assertThat(condition.getOffset()).isEqualTo(20);
    }

    @Test
    void 상태필터는_DB_CHECK_제약조건에_정의된_네_상태를_제공한다() {
        assertThat(historyService.getStatusOptions())
                .extracting("value", "label")
                .containsExactly(
                        tuple("SCHEDULED", "예약"),
                        tuple("SENDING", "전송중"),
                        tuple("SENT", "완료"),
                        tuple("FAILED", "실패")
                );
    }
}
