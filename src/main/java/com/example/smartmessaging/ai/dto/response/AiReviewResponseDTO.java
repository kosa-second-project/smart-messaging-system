package com.example.smartmessaging.ai.dto.response;

import com.example.smartmessaging.ai.dto.type.ReviewStatus;

import java.util.List;

public record AiReviewResponseDTO(
        ReviewStatus status,
        String summary,
        List<ValidationIssueResponseDTO> issues,
        String suggestedRewrite,
        boolean needsHumanReview
) {
}
