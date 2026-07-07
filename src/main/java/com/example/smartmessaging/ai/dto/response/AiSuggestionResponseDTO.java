package com.example.smartmessaging.ai.dto.response;

import java.util.List;

public record AiSuggestionResponseDTO(
        List<AiSuggestionItemResponseDTO> suggestions
) {
}
