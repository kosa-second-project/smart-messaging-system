package com.example.smartmessaging.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
public class StatsBatchLauncherService {

    // Spring Batch Job을 실제로 실행시키는 실행기.
    private final JobLauncher jobLauncher;

    // Spring Batch 메타데이터에서 현재 실행 중인 Job 정보를 조회한다.
    private final JobExplorer jobExplorer;

    // StatsBatchJobConfig에서 등록한 통계 일별 집계 Job.
    private final Job statsDailyAggregationJob;

    private final ZoneId batchZoneId;

    public StatsBatchLauncherService(
            JobLauncher jobLauncher,
            JobExplorer jobExplorer,
            @Qualifier("statsDailyAggregationJob") Job statsDailyAggregationJob,
            @Value("${stats.batch.zone}") String batchZone
    ) {
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
        this.statsDailyAggregationJob = statsDailyAggregationJob;
        this.batchZoneId = ZoneId.of(batchZone);
    }

    /**
     * 지정한 날짜를 JobParameter로 넘겨 통계 배치 Job을 실행한다.
     *
     * run.id는 같은 statDate로도 수동 재실행할 수 있게 매번 다른 값으로 넣는다.
     */
    public synchronized JobExecution run(LocalDate statDate)
            throws JobExecutionAlreadyRunningException,
            JobRestartException,
            JobInstanceAlreadyCompleteException,
            JobParametersInvalidException {
        rejectTodayOrFutureStatDate(statDate);
        rejectIfSameDateJobRunning(statDate);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("statDate", statDate.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();

        log.info("[StatsBatch] job launch requested - statDate: {}", statDate);
        return jobLauncher.run(statsDailyAggregationJob, jobParameters);
    }

    private void rejectTodayOrFutureStatDate(LocalDate statDate) {
        LocalDate today = LocalDate.now(batchZoneId);

        if (!statDate.isBefore(today)) {
            throw new IllegalArgumentException(
                    "오늘 또는 미래 날짜의 통계 배치는 실행할 수 없습니다. statDate="
                            + statDate + ", today=" + today + ", zone=" + batchZoneId
            );
        }
    }

    private void rejectIfSameDateJobRunning(LocalDate statDate)
            throws JobExecutionAlreadyRunningException {
        boolean alreadyRunning = jobExplorer
                .findRunningJobExecutions(statsDailyAggregationJob.getName())
                .stream()
                .map(JobExecution::getJobParameters)
                .map(parameters -> parameters.getString("statDate"))
                .anyMatch(statDate.toString()::equals);

        if (alreadyRunning) {
            throw new JobExecutionAlreadyRunningException(
                    "이미 같은 statDate의 통계 배치가 실행 중입니다. statDate=" + statDate
            );
        }
    }
}
