package com.example.smartmessaging.ai.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagReindexResponseDTO(
        String status,
        int indexedCount,
        String sourcePath,
        String collectionName,
        LocalDateTime indexedAt,
        String message
) {
    public static RagReindexResponseDTO success(
            int indexedCount,
            String sourcePath,
            String collectionName,
            LocalDateTime indexedAt
    ) {
        return new RagReindexResponseDTO("SUCCESS", indexedCount, sourcePath, collectionName, indexedAt, null);
    }

    public static RagReindexResponseDTO failed(String sourcePath, String collectionName, String message) {
        return new RagReindexResponseDTO("FAILED", 0, sourcePath, collectionName, null, message);
    }
}
