package com.example.smartmessaging.service.impl;

import com.example.smartmessaging.dto.request.ReportSearchRequest;
import com.example.smartmessaging.dto.response.ReportCardResponse;
import com.example.smartmessaging.dto.response.ReportChartDatasetResponse;
import com.example.smartmessaging.dto.response.ReportChartResponse;
import com.example.smartmessaging.dto.response.ReportPageResponse;
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
                .tables(List.of())
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

}
