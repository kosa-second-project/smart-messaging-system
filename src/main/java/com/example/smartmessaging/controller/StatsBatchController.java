package com.example.smartmessaging.controller;

import com.example.smartmessaging.service.StatsBatchLauncherService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats/batch")
@RequiredArgsConstructor
public class StatsBatchController {

    // 수동 API 요청을 Spring Batch Job 실행으로 연결하는 서비스.
    private final StatsBatchLauncherService statsBatchLauncherService;

    /**
     * 특정 날짜의 통계 배치를 수동으로 재실행한다.
     *
     * 예: POST /api/stats/batch/rebuild?date=2026-07-03
     */
    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuild(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) throws Exception {
        JobExecution jobExecution = statsBatchLauncherService.run(date);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statDate", date);
        response.put("jobName", jobExecution.getJobInstance().getJobName());
        response.put("jobExecutionId", jobExecution.getId());
        response.put("status", jobExecution.getStatus());
        response.put("startedAt", jobExecution.getStartTime());
        response.put("endedAt", jobExecution.getEndTime());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(JobExecutionAlreadyRunningException.class)
    public ResponseEntity<Map<String, Object>> handleJobAlreadyRunning(
            JobExecutionAlreadyRunningException e
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
