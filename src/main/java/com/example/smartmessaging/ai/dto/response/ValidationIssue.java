package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationIssue {

    private final String ruleId;
    private final IssueSource source;
    private final IssueSeverity severity;
    private final ReviewStatus status;
    private final String field;
    private final String message;
    private final String targetText;
    private final String suggestion;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(
            description = "감지된 유해성 카테고리 또는 외부 검사 실패 유형. 값이 없으면 응답에서 생략됩니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            example = "[\"harassment\"]"
    )
    private final List<String> detail;

    // 기존 서버 룰과 욕설 필터의 생성 방식 및 기존 JSON 응답 형태를 유지한다.
    public ValidationIssue(
            String ruleId,
            IssueSeverity severity,
            String message,
            String targetText,
            String suggestion
    ) {
        this(ruleId, null, severity, statusOf(severity), null, message, targetText, suggestion, List.of());
    }

    public ValidationIssue(
            String ruleId,
            IssueSeverity severity,
            String message,
            String targetText,
            String suggestion,
            List<String> detail
    ) {
        this(ruleId, null, severity, statusOf(severity), null, message, targetText, suggestion, detail);
    }

    public ValidationIssue(
            String ruleId,
            IssueSource source,
            IssueSeverity severity,
            ReviewStatus status,
            String field,
            String message,
            String targetText,
            String suggestion,
            List<String> detail
    ) {
        this.ruleId = ruleId;
        this.source = source;
        this.severity = severity;
        this.status = status == null ? statusOf(severity) : status;
        this.field = field;
        this.message = message;
        this.targetText = targetText;
        this.suggestion = suggestion;
        this.detail = detail == null ? List.of() : List.copyOf(detail);
    }

    public ValidationIssue withMetadata(IssueSource source, String field) {
        return new ValidationIssue(
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
