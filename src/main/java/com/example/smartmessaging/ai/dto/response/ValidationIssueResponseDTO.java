package com.example.smartmessaging.ai.dto.response;

import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ValidationIssueResponseDTO(
        String ruleId,
        IssueSource source,
        IssueSeverity severity,
        ReviewStatus status,
        String field,
        String message,
        String targetText,
        String suggestion,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Schema(
                description = "감지 카테고리 또는 외부 검사 실패 유형. 값이 없으면 응답에서 생략합니다.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                example = "[\"harassment\"]"
        )
        List<String> detail
) {
    public ValidationIssueResponseDTO {
        status = status == null ? statusOf(severity) : status;
        detail = detail == null ? List.of() : List.copyOf(detail);
    }

    public ValidationIssueResponseDTO(
            String ruleId,
            IssueSeverity severity,
            String message,
            String targetText,
            String suggestion
    ) {
        this(ruleId, null, severity, statusOf(severity), null, message, targetText, suggestion, List.of());
    }

    public ValidationIssueResponseDTO(
            String ruleId,
            IssueSeverity severity,
            String message,
            String targetText,
            String suggestion,
            List<String> detail
    ) {
        this(ruleId, null, severity, statusOf(severity), null, message, targetText, suggestion, detail);
    }

    public ValidationIssueResponseDTO withMetadata(IssueSource source, String field) {
        return new ValidationIssueResponseDTO(
                ruleId,
                source,
                severity,
                statusOf(severity),
                field,
                message,
                targetText,
                suggestion,
                detail
        );
    }

    public static ReviewStatus statusOf(IssueSeverity severity) {
        if (severity == null) {
            return ReviewStatus.NOTICE;
        }
        return switch (severity) {
            case LOW -> ReviewStatus.NOTICE;
            case MEDIUM -> ReviewStatus.WARNING;
            case HIGH -> ReviewStatus.FAIL;
        };
    }
}
