package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.StatCardResponse;
import com.example.smartmessaging.dto.response.StatChartDatasetResponse;
import com.example.smartmessaging.dto.response.StatChartResponse;
import com.example.smartmessaging.dto.response.StatPageResponse;
import com.example.smartmessaging.dto.response.StatTableResponse;
import com.example.smartmessaging.service.StatService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatServiceImpl implements StatService {

    @Override
    public StatPageResponse getDeliveryStats(StatSearchRequest request) {
        return StatPageResponse.builder()
                .cards(buildDeliveryCards())
                .charts(buildDeliveryCharts())
                .tables(buildDeliveryTables())
                .build();
    }

    @Override
    public StatPageResponse getChannelStats(StatSearchRequest request) {
        return StatPageResponse.builder()
                .cards(buildChannelCards())
                .charts(buildChannelCharts())
                .tables(buildChannelTables())
                .build();
    }

    @Override
    public StatPageResponse getCostStats(StatSearchRequest request) {
        return StatPageResponse.builder()
                .cards(buildCostCards())
                .charts(buildCostCharts())
                .tables(buildCostTables())
                .build();
    }

    @Override
    public StatPageResponse getCustomerStats(StatSearchRequest request) {
        return StatPageResponse.builder()
                .cards(buildCustomerCards())
                .charts(buildCustomerCharts())
                .tables(buildCustomerTables())
                .build();
    }

    @Override
    public StatPageResponse getPerformanceStats(StatSearchRequest request) {
        return StatPageResponse.builder()
                .cards(buildPerformanceCards())
                .charts(buildPerformanceCharts())
                .tables(buildPerformanceTables())
                .build();
    }

    private List<StatCardResponse> buildDeliveryCards() {
        return List.of(
                StatCardResponse.builder()
                        .title("총 발송")
                        .value("892,451")
                        .subText("2026-06-23 ~ 2026-06-29 (7일)")
                        .build(),
                StatCardResponse.builder()
                        .title("평균 성공률")
                        .value("98.7%")
                        .subText("실패 165건")
                        .build(),
                StatCardResponse.builder()
                        .title("기간 평균 발송")
                        .value("127,493")
                        .subText("일 평균 기준")
                        .build(),
                StatCardResponse.builder()
                        .title("실제 청구 비용")
                        .value("18,700,000원")
                        .subText("선택 기간 누적")
                        .build(),
                StatCardResponse.builder()
                        .title("스마트 라우팅 절감")
                        .value("5,500,000원")
                        .subText("최대 비용 대비")
                        .build()
        );
    }

    private List<StatChartResponse> buildDeliveryCharts() {
        return List.of(
                StatChartResponse.builder()
                        .chartId("channelTrend")
                        .title("채널별 발송 현황")
                        .type("bar")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("카카오톡")
                                        .data(List.<Number>of(82000, 94400, 116000, 133200, 70100, 90200, 107500))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("SMS")
                                        .data(List.<Number>of(43800, 50500, 62000, 71200, 37500, 48200, 57500))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("LMS")
                                        .data(List.<Number>of(16200, 18700, 22900, 26300, 13900, 17800, 21200))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("이메일")
                                        .data(List.<Number>of(7600, 8800, 10800, 12400, 6500, 8400, 10000))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("sendSuccessTrend")
                        .title("발송 & 성공 추이")
                        .type("area")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("발송")
                                        .data(List.<Number>of(58838, 72655, 125996, 100289, 114106, 55923, 69740))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("성공")
                                        .data(List.<Number>of(57602, 71347, 124358, 99186, 113079, 54469, 68205))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("성공률")
                                        .data(List.<Number>of(97.9, 98.2, 98.7, 98.9, 99.1, 97.4, 97.8))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("fallbackSuccess")
                        .title("Fallback 채널별 성공률")
                        .type("bar")
                        .labels(List.of("1차", "2차", "3차"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("카카오")
                                        .data(List.<Number>of(99.2, 98.8, 98.1))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("SMS")
                                        .data(List.<Number>of(98.7, 98.2, 97.6))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("LMS")
                                        .data(List.<Number>of(97.9, 97.3, 96.8))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<StatTableResponse> buildDeliveryTables() {
        return List.of();
    }

    private List<StatCardResponse> buildChannelCards() {
        return List.of(
                StatCardResponse.builder()
                        .title("카카오톡 발송")
                        .value("535,279")
                        .subText("성공률 99.1%")
                        .build(),
                StatCardResponse.builder()
                        .title("SMS 발송")
                        .value("249,886")
                        .subText("성공률 99.1%")
                        .build(),
                StatCardResponse.builder()
                        .title("LMS 발송")
                        .value("80,241")
                        .subText("성공률 98.2%")
                        .build(),
                StatCardResponse.builder()
                        .title("이메일 발송")
                        .value("26,739")
                        .subText("성공률 97.8%")
                        .build()
        );
    }

    private List<StatChartResponse> buildChannelCharts() {
        return List.of(
                StatChartResponse.builder()
                        .chartId("channelSuccessRate")
                        .title("채널별 성공률")
                        .type("bar")
                        .labels(List.of("카카오톡", "SMS", "LMS", "이메일"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("성공률")
                                        .data(List.<Number>of(99.1, 99.1, 98.2, 97.8))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("channelTrend")
                        .title("채널별 발송 추이")
                        .type("line")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("카카오톡")
                                        .data(List.<Number>of(82000, 94400, 116000, 133200, 70100, 90200, 107500))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("SMS")
                                        .data(List.<Number>of(43800, 50500, 62000, 71200, 37500, 48200, 57500))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("LMS")
                                        .data(List.<Number>of(16200, 18700, 22900, 26300, 13900, 17800, 21200))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("이메일")
                                        .data(List.<Number>of(7600, 8800, 10800, 12400, 6500, 8400, 10000))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("channelShare")
                        .title("채널별 발송 비중")
                        .type("doughnut")
                        .labels(List.of("카카오톡", "SMS", "LMS", "이메일"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("발송 비중")
                                        .data(List.<Number>of(60, 28, 9, 3))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<StatTableResponse> buildChannelTables() {
        return List.of(
                StatTableResponse.builder()
                        .tableId("channelCost")
                        .title("채널별 비용")
                        .columns(List.of("채널", "발송량", "성공률", "총 비용"))
                        .rows(List.of(
                                List.of("카카오톡", "535,279건", "99.1%", "3,586,503원"),
                                List.of("SMS", "249,886건", "99.1%", "2,498,860원"),
                                List.of("LMS", "80,241건", "98.2%", "2,407,230원"),
                                List.of("이메일", "26,739건", "97.8%", "80,217원")
                        ))
                        .build()
        );
    }

    private List<StatCardResponse> buildCostCards() {
        return List.of(
                StatCardResponse.builder()
                        .title("실제 청구 비용")
                        .value("18,700,000원")
                        .subText("선택 기간 누적")
                        .build(),
                StatCardResponse.builder()
                        .title("최대 비용")
                        .value("24,200,000원")
                        .subText("동일 물량 기준")
                        .build(),
                StatCardResponse.builder()
                        .title("기간 절감액")
                        .value("5,500,000원")
                        .subText("절감률 22.7%")
                        .build(),
                StatCardResponse.builder()
                        .title("대체 발송 전환")
                        .value("5,549")
                        .subText("전환율 1.9%")
                        .build()
        );
    }

    private List<StatChartResponse> buildCostCharts() {
        return List.of(
                StatChartResponse.builder()
                        .chartId("costComparison")
                        .title("비용 비교 현황")
                        .type("line")
                        .labels(List.of("1월", "2월", "3월", "4월", "5월", "6월"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("실제 청구 비용")
                                        .data(List.<Number>of(9800000, 10600000, 12100000, 13800000, 16200000, 18700000))
                                        .build(),
                                StatChartDatasetResponse.builder()
                                        .label("최대 비용")
                                        .data(List.<Number>of(11200000, 12450000, 14900000, 16950000, 20700000, 24200000))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("costSavings")
                        .title("절감액 추이")
                        .type("line")
                        .labels(List.of("1월", "2월", "3월", "4월", "5월", "6월"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("절감액")
                                        .data(List.<Number>of(1400000, 1850000, 2800000, 3150000, 4500000, 5500000))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<StatTableResponse> buildCostTables() {
        return List.of();
    }

    private List<StatCardResponse> buildCustomerCards() {
        return List.of(
                StatCardResponse.builder()
                        .title("전체 고객")
                        .value("198,341")
                        .subText("분석 가능 고객")
                        .build(),
                StatCardResponse.builder()
                        .title("일반 고객")
                        .value("127,721")
                        .subText("주요 발송 대상")
                        .build(),
                StatCardResponse.builder()
                        .title("신규 고객")
                        .value("2,184")
                        .subText("7일 누적 가입")
                        .build(),
                StatCardResponse.builder()
                        .title("휴면 고객")
                        .value("18,940")
                        .subText("6개월 이상 미활동")
                        .build()
        );
    }

    private List<StatChartResponse> buildCustomerCharts() {
        return List.of(
                StatChartResponse.builder()
                        .chartId("newCustomerTrend")
                        .title("신규 고객 추이")
                        .type("bar")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("신규 고객")
                                        .data(List.<Number>of(280, 312, 348, 290, 361, 295, 298))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<StatTableResponse> buildCustomerTables() {
        return List.of(
                StatTableResponse.builder()
                        .tableId("customerConsent")
                        .title("채널별 동의 현황")
                        .columns(List.of("채널", "동의", "미동의", "동의율"))
                        .rows(List.of(
                                List.of("메시지", "197,709명", "108,818명", "64.5%"),
                                List.of("카카오톡", "240,317명", "66,210명", "78.4%"),
                                List.of("이메일", "203,840명", "102,687명", "66.5%")
                        ))
                        .build()
        );
    }

    private List<StatCardResponse> buildPerformanceCards() {
        return List.of(
                StatCardResponse.builder()
                        .title("클릭수 / 전체 클릭대상수")
                        .value("38,240 / 200,000")
                        .subText("전체 기준 클릭률 19.1%")
                        .build(),
                StatCardResponse.builder()
                        .title("전환수 / 전체 전환대상수")
                        .value("11,600 / 200,000")
                        .subText("전체 기준 전환율 5.8%")
                        .build(),
                StatCardResponse.builder()
                        .title("동의수 / 전체 동의대상수")
                        .value("154,800 / 198,341")
                        .subText("전체 기준 동의율 78.0%")
                        .build(),
                StatCardResponse.builder()
                        .title("수신거부수 / 전체 대상수")
                        .value("360 / 198,341")
                        .subText("전체 기준 수신거부율 0.18%")
                        .build()
        );
    }

    private List<StatChartResponse> buildPerformanceCharts() {
        return List.of(
                StatChartResponse.builder()
                        .chartId("performanceTrend")
                        .title("클릭 수 추이")
                        .type("line")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("클릭 수")
                                        .data(List.<Number>of(12840, 14220, 15180, 16840, 13910, 11860, 12490))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("weekdayClick")
                        .title("요일별 클릭 수")
                        .type("bar")
                        .labels(List.of("월", "화", "수", "목", "금", "토", "일"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("클릭 수")
                                        .data(List.<Number>of(12450, 16820, 18140, 17320, 15980, 11420, 10190))
                                        .build()
                        ))
                        .build(),
                StatChartResponse.builder()
                        .chartId("hourlyClick")
                        .title("시간별 클릭 수")
                        .type("line")
                        .labels(List.of("00시", "01시", "02시", "03시", "04시", "05시", "06시", "07시", "08시", "09시", "10시", "11시", "12시", "13시", "14시", "15시", "16시", "17시", "18시", "19시", "20시", "21시", "22시", "23시"))
                        .datasets(List.of(
                                StatChartDatasetResponse.builder()
                                        .label("클릭 수")
                                        .data(List.<Number>of(210, 140, 90, 70, 80, 120, 260, 530, 840, 1380, 1870, 1690, 1420, 1560, 1790, 1940, 2130, 1810, 1360, 1020, 870, 610, 430, 300))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<StatTableResponse> buildPerformanceTables() {
        return List.of();
    }
}
