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
        // Swagger 테스트 API용 응답은 Document를 DTO로 변환해 content와 metadata를 그대로 확인할 수 있게 한다.
        List<RagSearchResultDTO> results = similaritySearch(query, topK).stream()
                .map(this::toResult)
                .toList();
        return new RagSearchResponseDTO(query, topK, results);
    }

    public List<Document> similaritySearch(String query, int topK) {
        try {
            // VectorStore는 lazy bean이라 실제 Qdrant 연결은 검색이 필요한 시점에만 시도된다.
            VectorStore vectorStore = vectorStoreProvider.getObject();
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build()
            );

            log.debug("RAG search completed: queryLength={}, topK={}, resultCount={}",
                    query.length(), topK, documents.size());
            return documents;
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
        // Spring AI 내부 ID보다 seed 원본 doc_id가 운영 확인과 Swagger 테스트에 더 읽기 쉽다.
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
