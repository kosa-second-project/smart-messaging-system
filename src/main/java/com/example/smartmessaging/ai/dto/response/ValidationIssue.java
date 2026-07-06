package com.example.smartmessaging.ai.dto.response;

import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ValidationIssue {

    private final String ruleId;
    private final IssueSeverity severity;
    private final String message;
    private final String targetText;
    private final String suggestion;
}
