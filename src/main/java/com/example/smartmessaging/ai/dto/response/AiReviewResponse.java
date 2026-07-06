package com.example.smartmessaging.ai.dto.response;

import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AiReviewResponse {

    private final ReviewStatus status;
    private final String summary;
    private final List<ValidationIssue> issues;
    private final String suggestedRewrite;
    private final boolean needsHumanReview;
}
