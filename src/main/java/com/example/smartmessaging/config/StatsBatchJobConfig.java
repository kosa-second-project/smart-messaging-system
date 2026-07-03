package com.example.smartmessaging.config;

import com.example.smartmessaging.service.StatsBatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Slf4j
@Configuration
public class StatsBatchJobConfig {

    /**
     * 통계 일별 집계 Job.
     *
     * 현재는 message_stat Step만 연결한다.
     * 이후 message_stat_by_degree, customer_stat, channel_stat, click_stat Step을 순서대로 추가한다.
     */
    @Bean
    public Job statsDailyAggregationJob(
            JobRepository jobRepository,
            Step messageStatStep,
            Step messageStatByDegreeStep,
            Step customerStatStep
    ) {
        return new JobBuilder("statsDailyAggregationJob", jobRepository)
                .start(messageStatStep)
                .next(messageStatByDegreeStep)
                .next(customerStatStep)
                .build();
    }

    /**
     * message_stat 집계 Step.
     *
     * JobParameter의 statDate를 읽어서 해당 날짜의 발송 통계를 집계한다.
     */
    @Bean
    public Step messageStatStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatsBatchService statsBatchService
    ) {
        return new StepBuilder("messageStatStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String statDateParameter = chunkContext
                            .getStepContext()
                            .getJobParameters()
                            .get("statDate")
                            .toString();

                    LocalDate statDate = LocalDate.parse(statDateParameter);

                    log.info("[StatsBatch] messageStatStep started - statDate: {}", statDate);
                    statsBatchService.aggregateMessageStat(statDate);
                    log.info("[StatsBatch] messageStatStep finished - statDate: {}", statDate);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * message_stat_by_degree 집계 Step.
     *
     * JobParameter의 statDate를 읽어서 해당 날짜의 차수/채널별 발송 통계를 집계한다.
     */
    @Bean
    public Step messageStatByDegreeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatsBatchService statsBatchService
    ) {
        return new StepBuilder("messageStatByDegreeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String statDateParameter = chunkContext
                            .getStepContext()
                            .getJobParameters()
                            .get("statDate")
                            .toString();

                    LocalDate statDate = LocalDate.parse(statDateParameter);

                    log.info("[StatsBatch] messageStatByDegreeStep started - statDate: {}", statDate);
                    statsBatchService.aggregateMessageStatByDegree(statDate);
                    log.info("[StatsBatch] messageStatByDegreeStep finished - statDate: {}", statDate);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * customer_stat 집계 Step.
     *
     * JobParameter의 statDate를 읽어서 해당 날짜 기준의 고객 스냅샷 통계를 만든다.
     */
    @Bean
    public Step customerStatStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatsBatchService statsBatchService
    ) {
        return new StepBuilder("customerStatStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String statDateParameter = chunkContext
                            .getStepContext()
                            .getJobParameters()
                            .get("statDate")
                            .toString();

                    LocalDate statDate = LocalDate.parse(statDateParameter);

                    log.info("[StatsBatch] customerStatStep started - statDate: {}", statDate);
                    statsBatchService.aggregateCustomerStat(statDate);
                    log.info("[StatsBatch] customerStatStep finished - statDate: {}", statDate);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
