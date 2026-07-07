package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.client.ProfanityFilterClient;
import com.example.smartmessaging.ai.dto.response.ProfanityFilterResponseDTO;
import com.example.smartmessaging.ai.dto.response.ValidationIssueResponseDTO;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfanityValidationService {

    private static final int SUCCESS_CODE = 2000;
    private static final String PROFANITY_DETECTED = "PROFANITY_DETECTED";

    private final ProfanityFilterClient profanityFilterClient;

    public List<ValidationIssueResponseDTO> validate(String content) {
        try {
            return profanityFilterClient.filter(content)
                    .map(this::toIssues)
                    .orElseGet(List::of);
        } catch (RuntimeException exception) {
            // 외부 API 장애가 1차 서버 룰 검사까지 실패시키지 않도록 빈 결과로 복구한다.
            log.warn("욕설 필터 API 검사에 실패하여 기존 서버 룰 결과만 유지합니다.", exception);
            return List.of();
        }
    }

    private List<ValidationIssueResponseDTO> toIssues(ProfanityFilterResponseDTO response) {
        // 이 API는 HTTP 200으로 오류를 반환할 수 있으므로 body의 status.code를 기준으로 판단한다.
        if (!isSuccessful(response)) {
            Integer statusCode = response.status() == null ? null : response.status().code();
            log.warn("욕설 필터 API가 비정상 상태 코드를 반환하여 검사 결과를 제외합니다. statusCode={}", statusCode);
            return List.of();
        }

        List<String> detectedWords = response.detected() == null
                ? List.of()
                : response.detected().stream()
                        .map(ProfanityFilterResponseDTO.DetectedWord::filteredWord)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();

        if (detectedWords.isEmpty()) {
            return List.of();
        }

        return List.of(new ValidationIssueResponseDTO(
                PROFANITY_DETECTED,
                IssueSeverity.HIGH,
                "본문에 부적절한 표현이 포함되어 있습니다.",
                String.join(", ", detectedWords),
                "부적절한 표현을 제거하거나 완화된 표현으로 수정하세요."
        ));
    }

    private boolean isSuccessful(ProfanityFilterResponseDTO response) {
        return response != null
                && response.status() != null
                && response.status().code() != null
                && response.status().code() == SUCCESS_CODE;
    }
}
