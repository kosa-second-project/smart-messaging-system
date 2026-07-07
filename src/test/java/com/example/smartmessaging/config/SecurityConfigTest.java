package com.example.smartmessaging.config;

import com.example.smartmessaging.controller.StatsBatchController;
import com.example.smartmessaging.service.StatsBatchLauncherService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsBatchController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsBatchLauncherService statsBatchLauncherService;

    @Test
    void statsBatchApiRejectsNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/stats/batch/rebuild")
                        .param("date", "2026-07-03")
                        .with(user("1002").authorities(() -> "ROLE_MARKETER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void statsBatchApiAllowsAdminUser() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 3);
        when(statsBatchLauncherService.run(eq(date))).thenReturn(jobExecution());

        mockMvc.perform(post("/api/stats/batch/rebuild")
                        .param("date", "2026-07-03")
                        .with(user("1001").authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }

    private JobExecution jobExecution() {
        JobExecution jobExecution = new JobExecution(
                new JobInstance(1L, "statsDailyAggregationJob"),
                new JobParameters()
        );
        jobExecution.setStatus(BatchStatus.COMPLETED);
        return jobExecution;
    }
}
