package com.example.smartmessaging.ai.controller;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.ai.service.AiReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiMessageControllerTest {

    private MockMvc mockMvc;
    private AiReviewService aiReviewService;

    @BeforeEach
    void setUp() {
        aiReviewService = mock(AiReviewService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiMessageController(aiReviewService)).build();
    }

    @Test
    void 정상_요청을_검사하고_점수_없는_응답을_반환한다() throws Exception {
        ValidationIssue issue = new ValidationIssue(
                "MISSING_AD_PREFIX",
                IssueSource.SERVER_RULE,
                IssueSeverity.HIGH,
                ReviewStatus.FAIL,
                "content",
                "광고성 메시지에는 본문 시작부에 '(광고)' 문구가 필요합니다.",
                null,
                "본문 시작부에 '(광고)'를 추가하세요.",
                List.of()
        );
        AiReviewResponse response = new AiReviewResponse(
                ReviewStatus.FAIL,
                "검사 결과 반드시 수정해야 하는 항목이 있습니다.",
                List.of(issue),
                null,
                true
        );
        when(aiReviewService.review(any())).thenReturn(response);

        mockMvc.perform(post("/api/ai/messages/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contextType": "MESSAGE_SEND",
                                  "messageType": "AD",
                                  "channels": ["SMS", "LMS", "KAKAO"],
                                  "customerTags": ["최근구매", "패션"],
                                  "title": "여름 쿠폰 안내",
                                  "content": "#{고객명}님, 오늘만 20% 쿠폰 혜택을 확인해보세요.",
                                  "availableVariables": ["#{고객명}", "#{쿠폰명}"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.summary").value("검사 결과 반드시 수정해야 하는 항목이 있습니다."))
                .andExpect(jsonPath("$.issues[0].ruleId").value("MISSING_AD_PREFIX"))
                .andExpect(jsonPath("$.issues[0].source").value("SERVER_RULE"))
                .andExpect(jsonPath("$.issues[0].status").value("FAIL"))
                .andExpect(jsonPath("$.issues[0].field").value("content"))
                .andExpect(jsonPath("$.issues[0].detail").doesNotExist())
                .andExpect(jsonPath("$.suggestedRewrite").isEmpty())
                .andExpect(jsonPath("$.needsHumanReview").value(true))
                .andExpect(jsonPath("$.overallScore").doesNotExist());

        ArgumentCaptor<AiReviewRequest> requestCaptor = ArgumentCaptor.forClass(AiReviewRequest.class);
        verify(aiReviewService).review(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getChannels())
                .containsExactly(ChannelType.SMS, ChannelType.LMS, ChannelType.KAKAO);
    }
}
