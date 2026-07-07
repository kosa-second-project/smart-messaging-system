package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.OpenAiModerationClient;
import com.example.smartmessaging.ai.client.OpenAiModerationException;
import com.example.smartmessaging.ai.dto.response.OpenAiModerationResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.API_ERROR;
import static com.example.smartmessaging.ai.client.OpenAiModerationException.FailureType.PARSING_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAiModerationValidationService {

    private static final String AI_SAFETY_DETECTED = "AI_SAFETY_DETECTED";
    private static final String MODERATION_UNAVAILABLE = "MODERATION_UNAVAILABLE";

    private final OpenAiModerationClient openAiModerationClient;

    public List<ValidationIssueResponseDTO> validate(String content) {
        try {
            return toIssues(openAiModerationClient.moderate(content), content);
        } catch (OpenAiModerationException exception) {
            return unavailableIssue(exception);
        } catch (RuntimeException exception) {
            // 예상하지 못한 클라이언트 오류도 기존 검사 결과를 깨뜨리지 않는 시스템 경고로 변환한다.
            log.warn(
                    "OpenAI Moderation 검사 중 예상하지 못한 오류가 발생했습니다. "
                            + "failureType={}, exceptionType={}",
                    API_ERROR,
                    exception.getClass().getSimpleName(),
                    exception
            );
            return createUnavailableIssue(API_ERROR);
        }
    }

    private List<ValidationIssueResponseDTO> toIssues(OpenAiModerationResponseDTO response, String content) {
        if (response.results() == null || response.results().isEmpty()) {
            return unavailableIssue(PARSING_ERROR);
        }

        OpenAiModerationResponseDTO.Result result = response.results().get(0);
        if (result == null || result.flagged() == null) {
            return unavailableIssue(PARSING_ERROR);
        }
        if (!Boolean.TRUE.equals(result.flagged())) {
            return List.of();
        }

        List<String> categories = trueCategories(result.categories());
        String targetText = categories.isEmpty() ? content : String.join(", ", categories);

        // 모델의 flagged 결과는 정책상 자동 차단하지 않고 MEDIUM 경고로만 병합한다.
        return List.of(new ValidationIssueResponseDTO(
                AI_SAFETY_DETECTED,
                IssueSeverity.MEDIUM,
                "OpenAI Moderation 검사에서 유해 가능 표현이 감지되었습니다.",
                targetText,
                "유해하거나 공격적으로 해석될 수 있는 표현을 완화해 주세요.",
                categories
        ));
    }

    private List<ValidationIssueResponseDTO> unavailableIssue(OpenAiModerationException.FailureType failureType) {
        return unavailableIssue(new OpenAiModerationException(failureType));
    }

    private List<ValidationIssueResponseDTO> unavailableIssue(OpenAiModerationException exception) {
        logUnavailable(exception);
        return createUnavailableIssue(exception.getFailureType());
    }

    private List<ValidationIssueResponseDTO> createUnavailableIssue(
            OpenAiModerationException.FailureType failureType
    ) {
        return List.of(new ValidationIssueResponseDTO(
                MODERATION_UNAVAILABLE,
                IssueSeverity.MEDIUM,
                "OpenAI Moderation 검사를 완료하지 못했습니다. 잠시 후 다시 시도해주세요.",
                null,
                "잠시 후 다시 검사하거나 관리자에게 문의하세요.",
                List.of(failureType.name())
        ));
    }

    private void logUnavailable(OpenAiModerationException exception) {
        // API key, Authorization, 요청 본문과 원문 오류 메시지는 제외하고 추적 가능한 값만 기록한다.
        log.warn(
                "OpenAI Moderation 검사를 완료하지 못했습니다. "
                        + "failureType={}, httpStatus={}, requestId={}, errorType={}, errorCode={}",
                exception.getFailureType(),
                exception.getHttpStatus(),
                exception.getRequestId(),
                exception.getErrorType(),
                exception.getErrorCode()
        );
    }

    private List<String> trueCategories(OpenAiModerationResponseDTO.Categories categories) {
        if (categories == null) {
            return List.of();
        }

        List<String> detected = new ArrayList<>();
        addIfTrue(detected, "harassment", categories.harassment());
        addIfTrue(detected, "harassment/threatening", categories.harassmentThreatening());
        addIfTrue(detected, "hate", categories.hate());
        addIfTrue(detected, "hate/threatening", categories.hateThreatening());
        addIfTrue(detected, "illicit", categories.illicit());
        addIfTrue(detected, "illicit/violent", categories.illicitViolent());
        addIfTrue(detected, "self-harm", categories.selfHarm());
        addIfTrue(detected, "self-harm/intent", categories.selfHarmIntent());
        addIfTrue(detected, "self-harm/instructions", categories.selfHarmInstructions());
        addIfTrue(detected, "sexual", categories.sexual());
        addIfTrue(detected, "sexual/minors", categories.sexualMinors());
        addIfTrue(detected, "violence", categories.violence());
        addIfTrue(detected, "violence/graphic", categories.violenceGraphic());
        return List.copyOf(detected);
    }

    private void addIfTrue(List<String> detected, String category, Boolean flagged) {
        if (Boolean.TRUE.equals(flagged)) {
            detected.add(category);
        }
    }
}
