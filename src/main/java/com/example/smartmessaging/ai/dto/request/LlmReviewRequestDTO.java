package com.example.smartmessaging.ai.dto.request;

import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;

import java.util.List;

public record LlmReviewRequestDTO(
        AiContextType contextType,
        MessageType messageType,
        List<ChannelType> channels,
        List<String> customerTags,
        TemplateCategory category,
        String title,
        String content,
        List<String> availableVariables,
        // LLM 검사 단계에서만 쓰는 RAG 참고자료입니다. API 응답으로 노출하지 않고 프롬프트 입력에만 포함합니다.
        String ragContext,
        List<ExistingIssue> existingIssues
) {
    public static LlmReviewRequestDTO from(AiReviewRequestDTO request, List<ValidationIssueResponseDTO> issues) {
        return from(request, issues, "");
    }

    public static LlmReviewRequestDTO from(
            AiReviewRequestDTO request,
            List<ValidationIssueResponseDTO> issues,
            String ragContext
    ) {
        // 기존 서버 룰/필터 이슈는 그대로 전달하고, RAG는 별도 context로만 붙여 판단 주체가 되지 않게 분리합니다.
        List<ExistingIssue> existingIssues = issues.stream()
                .map(ExistingIssue::from)
                .toList();
        return new LlmReviewRequestDTO(
                request.contextType(),
                request.messageType(),
                request.channels(),
                request.customerTags(),
                request.category(),
                request.title(),
                request.content(),
                request.availableVariables(),
                ragContext,
                existingIssues
        );
    }

    public record ExistingIssue(
            String ruleId,
            IssueSource source,
            IssueSeverity severity,
            ReviewStatus status,
            String field,
            String message,
            String targetText
    ) {
        private static ExistingIssue from(ValidationIssueResponseDTO issue) {
            return new ExistingIssue(
                    issue.ruleId(),
                    issue.source(),
                    issue.severity(),
                    issue.status(),
                    issue.field(),
                    issue.message(),
                    issue.targetText()
            );
        }
    }
}
