package com.example.smartmessaging.ai.dto.request;

import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiSuggestionRequestDTO(
        @NotNull AiContextType contextType,
        @NotNull MessageType messageType,
        @NotEmpty List<@NotNull ChannelType> channels,
        List<@NotBlank String> customerTags,
        String direction,
        TemplateCategory category,
        @Valid List<@NotBlank String> availableVariables
) {
    public AiSuggestionRequestDTO {
        customerTags = customerTags == null ? List.of() : List.copyOf(customerTags);
    }
}
