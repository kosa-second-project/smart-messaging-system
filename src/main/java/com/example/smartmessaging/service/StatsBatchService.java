package com.example.smartmessaging.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
public class StatsBatchService {

    // Spring Batch Job을 실제로 실행시키는 실행기
    private final JobLauncher jobLauncher;

    // StatsBatchJobConfig에서 등록한 통계 집계 Job
    private final Job statsDailyAggregationJob;

    public StatsBatchService(
            JobLauncher jobLauncher,

            // Job 타입 Bean이 여러 개 생길 수 있으므로 이름으로 명확히 지정
            @Qualifier("statsDailyAggregationJob") Job statsDailyAggregationJob
    ) {
        this.jobLauncher = jobLauncher;
        this.statsDailyAggregationJob = statsDailyAggregationJob;
    }

    // 지정한 날짜(statDate)를 기준으로 통계 배치 Job을 실행한다.
    public JobExecution run(LocalDate statDate) throws Exception {
        // JobParameters는 Batch Job 실행 시 전달하는 파라미터다.
        // statDate: 실제 통계를 집계할 기준 날짜
        // run.id: 같은 statDate로도 여러 번 재실행할 수 있도록 매번 다른 값 부여
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("statDate", statDate.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        log.info("[StatsBatch] job launch requested - statDate: {}", statDate);

        // JobLauncher가 등록된 Job을 JobParameters와 함께 실행한다.
        // 실행 결과와 상태는 JobExecution에 담긴다.
        return jobLauncher.run(statsDailyAggregationJob, jobParameters);
    }
}