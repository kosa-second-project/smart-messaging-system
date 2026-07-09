package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.aop.AiServiceLoggingAspect;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.response.AiReviewResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.IssueSource;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService;
import com.example.smartmessaging.ai.rag.service.RagPromptContextService.RagPromptContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class AiReviewService {

    private static final String MODERATION_UNAVAILABLE = "MODERATION_UNAVAILABLE";
    private static final String LLM_REVIEW_UNAVAILABLE = "LLM_REVIEW_UNAVAILABLE";

    private final RuleValidationService ruleValidationService;
    private final ProfanityValidationService profanityValidationService;
    private final OpenAiModerationValidationService openAiModerationValidationService;
    private final LlmReviewService llmReviewService;
    private final Executor aiTaskExecutor;

    @Autowired(required = false)
    private RagPromptContextService ragPromptContextService;

    @Value("${ai.performance.review.parallel-enabled:true}")
    private boolean reviewParallelEnabled = true;

    public AiReviewService(
            RuleValidationService ruleValidationService,
            ProfanityValidationService profanityValidationService,
            OpenAiModerationValidationService openAiModerationValidationService,
            LlmReviewService llmReviewService,
            @Qualifier("aiTaskExecutor") Executor aiTaskExecutor
    ) {
        this.ruleValidationService = ruleValidationService;
        this.profanityValidationService = profanityValidationService;
        this.openAiModerationValidationService = openAiModerationValidationService;
        this.llmReviewService = llmReviewService;
        this.aiTaskExecutor = aiTaskExecutor;
    }
    public AiReviewResponseDTO review(AiReviewRequestDTO request) {
        // 서버 룰은 확정 규칙이라 가장 먼저 실행하고, 이후 LLM/RAG가 이 결과를 제거하지 못하게 유지한다.
        AiReviewResponseDTO ruleResponse = ruleValidationService.review(request);

        List<ValidationIssueResponseDTO> issues = new ArrayList<>();
        issues.addAll(withMetadata(ruleResponse.issues(), IssueSource.SERVER_RULE, "content"));

        RagPromptContext ragContext = null;
        if (reviewParallelEnabled) {
            // 서로 의존하지 않는 외부 API와 RAG 검색을 동시에 시작해 직렬 대기 시간을 줄인다.
            CompletableFuture<List<ValidationIssueResponseDTO>> profanityFuture = CompletableFuture.supplyAsync(AiServiceLoggingAspect.withCurrentTimingContext(() -> profanityValidationService.validate(request.content())), aiTaskExecutor);
            CompletableFuture<List<ValidationIssueResponseDTO>> moderationFuture = CompletableFuture.supplyAsync(AiServiceLoggingAspect.withCurrentTimingContext(() -> openAiModerationValidationService.validate(request.content())), aiTaskExecutor);
            CompletableFuture<RagPromptContext> ragFuture = ragPromptContextService == null
                    ? null
                    : CompletableFuture.supplyAsync(AiServiceLoggingAspect.withCurrentTimingContext(() -> ragPromptContextService.buildReviewPromptContext(request)), aiTaskExecutor);

            issues.addAll(withMetadata(
                    joinOrDefault(profanityFuture, List.of(), "profanity"),
                    IssueSource.PROFANITY_FILTER,
                    "content"
            ));
            issues.addAll(withModerationMetadata(joinOrDefault(moderationFuture, List.of(), "moderation")));
            if (ragFuture != null) {
                ragContext = joinOrDefault(ragFuture, RagPromptContext.empty(), "rag");
            }
        } else {
            issues.addAll(withMetadata(
                    profanityValidationService.validate(request.content()),
                    IssueSource.PROFANITY_FILTER,
                    "content"
            ));
            issues.addAll(withModerationMetadata(openAiModerationValidationService.validate(request.content())));
        }

        String suggestedRewrite = ruleResponse.suggestedRewrite();
        try {
            // 병렬로 만든 RAG context가 있으면 LLM 검사에 넘겨 RAG 검색이 한 번 더 실행되지 않게 한다.
            LlmReviewService.ReviewResult llmResult = ragContext == null
                    ? llmReviewService.review(request, issues)
                    : llmReviewService.review(request, issues, ragContext);
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

        ReviewStatus status = determineStatus(issues);
        return new AiReviewResponseDTO(
                status,
                summaryOf(status),
                List.copyOf(issues),
                suggestedRewrite,
                status == ReviewStatus.FAIL
        );
    }

    private <T> T joinOrDefault(CompletableFuture<T> future, T fallback, String taskName) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            log.warn("AI review parallel task failed: task={}, exceptionType={}",
                    taskName,
                    cause.getClass().getSimpleName());
            return fallback;
        }
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

    private List<ValidationIssueResponseDTO> withModerationMetadata(List<ValidationIssueResponseDTO> issues) {
        return issues.stream()
                .map(issue -> issue.withMetadata(
                        IssueSource.OPENAI_MODERATION,
                        MODERATION_UNAVAILABLE.equals(issue.ruleId()) ? null : "content"
                ))
                .toList();
    }

    private ValidationIssueResponseDTO llmUnavailableIssue() {
        return new ValidationIssueResponseDTO(
                LLM_REVIEW_UNAVAILABLE,
                IssueSource.LLM_REVIEW,
                IssueSeverity.LOW,
                ReviewStatus.NOTICE,
                null,
                "LLM 문구 검사를 완료하지 못했습니다.",
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

