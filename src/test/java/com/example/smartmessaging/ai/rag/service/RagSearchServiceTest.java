package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.ai.rag.dto.RagSearchResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RagSearchServiceTest {

    @Test
    void toResult_usesOriginalDocIdFromMetadataAndKeepsScore() {
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        RagSearchService service = new RagSearchService(vectorStoreProvider, new RagProperties());
        Document document = Document.builder()
                .id(RagIngestionService.toDeterministicDocumentId("hmall-campaign-157"))
                .text("Hmall 쿠폰 혜택 문구")
                .metadata(Map.of(
                        "doc_id", "hmall-campaign-157",
                        "source_type", "campaign_copy"
                ))
                .score(0.87)
                .build();

        RagSearchResultDTO result = service.toResult(document);

        assertThat(result.docId()).isEqualTo("hmall-campaign-157");
        assertThat(result.content()).isEqualTo("Hmall 쿠폰 혜택 문구");
        assertThat(result.metadata()).containsEntry("source_type", "campaign_copy");
        assertThat(result.score()).isEqualTo(0.87);
    }
}
