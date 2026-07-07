package com.example.smartmessaging.ai.rag.service;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequestDTO;
import com.example.smartmessaging.ai.rag.config.RagProperties;
import com.example.smartmessaging.dw.DwAiInsightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
@Service
public class RagPromptContextService {

    // 프롬프트가 너무 길어지면 Gemini 응답 품질이 흔들릴 수 있어 문서 본문은 짧게 잘라 넣는다.
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int MAX_LOG_PREVIEW_LENGTH = 60;

    private final RagSearchService ragSearchService;
    private final RagProperties ragProperties;
    private final DwAiInsightService dwAiInsightService;

    @Autowired
    public RagPromptContextService(
            RagSearchService ragSearchService,
            RagProperties ragProperties,
            DwAiInsightService dwAiInsightService
    ) {
        this.ragSearchService = ragSearchService;
        this.ragProperties = ragProperties;
        this.dwAiInsightService = dwAiInsightService;
    }

    RagPromptContextService(RagSearchService ragSearchService, RagProperties ragProperties) {
        this(ragSearchService, ragProperties, null);
    }

    public String buildSuggestionContext(AiSuggestionRequestDTO request) {
        return buildSuggestionPromptContext(request).promptText();
    }

    public RagPromptContext buildSuggestionPromptContext(AiSuggestionRequestDTO request) {
        String query = buildSuggestionQuery(request);
        RagPromptContext ragContext = buildContextSafely(query, "suggestion");
        return appendDwContext(ragContext, buildSuggestionDwContext(request));
    }

    public String buildReviewContext(AiReviewRequestDTO request) {
        return buildReviewPromptContext(request).promptText();
    }

    public RagPromptContext buildReviewPromptContext(AiReviewRequestDTO request) {
        String query = buildReviewQuery(request);
        RagPromptContext ragContext = buildContextSafely(query, "review");
        return appendDwContext(ragContext, buildReviewDwContext(request));
    }

    String buildSuggestionQuery(AiSuggestionRequestDTO request) {
        List<String> parts = new ArrayList<>();
        // Seed 데이터가 한국어 Hmall 문구 중심이라 검색 anchor도 한국어 브랜드/목적어로 시작한다.
        add(parts, "현대홈쇼핑 Hmall 마케팅 문구");
        add(parts, request.direction());
        add(parts, request.category());
        add(parts, request.messageType());
        add(parts, request.channels());
        add(parts, request.customerTags());
        return String.join(" ", parts);
    }

    String buildReviewQuery(AiReviewRequestDTO request) {
        List<String> parts = new ArrayList<>();
        // 검사는 실제 작성된 제목/본문과 가까운 브랜드톤 참고문구를 찾는 것이 목적이다.
        add(parts, "현대홈쇼핑 Hmall 메시지 검수 브랜드톤");
        add(parts, request.title());
        add(parts, request.content());
        add(parts, request.messageType());
        add(parts, request.channels());
        add(parts, request.category());
        return String.join(" ", parts);
    }

    String toPromptContext(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        // RAG는 판정 근거가 아니라 브랜드톤 참고자료라는 제한을 프롬프트 안에 함께 넣는다.
        builder.append("\n[참고자료 / RAG Reference Materials]\n");
        builder.append("Purpose: brand-tone reference only, not a policy decision source.\n");
        builder.append("Usage rules:\n");
        builder.append("- Use these materials only for Hmall/Hyundai Home Shopping tone and sentence structure.\n");
        builder.append("- Do not invent product names, benefits, gifts, periods, or conditions that the user did not provide.\n");
        builder.append("- Do not copy reference sentences verbatim.\n");
        builder.append("- Watch for overstated benefits, missing conditions, and misleading universal-benefit wording.\n");
        builder.append("- RAG does not determine legal or policy violations; it only helps expression review.\n");
        builder.append("Materials:\n");

        int index = 1;
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            builder.append(index++).append(". doc_id: ").append(metadataValue(metadata, "doc_id", document.getId())).append('\n');
            builder.append("   content: ").append(truncate(document.getText())).append('\n');
            // 프롬프트에는 필요한 metadata만 넣어 검색 score, URL, 내부 상태가 모델 판단에 섞이지 않게 한다.
            appendMetadata(builder, metadata, "source_type");
            appendMetadata(builder, metadata, "benefit_type");
            appendMetadata(builder, metadata, "rag_use");
            appendMetadata(builder, metadata, "caution_phrases");
        }
        return builder.toString();
    }

    List<RagReferenceLog> toReferenceLogs(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .map(this::toReferenceLog)
                .toList();
    }

    private RagPromptContext buildContextSafely(String query, String useCase) {
        try {
            int topK = ragProperties.getSearch().getTopK();
            List<Document> documents = ragSearchService.similaritySearch(query, topK);
            return new RagPromptContext(toPromptContext(documents), toReferenceLogs(documents));
        } catch (RuntimeException exception) {
            // AI 추천/검사는 RAG가 없어도 동작해야 하므로 검색 실패를 빈 context로 낮춰 처리한다.
            log.warn("RAG context unavailable for AI {}. Continuing without RAG context: queryLength={}, exceptionType={}, message={}",
                    useCase,
                    query.length(),
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            return RagPromptContext.empty();
        }
    }

    private DwAiInsightService.DwPromptContext buildSuggestionDwContext(AiSuggestionRequestDTO request) {
        if (dwAiInsightService == null) {
            return DwAiInsightService.DwPromptContext.empty();
        }
        try {
            return dwAiInsightService.buildSuggestionPromptContext(request);
        } catch (RuntimeException exception) {
            log.warn("DW context unavailable for AI suggestion. Continuing without DW context: exceptionType={}",
                    exception.getClass().getSimpleName());
            return DwAiInsightService.DwPromptContext.empty();
        }
    }

    private DwAiInsightService.DwPromptContext buildReviewDwContext(AiReviewRequestDTO request) {
        if (dwAiInsightService == null) {
            return DwAiInsightService.DwPromptContext.empty();
        }
        try {
            return dwAiInsightService.buildReviewPromptContext(request);
        } catch (RuntimeException exception) {
            log.warn("DW context unavailable for AI review. Continuing without DW context: exceptionType={}",
                    exception.getClass().getSimpleName());
            return DwAiInsightService.DwPromptContext.empty();
        }
    }

    private RagPromptContext appendDwContext(
            RagPromptContext ragContext,
            DwAiInsightService.DwPromptContext dwContext
    ) {
        if (dwContext == null || dwContext.promptText() == null || dwContext.promptText().isBlank()) {
            return ragContext;
        }
        List<RagReferenceLog> references = new ArrayList<>(ragContext.references());
        references.addAll(dwContext.references().stream()
                .map(reference -> new RagReferenceLog(dwReferenceId(reference), null, "bigquery_dw", null, List.of("performance_context"), null, ""))
                .toList());
        return new RagPromptContext(ragContext.promptText() + dwContext.promptText(), List.copyOf(references));
    }

    private String dwReferenceId(String reference) {
        if (reference == null || reference.isBlank()) {
            return "dw:unknown";
        }
        return reference.startsWith("dw:") ? reference : "dw:" + reference;
    }

    private RagReferenceLog toReferenceLog(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new RagReferenceLog(
                String.valueOf(metadataValue(metadata, "doc_id", document.getId())),
                document.getScore(),
                metadata.get("source_type"),
                metadata.get("benefit_type"),
                metadata.get("rag_use"),
                metadata.get("caution_phrases"),
                previewForLog(document.getText())
        );
    }

    private void appendMetadata(StringBuilder builder, Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value != null) {
            builder.append("   ").append(key).append(": ").append(formatMetadataValue(value)).append('\n');
        }
    }

    private Object metadataValue(Map<String, Object> metadata, String key, String fallback) {
        Object value = metadata.get(key);
        return value == null ? fallback : formatMetadataValue(value);
    }

    private String formatMetadataValue(Object value) {
        if (value instanceof Iterable<?> iterable) {
            StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (Object item : iterable) {
                joiner.add(String.valueOf(item));
            }
            return joiner.toString();
        }
        return String.valueOf(value);
    }

    private void add(List<String> parts, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                add(parts, item);
            }
            return;
        }

        String text = Objects.toString(value, "").trim();
        if (!text.isBlank()) {
            parts.add(text);
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_CONTENT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_CONTENT_LENGTH) + "...";
    }

    private String previewForLog(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_LOG_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LOG_PREVIEW_LENGTH) + "...";
    }

    public record RagPromptContext(
            String promptText,
            List<RagReferenceLog> references
    ) {
        public static RagPromptContext empty() {
            return new RagPromptContext("", List.of());
        }
    }

    public record RagReferenceLog(
            String docId,
            Double score,
            Object sourceType,
            Object benefitType,
            Object ragUse,
            Object cautionPhrases,
            String contentPreview
    ) {
    }
}
