package com.example.smartmessaging.ai.dto.response;

import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import lombok.Getter;

import java.util.List;

@Getter
public class RuleCheckResult {

    private final ReviewStatus status;
    private final List<ValidationIssue> issues;

    public RuleCheckResult(ReviewStatus status, List<ValidationIssue> issues) {
        this.status = status;
        this.issues = List.copyOf(issues);
    }
}
