package com.example.smartmessaging.ai.dto.request;

import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;

import java.util.List;

public record AiReviewRequestDTO(
        AiContextType contextType,
        MessageType messageType,
        List<ChannelType> channels,
        List<String> customerTags,
        String title,
        String content,
        List<String> availableVariables,
        TemplateCategory category,
        Long templateId,
        Long userId
) {
}
