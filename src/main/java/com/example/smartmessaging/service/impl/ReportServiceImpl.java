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

    private List<ReportCardResponse> buildDeliveryCards() {
        return List.of(
                ReportCardResponse.builder()
                        .title("Total sends")
                        .value("12,340")
                        .subText("Selected period")
                        .build(),
                ReportCardResponse.builder()
                        .title("Success rate")
                        .value("98.2%")
                        .subText("222 failed")
                        .build(),
                ReportCardResponse.builder()
                        .title("Billing cost")
                        .value("340,000 KRW")
                        .subText("Actual charged cost")
                        .build(),
                ReportCardResponse.builder()
                        .title("Saved cost")
                        .value("120,000 KRW")
                        .subText("Compared to max cost")
                        .build()
        );
    }

    private List<ReportChartResponse> buildDeliveryCharts() {
        return List.of(
                ReportChartResponse.builder()
                        .chartId("dailySendCount")
                        .title("Daily sends")
                        .type("bar")
                        .labels(List.of("7/1", "7/2", "7/3", "7/4", "7/5"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("Sends")
                                        .data(List.<Number>of(1200, 1500, 1100, 1800, 1650))
                                        .build()
                        ))
                        .build(),
                ReportChartResponse.builder()
                        .chartId("dailySuccessRate")
                        .title("Daily success rate")
                        .type("line")
                        .labels(List.of("7/1", "7/2", "7/3", "7/4", "7/5"))
                        .datasets(List.of(
                                ReportChartDatasetResponse.builder()
                                        .label("Success rate")
                                        .data(List.<Number>of(97.8, 98.1, 97.5, 98.6, 98.2))
                                        .build()
                        ))
                        .build()
        );
    }

    private List<ReportTableResponse> buildDeliveryTables() {
        return List.of(
                ReportTableResponse.builder()
                        .tableId("degreeStats")
                        .title("Delivery attempts by degree")
                        .columns(List.of("Degree", "Channel", "Sends", "Successes", "Success rate", "Cost"))
                        .rows(List.of(
                                List.of("1", "KakaoTalk", "10,000", "9,800", "98.0%", "250,000 KRW"),
                                List.of("2", "SMS", "1,500", "1,420", "94.7%", "60,000 KRW"),
                                List.of("3", "Email", "840", "790", "94.0%", "30,000 KRW")
                        ))
                        .build()
        );
    }
}
