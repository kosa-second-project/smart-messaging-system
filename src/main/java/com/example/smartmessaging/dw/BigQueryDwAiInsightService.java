package com.example.smartmessaging.dw;

import com.example.smartmessaging.ai.dto.request.AiReviewRequestDTO;
import com.example.smartmessaging.ai.dto.request.AiSuggestionRequestDTO;
import com.google.cloud.bigquery.QueryParameterValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dw.bigquery", name = "enabled", havingValue = "true")
public class BigQueryDwAiInsightService implements DwAiInsightService {

    private static final int MAX_INSIGHTS = 5;

    private final BigQueryDwClient client;
    private final BigQueryDwProperties properties;

    @Override
    public DwPromptContext buildSuggestionPromptContext(AiSuggestionRequestDTO request) {
        return buildContext("suggestion", enumName(request.messageType()), enumName(request.category()));
    }

    @Override
    public DwPromptContext buildReviewPromptContext(AiReviewRequestDTO request) {
        return buildContext("review", enumName(request.messageType()), enumName(request.category()));
    }

    private DwPromptContext buildContext(String useCase, String messageType, String category) {
        if (!properties.isConfigured()) {
            return DwPromptContext.empty();
        }
        try {
            Map<String, QueryParameterValue> parameters = new LinkedHashMap<>();
            parameters.put("use_case", QueryParameterValue.string(useCase));
            StringBuilder filters = new StringBuilder("""
                    WHERE COALESCE(is_deleted, 0) = 0
                      AND (insight_type = @use_case OR insight_type = 'common')
                    """);
            if (messageType != null) {
                filters.append("\n  AND (message_type IS NULL OR message_type = @message_type)");
                parameters.put("message_type", QueryParameterValue.string(messageType));
            }
            if (category != null) {
                filters.append("\n  AND (category IS NULL OR category = @category)");
                parameters.put("category", QueryParameterValue.string(category));
            }
            parameters.put("limit", QueryParameterValue.int64(MAX_INSIGHTS));

            List<DwInsight> insights = client.query("""
                    SELECT
                      COALESCE(doc_id, insight_id, title, 'dw-insight') AS reference_id,
                      title,
                      content,
                      channel_type,
                      message_type,
                      category,
                      metric_name,
                      metric_value
                    FROM %s
                    %s
                    ORDER BY metric_value DESC NULLS LAST, updated_at DESC
                    LIMIT @limit
                    """.formatted(properties.table(properties.getTables().getAiMarketingInsight()), filters), parameters, row -> new DwInsight(
                    BigQueryRowParsers.string(row, "reference_id"),
                    BigQueryRowParsers.string(row, "title"),
                    BigQueryRowParsers.string(row, "content"),
                    BigQueryRowParsers.string(row, "channel_type"),
                    BigQueryRowParsers.string(row, "message_type"),
                    BigQueryRowParsers.string(row, "category"),
                    BigQueryRowParsers.string(row, "metric_name"),
                    BigQueryRowParsers.doubleValue(row, "metric_value")
            ));
            return toPromptContext(insights);
        } catch (RuntimeException exception) {
            log.warn("BigQuery DW AI insights unavailable. Continuing without DW context: useCase={}, exceptionType={}",
                    useCase,
                    exception.getClass().getSimpleName());
            return DwPromptContext.empty();
        }
    }

    private DwPromptContext toPromptContext(List<DwInsight> insights) {
        if (insights == null || insights.isEmpty()) {
            return DwPromptContext.empty();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n[BigQuery DW Insights]\n");
        builder.append("Purpose: performance context only, not a policy decision source.\n");
        builder.append("Usage rules:\n");
        builder.append("- Use these insights to prefer proven tone, channel fit, and benefit clarity.\n");
        builder.append("- Do not invent offers, products, dates, or guarantees from DW rows.\n");
        builder.append("- Do not override SERVER_RULE, PROFANITY_FILTER, or OPENAI_MODERATION issues.\n");
        builder.append("Insights:\n");

        List<String> references = insights.stream()
                .map(insight -> "dw:" + insight.referenceId())
                .toList();
        int index = 1;
        for (DwInsight insight : insights) {
            builder.append(index++).append(". ");
            append(builder, insight.title());
            append(builder, insight.content());
            StringJoiner metadata = new StringJoiner(", ");
            add(metadata, "channel", insight.channelType());
            add(metadata, "messageType", insight.messageType());
            add(metadata, "category", insight.category());
            add(metadata, insight.metricName(), insight.metricValue() == null ? null : String.valueOf(insight.metricValue()));
            if (metadata.length() > 0) {
                builder.append(" (").append(metadata).append(")");
            }
            builder.append('\n');
        }
        return new DwPromptContext(builder.toString(), references);
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' ') {
                builder.append(' ');
            }
            builder.append(value.trim());
        }
    }

    private void add(StringJoiner joiner, String key, String value) {
        if (key != null && value != null && !value.isBlank()) {
            joiner.add(key + "=" + value);
        }
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private record DwInsight(
            String referenceId,
            String title,
            String content,
            String channelType,
            String messageType,
            String category,
            String metricName,
            Double metricValue
    ) {
    }
}
