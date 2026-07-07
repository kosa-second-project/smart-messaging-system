package com.example.smartmessaging.ai.rag.dto;

import java.time.LocalDateTime;

public record RagStatusResponseDTO(
        String status,
        String collectionName,
        String sourcePath,
        int lastIndexedCount,
        LocalDateTime lastIndexedAt
) {
}
