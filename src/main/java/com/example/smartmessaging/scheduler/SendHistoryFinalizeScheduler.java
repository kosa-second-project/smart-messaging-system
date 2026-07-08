package com.example.smartmessaging.scheduler;

import com.example.smartmessaging.service.repository.HistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendHistoryFinalizeScheduler {

    private static final int FINALIZE_LIMIT = 100;

    private final HistoryMapper historyMapper;

    @Scheduled(cron = "*/5 * * * * *")
    @Transactional
    public void finalizeCompletedHistories() {
        List<Long> sendHistoryIds = historyMapper.findReadyToFinalizeHistoryIds(FINALIZE_LIMIT);
        if (sendHistoryIds.isEmpty()) {
            return;
        }

        for (Long sendHistoryId : sendHistoryIds) {
            historyMapper.finalizeSendHistory(sendHistoryId, "SENT");
        }
        log.info("[SendHistoryFinalizeScheduler] finalized send histories count={}", sendHistoryIds.size());
    }
}