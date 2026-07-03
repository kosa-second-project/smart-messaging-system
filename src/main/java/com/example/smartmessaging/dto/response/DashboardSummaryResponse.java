package com.example.smartmessaging.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {
    private List<StatCardResponse> cards;
    private List<StatChartResponse> charts;
    private List<QueueStatus> queueStatuses;
    private List<RecentSend> recentSends;
    private List<TemplatePerformance> templatePerformance;
    private List<QueueJob> queueJobs;
    private List<ProcessStep> processSteps;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueueStatus {
        private String label;
        private long count;
        private String color;
        private String badge;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentSend {
        private String template;
        private String sentAt;
        private String targetType;
        private int count;
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplatePerformance {
        private String name;
        private double click;
        private Double conversion;
        private String source;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueueJob {
        private String id;
        private String title;
        private String channel;
        private String status;
        private int progress;
        private long requested;
        private long processed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessStep {
        private String title;
        private String status;
        private String state;
    }
}
