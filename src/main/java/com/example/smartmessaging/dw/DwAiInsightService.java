package com.example.smartmessaging.dw;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequestDTO;

import java.util.List;

public interface DwAiInsightService {

    DwPromptContext buildSuggestionPromptContext(AiSuggestionRequestDTO request);

    DwPromptContext buildReviewPromptContext(AiReviewRequestDTO request);

    record DwPromptContext(
            String promptText,
            List<String> references
    ) {
        public static DwPromptContext empty() {
            return new DwPromptContext("", List.of());
        }
    }
}
