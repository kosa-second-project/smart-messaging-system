package com.example.smartmessaging.ai.dto.response;

import com.example.smartmessaging.ai.dto.type.ReviewStatus;

import java.util.List;

public record RuleCheckResultResponseDTO(
        ReviewStatus status,
        List<ValidationIssueResponseDTO> issues
) {
    public RuleCheckResultResponseDTO {
        issues = List.copyOf(issues);
    }
}
