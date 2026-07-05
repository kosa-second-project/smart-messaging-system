package com.example.smartmessaging.scheduler;

import com.example.smartmessaging.service.StatsBatchLauncherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
public class StatsBatchScheduler {

    private final StatsBatchLauncherService statsBatchLauncherService;
    private final ZoneId batchZoneId;

    public StatsBatchScheduler(
            StatsBatchLauncherService statsBatchLauncherService,
            @Value("${stats.batch.zone}") String batchZone
    ) {
        this.statsBatchLauncherService = statsBatchLauncherService;
        this.batchZoneId = ZoneId.of(batchZone);
    }

    /**
     * 매일 설정된 시간에 전일 통계 배치를 실행한다.
     *
     * 예를 들어 Asia/Seoul 기준 2026-07-04 03:00에 실행되면
     * statDate는 2026-07-03으로 전달된다.
     */
    @Scheduled(cron = "${stats.batch.cron}", zone = "${stats.batch.zone}")
    public void runDailyStatsBatch() {
        LocalDate statDate = LocalDate.now(batchZoneId).minusDays(1);

        try {
            log.info("[StatsBatch] scheduled job requested - statDate: {}", statDate);
            statsBatchLauncherService.run(statDate);
            log.info("[StatsBatch] scheduled job completed - statDate: {}", statDate);
        } catch (JobExecutionAlreadyRunningException e) {
            log.info("[StatsBatch] scheduled job skipped - already running, statDate: {}", statDate);
        } catch (Exception e) {
            log.error("[StatsBatch] scheduled job failed - statDate: {}", statDate, e);
        }
    }
}
