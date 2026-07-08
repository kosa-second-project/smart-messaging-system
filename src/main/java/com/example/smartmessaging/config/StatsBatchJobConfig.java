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
     * statDate 기준 통계를 재집계하는 Job.
     */
    @Bean
    public Job statsDailyAggregationJob(
            JobRepository jobRepository,
            Step messageStatStep,
            Step messageStatByDegreeStep,
            Step customerStatStep,
            Step channelStatStep,
            Step clickStatStep,
            Step templateStatStep
    ) {
        return new JobBuilder("statsDailyAggregationJob", jobRepository)
                .start(messageStatStep)
                .next(messageStatByDegreeStep)
                .next(customerStatStep)
                .next(channelStatStep)
                .next(clickStatStep)
                .next(templateStatStep)
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

    /**
     * channel_stat 집계 Step.
     *
     * channel_stat에는 stat_date 컬럼이 없으므로, statDate 실행 시점의 전체 누적 채널 성과를 다시 만든다.
     */
    @Bean
    public Step channelStatStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatsBatchService statsBatchService
    ) {
        return new StepBuilder("channelStatStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String statDateParameter = chunkContext
                            .getStepContext()
                            .getJobParameters()
                            .get("statDate")
                            .toString();

                    LocalDate statDate = LocalDate.parse(statDateParameter);

                    log.info("[StatsBatch] channelStatStep started - statDate: {}", statDate);
                    statsBatchService.aggregateChannelStat(statDate);
                    log.info("[StatsBatch] channelStatStep finished - statDate: {}", statDate);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * click_stat 집계 Step.
     *
     * short_url.clicked_at 기준으로 statDate의 시간대별 클릭 수를 채널별로 집계한다.
     */
    @Bean
    public Step clickStatStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatsBatchService statsBatchService
    ) {
        return new StepBuilder("clickStatStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String statDateParameter = chunkContext
                            .getStepContext()
                            .getJobParameters()
                            .get("statDate")
                            .toString();

                    LocalDate statDate = LocalDate.parse(statDateParameter);

                    log.info("[StatsBatch] clickStatStep started - statDate: {}", statDate);
                    statsBatchService.aggregateClickStat(statDate);
                    log.info("[StatsBatch] clickStatStep finished - statDate: {}", statDate);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * template_stat 집계 Step.
     *
     * send_history.template_id와 short_url 성과를 기준으로 statDate의 템플릿별 성과를 집계한다.
     */
    @Bean
    public Step templateStatStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            StatsBatchService statsBatchService
    ) {
        return new StepBuilder("templateStatStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    String statDateParameter = chunkContext
                            .getStepContext()
                            .getJobParameters()
                            .get("statDate")
                            .toString();

                    LocalDate statDate = LocalDate.parse(statDateParameter);

                    log.info("[StatsBatch] templateStatStep started - statDate: {}", statDate);
                    statsBatchService.aggregateTemplateStat(statDate);
                    log.info("[StatsBatch] templateStatStep finished - statDate: {}", statDate);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
