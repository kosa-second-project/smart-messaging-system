package com.example.smartmessaging.ai.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequest;
import com.example.smartmessaging.ai.dto.response.AiReviewResponse;
import com.example.smartmessaging.ai.dto.response.RuleCheckResult;
import com.example.smartmessaging.ai.dto.response.ValidationIssue;
import com.example.smartmessaging.ai.dto.type.IssueSeverity;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.ReviewStatus;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DB 조회나 외부 AI 호출, RAG 검색 없이 확정 가능한 1차 서버 룰만 수행한다.
 * 모든 룰의 issue를 수집한 뒤 가장 높은 severity를 기준으로 최종 상태를 결정한다.
 */
@Service
public class RuleValidationService {

    private static final String MISSING_AD_PREFIX = "MISSING_AD_PREFIX";
    private static final String MISSING_OPT_OUT = "MISSING_OPT_OUT";
    private static final String INVALID_VARIABLE_FORMAT = "INVALID_VARIABLE_FORMAT";
    private static final String UNSUPPORTED_VARIABLE = "UNSUPPORTED_VARIABLE";
    private static final String PERSONAL_PHONE_NUMBER = "PERSONAL_PHONE_NUMBER";
    private static final String PERSONAL_EMAIL = "PERSONAL_EMAIL";
    private static final String PERSONAL_RRN = "PERSONAL_RRN";

    private static final Set<String> SUPPORTED_VARIABLES = Set.of(
            "#{고객명}", "#{주문번호}", "#{쿠폰명}"
    );

    private static final Pattern VALID_VARIABLE_PATTERN = Pattern.compile("#\\{[가-힣A-Za-z0-9_]+}");
    private static final Pattern VARIABLE_START_PATTERN = Pattern.compile("#\\{");

    // #{고객명} #{주문번호} #{쿠폰명} 문구를 검증
    // 정해진 변수명의 대체 표기만 검사해 일반 문구의 오탐을 방지하도록 하였음
    private static final String KNOWN_VARIABLE_NAME_PATTERN = "(?:고객명|주문번호|쿠폰명)";
    private static final Pattern ALTERNATE_VARIABLE_PATTERN = Pattern.compile(
            "\\$\\{" + KNOWN_VARIABLE_NAME_PATTERN + "}"
                    + "|(?<![$#])\\{" + KNOWN_VARIABLE_NAME_PATTERN + "}"
                    + "|#" + KNOWN_VARIABLE_NAME_PATTERN + "(?![가-힣A-Za-z0-9_])"
                    + "|\\[" + KNOWN_VARIABLE_NAME_PATTERN + "]"
    );
    private static final Pattern OPT_OUT_PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)080-\\d{3,4}-\\d{4}(?!\\d)"
    );

    // 본문에 직접 입력된 개인정보 형태의 패턴을 찾는다.
    // 순서대로 휴대폰 번호, 이메일, 주민등록번호 형태를 검사한다.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:010-\\d{4}-\\d{4}|010\\d{8})(?!\\d)"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern RRN_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{6}-\\d{7}(?!\\d)"
    );

    public AiReviewResponse review(AiReviewRequest request) {
        // 필수값 검증
        validateRequiredFields(request);

        // 실제 룰 검사
        RuleCheckResult result = validateRules(request);

        // 최종 status
        ReviewStatus status = result.getStatus();

        // 문구 재작성은 추후 LLM 단계에서 제공 예정
        return new AiReviewResponse(
                status,
                summaryOf(status),
                result.getIssues(),
                null,
                status == ReviewStatus.FAIL
        );
    }

    private void validateRequiredFields(AiReviewRequest request) {
        if (request == null) {
            throw invalidRequest("검사 요청은 필수입니다.");
        }
        if (request.getMessageType() == null) {
            throw invalidRequest("메시지 유형은 필수입니다.");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw invalidRequest("검사할 메시지 내용은 필수입니다.");
        }
    }

    private BusinessException invalidRequest(String message) {
        return new BusinessException(message, ErrorCode.AI_REVIEW_INVALID_REQUEST);
    }

    private RuleCheckResult validateRules(AiReviewRequest request) {
        String content = request.getContent();
        // 빈 issues 리스트 생성
        List<ValidationIssue> issues = new ArrayList<>();

        // 지정된 검사 실행
        checkAdPrefix(request.getMessageType(), content, issues);
        checkOptOut(request.getMessageType(), content, issues);
        checkVariableFormat(content, issues);
        checkSupportedVariables(content, request.getAvailableVariables(), issues);
        checkPersonalInformation(content, issues);

        return new RuleCheckResult(determineStatus(issues), issues);
    }

    // 광고성 메세지인 경우 '(광고)' 문구가 들어있는지 확인
    private void checkAdPrefix(MessageType messageType, String content, List<ValidationIssue> issues) {
        if (messageType == MessageType.AD && !content.trim().startsWith("(광고)")) {
            issues.add(new ValidationIssue(
                    MISSING_AD_PREFIX,
                    IssueSeverity.HIGH,
                    "광고성 메시지에는 본문 시작부에 '(광고)' 문구가 필요합니다.",
                    null,
                    "본문 시작부에 '(광고)'를 추가하세요."
            ));
        }
    }

    // 광고성 메시지는 수신거부 문구와 실제 080 번호가 모두 있어야 한다.
    private void checkOptOut(MessageType messageType, String content, List<ValidationIssue> issues) {
        if (messageType != MessageType.AD) {
            return;
        }

        boolean hasOptOutText = content.contains("무료수신거부") || content.contains("수신거부");
        boolean hasOptOutPhone = OPT_OUT_PHONE_PATTERN.matcher(content).find();
        if (!hasOptOutText || !hasOptOutPhone) {
            issues.add(new ValidationIssue(
                    MISSING_OPT_OUT,
                    IssueSeverity.HIGH,
                    "광고성 메시지에는 무료수신거부 방법이 필요합니다.",
                    null,
                    "본문 하단에 '무료수신거부 080-000-0000' 형식의 수신거부 문구를 추가하세요."
            ));
        }
    }

    // 변수 형식 검사
    private void checkVariableFormat(String content, List<ValidationIssue> issues) {
        String invalidVariable = findInvalidVariable(content);
        if (invalidVariable != null) {
            issues.add(new ValidationIssue(
                    INVALID_VARIABLE_FORMAT,
                    IssueSeverity.HIGH,
                    "템플릿 변수는 '#{변수명}' 형식으로 작성해야 합니다.",
                    invalidVariable,
                    "프로젝트 표준 변수 형식인 '#{고객명}' 형태로 수정하세요."
            ));
        }
    }

    private String findInvalidVariable(String content) {
        Matcher startMatcher = VARIABLE_START_PATTERN.matcher(content);
        while (startMatcher.find()) {
            int start = startMatcher.start();
            int closingBrace = content.indexOf('}', startMatcher.end());
            if (closingBrace < 0) {
                int lineEnd = content.indexOf('\n', start);
                return content.substring(start, lineEnd < 0 ? content.length() : lineEnd);
            }

            String candidate = content.substring(start, closingBrace + 1);
            if (!VALID_VARIABLE_PATTERN.matcher(candidate).matches()) {
                return candidate;
            }
        }

        Matcher alternateMatcher = ALTERNATE_VARIABLE_PATTERN.matcher(content);
        return alternateMatcher.find() ? alternateMatcher.group() : null;
    }

    // 허용 변수 검사
    private void checkSupportedVariables(
            String content,
            List<String> availableVariables,
            List<ValidationIssue> issues
    ) {
        // null/empty는 현재 화면 컨텍스트에서 허용된 변수가 없다는 의미다.
        // 표준 변수라도 이 목록에 없으면 화면별 변수 사용 범위를 지키기 위해 허용하지 않는다.
        Set<String> availableVariableSet = availableVariables == null
                ? Set.of()
                : new HashSet<>(availableVariables);

        Matcher matcher = VALID_VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            String variable = matcher.group();
            if (!SUPPORTED_VARIABLES.contains(variable) || !availableVariableSet.contains(variable)) {
                issues.add(new ValidationIssue(
                        UNSUPPORTED_VARIABLE,
                        IssueSeverity.HIGH,
                        "허용되지 않은 템플릿 변수가 사용되었습니다.",
                        variable,
                        "사용 가능한 변수 목록에 포함된 변수만 사용하세요."
                ));
                return;
            }
        }
    }

    // 개인정보 패턴 검사
    private void checkPersonalInformation(String content, List<ValidationIssue> issues) {
        // 실제 개인정보인지 DB로 확인하지 않고 본문에 직접 입력된 패턴만 찾는다. #{고객명} 같은 변수는 대상이 아니다.
        addPatternIssue(
                content,
                PHONE_PATTERN,
                PERSONAL_PHONE_NUMBER,
                IssueSeverity.HIGH,
                "본문에 휴대폰 번호로 보이는 값이 포함되어 있습니다.",
                issues
        );
        addPatternIssue(
                content,
                EMAIL_PATTERN,
                PERSONAL_EMAIL,
                IssueSeverity.MEDIUM,
                "본문에 이메일 주소로 보이는 값이 포함되어 있습니다.",
                issues
        );
        addPatternIssue(
                content,
                RRN_PATTERN,
                PERSONAL_RRN,
                IssueSeverity.HIGH,
                "본문에 주민등록번호로 보이는 값이 포함되어 있습니다.",
                issues
        );
    }

    private void addPatternIssue(
            String content,
            Pattern pattern,
            String ruleId,
            IssueSeverity severity,
            String message,
            List<ValidationIssue> issues
    ) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            issues.add(new ValidationIssue(
                    ruleId,
                    severity,
                    message,
                    matcher.group(),
                    "개인정보는 직접 입력하지 말고 필요한 경우 허용된 템플릿 변수로 처리하세요."
            ));
        }
    }

    // 최종 검사 결과 상태를 결정하는 메서드
    // 각 검사 항목의 심각도(severity)를 기준으로 판단
    private ReviewStatus determineStatus(List<ValidationIssue> issues) {
        // HIGH가 하나라도 있을 경우 FAIL
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.HIGH)) {
            return ReviewStatus.FAIL;
        }
        // HIGH는 없지만 MEDIUM이 있을 경우 WARNING
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.MEDIUM)) {
            return ReviewStatus.WARNING;
        }
        // HIGH, MEDIUM은 없지만 LOW가 있을 경우 NOTICE
        if (issues.stream().anyMatch(issue -> issue.getSeverity() == IssueSeverity.LOW)) {
            return ReviewStatus.NOTICE;
        }
        // issue가 없을 경우 PASS
        return ReviewStatus.PASS;
    }

    // 최종 검사 결과에 따라 문구 반환
    private String summaryOf(ReviewStatus status) {
        return switch (status) {
            case PASS -> "검사 결과 문제가 발견되지 않았습니다.";
            case NOTICE -> "검사 결과 참고할 항목이 있습니다.";
            case WARNING -> "검사 결과 주의가 필요한 항목이 있습니다.";
            case FAIL -> "검사 결과 수정이 필요한 항목이 있습니다.";
        };
    }
}
