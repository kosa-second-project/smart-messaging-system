package com.example.smartmessaging.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatsBatchLauncherServiceTest {

    private JobLauncher jobLauncher;
    private JobExplorer jobExplorer;
    private Job statsDailyAggregationJob;
    private StatsBatchLauncherService statsBatchLauncherService;

    @BeforeEach
    void setUp() {
        jobLauncher = mock(JobLauncher.class);
        jobExplorer = mock(JobExplorer.class);
        statsDailyAggregationJob = mock(Job.class);
        when(statsDailyAggregationJob.getName()).thenReturn("statsDailyAggregationJob");

        statsBatchLauncherService = new StatsBatchLauncherService(
                jobLauncher,
                jobExplorer,
                statsDailyAggregationJob,
                "Asia/Seoul"
        );
    }

    @Test
    void today_or_future_statDate_is_rejected() throws Exception {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        assertThatThrownBy(() -> statsBatchLauncherService.run(today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("오늘 또는 미래 날짜의 통계 배치는 실행할 수 없습니다.")
                .hasMessageContaining("statDate=" + today);

        verify(jobExplorer, never()).findRunningJobExecutions(any(String.class));
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void 같은_statDate_배치가_실행중이면_새_실행을_거절한다() throws Exception {
        LocalDate statDate = LocalDate.of(2026, 7, 3);
        JobExecution runningExecution = mock(JobExecution.class);
        JobParameters runningParameters = new JobParametersBuilder()
                .addString("statDate", statDate.toString())
                .addLong("run.id", 1L)
                .toJobParameters();
        when(runningExecution.getJobParameters()).thenReturn(runningParameters);
        when(jobExplorer.findRunningJobExecutions("statsDailyAggregationJob"))
                .thenReturn(Set.of(runningExecution));

        assertThatThrownBy(() -> statsBatchLauncherService.run(statDate))
                .isInstanceOf(JobExecutionAlreadyRunningException.class)
                .hasMessageContaining("statDate=2026-07-03");

        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void 실행중인_배치와_statDate가_다르면_새_배치를_실행한다() throws Exception {
        LocalDate requestedDate = LocalDate.of(2026, 7, 3);
        JobExecution runningExecution = mock(JobExecution.class);
        JobExecution launchedExecution = mock(JobExecution.class);
        JobParameters runningParameters = new JobParametersBuilder()
                .addString("statDate", "2026-07-02")
                .addLong("run.id", 1L)
                .toJobParameters();
        when(runningExecution.getJobParameters()).thenReturn(runningParameters);
        when(jobExplorer.findRunningJobExecutions("statsDailyAggregationJob"))
                .thenReturn(Set.of(runningExecution));
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(launchedExecution);

        JobExecution result = statsBatchLauncherService.run(requestedDate);

        ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), jobParametersCaptor.capture());
        assertThat(result).isSameAs(launchedExecution);
        assertThat(jobParametersCaptor.getValue().getString("statDate")).isEqualTo("2026-07-03");
        assertThat(jobParametersCaptor.getValue().getLong("run.id")).isNotNull();
    }
}
