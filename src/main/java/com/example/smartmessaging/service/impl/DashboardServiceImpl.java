package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.ProcessStep;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.QueueJob;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.QueueStatus;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.RecentSend;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse.TemplatePerformance;
import com.example.smartmessaging.dto.response.StatCardResponse;
import com.example.smartmessaging.dto.response.StatChartDatasetResponse;
import com.example.smartmessaging.dto.response.StatChartResponse;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import com.example.smartmessaging.dto.vo.SendHistoryVO;
import com.example.smartmessaging.mapper.DashboardMapper;
import com.example.smartmessaging.mapper.StatMapper;
import com.example.smartmessaging.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("#,###");
    private static final DecimalFormat RATE_FORMATTER = new DecimalFormat("0.0");

    private final StatMapper statMapper;
    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardSummaryResponse getSummary(StatSearchRequest request) {
        List<MessageStatVO> messageStats = statMapper.selectMessageStats(request);
        List<MessageStatByDegreeVO> degreeStats = statMapper.selectDeliveryMessageStatByDegrees(request);
        List<CustomerStatVO> customerStats = statMapper.selectCustomerStats(request);
        List<SendHistoryVO> recentSends = dashboardMapper.selectRecentSends();
        List<TemplatePerformance> templateTop = dashboardMapper.selectTemplatePerformanceTop(request);
        Map<Long, String> channelNames = channelNames();

        return DashboardSummaryResponse.builder()
                .cards(buildCards(messageStats, degreeStats, customerStats))
                .charts(List.of(
                        buildCostComparisonChart(messageStats),
                        buildChannelShareChart(degreeStats, channelNames),
                        buildDailySendTrendChart(messageStats)
                ))
                .queueStatuses(buildQueueStatuses())
                .recentSends(buildRecentSends(recentSends))
                .templatePerformance(buildTemplatePerformance(templateTop))
                .queueJobs(buildQueueJobs())
                .processSteps(buildProcessSteps())
                .build();
    }

    private List<StatCardResponse> buildCards(List<MessageStatVO> messageStats,
                                              List<MessageStatByDegreeVO> degreeStats,
                                              List<CustomerStatVO> customerStats) {
        long totalSend = messageStats.stream().mapToLong(stat -> n(stat.getTotalSendCount())).sum();
        long totalSuccess = messageStats.stream().mapToLong(stat -> n(stat.getTotalSuccessCount())).sum();
        long failCount = Math.max(totalSend - totalSuccess, 0);
        BigDecimal billingCost = sum(messageStats, MessageStatVO::getBillingCost);
        BigDecimal maxCost = sum(messageStats, MessageStatVO::getMaxCost);
        BigDecimal savingCost = maxCost.subtract(billingCost).max(BigDecimal.ZERO);
        CustomerStatVO latestCustomer = latestCustomerStat(customerStats);
        long activeCustomers = n(latestCustomer.getNormalCustomerCount()) + n(latestCustomer.getNewCustomerCount());

        if (totalSend == 0) {
            totalSend = degreeStats.stream().mapToLong(stat -> n(stat.getSendCount())).sum();
            totalSuccess = degreeStats.stream().mapToLong(stat -> n(stat.getSuccessCount())).sum();
            failCount = Math.max(totalSend - totalSuccess, 0);
        }

        return List.of(
                card("총 발송 건수", formatNumber(totalSend), ""),
                card("발송 성공률 / 실패 현황", formatRate(rate(totalSuccess, totalSend)) + "% / " + formatNumber(failCount) + "건", ""),
                card("활성 고객 수 (일반, 신규)", formatNumber(activeCustomers), ""),
                card("실제 청구 비용", formatWon(billingCost), ""),
                card("스마트 라우팅 절감 현황", formatWon(savingCost), "")
        );
    }

    private List<QueueStatus> buildQueueStatuses() {
        return List.of(
                queueStatus("대기", 0, "#94A3B8", "amber"),
                queueStatus("발송 중", 2500, "#3B82F6", "green"),
                queueStatus("완료", 12847, "#10B981", "green"),
                queueStatus("실패", 165, "#EF4444", "red")
        );
    }

    private List<RecentSend> buildRecentSends(List<SendHistoryVO> recentSends) {
        return recentSends.stream()
                .map(send -> RecentSend.builder()
                        .template(defaultText(send.getTitle(), "-"))
                        .sentAt(formatDateTime(send))
                        .targetType(defaultText(send.getPurpose(), "-"))
                        .count(n(send.getTotalTargetCount()))
                        .status(normalizeStatus(send.getStatus()))
                        .build())
                .toList();
    }

    private List<TemplatePerformance> buildTemplatePerformance(List<TemplatePerformance> templateTop) {
        return templateTop;
    }

    private List<QueueJob> buildQueueJobs() {
        return List.of(
                queueJob("Q-2401", "6월 여름 할인 이벤트", "카카오톡", "발송 중", 72, 284391, 204762),
                queueJob("Q-2402", "포인트 소멸 안내", "LMS", "대기", 0, 92841, 0),
                queueJob("Q-2403", "생일 축하 메시지", "SMS", "완료", 100, 1284, 1284),
                queueJob("Q-2404", "신규 가입 환영", "카카오톡", "실패", 91, 341, 338)
        );
    }

    private List<ProcessStep> buildProcessSteps() {
        return List.of(
                processStep("요청 접수", "완료", "done"),
                processStep("대상 검증", "완료", "done"),
                processStep("채널 라우팅", "진행 중", "active"),
                processStep("발송 처리", "진행 중", "active"),
                processStep("결과 집계", "대기", "")
        );
    }

    private StatChartResponse buildCostComparisonChart(List<MessageStatVO> stats) {
        List<MessageStatVO> sorted = latest(sortMessageStats(stats), 6);
        return StatChartResponse.builder()
                .chartId("costComparison")
                .title("비용 비교 현황")
                .type("line")
                .labels(dateLabels(sorted, MessageStatVO::getDate))
                .datasets(List.of(
                        dataset("실제 청구 비용", sorted.stream().map(stat -> (Number) toLong(stat.getBillingCost())).toList()),
                        dataset("최대 비용", sorted.stream().map(stat -> (Number) toLong(stat.getMaxCost())).toList())
                ))
                .build();
    }

    private StatChartResponse buildChannelShareChart(List<MessageStatByDegreeVO> stats, Map<Long, String> channelNames) {
        Map<Long, Long> sendsByChannel = stats.stream()
                .filter(stat -> stat.getChannelId() != null)
                .collect(Collectors.groupingBy(MessageStatByDegreeVO::getChannelId,
                        LinkedHashMap::new,
                        Collectors.summingLong(stat -> n(stat.getSendCount()))));
        long totalSend = sendsByChannel.values().stream().mapToLong(Long::longValue).sum();

        return StatChartResponse.builder()
                .chartId("channelShare")
                .title("채널별 발송 비중")
                .type("doughnut")
                .labels(sendsByChannel.keySet().stream().map(channelId -> channelName(channelNames, channelId)).toList())
                .datasets(List.of(dataset("발송 비중", sendsByChannel.values().stream()
                        .map(count -> (Number) round(rate(count, totalSend)))
                        .toList())))
                .build();
    }

    private StatChartResponse buildDailySendTrendChart(List<MessageStatVO> stats) {
        List<MessageStatVO> sorted = latest(sortMessageStats(stats), 7);
        return StatChartResponse.builder()
                .chartId("dailySendTrend")
                .title("일별 발송 추이")
                .type("area")
                .labels(dateLabels(sorted, MessageStatVO::getDate))
                .datasets(List.of(
                        dataset("발송", sorted.stream().map(stat -> (Number) n(stat.getTotalSendCount())).toList()),
                        dataset("성공", sorted.stream().map(stat -> (Number) n(stat.getTotalSuccessCount())).toList())
                ))
                .build();
    }

    private Map<Long, String> channelNames() {
        return statMapper.selectActiveChannels().stream()
                .filter(channel -> channel.getId() != null)
                .collect(Collectors.toMap(ChannelVO::getId,
                        channel -> displayChannelName(channel.getChannelType()),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private String normalizeStatus(String status) {
        String upper = defaultText(status, "").toUpperCase();
        if (upper.contains("FAIL") || upper.contains("ERROR") || statusContains(status, "실패")) {
            return "실패";
        }
        if (upper.contains("WAIT") || upper.contains("READY") || upper.contains("SCHEDULE") || statusContains(status, "대기")) {
            return "대기";
        }
        if (upper.contains("SEND") || upper.contains("PROGRESS") || statusContains(status, "진행") || statusContains(status, "발송 중")) {
            return "발송 중";
        }
        return "완료";
    }

    private boolean statusContains(String status, String keyword) {
        return status != null && status.contains(keyword);
    }

    private String displayChannelName(String channelType) {
        if (channelType == null) {
            return "-";
        }
        String upper = channelType.toUpperCase();
        if (upper.contains("KAKAO")) {
            return "카카오톡";
        }
        if (upper.contains("EMAIL") || upper.contains("MAIL")) {
            return "이메일";
        }
        return channelType;
    }

    private String channelName(Map<Long, String> channelNames, Long channelId) {
        return channelNames.getOrDefault(channelId, "채널 " + channelId);
    }

    private QueueStatus queueStatus(String label, long count, String color, String badge) {
        return QueueStatus.builder()
                .label(label)
                .count(count)
                .color(color)
                .badge(badge)
                .build();
    }

    private QueueJob queueJob(String id, String title, String channel, String status,
                                               int progress, long requested, long processed) {
        return QueueJob.builder()
                .id(id)
                .title(title)
                .channel(channel)
                .status(status)
                .progress(progress)
                .requested(requested)
                .processed(processed)
                .build();
    }

    private ProcessStep processStep(String title, String status, String state) {
        return ProcessStep.builder()
                .title(title)
                .status(status)
                .state(state)
                .build();
    }

    private StatCardResponse card(String title, String value, String subText) {
        return StatCardResponse.builder()
                .title(title)
                .value(value)
                .subText(subText)
                .build();
    }

    private StatChartDatasetResponse dataset(String label, List<Number> data) {
        return StatChartDatasetResponse.builder()
                .label(label)
                .data(data)
                .build();
    }

    private List<MessageStatVO> sortMessageStats(List<MessageStatVO> stats) {
        return stats.stream()
                .sorted(Comparator.comparing(MessageStatVO::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private <T> List<T> latest(List<T> items, int limit) {
        return items.size() <= limit ? items : items.subList(items.size() - limit, items.size());
    }

    private <T> List<String> dateLabels(List<T> stats, Function<T, LocalDate> dateGetter) {
        return stats.stream().map(dateGetter).map(this::dateLabel).toList();
    }

    private String dateLabel(LocalDate date) {
        return date == null ? "-" : date.format(DATE_LABEL_FORMATTER);
    }

    private String formatDateTime(SendHistoryVO send) {
        var dateTime = send.getCompletedAt() != null
                ? send.getCompletedAt()
                : send.getScheduledAt() != null ? send.getScheduledAt() : send.getCreatedAt();
        return dateTime == null ? "-" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private CustomerStatVO latestCustomerStat(List<CustomerStatVO> stats) {
        return stats.stream()
                .max(Comparator.comparing(CustomerStatVO::getDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseGet(CustomerStatVO::new);
    }

    private BigDecimal sum(List<MessageStatVO> stats, Function<MessageStatVO, BigDecimal> getter) {
        return stats.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatNumber(long value) {
        return NUMBER_FORMATTER.format(value);
    }

    private String formatWon(BigDecimal value) {
        return formatNumber(toLong(value)) + "원";
    }

    private String formatRate(double value) {
        return RATE_FORMATTER.format(value);
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return ((double) numerator / denominator) * 100;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal n(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long toLong(BigDecimal value) {
        return n(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
