package com.example.smartmessaging.ai.config;

import com.google.genai.Client;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GeminiEmbeddingConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel geminiEmbeddingModel(
            Client googleGenAiClient,
            @Value("${spring.ai.google.genai.embedding.options.model:gemini-embedding-001}") String model,
            @Value("${spring.ai.google.genai.embedding.options.output-dimensionality:3072}") int dimensions,
            @Value("${spring.ai.google.genai.embedding.options.batch-size:5}") int batchSize,
            @Value("${spring.ai.google.genai.embedding.options.max-attempts:2}") int maxAttempts
    ) {
        return new GoogleGenAiEmbeddingModel(googleGenAiClient, model, dimensions, batchSize, maxAttempts);
    }
}
