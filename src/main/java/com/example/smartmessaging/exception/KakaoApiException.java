package com.example.smartmessaging.exception;

public class KakaoApiException extends BusinessException {
    
    public KakaoApiException(String message) {
        super(message, ErrorCode.EXTERNAL_API_ERROR);
    }
    
    public KakaoApiException(String message, Throwable cause) {
        super(message, ErrorCode.EXTERNAL_API_ERROR, cause);
    }
}
