package com.example.smartmessaging.controller;

import com.example.smartmessaging.service.StatsBatchLauncherService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StatsBatchControllerTest {

    @Test
    void 같은_날짜_배치가_실행중이면_409를_반환한다() {
        StatsBatchController controller = new StatsBatchController(mock(StatsBatchLauncherService.class));
        JobExecutionAlreadyRunningException exception =
                new JobExecutionAlreadyRunningException("이미 같은 statDate의 통계 배치가 실행 중입니다.");

        ResponseEntity<Map<String, Object>> response = controller.handleJobAlreadyRunning(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("status", HttpStatus.CONFLICT.value())
                .containsEntry("message", "이미 같은 statDate의 통계 배치가 실행 중입니다.");
    }
}
