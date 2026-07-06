package com.example.smartmessaging.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfanityFilterRequest {

    private final String text;
    // 원문 치환을 막기 위해 현재 검사는 항상 NORMAL 모드를 사용한다.
    private final String mode;
}
