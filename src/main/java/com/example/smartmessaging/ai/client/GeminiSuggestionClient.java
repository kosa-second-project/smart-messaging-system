package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.dto.response.AiSuggestionResponse;
import com.example.smartmessaging.exception.BusinessException;
import com.example.smartmessaging.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GeminiSuggestionClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiSuggestionClient(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this(chatClientBuilder.build(), objectMapper);
    }

    GeminiSuggestionClient(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public AiSuggestionResponse generate(String prompt) {
        try {
            String content = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            return parseResponse(content);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Gemini suggestion request failed: exceptionType={}",
                    exception.getClass().getSimpleName());
            throw externalApiException(exception);
        }
    }

    AiSuggestionResponse parseResponse(String content) {
        if (content == null || content.isBlank()) {
            throw externalApiException(new IllegalArgumentException("Empty Gemini response"));
        }

        try {
            // 모델이 지시를 어기고 코드블록이나 설명을 붙여도 JSON 객체 부분만 추출한다.
            String json = extractJson(content);
            AiSuggestionResponse response = objectMapper.readValue(json, AiSuggestionResponse.class);
            if (response.getSuggestions() == null) {
                throw new IllegalArgumentException("Missing suggestions array");
            }
            return response;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("Gemini suggestion response parsing failed: exceptionType={}",
                    exception.getClass().getSimpleName());
            throw externalApiException(exception);
        }
    }

    private String extractJson(String content) {
        String trimmed = content.trim();
        // 응답의 첫 JSON 객체를 기준으로 앞뒤의 Markdown 및 설명 문장을 제거한다.
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("JSON object not found");
        }
        return trimmed.substring(start, end + 1);
    }

    private BusinessException externalApiException(Exception cause) {
        return new BusinessException(
                "AI 추천 문구 생성 중 오류가 발생했습니다.",
                ErrorCode.EXTERNAL_API_ERROR,
                cause
        );
    }
}
