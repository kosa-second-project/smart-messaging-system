package com.example.smartmessaging.ai.config;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GoogleGenAiEmbeddingModel implements EmbeddingModel {

    private static final String TASK_RETRIEVAL_DOCUMENT = "RETRIEVAL_DOCUMENT";
    private static final String TASK_RETRIEVAL_QUERY = "RETRIEVAL_QUERY";
    private static final String TASK_SEMANTIC_SIMILARITY = "SEMANTIC_SIMILARITY";

    private final Client client;
    private final String model;
    private final int dimensions;
    private final int batchSize;
    private final int maxAttempts;

    public GoogleGenAiEmbeddingModel(Client client, String model, int dimensions, int batchSize, int maxAttempts) {
        Assert.notNull(client, "client must not be null");
        Assert.hasText(model, "model must not be blank");
        Assert.isTrue(dimensions > 0, "dimensions must be positive");
        Assert.isTrue(batchSize > 0, "batchSize must be positive");
        Assert.isTrue(maxAttempts > 0, "maxAttempts must be positive");
        this.client = client;
        this.model = model;
        this.dimensions = dimensions;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        Assert.notNull(request, "request must not be null");
        return embedTexts(request.getInstructions(), TASK_SEMANTIC_SIMILARITY);
    }

    @Override
    public float[] embed(Document document) {
        Assert.notNull(document, "document must not be null");
        return embedTexts(List.of(getEmbeddingContent(document)), TASK_RETRIEVAL_DOCUMENT)
                .getResult()
                .getOutput();
    }

    @Override
    public float[] embed(String text) {
        Assert.notNull(text, "text must not be null");
        return embedTexts(List.of(text), TASK_RETRIEVAL_QUERY)
                .getResult()
                .getOutput();
    }

    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        Assert.notNull(documents, "documents must not be null");

        // Google GenAI embedding API는 한 번에 너무 많은 문서를 보내면 지연/취소가 발생할 수 있어
        // Spring AI 기본 batching 대신 운영 설정값(batchSize)으로 더 작게 나누어 호출한다.
        List<float[]> embeddings = new ArrayList<>(documents.size());
        for (int start = 0; start < documents.size(); start += batchSize) {
            int end = Math.min(start + batchSize, documents.size());
            List<String> texts = documents.subList(start, end).stream()
                    .map(this::getEmbeddingContent)
                    .toList();
            embedTexts(texts, TASK_RETRIEVAL_DOCUMENT).getResults()
                    .forEach(embedding -> embeddings.add(embedding.getOutput()));
        }
        Assert.isTrue(embeddings.size() == documents.size(),
                "Embeddings must have the same number as that of the documents");
        return embeddings;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private EmbeddingResponse embedTexts(List<String> texts, String taskType) {
        Assert.notEmpty(texts, "texts must not be empty");

        EmbedContentConfig config = EmbedContentConfig.builder()
                .taskType(taskType)
                .outputDimensionality(dimensions)
                .build();
        EmbedContentResponse response = executeWithRetry(texts, config);
        List<ContentEmbedding> contentEmbeddings = response.embeddings()
                .orElseThrow(() -> new IllegalStateException("Google GenAI embedding response is empty."));

        List<Embedding> embeddings = new ArrayList<>(contentEmbeddings.size());
        for (int index = 0; index < contentEmbeddings.size(); index++) {
            List<Float> values = contentEmbeddings.get(index).values()
                    .orElseThrow(() -> new IllegalStateException("Google GenAI embedding values are empty."));
            embeddings.add(new Embedding(toFloatArray(values), index));
        }
        return new EmbeddingResponse(embeddings);
    }

    private EmbedContentResponse executeWithRetry(List<String> texts, EmbedContentConfig config) {
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return client.models.embedContent(model, texts, config);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (attempt >= maxAttempts) {
                    break;
                }
                // 네트워크 취소/일시 지연은 reindex 전체를 바로 실패시키지 않도록 batch 단위로만 재시도한다.
                Throwable rootCause = rootCause(exception);
                log.warn("Google GenAI embedding batch failed. Retrying: attempt={}, maxAttempts={}, batchSize={}, exception={}, rootCause={}, rootMessage={}",
                        attempt,
                        maxAttempts,
                        texts.size(),
                        exception.getClass().getSimpleName(),
                        rootCause.getClass().getSimpleName(),
                        rootCause.getMessage());
                sleepBeforeRetry(attempt);
            }
        }
        throw lastException;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(1_000L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying Google GenAI embedding request.", exception);
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }
}
