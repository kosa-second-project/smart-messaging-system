package com.example.smartmessaging.exception;

import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        log.warn("Request validation failed: errorCount={}",
                exception.getBindingResult().getErrorCount());

        Map<String, Object> response = new HashMap<>();
        response.put("status", errorCode.getStatus());
        response.put("code", errorCode.getCode());
        response.put("message", errorCode.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 1. 공통 비즈니스 예외(BusinessException) 통합 처리
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException e, HttpServletRequest request, Model model) {
        log.error("Business Exception Occurred: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        
        // AJAX 또는 API 요청인 경우 JSON 응답 반환
        if (isJsonRequest(request)) {
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

    // 예외 발생시 JSON 에러 응답 대상인지 판단하는 메서드
    // 같은 예외가 발생해도 요청 종류에 따라 JSON 에러 응답, HTML 에러 페이지로 응답 형식이 다를 수 있기 때문
    private boolean isJsonRequest(HttpServletRequest request) {
        // Accept 헤더가 application/json을 명시적으로 허용하는 경우
        String acceptHeader = request.getHeader("Accept");
        if (acceptsJson(acceptHeader)) {
            return true;
        }

        // 현재 엔드포인트가 JSON을 produce할 경우
        Object producibleMediaTypes = request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        if (producibleMediaTypes instanceof Set<?> mediaTypes) {
            boolean producesJson = mediaTypes.stream()
                    .filter(MediaType.class::isInstance)
                    .map(MediaType.class::cast)
                    // produces 타입 중 와일드카드 타입이 아니고, application/json과 호환되는 타입이 있을 경우 true
                    .anyMatch(mediaType -> !mediaType.isWildcardType()
                            && mediaType.isCompatibleWith(MediaType.APPLICATION_JSON));
            if (producesJson) {
                return true;
            }
        }

        // 현재 매핑된 메서드가 @ResponseBody인 경우
        // produces 정보가 없더라도 @ResponseBody 메서드면 API 응답 대상으로 판단
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return handler instanceof HandlerMethod handlerMethod
                && handlerMethod.hasMethodAnnotation(ResponseBody.class);
    }

    // Accept 헤더가 명확하게 JSON을 허용하는지 확인
    private boolean acceptsJson(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return false;
        }

        try {
            // Accept 헤더를 MediaType으로 파싱해서 확인
            return MediaType.parseMediaTypes(acceptHeader).stream()
                    // 와일드카드 타입 제외
                    .filter(mediaType -> !mediaType.isWildcardType() && !mediaType.isWildcardSubtype())
                    // Accept: application/json;q=0 처럼 JSON을 거부한 경우는 제외
                    .filter(mediaType -> mediaType.getQualityValue() > 0)
                    // 최종적으로 application/json과 호환되는 타입인지 확인
                    .anyMatch(mediaType -> mediaType.isCompatibleWith(MediaType.APPLICATION_JSON));
        } catch (InvalidMediaTypeException e) {
            // 잘못된 Accept 헤더는 JSON 요청으로 판단하지 않고 이후 fallback 로직에 맡김
            return false;
        }
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
