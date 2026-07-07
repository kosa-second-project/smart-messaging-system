package com.example.smartmessaging.ai.rag.dto;

public record RagErrorResponseDTO(
        String status,
        String message
) {
    public static RagErrorResponseDTO failed(String message) {
        return new RagErrorResponseDTO("FAILED", message);
    }
}
