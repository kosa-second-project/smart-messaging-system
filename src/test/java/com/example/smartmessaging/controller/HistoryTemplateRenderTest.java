package com.example.smartmessaging.controller;

import com.example.smartmessaging.dto.request.HistorySearchRequestDTO;
import com.example.smartmessaging.dto.response.HistoryListResponseDTO;
import com.example.smartmessaging.dto.response.PageResponseDTO;
import com.example.smartmessaging.dto.response.HistoryStatusOptionDTO;
import com.example.smartmessaging.dto.response.HistoryFilterOptionDTO;
import com.example.smartmessaging.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class HistoryTemplateRenderTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HistoryService historyService;

    @Test
    void 빈_발송기록_화면의_Thymeleaf_템플릿을_렌더링한다() throws Exception {
        when(historyService.getHistories(any(HistorySearchRequestDTO.class)))
                .thenReturn(PageResponseDTO.of(List.<HistoryListResponseDTO>of(), 1, 10, 0));
        when(historyService.getChannelOptions()).thenReturn(List.of());
        when(historyService.getTagOptions()).thenReturn(List.of());
        when(historyService.getStatusOptions()).thenReturn(List.of(
                new HistoryStatusOptionDTO("SCHEDULED", "예약"),
                new HistoryStatusOptionDTO("SENDING", "전송중"),
                new HistoryStatusOptionDTO("SENT", "완료"),
                new HistoryStatusOptionDTO("FAILED", "실패")
        ));
        when(historyService.getPurposeOptions()).thenReturn(List.of("INFO", "AD"));

        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("조회된 전송 기록이 없습니다.")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">예약</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">전송중</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">완료</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">실패</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">정보성</option>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">광고성</option>")));
    }

    @Test
    void 데이터와_페이지네이션이_있는_Thymeleaf_템플릿을_렌더링한다() throws Exception {
        HistoryListResponseDTO history = new HistoryListResponseDTO();
        history.setId(1L);
        history.setTitle("테스트 전송");
        history.setPurpose("INFO");
        history.setStatus("SENT   ");
        history.setTotalTargetCount(10);
        history.setSuccessCount(8);
        history.setFailCount(2);
        history.setSuccessRate(new BigDecimal("80"));
        history.setTags(List.of("10대", "30대", "sms 동의", "휴면", "일반", "이메일 동의", "신규"));

        when(historyService.getHistories(any(HistorySearchRequestDTO.class)))
                .thenReturn(PageResponseDTO.of(List.of(history), 1, 10, 11));
        when(historyService.getChannelOptions()).thenReturn(List.of());
        HistoryFilterOptionDTO tag1 = new HistoryFilterOptionDTO();
        tag1.setId(1L);
        tag1.setLabel("10대");
        HistoryFilterOptionDTO tag2 = new HistoryFilterOptionDTO();
        tag2.setId(2L);
        tag2.setLabel("20대");
        when(historyService.getTagOptions()).thenReturn(List.of(tag1, tag2));
        when(historyService.getStatusOptions()).thenReturn(List.of());
        when(historyService.getPurposeOptions()).thenReturn(List.of());

        mockMvc.perform(get("/history")
                        .param("tagIds", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("총 11건 중 1-10")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("history-status--completed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">완료</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">+4</span>")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"1\" data-tag-submit id=\"tagIds1\" name=\"tagIds\" checked=\"checked\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"2\" data-tag-submit id=\"tagIds2\" name=\"tagIds\" checked=\"checked\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tagIds=1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("tagIds=2")));
    }
}
