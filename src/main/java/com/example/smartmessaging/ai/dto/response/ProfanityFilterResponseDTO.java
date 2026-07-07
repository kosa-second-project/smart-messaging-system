package com.example.smartmessaging.ai.dto.response;

import java.util.List;

public record ProfanityFilterResponseDTO(
        String trackingId,
        Status status,
        List<DetectedWord> detected,
        String filtered,
        String elapsed
) {
    public record Status(
            Integer code,
            String message,
            String description
    ) {
    }

    public record DetectedWord(
            Integer length,
            String filteredWord
    ) {
    }
}
