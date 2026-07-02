package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HistoryFilterOptionDTO {
    // 검색 필터 선택지 DTO
    private Long id; // 실제 요청에 사용되는 값
    private String label; // 화면에 보여줄 값
}
