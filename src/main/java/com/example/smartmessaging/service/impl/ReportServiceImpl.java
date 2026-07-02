package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.ReportSearchRequest;
import com.example.smartmessaging.dto.response.ReportCardResponse;
import com.example.smartmessaging.dto.response.ReportChartDatasetResponse;
import com.example.smartmessaging.dto.response.ReportChartResponse;
import com.example.smartmessaging.dto.response.ReportPageResponse;
import com.example.smartmessaging.dto.response.ReportTableResponse;
import com.example.smartmessaging.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Override
    public ReportPageResponse getDeliveryReport(ReportSearchRequest request) {
        return ReportPageResponse.builder()
                .cards(buildDeliveryCards())
                .charts(buildDeliveryCharts())
                .tables(buildDeliveryTables())
                .build();
    }

    @Override
    public ReportPageResponse getChannelReport(ReportSearchRequest request) {
        return ReportPageResponse.builder()
                .cards(buildChannelCards())
                .charts(buildChannelCharts())
                .tables(buildChannelTables())
                .build();
    }

    @Override
    public ReportPageResponse getCostReport(ReportSearchRequest request) {
        return ReportPageResponse.builder()
                .cards(buildCostCards())
                .charts(buildCostCharts())
                .tables(buildCostTables())
                .build();
    }

    @Override
    public ReportPageResponse getCustomerReport(ReportSearchRequest request) {
        return ReportPageResponse.builder()
                .cards(buildCustomerCards())
                .charts(buildCustomerCharts())
                .tables(buildCustomerTables())
                .build();
    }

    @Override
    public ReportPageResponse getPerformanceReport(ReportSearchRequest request) {
        return ReportPageResponse.builder()
                .cards(buildPerformanceCards())
                .charts(buildPerformanceCharts())
                .tables(buildPerformanceTables())
                .build();
    }

    private List<ReportCardResponse> buildDeliveryCards() {
        return List.of(
                ReportCardResponse.builder()
                        .title("총 발송")
                        .value("892,451")
                        .subText("2026-06-23 ~ 2026-06-29 (7일)")
                        .build(),
                ReportCardResponse.builder()
                        .title("평균 성공률")
                        .value("98.7%")
                        .subText("실패 165건")
                        .build(),
                ReportCardResponse.builder()
                        .title("기간 평균 발송")
                        .value("127,493")
                        .subText("일 평균 기준")
                        .build(),
                ReportCardResponse.builder()
                        .title("실제 청구 비용")
                        .value("18,700,000원")
                        .subText("선택 기간 누적")
                        .build(),
                ReportCardResponse.builder()
                        .title("스마트 라우팅 절감")
                        .value("5,500,000원")
                        .subText("최대 비용 대비")
                        .build()
        );
    }

    private List<ReportChartResponse> buildDeliveryCharts() {
        return List.of(
                ReportChartResponse.builder()
                        .chartId("channelTrend")
                        .title("채널별 발송 현황")
                        .type("bar")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("카카오톡")
                                        .data(List.<Number>of(82000, 94400, 116000, 133200, 70100, 90200, 107500))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("SMS")
                                        .data(List.<Number>of(43800, 50500, 62000, 71200, 37500, 48200, 57500))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("LMS")
                                        .data(List.<Number>of(16200, 18700, 22900, 26300, 13900, 17800, 21200))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("이메일")
                                        .data(List.<Number>of(7600, 8800, 10800, 12400, 6500, 8400, 10000))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("sendSuccessTrend")
                        .title("발송 & 성공 추이")
                        .type("area")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("발송")
                                        .data(List.<Number>of(58838, 72655, 125996, 100289, 114106, 55923, 69740))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("성공")
                                        .data(List.<Number>of(57602, 71347, 124358, 99186, 113079, 54469, 68205))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("성공률")
                                        .data(List.<Number>of(97.9, 98.2, 98.7, 98.9, 99.1, 97.4, 97.8))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("fallbackSuccess")
                        .title("Fallback 채널별 성공률")
                        .type("bar")
                        .labels(List.of("1차", "2차", "3차"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("카카오")
                                        .data(List.<Number>of(99.2, 98.8, 98.1))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("SMS")
                                        .data(List.<Number>of(98.7, 98.2, 97.6))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("LMS")
                                        .data(List.<Number>of(97.9, 97.3, 96.8))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<ReportTableResponse> buildDeliveryTables() {
        return List.of();
    }

    private List<ReportCardResponse> buildChannelCards() {
        return List.of(
                ReportCardResponse.builder()
                        .title("카카오톡 발송")
                        .value("535,279")
                        .subText("성공률 99.1%")
                        .build(),
                ReportCardResponse.builder()
                        .title("SMS 발송")
                        .value("249,886")
                        .subText("성공률 99.1%")
                        .build(),
                ReportCardResponse.builder()
                        .title("LMS 발송")
                        .value("80,241")
                        .subText("성공률 98.2%")
                        .build(),
                ReportCardResponse.builder()
                        .title("이메일 발송")
                        .value("26,739")
                        .subText("성공률 97.8%")
                        .build()
        );
    }

    private List<ReportChartResponse> buildChannelCharts() {
        return List.of(
                ReportChartResponse.builder()
                        .chartId("channelSuccessRate")
                        .title("채널별 성공률")
                        .type("bar")
                        .labels(List.of("카카오톡", "SMS", "LMS", "이메일"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("성공률")
                                        .data(List.<Number>of(99.1, 99.1, 98.2, 97.8))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("channelTrend")
                        .title("채널별 발송 추이")
                        .type("line")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("카카오톡")
                                        .data(List.<Number>of(82000, 94400, 116000, 133200, 70100, 90200, 107500))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("SMS")
                                        .data(List.<Number>of(43800, 50500, 62000, 71200, 37500, 48200, 57500))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("LMS")
                                        .data(List.<Number>of(16200, 18700, 22900, 26300, 13900, 17800, 21200))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("이메일")
                                        .data(List.<Number>of(7600, 8800, 10800, 12400, 6500, 8400, 10000))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("channelShare")
                        .title("채널별 발송 비중")
                        .type("doughnut")
                        .labels(List.of("카카오톡", "SMS", "LMS", "이메일"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("발송 비중")
                                        .data(List.<Number>of(60, 28, 9, 3))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<ReportTableResponse> buildChannelTables() {
        return List.of(
                ReportTableResponse.builder()
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

    private List<ReportCardResponse> buildCostCards() {
        return List.of(
                ReportCardResponse.builder()
                        .title("실제 청구 비용")
                        .value("18,700,000원")
                        .subText("선택 기간 누적")
                        .build(),
                ReportCardResponse.builder()
                        .title("최대 비용")
                        .value("24,200,000원")
                        .subText("동일 물량 기준")
                        .build(),
                ReportCardResponse.builder()
                        .title("기간 절감액")
                        .value("5,500,000원")
                        .subText("절감률 22.7%")
                        .build(),
                ReportCardResponse.builder()
                        .title("대체 발송 전환")
                        .value("5,549")
                        .subText("전환율 1.9%")
                        .build()
        );
    }

    private List<ReportChartResponse> buildCostCharts() {
        return List.of(
                ReportChartResponse.builder()
                        .chartId("costComparison")
                        .title("비용 비교 현황")
                        .type("line")
                        .labels(List.of("1월", "2월", "3월", "4월", "5월", "6월"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("실제 청구 비용")
                                        .data(List.<Number>of(9800000, 10600000, 12100000, 13800000, 16200000, 18700000))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("최대 비용")
                                        .data(List.<Number>of(11200000, 12450000, 14900000, 16950000, 20700000, 24200000))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("costSavings")
                        .title("절감액 추이")
                        .type("line")
                        .labels(List.of("1월", "2월", "3월", "4월", "5월", "6월"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("절감액")
                                        .data(List.<Number>of(1400000, 1850000, 2800000, 3150000, 4500000, 5500000))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<ReportTableResponse> buildCostTables() {
        return List.of();
    }

    private List<ReportCardResponse> buildCustomerCards() {
        return List.of(
                ReportCardResponse.builder()
                        .title("전체 고객")
                        .value("198,341")
                        .subText("분석 가능 고객")
                        .build(),
                ReportCardResponse.builder()
                        .title("일반 고객")
                        .value("127,721")
                        .subText("주요 발송 대상")
                        .build(),
                ReportCardResponse.builder()
                        .title("신규 고객")
                        .value("2,184")
                        .subText("7일 누적 가입")
                        .build(),
                ReportCardResponse.builder()
                        .title("휴면 고객")
                        .value("18,940")
                        .subText("6개월 이상 미활동")
                        .build()
        );
    }

    private List<ReportChartResponse> buildCustomerCharts() {
        return List.of(
                ReportChartResponse.builder()
                        .chartId("newCustomerTrend")
                        .title("신규 고객 추이")
                        .type("bar")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("신규 고객")
                                        .data(List.<Number>of(280, 312, 348, 290, 361, 295, 298))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<ReportTableResponse> buildCustomerTables() {
        return List.of(
                ReportTableResponse.builder()
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

    private List<ReportCardResponse> buildPerformanceCards() {
        return List.of(
                ReportCardResponse.builder()
                        .title("선택 채널")
                        .value("카카오톡")
                        .subText("채널별 데이터 표시")
                        .build(),
                ReportCardResponse.builder()
                        .title("평균 클릭률")
                        .value("19.1%")
                        .subText("업계 평균 8.2%")
                        .build(),
                ReportCardResponse.builder()
                        .title("전환율")
                        .value("5.8%")
                        .subText("선택 기간 평균")
                        .build(),
                ReportCardResponse.builder()
                        .title("수신 거부율")
                        .value("0.18%")
                        .subText("업계 평균 0.41%")
                        .build()
        );
    }

    private List<ReportChartResponse> buildPerformanceCharts() {
        return List.of(
                ReportChartResponse.builder()
                        .chartId("performanceTrend")
                        .title("클릭률 & 전환율 추이")
                        .type("line")
                        .labels(List.of("6/23", "6/24", "6/25", "6/26", "6/27", "6/28", "6/29"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("클릭률")
                                        .data(List.<Number>of(17.8, 18.4, 19.1, 20.2, 18.9, 19.4, 20.0))
                                        .build(),
                                ReportChartDatasetResponse.builder()
                                        .label("전환율")
                                        .data(List.<Number>of(5.1, 5.3, 5.8, 6.1, 5.6, 5.9, 6.0))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("weekdayClick")
                        .title("요일별 클릭률")
                        .type("bar")
                        .labels(List.of("월", "화", "수", "목", "금", "토", "일"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("클릭률")
                                        .data(List.<Number>of(12.1, 16.8, 18.2, 17.3, 15.9, 11.4, 10.1))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("hourlyClick")
                        .title("시간별 클릭률")
                        .type("line")
                        .labels(List.of("00시", "01시", "02시", "03시", "04시", "05시", "06시", "07시", "08시", "09시", "10시", "11시", "12시", "13시", "14시", "15시", "16시", "17시", "18시", "19시", "20시", "21시", "22시", "23시"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("클릭률")
                                        .data(List.<Number>of(2.1, 1.4, 0.9, 0.7, 0.8, 1.2, 2.6, 5.3, 8.4, 13.8, 18.7, 16.9, 14.2, 15.6, 17.9, 19.4, 21.3, 18.1, 13.6, 10.2, 8.7, 6.1, 4.3, 3.0))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<ReportTableResponse> buildPerformanceTables() {
        return List.of();
    }
}
