package com.example.smartmessaging.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HistoryStatusOptionDTO {
    // 상태 필터 선택지 DTO
    private final String value;
    private final String label;
}
