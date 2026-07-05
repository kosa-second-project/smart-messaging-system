package com.example.smartmessaging.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 1. 시스템/보안 에러 코드
    UNAUTHORIZED_AUDIT_USER(401, "SYS-401", "감사 필드 주입을 위한 인증 사용자 정보를 찾을 수 없습니다."),
    INVALID_INPUT_VALUE(400, "SYS-400", "올바르지 않은 입력값입니다."),
    SEND_HISTORY_NOT_FOUND(404, "HISTORY-404", "전송 기록을 찾을 수 없습니다."),
    DRAFT_NOT_FOUND(404, "DRAFT-404", "발송 세션이 만료되었거나 존재하지 않습니다."),
    INTERNAL_SERVER_ERROR(500, "SYS-500", "서버 내부 오류가 발생했습니다."),
    EXTERNAL_API_ERROR(502, "API-502", "외부 API 연동 중 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
