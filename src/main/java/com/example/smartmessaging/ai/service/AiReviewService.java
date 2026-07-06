package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiReviewService {

    private final RuleValidationService ruleValidationService;
    private final ProfanityValidationService profanityValidationService;

    public AiReviewResponse review(AiReviewRequest request) {
        AiReviewResponse ruleResponse = ruleValidationService.review(request);

        List<ValidationIssue> issues = new ArrayList<>(ruleResponse.getIssues());
        issues.addAll(profanityValidationService.validate(request.getContent()));

        ReviewStatus status = determineStatus(issues);
        return new AiReviewResponse(
                status,
                summaryOf(status),
                List.copyOf(issues),
                ruleResponse.getSuggestedRewrite(),
                status == ReviewStatus.FAIL
        );
    }

    private ReviewStatus determineStatus(List<ValidationIssue> issues) {
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.HIGH)) {
            return ReviewStatus.FAIL;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.MEDIUM)) {
            return ReviewStatus.WARNING;
        }
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.LOW)) {
            return ReviewStatus.NOTICE;
        }
        return ReviewStatus.PASS;
    }

    private String summaryOf(ReviewStatus status) {
        return switch (status) {
            case PASS -> "검사 결과 문제가 발견되지 않았습니다.";
            case NOTICE -> "검사 결과 참고할 항목이 있습니다.";
            case WARNING -> "검사 결과 주의가 필요한 항목이 있습니다.";
            case FAIL -> "검사 결과 수정이 필요한 항목이 있습니다.";
        };
    }
}
