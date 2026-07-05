package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmReviewResponse {

    private List<ReviewedExistingIssue> reviewedExistingIssues;
    private List<NewIssue> newIssues;
    private String suggestedRewrite;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReviewedExistingIssue {
        private String ruleId;
        private String source;
        private String reviewResult;
        private String reason;
        private String suggestion;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NewIssue {
        private String ruleId;
        private String riskLevel;
        private String field;
        private String targetText;
        private String message;
        private String suggestion;
    }
}
