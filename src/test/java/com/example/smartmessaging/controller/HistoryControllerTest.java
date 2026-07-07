package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.HistoryDetailResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.example.smartmessaging.exception.GlobalExceptionHandler;
import com.example.smartmessaging.service.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class HistoryControllerTest {
    private MockMvc mockMvc;
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = mock(HistoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HistoryController(historyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 검색조건을_바인딩하고_발송기록_화면을_반환한다() throws Exception {
        PageResponseDTO<HistoryListResponseDTO> page = PageResponseDTO.of(List.of(), 1, 10, 0);
        when(historyService.getHistories(any(HistorySearchRequestDTO.class))).thenReturn(page);
        when(historyService.getChannelOptions()).thenReturn(List.of());
        when(historyService.getTagOptions()).thenReturn(List.of());
        when(historyService.getStatusOptions()).thenReturn(List.of());
        when(historyService.getPurposeOptions()).thenReturn(List.of());

        mockMvc.perform(get("/history")
                        .param("keyword", "쿠폰")
                        .param("sort", "mostSent")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/history"))
                .andExpect(model().attributeExists("searchCondition", "historyPage", "channels", "tags", "statuses", "purposes"))
                .andExpect(model().attribute("pageTitle", "전송 기록"));
    }

    @Test
    void 전송기록_상세를_JSON으로_반환한다() throws Exception {
        HistoryDetailResponseDTO detail = new HistoryDetailResponseDTO(
                11L,
                "배송 완료 안내",
                null,
                null,
                "INFO",
                "SENT",
                null,
                null,
                null,
                null,
                null,
                null
        ).withChannels(List.of("SMS")).withTags(List.of("배송 완료자"));
        when(historyService.getHistoryDetail(11L)).thenReturn(detail);

        mockMvc.perform(get("/history/11").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sendHistoryId").value(11))
                .andExpect(jsonPath("$.title").value("배송 완료 안내"))
                .andExpect(jsonPath("$.purposeLabel").value("정보성"))
                .andExpect(jsonPath("$.statusLabel").value("완료"))
                .andExpect(jsonPath("$.channels[0]").value("SMS"))
                .andExpect(jsonPath("$.tags[0]").value("배송 완료자"));
    }

    @Test
    void 없는_전송기록_상세는_404_JSON을_반환한다() throws Exception {
        when(historyService.getHistoryDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND));

        mockMvc.perform(get("/history/999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HISTORY-404"));
    }

    @Test
    void Accept_헤더가_없어도_없는_전송기록_상세는_404_JSON을_반환한다() throws Exception {
        when(historyService.getHistoryDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND));

        mockMvc.perform(get("/history/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("HISTORY-404"));
    }

    @Test
    void Accept_헤더가_와일드카드여도_없는_전송기록_상세는_404_JSON을_반환한다() throws Exception {
        when(historyService.getHistoryDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.SEND_HISTORY_NOT_FOUND));

        mockMvc.perform(get("/history/999").accept(MediaType.ALL))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("HISTORY-404"));
    }
}
