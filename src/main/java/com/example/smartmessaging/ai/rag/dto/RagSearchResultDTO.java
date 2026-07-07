package com.example.smartmessaging.ai.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagSearchResultDTO(
        String docId,
        String content,
        Map<String, Object> metadata,
        Double score
) {
}
