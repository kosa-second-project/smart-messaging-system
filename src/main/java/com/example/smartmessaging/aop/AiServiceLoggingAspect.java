package com.example.smartmessaging.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Aspect
@Component
@Slf4j
public class AiServiceLoggingAspect {

    private static final AtomicLong REQUEST_SEQUENCE = new AtomicLong();
    private static final AtomicLong ACTIVE_REQUEST_ID = new AtomicLong();
    private static final ThreadLocal<Long> CURRENT_REQUEST_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_REQUEST_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<Long> EXTERNAL_AI_CALL_NANOS = ThreadLocal.withInitial(() -> 0L);
    private static final ThreadLocal<Integer> CALL_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<RuleCheckSummary> SUGGESTION_RULE_CHECK_SUMMARY =
            ThreadLocal.withInitial(RuleCheckSummary::new);

    public static <T> Supplier<T> withCurrentTimingContext(Supplier<T> delegate) {
        TimingContext context = new TimingContext(
                CURRENT_REQUEST_ID.get(),
                CURRENT_REQUEST_TYPE.get(),
                CALL_DEPTH.get()
        );
        return () -> {
            if (context.requestId() == null) {
                return delegate.get();
            }
            CURRENT_REQUEST_ID.set(context.requestId());
            CURRENT_REQUEST_TYPE.set(context.requestType());
            CALL_DEPTH.set(context.callDepth());
            try {
                return delegate.get();
            } finally {
                CURRENT_REQUEST_ID.remove();
                CURRENT_REQUEST_TYPE.remove();
                CALL_DEPTH.remove();
            }
        };
    }
    @Around("""
            within(com.example.smartmessaging.ai.service..*)
            || within(com.example.smartmessaging.ai.rag.service..*)
            || within(com.example.smartmessaging.ai.client..*)
            """)
    public Object logAiExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String displayName = className + "." + methodName + "()";
        boolean rootRequest = isRootAiRequest(className, methodName);
        boolean startedRequest = false;

        if (rootRequest && CURRENT_REQUEST_ID.get() == null) {
            long requestId = REQUEST_SEQUENCE.incrementAndGet();
            CURRENT_REQUEST_ID.set(requestId);
            ACTIVE_REQUEST_ID.set(requestId);
            CURRENT_REQUEST_TYPE.set(requestType(className));
            CALL_DEPTH.set(0);
            startedRequest = true;
            log.info("");
            log.info("============ AI {} 시간 측정 시작 #{} ============", requestType(className), requestId);
        }

        int depth = CALL_DEPTH.get();
        CALL_DEPTH.set(depth + 1);

        try {
            Object result = joinPoint.proceed();
            logSuccess(displayName, start, rootRequest);
            return result;
        } catch (Throwable throwable) {
            logFailure(displayName, start, rootRequest, throwable);
            throw throwable;
        } finally {
            accumulateSuggestionRuleCheck(displayName, start);
            CALL_DEPTH.set(depth);
            if (startedRequest) {
                Long requestId = CURRENT_REQUEST_ID.get();
                log.info("============ AI {} 시간 측정 종료 #{} ============",
                        requestType(className),
                        requestId);
                log.info("");
                ACTIVE_REQUEST_ID.compareAndSet(requestId, 0L);
                CURRENT_REQUEST_ID.remove();
                CURRENT_REQUEST_TYPE.remove();
                EXTERNAL_AI_CALL_NANOS.remove();
                CALL_DEPTH.remove();
                SUGGESTION_RULE_CHECK_SUMMARY.remove();
            }
        }
    }

    private boolean isRootAiRequest(String className, String methodName) {
        return ("AiSuggestionService".equals(className) && "suggest".equals(methodName))
                || ("AiReviewService".equals(className) && "review".equals(methodName));
    }

    private String requestType(String className) {
        if ("AiSuggestionService".equals(className)) {
            return "추천";
        }
        if ("AiReviewService".equals(className)) {
            return "검사";
        }
        return "처리";
    }

    private void logSuccess(String displayName, long start, boolean rootRequest) {
        if (isSuggestionCandidateRuleCheck(displayName)) {
            return;
        }
        if (rootRequest) {
            logSuggestionRuleCheckSummary();
            logSummary(displayName, start);
            return;
        }
        log.info("[AI Timing #{}] {} completed in {}",
                currentRequestId(),
                stepLabel(displayName),
                elapsed(start));
    }

    private void logFailure(String displayName, long start, boolean rootRequest, Throwable throwable) {
        if (isSuggestionCandidateRuleCheck(displayName)) {
            return;
        }
        if (rootRequest) {
            logSuggestionRuleCheckSummary();
            log.info("==================== AI {} 시간 측정 요약 #{} ====================",
                    CURRENT_REQUEST_TYPE.get(),
                    currentRequestId());
            log.warn("[AI Timing #{}] [전체] {} failed in {}: exceptionType={}",
                    currentRequestId(),
                    stepLabel(displayName),
                    elapsed(start),
                    throwable.getClass().getSimpleName());
            return;
        }
        log.warn("[AI Timing #{}] {} failed in {}: exceptionType={}",
                currentRequestId(),
                stepLabel(displayName),
                elapsed(start),
                throwable.getClass().getSimpleName());
    }

    private Long currentRequestId() {
        Long requestId = CURRENT_REQUEST_ID.get();
        if (requestId != null) {
            return requestId;
        }
        long activeRequestId = ACTIVE_REQUEST_ID.get();
        return activeRequestId == 0L ? 0L : activeRequestId;
    }

    private String elapsed(long start) {
        return "%.1fms".formatted((System.nanoTime() - start) / 1_000_000.0);
    }

    private void logSummary(String displayName, long start) {
        long totalNanos = System.nanoTime() - start;
        long withoutExternalAiNanos = Math.max(0, totalNanos - EXTERNAL_AI_CALL_NANOS.get());

        log.info("==================== AI {} 시간 측정 요약 #{} ====================",
                CURRENT_REQUEST_TYPE.get(),
                currentRequestId());
        log.info("[AI Timing #{}] [외부 AI 제외] AI {} 처리 시간 completed in {}",
                currentRequestId(),
                CURRENT_REQUEST_TYPE.get(),
                formatNanos(withoutExternalAiNanos));
        log.info("[AI Timing #{}] [전체] {} completed in {}",
                currentRequestId(),
                stepLabel(displayName),
                formatNanos(totalNanos));
    }

    private boolean isSuggestionCandidateRuleCheck(String displayName) {
        return "추천".equals(CURRENT_REQUEST_TYPE.get())
                && "RuleValidationService.review()".equals(displayName);
    }

    private void accumulateSuggestionRuleCheck(String displayName, long start) {
        if (isSuggestionCandidateRuleCheck(displayName)) {
            SUGGESTION_RULE_CHECK_SUMMARY.get().add(System.nanoTime() - start);
        }
        if (isExternalAiCall(displayName)) {
            EXTERNAL_AI_CALL_NANOS.set(EXTERNAL_AI_CALL_NANOS.get() + (System.nanoTime() - start));
        }
    }

    private void logSuggestionRuleCheckSummary() {
        if (!"추천".equals(CURRENT_REQUEST_TYPE.get())) {
            return;
        }

        RuleCheckSummary summary = SUGGESTION_RULE_CHECK_SUMMARY.get();
        if (summary.count() == 0) {
            return;
        }

        log.info("[AI Timing #{}] 추천 후보 전체 제목/내용 서버 룰 검증 completed in {} (calls={})",
                currentRequestId(),
                formatNanos(summary.totalNanos()),
                summary.count());
    }

    private String formatNanos(long nanos) {
        return "%.1fms".formatted(nanos / 1_000_000.0);
    }

    private boolean isExternalAiCall(String displayName) {
        return "GeminiSuggestionClient.generate()".equals(displayName)
                || "GeminiReviewClient.review()".equals(displayName);
    }

    private String stepLabel(String displayName) {
        return switch (displayName) {
            case "AiSuggestionService.suggest()" -> "AI 추천 전체";
            case "AiReviewService.review()" -> "AI 검사 전체";
            case "RuleValidationService.review()" -> "[서버] 서버 룰 검사";
            case "ProfanityValidationService.validate()" -> "[서버] 비속어 필터 결과 처리";
            case "ProfanityFilterClient.filter()" -> "[외부 API] 비속어 필터 API 호출";
            case "OpenAiModerationValidationService.validate()" -> "[서버] OpenAI Moderation 결과 처리";
            case "OpenAiModerationClient.moderate()" -> "[외부 API] OpenAI Moderation API 호출";
            case "LlmReviewService.review()" -> "[서버] LLM 문맥 검사 결과 처리";
            case "GeminiReviewClient.review()" -> "[외부 AI] AI 검사 호출";
            case "GeminiSuggestionClient.generate()" -> "[외부 AI] AI 추천 호출";
            case "RagPromptContextService.buildSuggestionPromptContext()" -> "[서버] 추천 RAG 참고자료 생성";
            case "RagPromptContextService.buildReviewPromptContext()" -> "[서버] 검사 RAG 참고자료 생성";
            case "RagSearchService.similaritySearch()" -> "[서버] RAG 유사도 검색";
            case "RagSearchService.search()" -> "[서버] RAG 검색 API";
            default -> displayName;
        };
    }

    private record TimingContext(
            Long requestId,
            String requestType,
            Integer callDepth
    ) {
    }

    private static class RuleCheckSummary {
        private int count;
        private long totalNanos;

        void add(long elapsedNanos) {
            count++;
            totalNanos += elapsedNanos;
        }

        int count() {
            return count;
        }

        long totalNanos() {
            return totalNanos;
        }
    }
}
