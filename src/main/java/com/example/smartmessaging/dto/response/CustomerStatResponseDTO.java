package com.example.smartmessaging.dto.response;

import lombok.Data;

@Data
public class CustomerStatResponseDTO {
    private int totalCount;       // 전체 고객 수
    private int regularCount;     // 일반 고객 수
    private int newCount;         // 신규 고객 수
    private int dormantCount;     // 휴면 고객 수
}
