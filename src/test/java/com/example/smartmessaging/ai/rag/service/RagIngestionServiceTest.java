package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagIngestionServiceTest {

    @Test
    void loadDocuments_ignoresBlankAndRowsWithoutRequiredFields() throws Exception {
        String jsonl = """
                {"doc_id":"hmall-campaign-157","content":"쿠폰 혜택 안내","source_type":"campaign_copy","benefit_type":["coupon"],"posted_at":null}

                {"doc_id":"missing-content","source_type":"campaign_copy"}
                {"content":"doc id 없음","source_type":"campaign_copy"}
                """;
        RagIngestionService service = serviceWith(jsonl);

        List<Document> documents = service.loadDocuments("classpath:test-rag.jsonl");

        assertThat(documents).hasSize(1);
        Document document = documents.get(0);
        assertThat(document.getId()).isEqualTo(RagIngestionService.toDeterministicDocumentId("hmall-campaign-157"));
        assertThat(document.getText()).isEqualTo("쿠폰 혜택 안내");
        assertThat(document.getMetadata())
                .containsEntry("doc_id", "hmall-campaign-157")
                .containsEntry("source_type", "campaign_copy")
                .doesNotContainKey("content")
                .doesNotContainKey("posted_at");
        assertThat(document.getMetadata().get("benefit_type")).asList().containsExactly("coupon");
    }

    @Test
    void deterministicDocumentId_isStableUuid() {
        String first = RagIngestionService.toDeterministicDocumentId("hmall-campaign-157");
        String second = RagIngestionService.toDeterministicDocumentId("hmall-campaign-157");
        String different = RagIngestionService.toDeterministicDocumentId("hmall-campaign-158");

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(different);
        assertThat(first).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void reindex_usesUpsertWithoutPreDelete() {
        String jsonl = """
                {"doc_id":"hmall-campaign-157","content":"coupon benefit guide","source_type":"campaign_copy"}
                """;
        RagProperties properties = new RagProperties();
        RagStatusService statusService = new RagStatusService(properties, "smart-message-rag");
        VectorStore vectorStore = mock(VectorStore.class);
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getObject()).thenReturn(vectorStore);
        RagIngestionService service = new RagIngestionService(
                vectorStoreProvider,
                new ObjectMapper(),
                new InMemoryResourceLoader(jsonl),
                properties,
                statusService,
                "smart-message-rag"
        );

        service.reindex();

        verify(vectorStore, never()).delete(anyList());
        verify(vectorStore).add(argThat(documents -> documents.size() == 1));
    }

    private RagIngestionService serviceWith(String jsonl) {
        RagProperties properties = new RagProperties();
        RagStatusService statusService = new RagStatusService(properties, "smart-message-rag");
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getObject()).thenReturn(mock(VectorStore.class));
        return new RagIngestionService(
                vectorStoreProvider,
                new ObjectMapper(),
                new InMemoryResourceLoader(jsonl),
                properties,
                statusService,
                "smart-message-rag"
        );
    }

    private static class InMemoryResourceLoader implements ResourceLoader {
        private final String content;

        private InMemoryResourceLoader(String content) {
            this.content = content;
        }

        @Override
        public Resource getResource(String location) {
            return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClass().getClassLoader();
        }
    }
}
