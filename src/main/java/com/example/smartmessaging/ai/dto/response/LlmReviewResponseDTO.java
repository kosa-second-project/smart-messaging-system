package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmReviewResponseDTO(
        List<ReviewedExistingIssue> reviewedExistingIssues,
        List<NewIssue> newIssues,
        String suggestedRewrite
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReviewedExistingIssue(
            String ruleId,
            String source,
            String reviewResult,
            String reason,
            String suggestion
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NewIssue(
            String ruleId,
            String riskLevel,
            String field,
            String targetText,
            String message,
            String suggestion
    ) {
    }
}
