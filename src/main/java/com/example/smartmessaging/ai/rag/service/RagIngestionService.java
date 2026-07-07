package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.ai.rag.dto.RagReindexResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RagIngestionService {

    private static final String DOCUMENT_ID_NAMESPACE = "smart-message-rag:";
    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
    };

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final RagProperties ragProperties;
    private final RagStatusService ragStatusService;
    private final String collectionName;

    public RagIngestionService(
            ObjectProvider<VectorStore> vectorStoreProvider,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            RagProperties ragProperties,
            RagStatusService ragStatusService,
            @Value("${spring.ai.vectorstore.qdrant.collection-name:smart-message-rag}") String collectionName
    ) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.ragProperties = ragProperties;
        this.ragStatusService = ragStatusService;
        this.collectionName = collectionName;
    }

    public RagReindexResponseDTO reindex() {
        String sourcePath = ragProperties.getSeed().getPath();
        log.info("RAG reindex started: sourcePath={}, collectionName={}", sourcePath, collectionName);

        try {
            List<Document> documents = loadDocuments(sourcePath);
            if (!documents.isEmpty()) {
                // Qdrant add is an upsert for deterministic UUID ids. Avoid pre-delete
                // so existing points survive if embedding or upsert fails midway.
                VectorStore vectorStore = vectorStoreProvider.getObject();
                vectorStore.add(documents);
            }

            LocalDateTime indexedAt = LocalDateTime.now();
            ragStatusService.markReady(documents.size(), indexedAt);
            log.info("RAG reindex completed: indexedCount={}, collectionName={}", documents.size(), collectionName);
            return RagReindexResponseDTO.success(documents.size(), sourcePath, collectionName, indexedAt);
        } catch (Exception exception) {
            ragStatusService.markFailed();
            Throwable rootCause = rootCause(exception);
            log.warn("RAG reindex failed: exception={}, message={}, rootCause={}, rootMessage={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    rootCause.getClass().getSimpleName(),
                    rootCause.getMessage());
            throw new RagIngestionException("RAG reindex failed.", exception);
        }
    }

    List<Document> loadDocuments(String sourcePath) throws IOException {
        Resource resource = resourceLoader.getResource(sourcePath);
        if (!resource.exists()) {
            throw new IOException("RAG seed file not found: " + sourcePath);
        }

        List<Document> documents = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                Map<String, Object> row = parseLine(line, lineNumber);
                Document document = toDocument(row);
                if (document != null) {
                    documents.add(document);
                }
            }
        }
        return documents;
    }

    private Map<String, Object> parseLine(String line, int lineNumber) throws JsonProcessingException {
        try {
            return objectMapper.readValue(line, JSON_OBJECT_TYPE);
        } catch (JsonProcessingException exception) {
            log.warn("RAG seed JSON parse failed: lineNumber={}, exception={}, message={}",
                    lineNumber, exception.getClass().getSimpleName(), exception.getOriginalMessage());
            throw exception;
        }
    }

    Document toDocument(Map<String, Object> row) {
        String docId = asText(row.get("doc_id"));
        String content = asText(row.get("content"));
        if (!StringUtils.hasText(docId) || !StringUtils.hasText(content)) {
            return null;
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            if (!"content".equals(key) && value != null) {
                metadata.put(key, value);
            }
        });

        // Embed only content; keep the original doc_id and other fields as metadata.
        return new Document(toDeterministicDocumentId(docId), content, metadata);
    }

    static String toDeterministicDocumentId(String docId) {
        // Spring AI Qdrant requires UUID point ids, so map source doc_id stably.
        return UUID.nameUUIDFromBytes((DOCUMENT_ID_NAMESPACE + docId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
