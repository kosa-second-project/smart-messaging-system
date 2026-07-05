package com.example.smartmessaging.ai.controller;

import com.example.smartmessaging.ai.dto.request.AiSuggestionRequest;
import com.example.smartmessaging.ai.dto.response.AiSuggestionResponse;
import com.example.smartmessaging.ai.service.AiSuggestionService;
import com.example.smartmessaging.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "AI 문구 추천", description = "Gemini를 이용한 메시지·템플릿 문구 추천 API")
@RestController
@RequestMapping("/api/ai/suggestions")
@RequiredArgsConstructor
public class AiSuggestionController {

    private final AiSuggestionService aiSuggestionService;

    @Operation(summary = "AI 문구 추천", description = "서버 룰 검증을 통과한 제목과 본문 조합을 최대 3개 반환합니다.")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AiSuggestionResponse> suggest(
            @Valid @RequestBody AiSuggestionRequest request
    ) {
        return ResponseEntity.ok(aiSuggestionService.suggest(request));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(Exception exception) {
        // 이 API의 Bean Validation 및 JSON 역직렬화 오류는 동일한 400 형식으로 응답한다.
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", errorCode.getStatus());
        response.put("code", errorCode.getCode());
        response.put("message", errorCode.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
