package com.example.smartmessaging.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HistoryTagResponseDTO {
    // 전송 기록별 태그 목록 DTO
    private Long sendHistoryId;
    private String tagName;
}
