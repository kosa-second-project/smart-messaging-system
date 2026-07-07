package com.example.smartmessaging.ai.controller;

import com.example.smartmessaging.ai.dto.response.AiSuggestionItem;
import com.example.smartmessaging.ai.dto.response.AiSuggestionResponse;
import com.example.smartmessaging.ai.service.AiSuggestionService;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiSuggestionControllerTest {

    private MockMvc mockMvc;
    private AiSuggestionService aiSuggestionService;

    @BeforeEach
    void setUp() {
        aiSuggestionService = mock(AiSuggestionService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AiSuggestionController(aiSuggestionService))
                .setControllerAdvice(new SuggestionBusinessExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 정상_요청은_추천_결과를_반환한다() throws Exception {
        when(aiSuggestionService.suggest(any())).thenReturn(new AiSuggestionResponse(List.of(
                new AiSuggestionItem("주말 혜택 안내", "(광고) 혜택을 확인하세요. 무료수신거부 080-000-0000")
        )));

        mockMvc.perform(post("/api/ai/suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contextType": "MESSAGE_SEND",
                                  "messageType": "AD",
                                  "channels": ["SMS"],
                                  "customerTags": ["NEW"],
                                  "direction": null,
                                  "availableVariables": ["#{고객명}"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].title").value("주말 혜택 안내"))
                .andExpect(jsonPath("$.suggestions[0].content").exists())
                .andExpect(jsonPath("$.suggestions[0].issues").doesNotExist());
    }

    @Test
    void customerTags가_비어_있어도_추천을_요청한다() throws Exception {
        when(aiSuggestionService.suggest(any())).thenReturn(new AiSuggestionResponse(List.of()));
        mockMvc.perform(post("/api/ai/suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contextType": "MESSAGE_SEND",
                                  "messageType": "INFO",
                                  "channels": ["SMS"],
                                  "customerTags": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").isArray());
        verify(aiSuggestionService).suggest(any());
    }

    @Test
    void channels가_비어_있으면_400을_반환한다() throws Exception {
        assertBadRequest("""
                {
                  "contextType": "MESSAGE_SEND",
                  "messageType": "INFO",
                  "channels": [],
                  "customerTags": ["NEW"]
                }
                """);
    }

    @Test
    void messageType이_없으면_400을_반환한다() throws Exception {
        assertBadRequest("""
                {
                  "contextType": "MESSAGE_SEND",
                  "channels": ["SMS"],
                  "customerTags": ["NEW"]
                }
                """);
    }

    @Test
    void contextType이_없으면_400을_반환한다() throws Exception {
        assertBadRequest("""
                {
                  "messageType": "INFO",
                  "channels": ["SMS"],
                  "customerTags": ["NEW"]
                }
                """);
    }

    @Test
    void 통과_후보를_생성하지_못하면_422을_반환한다() throws Exception {
        when(aiSuggestionService.suggest(any()))
                .thenThrow(new BusinessException(ErrorCode.AI_SUGGESTION_NO_VALID_CANDIDATE));

        mockMvc.perform(post("/api/ai/suggestions")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contextType": "MESSAGE_SEND",
                                  "messageType": "INFO",
                                  "channels": ["SMS"],
                                  "customerTags": ["NEW"]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AI-SUGGESTION-422"))
                .andExpect(jsonPath("$.message")
                        .value("조건을 만족하는 추천 문구를 생성하지 못했습니다."));
    }

    private void assertBadRequest(String content) throws Exception {
        mockMvc.perform(post("/api/ai/suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SYS-400"));
    }

    private static class SuggestionBusinessExceptionHandler
            extends com.example.smartmessaging.exception.GlobalExceptionHandler {
    }
}
