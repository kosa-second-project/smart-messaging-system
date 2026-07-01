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
public class ReportPageResponse {

    private List<ReportCardResponse> cards;
    private List<ReportChartResponse> charts;
    private List<ReportTableResponse> tables;
}
