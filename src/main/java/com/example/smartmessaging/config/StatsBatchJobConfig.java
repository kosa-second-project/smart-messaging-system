package com.example.smartmessaging.config;

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

@Slf4j
@Configuration
public class StatsBatchJobConfig {

    @Bean
    public Job statsDailyAggregationJob(
            JobRepository jobRepository,
            Step statsSampleStep
    ) {
        return new JobBuilder("statsDailyAggregationJob", jobRepository)
                .start(statsSampleStep)
                .build();
    }

    @Bean
    public Step statsSampleStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("statsSampleStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("[StatsBatch] sample step executed");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}