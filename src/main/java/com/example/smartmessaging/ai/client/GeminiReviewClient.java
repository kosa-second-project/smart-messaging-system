package com.example.smartmessaging.ai.client;

import com.example.smartmessaging.ai.dto.response.LlmReviewResponse;
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
public class GeminiReviewClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiReviewClient(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this(chatClientBuilder.build(), objectMapper);
    }

    GeminiReviewClient(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public LlmReviewResponse review(String systemPrompt, String userPrompt) {
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            return parseResponse(content);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Gemini review request failed: exceptionType={}",
                    exception.getClass().getSimpleName());
            throw externalApiException(exception);
        }
    }

    LlmReviewResponse parseResponse(String content) {
        if (content == null || content.isBlank()) {
            throw externalApiException(new IllegalArgumentException("Empty Gemini response"));
        }

        try {
            return objectMapper.readValue(extractFirstJsonObject(content), LlmReviewResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.warn("Gemini review response parsing failed: exceptionType={}",
                    exception.getClass().getSimpleName());
            throw externalApiException(exception);
        }
    }

    private String extractFirstJsonObject(String content) {
        int start = content.indexOf('{');
        if (start < 0) {
            throw new IllegalArgumentException("JSON object not found");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < content.length(); index++) {
            char current = content.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, index + 1);
                }
            }
        }
        throw new IllegalArgumentException("Incomplete JSON object");
    }

    private BusinessException externalApiException(Exception cause) {
        return new BusinessException(
                "LLM 문맥 검사 중 오류가 발생했습니다.",
                ErrorCode.EXTERNAL_API_ERROR,
                cause
        );
    }
}
