package com.example.smartmessaging.controller;

import com.example.smartmessaging.exception.GlobalExceptionHandler;
import com.example.smartmessaging.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplateApiControllerTest {

    private TemplateService templateService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        templateService = mock(TemplateService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TemplateApiController(templateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void 잘못된_템플릿_저장_요청은_400_JSON을_반환한다() throws Exception {
        mockMvc.perform(post("/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " ",
                                  "content": "",
                                  "category": null,
                                  "purpose": "",
                                  "channelIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("SYS-400"))
                .andExpect(jsonPath("$.message").value("올바르지 않은 입력값입니다."));

        verify(templateService, never()).createTemplate(any(), any());
    }
}
