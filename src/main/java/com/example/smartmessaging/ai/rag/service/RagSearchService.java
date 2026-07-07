package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.ai.rag.dto.RagSearchResponseDTO;
import com.example.smartmessaging.ai.rag.dto.RagSearchResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RagSearchService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;

    public RagSearchService(ObjectProvider<VectorStore> vectorStoreProvider, RagProperties ragProperties) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.ragProperties = ragProperties;
    }

    public RagSearchResponseDTO search(String query) {
        int topK = ragProperties.getSearch().getTopK();
        try {
            // VectorStore는 lazy bean이라 검색 요청이 들어온 시점에 실제 Qdrant 연결을 시도한다.
            VectorStore vectorStore = vectorStoreProvider.getObject();
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );

            List<RagSearchResultDTO> results = documents.stream()
                    .map(this::toResult)
                    .toList();

            log.info("RAG search completed: queryLength={}, topK={}, resultCount={}",
                    query.length(), topK, results.size());
            return new RagSearchResponseDTO(query, topK, results);
        } catch (Exception exception) {
            Throwable rootCause = rootCause(exception);
            log.warn("RAG search failed: queryLength={}, exception={}, message={}, rootCause={}, rootMessage={}",
                    query.length(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    rootCause.getClass().getSimpleName(),
                    rootCause.getMessage());
            throw new RagSearchException("RAG search failed.", exception);
        }
    }

    RagSearchResultDTO toResult(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object docId = metadata.get("doc_id");
        // 내부 UUID 대신 seed 원본 doc_id를 API 응답에 노출한다.
        return new RagSearchResultDTO(
                docId == null ? document.getId() : docId.toString(),
                document.getText(),
                metadata,
                document.getScore()
        );
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
