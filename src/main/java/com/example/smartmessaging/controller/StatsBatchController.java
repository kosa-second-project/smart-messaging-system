package com.example.smartmessaging.controller;

import com.example.smartmessaging.service.StatsBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    // Batch Job 실행을 담당하는 서비스
    private final StatsBatchService statsBatchService;

    /**
     * 특정 날짜의 통계 배치를 수동으로 재실행한다.
     *
     * 호출 예:
     * POST /api/stats/batch/rebuild?date=2026-07-03
     *
     * date 파라미터는 yyyy-MM-dd 형식으로 받는다.
     */
    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuild(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) throws Exception {
        // 요청으로 받은 날짜를 기준으로 Batch Job 실행
        JobExecution jobExecution = statsBatchService.run(date);

        // Batch 실행 결과를 API 응답으로 내려주기 위한 Map
        Map<String, Object> response = new LinkedHashMap<>();

        // 실제 집계 기준 날짜
        response.put("statDate", date);

        // 실행된 Job 이름
        response.put("jobName", jobExecution.getJobInstance().getJobName());

        // Spring Batch가 부여한 Job 실행 ID
        response.put("jobExecutionId", jobExecution.getId());

        // 실행 상태: STARTING, STARTED, COMPLETED, FAILED 등
        response.put("status", jobExecution.getStatus());

        // Job 시작 시각
        response.put("startedAt", jobExecution.getStartTime());

        // Job 종료 시각
        response.put("endedAt", jobExecution.getEndTime());

        return ResponseEntity.ok(response);
    }
}