package com.example.smartmessaging.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfanityFilterRequest {

    private final String text;
    private final String mode;
}
