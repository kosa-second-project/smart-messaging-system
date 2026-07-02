package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.service.HistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class HistoryControllerTest {
    private MockMvc mockMvc;
    private HistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = mock(HistoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HistoryController(historyService)).build();
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
}
