package com.example.smartmessaging.ai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OpenAiModerationRequest {

    private final String model;
    private final String input;
}
