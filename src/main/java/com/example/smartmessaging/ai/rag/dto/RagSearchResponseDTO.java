package com.example.smartmessaging.ai.rag.dto;

import java.util.List;

public record RagSearchResponseDTO(
        String query,
        int topK,
        List<RagSearchResultDTO> results
) {
}
