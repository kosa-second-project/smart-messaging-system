package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequestDTO;
import com.example.smartmessaging.ai.dto.type.AiContextType;
import com.example.smartmessaging.ai.dto.type.ChannelType;
import com.example.smartmessaging.ai.dto.type.MessageType;
import com.example.smartmessaging.ai.dto.type.TemplateCategory;
import com.example.smartmessaging.ai.rag.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagPromptContextServiceTest {

    @Test
    void suggestionQuery_usesRequestConditions() {
        RagPromptContextService service = new RagPromptContextService(mock(RagSearchService.class), new RagProperties());
        // 추천 검색은 사용자가 원하는 작성 방향과 템플릿 조건을 함께 넣어 관련 Hmall 문구를 찾는다.
        AiSuggestionRequestDTO request = new AiSuggestionRequestDTO(
                AiContextType.TEMPLATE_CREATE,
                MessageType.AD,
                List.of(ChannelType.SMS),
                List.of("VIP"),
                "coupon discount tone",
                TemplateCategory.BENEFIT,
                List.of()
        );

        String query = service.buildSuggestionQuery(request);

        assertThat(query)
                .contains("현대홈쇼핑 Hmall 마케팅 문구")
                .contains("coupon discount tone")
                .contains("BENEFIT")
                .contains("AD")
                .contains("SMS")
                .contains("VIP");
    }

    @Test
    void reviewQuery_usesActualMessageText() {
        RagPromptContextService service = new RagPromptContextService(mock(RagSearchService.class), new RagProperties());
        // 검수 검색은 실제 제목/본문을 중심으로 유사한 브랜드톤 참고문구를 찾는다.
        AiReviewRequestDTO request = new AiReviewRequestDTO(
                AiContextType.MESSAGE_SEND,
                MessageType.AD,
                List.of(ChannelType.LMS),
                List.of(),
                "Benefit title",
                "Check this coupon benefit today",
                List.of(),
                TemplateCategory.EVENT,
                null,
                null
        );

        String query = service.buildReviewQuery(request);

        assertThat(query)
                .contains("현대홈쇼핑 Hmall 메시지 검수 브랜드톤")
                .contains("Benefit title")
                .contains("Check this coupon benefit today")
                .contains("AD")
                .contains("LMS")
                .contains("EVENT");
    }

    @Test
    void promptContext_includesOnlyAllowedMetadata() {
        RagPromptContextService service = new RagPromptContextService(mock(RagSearchService.class), new RagProperties());
        // 프롬프트에는 모델이 참고해야 할 최소 metadata만 넣고 URL 같은 원천 정보는 제외한다.
        Document document = document();

        String context = service.toPromptContext(List.of(document));

        assertThat(context)
                .contains("[참고자료 / RAG Reference Materials]")
                .contains("hmall-campaign-001")
                .contains("campaign_copy")
                .contains("[coupon]")
                .contains("[tone_reference]")
                .contains("[100%]")
                .doesNotContain("source_url")
                .doesNotContain("https://example.com/hidden");
    }

    @Test
    void referenceLogs_includeScoreAndAllowedMetadataOnly() {
        RagPromptContextService service = new RagPromptContextService(mock(RagSearchService.class), new RagProperties());

        List<RagPromptContextService.RagReferenceLog> references = service.toReferenceLogs(List.of(document()));

        assertThat(references).singleElement().satisfies(reference -> {
            assertThat(reference.docId()).isEqualTo("hmall-campaign-001");
            assertThat(reference.score()).isEqualTo(0.82);
            assertThat(reference.sourceType()).isEqualTo("campaign_copy");
            assertThat(reference.benefitType()).isEqualTo(List.of("coupon"));
            assertThat(reference.ragUse()).isEqualTo(List.of("tone_reference"));
            assertThat(reference.cautionPhrases()).isEqualTo(List.of("100%"));
            assertThat(reference.contentPreview()).isEqualTo("full reference content must not be copied to log summary");
            assertThat(reference.toString())
                    .doesNotContain("source_url")
                    .doesNotContain("https://example.com/hidden");
        });
    }

    @Test
    void referenceLogPreview_isShortened() {
        RagPromptContextService service = new RagPromptContextService(mock(RagSearchService.class), new RagProperties());
        Document longDocument = Document.builder()
                .id("generated-id")
                .text("1234567890 1234567890 1234567890 1234567890 1234567890 1234567890 too long")
                .metadata(Map.of("doc_id", "hmall-campaign-long"))
                .score(0.5)
                .build();

        List<RagPromptContextService.RagReferenceLog> references = service.toReferenceLogs(List.of(longDocument));

        assertThat(references).singleElement().satisfies(reference -> {
            assertThat(reference.contentPreview()).endsWith("...");
            assertThat(reference.contentPreview().length()).isLessThanOrEqualTo(63);
        });
    }

    @Test
    void searchFailure_returnsEmptyContextAndReferences() {
        RagSearchService ragSearchService = mock(RagSearchService.class);
        // Qdrant 장애가 AI 추천/검사 API 전체 실패로 번지지 않도록 빈 context fallback을 확인한다.
        when(ragSearchService.similaritySearch(anyString(), anyInt()))
                .thenThrow(new RagSearchException("failed", new RuntimeException("qdrant down")));
        RagPromptContextService service = new RagPromptContextService(ragSearchService, new RagProperties());

        RagPromptContextService.RagPromptContext context = service.buildSuggestionPromptContext(new AiSuggestionRequestDTO(
                AiContextType.MESSAGE_SEND,
                MessageType.AD,
                List.of(ChannelType.SMS),
                List.of(),
                "discount",
                null,
                List.of()
        ));

        assertThat(context.promptText()).isEmpty();
        assertThat(context.references()).isEmpty();
    }

    private Document document() {
        return Document.builder()
                .id("generated-id")
                .text("full reference content must not be copied to log summary")
                .metadata(Map.of(
                        "doc_id", "hmall-campaign-001",
                        "source_type", "campaign_copy",
                        "benefit_type", List.of("coupon"),
                        "rag_use", List.of("tone_reference"),
                        "caution_phrases", List.of("100%"),
                        "source_url", "https://example.com/hidden"
                ))
                .score(0.82)
                .build();
    }
}
