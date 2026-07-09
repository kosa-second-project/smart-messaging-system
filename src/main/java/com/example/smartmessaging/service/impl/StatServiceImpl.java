package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatCardResponse;
import com.example.smartmessaging.dto.response.StatChartDatasetResponse;
import com.example.smartmessaging.dto.response.StatChartResponse;
import com.example.smartmessaging.dto.response.StatPageResponse;
import com.example.smartmessaging.dto.response.StatTableResponse;
import com.example.smartmessaging.dto.vo.ChannelStatVO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.ClickStatVO;
import com.example.smartmessaging.dto.vo.CustomerChannelConsentSummaryVO;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import com.example.smartmessaging.service.repository.StatMapper;
import com.example.smartmessaging.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatServiceImpl implements StatService {

    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("M/d");

    private final StatMapper statMapper;

    @Override
    public StatPageResponse getDeliveryStats(StatSearchRequest request) {
        List<MessageStatVO> messageStats = statMapper.selectMessageStats(request);
        List<MessageStatByDegreeVO> degreeStats = statMapper.selectDeliveryMessageStatByDegrees(request);
        DegreeStatsSummary degreeSummary = summarizeDegreeStats(degreeStats);
        Map<Long, String> channelNames = channelNames();

        return StatPageResponse.builder()
                .cards(buildDeliveryCards(request, messageStats))
                .charts(List.of(
                        buildChannelTrendChart(degreeSummary, channelNames, "channelTrend", "채널별 발송 현황", "bar"),
                        buildSendSuccessTrendChart(messageStats),
                        buildFallbackSuccessChart(degreeSummary, channelNames)
                ))
                .tables(List.of())
                .build();
    }

    @Override
    public StatPageResponse getChannelStats(StatSearchRequest request) {
        List<MessageStatByDegreeVO> degreeStats = statMapper.selectDeliveryMessageStatByDegrees(request);
        DegreeStatsSummary degreeSummary = summarizeDegreeStats(degreeStats);
        Map<Long, String> channelNames = channelNames();

        return StatPageResponse.builder()
                .cards(List.of())
                .charts(List.of(
                        buildChannelSuccessRateChart(degreeSummary, channelNames),
                        buildChannelTrendChart(degreeSummary, channelNames, "channelTrend", "채널별 추이", "line"),
                        buildChannelShareChart(degreeSummary, channelNames)
                ))
                .tables(List.of(buildChannelCostTable(degreeSummary, channelNames)))
                .build();
    }

    @Override
    public StatPageResponse getCostStats(StatSearchRequest request) {
        List<MessageStatVO> messageStats = statMapper.selectMessageStats(request);
        List<MessageStatByDegreeVO> degreeStats = statMapper.selectDeliveryMessageStatByDegrees(request);

        return StatPageResponse.builder()
                .cards(buildCostCards(messageStats, degreeStats))
                .charts(List.of(
                        buildCostComparisonChart(messageStats),
                        buildCostSavingsChart(messageStats)
                ))
                .tables(List.of())
                .build();
    }

    @Override
    public StatPageResponse getCustomerStats(StatSearchRequest request) {
        List<CustomerStatVO> customerStats = statMapper.selectCustomerStats(request);
        List<CustomerChannelConsentSummaryVO> consents = statMapper.selectCustomerChannelConsents(request);
        Map<Long, String> channelNames = channelNames();

        return StatPageResponse.builder()
                .cards(buildCustomerCards(customerStats))
                .charts(List.of(buildNewCustomerTrendChart(customerStats)))
                .tables(List.of(buildCustomerConsentTable(consents, channelNames)))
                .build();
    }

    @Override
    public StatPageResponse getPerformanceStats(StatSearchRequest request) {
        List<ChannelStatVO> channelStats = statMapper.selectChannelStats(request);
        List<ClickStatVO> clickStats = statMapper.selectPerformanceClickStats(request);
        List<ClickStatVO> conversionStats = statMapper.selectPerformanceConversionStats(request);

        return StatPageResponse.builder()
                .cards(buildPerformanceCards(request, channelStats))
                .charts(List.of(
                        buildPerformanceTrendChart(clickStats, conversionStats),
                        buildWeekdayClickChart(clickStats, conversionStats),
                        buildHourlyClickChart(clickStats, conversionStats)
                ))
                .tables(List.of())
                .build();
    }

    private List<StatCardResponse> buildDeliveryCards(StatSearchRequest request, List<MessageStatVO> stats) {
        long totalSend = stats.stream().mapToLong(stat -> n(stat.getTotalSendCount())).sum();
        long totalSuccess = stats.stream().mapToLong(stat -> n(stat.getTotalSuccessCount())).sum();
        long failCount = Math.max(totalSend - totalSuccess, 0);
        BigDecimal billingCost = sumMessageCost(stats, MessageStatVO::getBillingCost);
        BigDecimal maxCost = sumMessageCost(stats, MessageStatVO::getMaxCost);
        BigDecimal savingCost = maxCost.subtract(billingCost).max(BigDecimal.ZERO);
        long averageSend = totalSend == 0 ? 0 : Math.round((double) totalSend / periodDays(request, stats));

        return List.of(
                card("총 발송", formatNumber(totalSend), periodText(request, stats)),
                card("평균 성공률", formatRate(rate(totalSuccess, totalSend)) + "%", "실패 " + formatNumber(failCount) + "건"),
                card("기간 평균 발송", formatNumber(averageSend), "일 평균 기준"),
                card("실제 청구 비용", formatWon(billingCost), "선택 기간 누적"),
                card("스마트 라우팅 절감", formatWon(savingCost), "최대 비용 대비")
        );
    }

    private List<StatCardResponse> buildCostCards(List<MessageStatVO> stats, List<MessageStatByDegreeVO> degreeStats) {
        BigDecimal billingCost = sumMessageCost(stats, MessageStatVO::getBillingCost);
        BigDecimal maxCost = sumMessageCost(stats, MessageStatVO::getMaxCost);
        BigDecimal savingCost = maxCost.subtract(billingCost).max(BigDecimal.ZERO);
        long fallbackCount = degreeStats.stream()
                .filter(stat -> n(stat.getDegree()) > 1)
                .mapToLong(stat -> n(stat.getSendCount()))
                .sum();
        long totalSend = stats.stream().mapToLong(stat -> n(stat.getTotalSendCount())).sum();

        return List.of(
                card("실제 청구 비용", formatWon(billingCost), "선택 기간 누적"),
                card("최대 비용", formatWon(maxCost), "동일 물량 기준"),
                card("기간 절감액", formatWon(savingCost), "절감률 " + formatRate(rate(savingCost, maxCost)) + "%"),
                card("대체 발송 전환", formatNumber(fallbackCount), "전환율 " + formatRate(rate(fallbackCount, totalSend)) + "%")
        );
    }

    private List<StatCardResponse> buildCustomerCards(List<CustomerStatVO> stats) {
        CustomerStatVO latest = latestCustomerStat(stats);
        long newCustomers = n(latest.getNewCustomerCount());
        long joinedCustomers = n(latest.getJoinedCustomerCount());

        return List.of(
                card("전체 고객", formatNumber(n(latest.getTotalCustomerCount())), "분석 가능 고객"),
                card("일반 고객", formatNumber(n(latest.getNormalCustomerCount())), "주요 발송 대상"),
                card("신규 고객", formatNumber(newCustomers), "최근 7일 동안 가입한 고객 수"),
                card("가입 고객", formatNumber(joinedCustomers), "어제 가입한 고객 수")
        );
    }

    private List<StatCardResponse> buildPerformanceCards(StatSearchRequest request, List<ChannelStatVO> stats) {
        long clickTarget = stats.stream().mapToLong(stat -> n(stat.getClickTargetCount())).sum();
        long clickCount = stats.stream().mapToLong(stat -> n(stat.getClickCount())).sum();
        long conversionTarget = stats.stream().mapToLong(stat -> n(stat.getConversionTargetCount())).sum();
        long conversionCount = stats.stream().mapToLong(stat -> n(stat.getConversionCount())).sum();
        long consentTarget = stats.stream().mapToLong(stat -> n(stat.getConsentTargetCount())).sum();
        long consentCount = stats.stream().mapToLong(stat -> n(stat.getConsentCount())).sum();
        String channelName = performanceChannelName(request.getChannel());

        return List.of(
                card("클릭수 / 전체 클릭대상수", formatNumber(clickCount) + " / " + formatNumber(clickTarget),
                        channelName + " 클릭률 " + formatRate(rate(clickCount, clickTarget)) + "%"),
                card("전환수 / 전체 전환대상수", formatNumber(conversionCount) + " / " + formatNumber(conversionTarget),
                        channelName + " 전환율 " + formatRate(rate(conversionCount, conversionTarget)) + "%"),
                card("동의수 / 전체 동의대상수", formatNumber(consentCount) + " / " + formatNumber(consentTarget),
                        channelName + " 동의율 " + formatRate(rate(consentCount, consentTarget)) + "%"),
                card("수신거부수 / 전체 대상수", "0 / " + formatNumber(consentTarget),
                        channelName + " 기준 동의율 " + formatRate(rate(consentCount, consentTarget)) + "%")
        );
    }

    private StatChartResponse buildSendSuccessTrendChart(List<MessageStatVO> stats) {
        List<MessageStatVO> sorted = sortMessageStats(stats);

        return StatChartResponse.builder()
                .chartId("sendSuccessTrend")
                .title("발송 & 성공 추이")
                .type("area")
                .labels(dateLabels(sorted, MessageStatVO::getDate))
                .datasets(List.of(
                        dataset("발송", sorted.stream().map(stat -> (Number) n(stat.getTotalSendCount())).toList()),
                        dataset("성공", sorted.stream().map(stat -> (Number) n(stat.getTotalSuccessCount())).toList()),
                        dataset("성공률", sorted.stream()
                                .map(stat -> (Number) round(rate(n(stat.getTotalSuccessCount()), n(stat.getTotalSendCount()))))
                                .toList())
                ))
                .build();
    }

    private StatChartResponse buildChannelTrendChart(DegreeStatsSummary summary, Map<Long, String> channelNames,
                                                     String chartId, String title, String type) {
        return StatChartResponse.builder()
                .chartId(chartId)
                .title(title)
                .type(type)
                .labels(summary.dates().stream().map(this::dateLabel).toList())
                .datasets(summary.channelIds().stream()
                        .map(channelId -> dataset(channelName(channelNames, channelId), summary.dates().stream()
                                .map(date -> (Number) summary.sendByDateAndChannel(date, channelId))
                                .toList()))
                        .toList())
                .build();
    }

    private StatChartResponse buildFallbackSuccessChart(DegreeStatsSummary summary, Map<Long, String> channelNames) {
        List<Integer> fallbackDegrees = List.of(1, 2, 3);

        return StatChartResponse.builder()
                .chartId("fallbackSuccess")
                .title("Fallback 채널별 성공률")
                .type("bar")
                .labels(fallbackDegrees.stream().map(degree -> degree + "차").toList())
                .datasets(summary.channelIds().stream()
                        .map(channelId -> dataset(channelName(channelNames, channelId), fallbackDegrees.stream()
                                .map(degree -> (Number) round(summary.successRateByDegreeAndChannel(degree, channelId)))
                                .toList()))
                        .toList())
                .build();
    }

    private StatChartResponse buildChannelSuccessRateChart(DegreeStatsSummary summary, Map<Long, String> channelNames) {
        return StatChartResponse.builder()
                .chartId("channelSuccessRate")
                .title("채널별 성공률")
                .type("bar")
                .labels(summary.channelIds().stream().map(channelId -> channelName(channelNames, channelId)).toList())
                .datasets(List.of(dataset("성공률", summary.channelIds().stream()
                        .map(channelId -> (Number) round(summary.successRateByChannel(channelId)))
                        .toList())))
                .build();
    }

    private StatChartResponse buildChannelShareChart(DegreeStatsSummary summary, Map<Long, String> channelNames) {
        return StatChartResponse.builder()
                .chartId("channelShare")
                .title("채널별 발송 비중")
                .type("doughnut")
                .labels(summary.channelIds().stream().map(channelId -> channelName(channelNames, channelId)).toList())
                .datasets(List.of(dataset("발송 비중", summary.channelIds().stream()
                        .map(channelId -> (Number) round(rate(summary.sendByChannel(channelId), summary.totalSend())))
                        .toList())))
                .build();
    }

    private StatTableResponse buildChannelCostTable(DegreeStatsSummary summary, Map<Long, String> channelNames) {
        List<List<String>> rows = summary.channelIds().stream()
                .map(channelId -> List.of(
                        channelName(channelNames, channelId),
                        formatNumber(summary.sendByChannel(channelId)) + "건",
                        formatRate(summary.successRateByChannel(channelId)) + "%",
                        formatWon(summary.costByChannel(channelId))
                ))
                .toList();

        return StatTableResponse.builder()
                .tableId("channelCost")
                .title("채널별 비용")
                .columns(List.of("채널", "발송수", "성공률", "총 비용"))
                .rows(rows)
                .build();
    }

    private StatChartResponse buildCostComparisonChart(List<MessageStatVO> stats) {
        List<MessageStatVO> sorted = sortMessageStats(stats);

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

    private StatChartResponse buildCostSavingsChart(List<MessageStatVO> stats) {
        List<MessageStatVO> sorted = sortMessageStats(stats);

        return StatChartResponse.builder()
                .chartId("costSavings")
                .title("절감액 추이")
                .type("line")
                .labels(dateLabels(sorted, MessageStatVO::getDate))
                .datasets(List.of(dataset("절감액", sorted.stream()
                        .map(stat -> (Number) toLong(n(stat.getMaxCost()).subtract(n(stat.getBillingCost())).max(BigDecimal.ZERO)))
                        .toList())))
                .build();
    }

    private StatChartResponse buildNewCustomerTrendChart(List<CustomerStatVO> stats) {
        List<CustomerStatVO> sorted = stats.stream()
                .sorted(Comparator.comparing(CustomerStatVO::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return StatChartResponse.builder()
                .chartId("newCustomerTrend")
                .title("신규 고객 추이")
                .type("bar")
                .labels(dateLabels(sorted, CustomerStatVO::getDate))
                .datasets(List.of(dataset("신규 고객", sorted.stream()
                        .map(stat -> (Number) n(stat.getNewCustomerCount()))
                        .toList())))
                .build();
    }

    private StatTableResponse buildCustomerConsentTable(List<CustomerChannelConsentSummaryVO> consents, Map<Long, String> channelNames) {
        List<List<String>> rows = consents.stream()
                .filter(consent -> consent.getChannelId() != null)
                .map(consent -> {
                    long total = n(consent.getTotalCount());
                    long consented = n(consent.getConsentedCount());
                    long rejected = consent.getRejectedCount() == null
                            ? Math.max(total - consented, 0)
                            : n(consent.getRejectedCount());
                    return List.of(
                            channelName(channelNames, consent.getChannelId()),
                            formatNumber(consented) + "명",
                            formatNumber(rejected) + "명",
                            formatRate(rate(consented, total)) + "%"
                    );
                })
                .toList();

        return StatTableResponse.builder()
                .tableId("customerConsent")
                .title("채널별 동의 현황")
                .columns(List.of("채널", "동의", "미동의", "동의율"))
                .rows(rows)
                .build();
    }

    private StatChartResponse buildPerformanceTrendChart(List<ClickStatVO> clickStats, List<ClickStatVO> conversionStats) {
        List<LocalDate> dates = sortedPerformanceDates(clickStats, conversionStats);

        return StatChartResponse.builder()
                .chartId("performanceTrend")
                .title("클릭/전환 추이")
                .type("line")
                .labels(dates.stream().map(this::dateLabel).toList())
                .datasets(List.of(
                        dataset("클릭수", dates.stream()
                                .map(date -> (Number) totalByDate(clickStats, date))
                                .toList()),
                        dataset("전환수", dates.stream()
                                .map(date -> (Number) totalByDate(conversionStats, date))
                                .toList())
                ))
                .build();
    }

    private StatChartResponse buildWeekdayClickChart(List<ClickStatVO> clickStats, List<ClickStatVO> conversionStats) {
        Map<DayOfWeek, Long> clicksByWeekday = clickStats.stream()
                .filter(stat -> stat.getStatDate() != null)
                .collect(Collectors.groupingBy(stat -> stat.getStatDate().getDayOfWeek(),
                        Collectors.summingLong(this::totalHourlyClick)));
        Map<DayOfWeek, Long> conversionsByWeekday = conversionStats.stream()
                .filter(stat -> stat.getStatDate() != null)
                .collect(Collectors.groupingBy(stat -> stat.getStatDate().getDayOfWeek(),
                        Collectors.summingLong(this::totalHourlyClick)));
        List<DayOfWeek> weekdays = List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        );

        return StatChartResponse.builder()
                .chartId("weekdayClick")
                .title("요일별 클릭/전환수")
                .type("bar")
                .labels(List.of("월", "화", "수", "목", "금", "토", "일"))
                .datasets(List.of(
                        dataset("클릭수", weekdays.stream()
                                .map(day -> (Number) clicksByWeekday.getOrDefault(day, 0L))
                                .toList()),
                        dataset("전환수", weekdays.stream()
                                .map(day -> (Number) conversionsByWeekday.getOrDefault(day, 0L))
                                .toList())
                ))
                .build();
    }

    private StatChartResponse buildHourlyClickChart(List<ClickStatVO> clickStats, List<ClickStatVO> conversionStats) {
        List<Number> hourlyClicks = new ArrayList<>();
        List<Number> hourlyConversions = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            final int hourIndex = hour;
            hourlyClicks.add(clickStats.stream().mapToLong(stat -> hourlyClick(stat, hourIndex)).sum());
            hourlyConversions.add(conversionStats.stream().mapToLong(stat -> hourlyClick(stat, hourIndex)).sum());
        }

        return StatChartResponse.builder()
                .chartId("hourlyClick")
                .title("시간별 클릭/전환수")
                .type("line")
                .labels(List.of("00시", "01시", "02시", "03시", "04시", "05시", "06시", "07시",
                        "08시", "09시", "10시", "11시", "12시", "13시", "14시", "15시",
                        "16시", "17시", "18시", "19시", "20시", "21시", "22시", "23시"))
                .datasets(List.of(
                        dataset("클릭수", hourlyClicks),
                        dataset("전환수", hourlyConversions)
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

    private List<ClickStatVO> sortClickStats(List<ClickStatVO> stats) {
        return stats.stream()
                .sorted(Comparator.comparing(ClickStatVO::getStatDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private <T> List<String> dateLabels(List<T> stats, Function<T, LocalDate> dateGetter) {
        return stats.stream()
                .map(dateGetter)
                .map(this::dateLabel)
                .toList();
    }

    private String dateLabel(LocalDate date) {
        return date == null ? "-" : date.format(DATE_LABEL_FORMATTER);
    }

    private List<LocalDate> sortedDates(List<MessageStatByDegreeVO> stats) {
        return stats.stream()
                .map(MessageStatByDegreeVO::getDate)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private List<Long> sortedChannelIds(List<MessageStatByDegreeVO> stats) {
        return stats.stream()
                .map(MessageStatByDegreeVO::getChannelId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private String channelName(Map<Long, String> channelNames, Long channelId) {
        return channelNames.getOrDefault(channelId, "채널 " + channelId);
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

    private String performanceChannelName(String channelType) {
        if (channelType == null || channelType.isBlank()) {
            return "전체";
        }
        return displayChannelName(channelType);
    }

    private DegreeStatsSummary summarizeDegreeStats(List<MessageStatByDegreeVO> stats) {
        List<LocalDate> dates = sortedDates(stats);
        List<Long> channelIds = sortedChannelIds(stats);
        List<Integer> degrees = stats.stream()
                .map(MessageStatByDegreeVO::getDegree)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        Map<DateChannelKey, Long> sendByDateAndChannel = new LinkedHashMap<>();
        Map<Long, Long> sendByChannel = new LinkedHashMap<>();
        Map<Long, Long> successByChannel = new LinkedHashMap<>();
        Map<Long, BigDecimal> costByChannel = new LinkedHashMap<>();
        Map<DegreeChannelKey, Long> sendByDegreeAndChannel = new LinkedHashMap<>();
        Map<DegreeChannelKey, Long> successByDegreeAndChannel = new LinkedHashMap<>();
        long totalSend = 0;

        for (MessageStatByDegreeVO stat : stats) {
            long sendCount = n(stat.getSendCount());
            long successCount = n(stat.getSuccessCount());
            totalSend += sendCount;

            Long channelId = stat.getChannelId();
            if (channelId != null) {
                sendByChannel.merge(channelId, sendCount, Long::sum);
                successByChannel.merge(channelId, successCount, Long::sum);
                costByChannel.merge(channelId, n(stat.getCost()), BigDecimal::add);
            }

            LocalDate date = stat.getDate();
            if (date != null && channelId != null) {
                sendByDateAndChannel.merge(new DateChannelKey(date, channelId), sendCount, Long::sum);
            }

            Integer degree = stat.getDegree();
            if (degree != null && channelId != null) {
                DegreeChannelKey key = new DegreeChannelKey(degree, channelId);
                sendByDegreeAndChannel.merge(key, sendCount, Long::sum);
                successByDegreeAndChannel.merge(key, successCount, Long::sum);
            }
        }

        return new DegreeStatsSummary(
                dates,
                channelIds,
                degrees,
                sendByDateAndChannel,
                sendByChannel,
                successByChannel,
                costByChannel,
                sendByDegreeAndChannel,
                successByDegreeAndChannel,
                totalSend
        );
    }

    private record DateChannelKey(LocalDate date, Long channelId) {
    }

    private record DegreeChannelKey(Integer degree, Long channelId) {
    }

    private record DegreeStatsSummary(
            List<LocalDate> dates,
            List<Long> channelIds,
            List<Integer> degrees,
            Map<DateChannelKey, Long> sendByDateAndChannel,
            Map<Long, Long> sendByChannel,
            Map<Long, Long> successByChannel,
            Map<Long, BigDecimal> costByChannel,
            Map<DegreeChannelKey, Long> sendByDegreeAndChannel,
            Map<DegreeChannelKey, Long> successByDegreeAndChannel,
            long totalSend
    ) {

        long sendByDateAndChannel(LocalDate date, Long channelId) {
            return sendByDateAndChannel.getOrDefault(new DateChannelKey(date, channelId), 0L);
        }

        long sendByChannel(Long channelId) {
            return sendByChannel.getOrDefault(channelId, 0L);
        }

        double successRateByChannel(Long channelId) {
            return rate(successByChannel.getOrDefault(channelId, 0L), sendByChannel(channelId));
        }

        double successRateByDegreeAndChannel(Integer degree, Long channelId) {
            DegreeChannelKey key = new DegreeChannelKey(degree, channelId);
            return rate(
                    successByDegreeAndChannel.getOrDefault(key, 0L),
                    sendByDegreeAndChannel.getOrDefault(key, 0L)
            );
        }

        BigDecimal costByChannel(Long channelId) {
            return costByChannel.getOrDefault(channelId, BigDecimal.ZERO);
        }

        private double rate(long numerator, long denominator) {
            if (denominator == 0) {
                return 0;
            }
            return ((double) numerator / denominator) * 100;
        }
    }

    private CustomerStatVO latestCustomerStat(List<CustomerStatVO> stats) {
        return stats.stream()
                .max(Comparator.comparing(CustomerStatVO::getDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseGet(CustomerStatVO::new);
    }

    private String periodText(StatSearchRequest request, List<MessageStatVO> stats) {
        if (request.getFrom() != null && request.getTo() != null) {
            return request.getFrom() + " ~ " + request.getTo() + " (" + periodDays(request, stats) + "일)";
        }
        List<MessageStatVO> sorted = sortMessageStats(stats);
        if (sorted.isEmpty()) {
            return "조회 데이터 없음";
        }
        LocalDate from = sorted.get(0).getDate();
        LocalDate to = sorted.get(sorted.size() - 1).getDate();
        return from + " ~ " + to + " (" + Math.max(ChronoUnit.DAYS.between(from, to) + 1, 1) + "일)";
    }

    private long periodDays(StatSearchRequest request, List<MessageStatVO> stats) {
        if (request.getFrom() != null && request.getTo() != null) {
            return Math.max(ChronoUnit.DAYS.between(request.getFrom(), request.getTo()) + 1, 1);
        }
        return Math.max(stats.stream().map(MessageStatVO::getDate).filter(Objects::nonNull).distinct().count(), 1);
    }

    private long totalHourlyClick(ClickStatVO stat) {
        long total = 0;
        for (int hour = 0; hour < 24; hour++) {
            total += hourlyClick(stat, hour);
        }
        return total;
    }

    private long totalByDate(List<ClickStatVO> stats, LocalDate date) {
        return stats.stream()
                .filter(stat -> Objects.equals(stat.getStatDate(), date))
                .mapToLong(this::totalHourlyClick)
                .sum();
    }

    private List<LocalDate> sortedPerformanceDates(List<ClickStatVO> clickStats, List<ClickStatVO> conversionStats) {
        List<LocalDate> dates = new ArrayList<>();
        clickStats.stream()
                .map(ClickStatVO::getStatDate)
                .filter(Objects::nonNull)
                .forEach(dates::add);
        conversionStats.stream()
                .map(ClickStatVO::getStatDate)
                .filter(Objects::nonNull)
                .forEach(dates::add);
        return dates.stream()
                .distinct()
                .sorted()
                .toList();
    }

    private long hourlyClick(ClickStatVO stat, int hour) {
        return switch (hour) {
            case 0 -> n(stat.getHour00());
            case 1 -> n(stat.getHour01());
            case 2 -> n(stat.getHour02());
            case 3 -> n(stat.getHour03());
            case 4 -> n(stat.getHour04());
            case 5 -> n(stat.getHour05());
            case 6 -> n(stat.getHour06());
            case 7 -> n(stat.getHour07());
            case 8 -> n(stat.getHour08());
            case 9 -> n(stat.getHour09());
            case 10 -> n(stat.getHour10());
            case 11 -> n(stat.getHour11());
            case 12 -> n(stat.getHour12());
            case 13 -> n(stat.getHour13());
            case 14 -> n(stat.getHour14());
            case 15 -> n(stat.getHour15());
            case 16 -> n(stat.getHour16());
            case 17 -> n(stat.getHour17());
            case 18 -> n(stat.getHour18());
            case 19 -> n(stat.getHour19());
            case 20 -> n(stat.getHour20());
            case 21 -> n(stat.getHour21());
            case 22 -> n(stat.getHour22());
            case 23 -> n(stat.getHour23());
            default -> 0;
        };
    }

    private BigDecimal sumMessageCost(List<MessageStatVO> stats, Function<MessageStatVO, BigDecimal> getter) {
        return stats.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatNumber(long value) {
        return new DecimalFormat("#,###").format(value);
    }

    private String formatWon(BigDecimal value) {
        return formatNumber(toLong(value)) + "원";
    }

    private String formatRate(double value) {
        return new DecimalFormat("0.0").format(value);
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return ((double) numerator / denominator) * 100;
    }

    private double rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private long n(Long value) {
        return value == null ? 0 : value;
    }

    private BigDecimal n(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long toLong(BigDecimal value) {
        return n(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
