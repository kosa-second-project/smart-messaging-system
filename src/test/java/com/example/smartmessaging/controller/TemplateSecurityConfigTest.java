package com.example.smartmessaging.controller;

import com.example.smartmessaging.config.SecurityConfig;
import com.example.smartmessaging.dto.response.PageResponse;
import com.example.smartmessaging.dto.response.TemplateResponse;
import com.example.smartmessaging.dto.vo.UsersVO;
import com.example.smartmessaging.security.CustomUserDetails;
import com.example.smartmessaging.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateApiController.class)
@Import(SecurityConfig.class)
class TemplateSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateService templateService;

    @Test
    void templateUpdateRejectsNonAdminUser() throws Exception {
        mockMvc.perform(put("/api/templates/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTemplateJson())
                        .with(user(principal("1002", 1002, 2L, "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void templateDeleteRejectsNonAdminUser() throws Exception {
        mockMvc.perform(delete("/api/templates/7")
                        .with(user(principal("1002", 1002, 2L, "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void templateUpdateAllowsAdminUser() throws Exception {
        mockMvc.perform(put("/api/templates/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTemplateJson())
                        .with(user(principal("1001", 1001, 1L, "ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(templateService).updateTemplate(eq(1L), eq(7L), any());
    }

    @Test
    void templateDeleteAllowsAdminUser() throws Exception {
        mockMvc.perform(delete("/api/templates/7")
                        .with(user(principal("1001", 1001, 1L, "ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(templateService).deleteTemplate(1L, 7L);
    }

    @Test
    void templateListAllowsNonAdminUser() throws Exception {
        when(templateService.getTemplateList(any()))
                .thenReturn(new PageResponse<TemplateResponse>(List.of(), 0, 1, 10));

        mockMvc.perform(get("/api/templates")
                        .with(user(principal("1002", 1002, 2L, "ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void templateDetailAllowsNonAdminUser() throws Exception {
        when(templateService.getTemplateDetail(2L, 7L)).thenReturn(new TemplateResponse());

        mockMvc.perform(get("/api/templates/7")
                        .with(user(principal("1002", 1002, 2L, "ROLE_USER"))))
                .andExpect(status().isOk());
    }

    private CustomUserDetails principal(String username, Integer empNum, Long userId, String role) {
        UsersVO usersVO = UsersVO.builder()
                .userId(userId)
                .empNum(empNum)
                .pwd("{noop}password")
                .name(username)
                .isActive(true)
                .build();
        return new CustomUserDetails(usersVO, List.of(role));
    }

    private String validTemplateJson() {
        return """
                {
                  "title": "Updated",
                  "content": "Updated content",
                  "isAiGenerated": false,
                  "category": "NOTICE",
                  "purpose": "INFO",
                  "channelIds": [1]
                }
                """;
    }
}
