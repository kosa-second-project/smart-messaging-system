package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.response.AiReviewResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiReviewService {

    private static final String MODERATION_UNAVAILABLE = "MODERATION_UNAVAILABLE";
    private static final String LLM_REVIEW_UNAVAILABLE = "LLM_REVIEW_UNAVAILABLE";

    private final RuleValidationService ruleValidationService;
    private final ProfanityValidationService profanityValidationService;
    private final OpenAiModerationValidationService openAiModerationValidationService;
    private final LlmReviewService llmReviewService;

    public AiReviewResponseDTO review(AiReviewRequestDTO request) {
        // 필수값 검증과 1차 확정 룰은 기존 서비스가 그대로 담당한다.
        AiReviewResponseDTO ruleResponse = ruleValidationService.review(request);

        // 외부 욕설 검사 결과는 기존 이슈를 제거하지 않고 뒤에 추가한다.
        List<ValidationIssueResponseDTO> issues = new ArrayList<>();
        issues.addAll(withMetadata(ruleResponse.issues(), IssueSource.SERVER_RULE, "content"));
        issues.addAll(withMetadata(
                profanityValidationService.validate(request.content()),
                IssueSource.PROFANITY_FILTER,
                "content"
        ));

        // OpenAI Moderation은 욕설 필터를 대체하지 않고 그 다음 단계의 유해성 검사로 추가한다.
        List<ValidationIssueResponseDTO> moderationIssues = openAiModerationValidationService.validate(request.content());
        issues.addAll(moderationIssues.stream()
                .map(issue -> issue.withMetadata(
                        IssueSource.OPENAI_MODERATION,
                        MODERATION_UNAVAILABLE.equals(issue.ruleId()) ? null : "content"
                ))
                .toList());

        String suggestedRewrite = ruleResponse.suggestedRewrite();
        try {
            LlmReviewService.ReviewResult llmResult = llmReviewService.review(request, issues);
            issues = new ArrayList<>(llmResult.existingIssues());
            issues.addAll(llmResult.newIssues());
            if (llmResult.suggestedRewrite() != null) {
                suggestedRewrite = llmResult.suggestedRewrite();
            }
        } catch (RuntimeException exception) {
            log.warn("LLM review unavailable: exceptionType={}",
                    exception.getClass().getSimpleName());
            issues.add(llmUnavailableIssue());
        }

        // 병합된 전체 이슈를 기준으로 최종 상태와 사용자 안내 문구를 다시 계산한다.
        ReviewStatus status = determineStatus(issues);
        return new AiReviewResponseDTO(
                status,
                summaryOf(status),
                List.copyOf(issues),
                suggestedRewrite,
                status == ReviewStatus.FAIL
        );
    }

    private List<ValidationIssueResponseDTO> withMetadata(
            List<ValidationIssueResponseDTO> issues,
            IssueSource source,
            String field
    ) {
        return issues.stream()
                .map(issue -> issue.withMetadata(source, field))
                .toList();
    }

    private ValidationIssueResponseDTO llmUnavailableIssue() {
        return new ValidationIssueResponseDTO(
                LLM_REVIEW_UNAVAILABLE,
                IssueSource.LLM_REVIEW,
                IssueSeverity.LOW,
                ReviewStatus.NOTICE,
                null,
                "LLM 문맥 검사를 완료하지 못했습니다.",
                null,
                "잠시 후 다시 검사하거나 관리자에게 문의하세요.",
                List.of()
        );
    }

    private ReviewStatus determineStatus(List<ValidationIssueResponseDTO> issues) {
        if (issues.stream().anyMatch(issue -> issue.severity() == IssueSeverity.HIGH)) {
            return ReviewStatus.FAIL;
        }
        if (issues.stream().anyMatch(issue -> issue.severity() == IssueSeverity.MEDIUM)) {
            return ReviewStatus.WARNING;
        }
        if (issues.stream().anyMatch(issue -> issue.severity() == IssueSeverity.LOW)) {
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
