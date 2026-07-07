package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.ai.rag.dto.RagStatusResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RagStatusService {

    private final RagProperties ragProperties;
    private final String collectionName;

    private RagStatus status = RagStatus.NOT_INDEXED;
    private int lastIndexedCount = 0;
    private LocalDateTime lastIndexedAt;

    public RagStatusService(
            RagProperties ragProperties,
            @Value("${spring.ai.vectorstore.qdrant.collection-name:smart-message-rag}") String collectionName
    ) {
        this.ragProperties = ragProperties;
        this.collectionName = collectionName;
    }

    public synchronized void markReady(int indexedCount, LocalDateTime indexedAt) {
        this.status = RagStatus.READY;
        this.lastIndexedCount = indexedCount;
        this.lastIndexedAt = indexedAt;
    }

    public synchronized void markFailed() {
        this.status = RagStatus.FAILED;
    }

    public synchronized RagStatusResponseDTO getStatus() {
        return new RagStatusResponseDTO(
                status.name(),
                collectionName,
                ragProperties.getSeed().getPath(),
                lastIndexedCount,
                lastIndexedAt
        );
    }
}
