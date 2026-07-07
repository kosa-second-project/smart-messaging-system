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
        List<ExistingIssue> existingIssues
) {
    public static LlmReviewRequestDTO from(AiReviewRequestDTO request, List<ValidationIssueResponseDTO> issues) {
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
