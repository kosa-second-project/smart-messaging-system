package com.example.smartmessaging.ai.dto.request;

import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LlmReviewRequest {

    private final AiContextType contextType;
    private final MessageType messageType;
    private final List<ChannelType> channels;
    private final List<String> customerTags;
    private final TemplateCategory category;
    private final String title;
    private final String content;
    private final List<String> availableVariables;
    private final List<ExistingIssue> existingIssues;

    public static LlmReviewRequest from(AiReviewRequest request, List<ValidationIssue> issues) {
        List<ExistingIssue> existingIssues = issues.stream()
                .map(ExistingIssue::from)
                .toList();
        return new LlmReviewRequest(
                request.getContextType(),
                request.getMessageType(),
                request.getChannels(),
                request.getCustomerTags(),
                request.getCategory(),
                request.getTitle(),
                request.getContent(),
                request.getAvailableVariables(),
                existingIssues
        );
    }

    @Getter
    @AllArgsConstructor
    public static class ExistingIssue {
        private final String ruleId;
        private final IssueSource source;
        private final IssueSeverity severity;
        private final ReviewStatus status;
        private final String field;
        private final String message;
        private final String targetText;

        private static ExistingIssue from(ValidationIssue issue) {
            return new ExistingIssue(
                    issue.getRuleId(),
                    issue.getSource(),
                    issue.getSeverity(),
                    issue.getStatus(),
                    issue.getField(),
                    issue.getMessage(),
                    issue.getTargetText()
            );
        }
    }
}
