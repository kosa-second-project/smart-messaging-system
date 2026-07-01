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
public class ReportTableResponse {

    private String tableId;
    private String title;
    private List<String> columns;
    private List<List<String>> rows;
}
