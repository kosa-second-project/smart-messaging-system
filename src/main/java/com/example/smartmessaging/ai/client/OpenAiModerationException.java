package com.example.smartmessaging.ai.client;

public class OpenAiModerationException extends RuntimeException {

    private final FailureType failureType;
    private final Integer httpStatus;
    private final String requestId;
    private final String errorType;
    private final String errorCode;

    public OpenAiModerationException(FailureType failureType) {
        this(failureType, null, null, null, null, null);
    }

    public OpenAiModerationException(FailureType failureType, Throwable cause) {
        this(failureType, null, null, null, null, cause);
    }

    public OpenAiModerationException(
            FailureType failureType,
            Integer httpStatus,
            String requestId,
            String errorType,
            String errorCode,
            Throwable cause
    ) {
        super(failureType.name(), cause);
        this.failureType = failureType;
        this.httpStatus = httpStatus;
        this.requestId = requestId;
        this.errorType = errorType;
        this.errorCode = errorCode;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public enum FailureType {
        MISSING_API_KEY,
        DISABLED,
        TIMEOUT,
        API_ERROR,
        PARSING_ERROR
    }
}
