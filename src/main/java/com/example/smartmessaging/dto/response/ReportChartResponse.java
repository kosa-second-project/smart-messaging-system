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
public class ReportChartResponse {

    private String chartId;
    private String title;
    private String type;
    private List<String> labels;
    private List<ReportChartDatasetResponse> datasets;
}
