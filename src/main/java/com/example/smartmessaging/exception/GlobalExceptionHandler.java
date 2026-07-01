package com.example.smartmessaging.exception;

import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. 공통 비즈니스 예외(BusinessException) 통합 처리
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException e, HttpServletRequest request, Model model) {
        log.error("Business Exception Occurred: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        
        // AJAX 또는 API 요청인 경우 JSON 응답 반환
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", e.getErrorCode().getStatus());
            response.put("code", e.getErrorCode().getCode());
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.valueOf(e.getErrorCode().getStatus()));
        }
        
        // 일반 브라우저 웹 페이지 요청인 경우 에러 페이지로 이동
        model.addAttribute("errorMessage", e.getMessage());
        return "error/500";
    }

    // 2. 일반 웹 화면(Thymeleaf) 요청 도중 발생한 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public String handleWebException(Exception e, Model model) {
        log.error("Web Request Exception: ", e);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/500"; // templates/error/500.html 에러 페이지로 이동
    }


    // 2. 존재하지 않는 URL 리소스 요청 시 (404 Error)
    @ExceptionHandler(NoResourceFoundException.class)
    public String handle404(NoResourceFoundException e) {
        log.warn("404 Page Not Found: {}", e.getMessage());
        return "error/404"; // templates/error/404.html 페이지로 이동
    }

    // 3. 비동기 AJAX / API 요청 도중 발생한 예외 처리 (JSON 반환)
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleApiException(RuntimeException e) {
        log.error("API Request Exception: ", e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", e.getMessage());
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
