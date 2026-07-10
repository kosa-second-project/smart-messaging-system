package com.example.smartmessaging.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardQueueStatusResponse {
    private String status;
    private String statusLabel;
    private String statusMessage;
    private LocalDateTime refreshedAt;
    private long totalReadyCount;
    private int totalConsumerCount;
    private List<QueueItem> queues;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueueItem {
        private String queueName;
        private String label;
        private long readyCount;
        private long unackedCount;
        private long totalCount;
        private int consumerCount;
        private String color;
        private String badge;
        private boolean available;
    }
}
