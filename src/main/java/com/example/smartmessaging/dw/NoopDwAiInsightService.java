package com.example.smartmessaging.dw;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequestDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dw.bigquery", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopDwAiInsightService implements DwAiInsightService {

    @Override
    public DwPromptContext buildSuggestionPromptContext(AiSuggestionRequestDTO request) {
        return DwPromptContext.empty();
    }

    @Override
    public DwPromptContext buildReviewPromptContext(AiReviewRequestDTO request) {
        return DwPromptContext.empty();
    }
}
