package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationIssue {

    private final String ruleId;
    private final IssueSeverity severity;
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
        this(ruleId, severity, message, targetText, suggestion, List.of());
    }

    public ValidationIssue(
            String ruleId,
            IssueSeverity severity,
            String message,
            String targetText,
            String suggestion,
            List<String> detail
    ) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.message = message;
        this.targetText = targetText;
        this.suggestion = suggestion;
        this.detail = detail == null ? List.of() : List.copyOf(detail);
    }
}
