package com.example.smartmessaging.ai.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiSuggestionResponseDTO(
        List<AiSuggestionItemResponseDTO> suggestions
) {
    public AiSuggestionResponseDTO {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }
}
