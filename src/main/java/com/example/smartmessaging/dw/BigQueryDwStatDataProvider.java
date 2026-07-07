package com.example.smartmessaging.dw;

import com.example.smartmessaging.dto.request.StatSearchRequest;
import com.example.smartmessaging.dto.response.DashboardSummaryResponse;
import com.example.smartmessaging.dto.vo.ChannelStatVO;
import com.example.smartmessaging.dto.vo.ChannelVO;
import com.example.smartmessaging.dto.vo.ClickStatVO;
import com.example.smartmessaging.dto.vo.CustomerChannelConsentSummaryVO;
import com.example.smartmessaging.dto.vo.CustomerStatVO;
import com.example.smartmessaging.dto.vo.MessageStatByDegreeVO;
import com.example.smartmessaging.dto.vo.MessageStatVO;
import com.google.cloud.bigquery.QueryParameterValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dw.bigquery", name = "enabled", havingValue = "true")
public class BigQueryDwStatDataProvider implements DwStatDataProvider {

    private final BigQueryDwClient client;
    private final BigQueryDwProperties properties;

    @Override
    public Optional<List<ChannelVO>> selectActiveChannels() {
        return querySafely("active channels", () -> client.query("""
                SELECT
                  id,
                  channel_type,
                  cost_per_msg,
                  max_length,
                  is_active
                FROM %s
                WHERE COALESCE(is_deleted, 0) = 0
                  AND COALESCE(is_active, 1) = 1
                ORDER BY id
                """.formatted(table(properties.getTables().getChannel())), Map.of(), row -> ChannelVO.builder()
                .id(BigQueryRowParsers.longValue(row, "id"))
                .channelType(BigQueryRowParsers.string(row, "channel_type"))
                .costPerMsg(BigQueryRowParsers.decimal(row, "cost_per_msg"))
                .maxLength(BigQueryRowParsers.intValue(row, "max_length"))
                .isActive(BigQueryRowParsers.bool(row, "is_active"))
                .build()));
    }

    @Override
    public Optional<List<MessageStatVO>> selectMessageStats(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "stat_date", "WHERE COALESCE(is_deleted, 0) = 0");
        return querySafely("message stats", () -> client.query("""
                SELECT
                  total_send_count,
                  total_success_count,
                  billing_cost,
                  max_cost,
                  stat_date
                FROM %s
                %s
                ORDER BY stat_date
                """.formatted(table(properties.getTables().getMessageStat()), filter.sql()), filter.parameters(), this::messageStat));
    }

    @Override
    public Optional<List<MessageStatByDegreeVO>> selectDeliveryMessageStatByDegrees(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "msbd.stat_date", """
                WHERE COALESCE(msbd.is_deleted, 0) = 0
                  AND COALESCE(c.is_deleted, 0) = 0
                """);
        Map<String, QueryParameterValue> parameters = new LinkedHashMap<>(filter.parameters());
        String channelFilter = "";
        if (hasText(request.getChannel())) {
            channelFilter = "\n  AND c.channel_type = @channel";
            parameters.put("channel", QueryParameterValue.string(request.getChannel()));
        }

        String sql = """
                SELECT
                  msbd.id,
                  msbd.degree,
                  msbd.send_count,
                  msbd.success_count,
                  msbd.stat_date,
                  msbd.cost,
                  msbd.channel_id
                FROM %s msbd
                INNER JOIN %s c
                        ON c.id = msbd.channel_id
                %s%s
                ORDER BY msbd.stat_date, msbd.degree, msbd.channel_id
                """.formatted(
                table(properties.getTables().getMessageStatByDegree()),
                table(properties.getTables().getChannel()),
                filter.sql(),
                channelFilter
        );
        return querySafely("message stats by degree", () -> client.query(sql, parameters, this::messageStatByDegree));
    }

    @Override
    public Optional<List<ChannelStatVO>> selectChannelStats(StatSearchRequest request) {
        Map<String, QueryParameterValue> parameters = new LinkedHashMap<>();
        String channelFilter = "";
        if (hasText(request.getChannel())) {
            channelFilter = "\n  AND c.channel_type = @channel";
            parameters.put("channel", QueryParameterValue.string(request.getChannel()));
        }
        String sql = """
                SELECT
                  cs.id,
                  cs.click_target_count,
                  cs.click_count,
                  cs.conversion_target_count,
                  cs.conversion_count,
                  cs.consent_target_count,
                  cs.consent_count,
                  cs.channel_id
                FROM %s cs
                INNER JOIN %s c
                        ON c.id = cs.channel_id
                WHERE COALESCE(cs.is_deleted, 0) = 0
                  AND COALESCE(c.is_deleted, 0) = 0%s
                ORDER BY cs.channel_id
                """.formatted(table(properties.getTables().getChannelStat()), table(properties.getTables().getChannel()), channelFilter);
        return querySafely("channel stats", () -> client.query(sql, parameters, row -> ChannelStatVO.builder()
                .id(BigQueryRowParsers.longValue(row, "id"))
                .clickTargetCount(BigQueryRowParsers.intValue(row, "click_target_count"))
                .clickCount(BigQueryRowParsers.intValue(row, "click_count"))
                .conversionTargetCount(BigQueryRowParsers.intValue(row, "conversion_target_count"))
                .conversionCount(BigQueryRowParsers.intValue(row, "conversion_count"))
                .consentTargetCount(BigQueryRowParsers.intValue(row, "consent_target_count"))
                .consentCount(BigQueryRowParsers.intValue(row, "consent_count"))
                .channelId(BigQueryRowParsers.longValue(row, "channel_id"))
                .build()));
    }

    @Override
    public Optional<List<CustomerStatVO>> selectCustomerStats(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "stat_date", "WHERE COALESCE(is_deleted, 0) = 0");
        return querySafely("customer stats", () -> client.query("""
                SELECT
                  id,
                  stat_date,
                  total_customer_count,
                  normal_customer_count,
                  new_customer_count,
                  dormant_customer_count,
                  sms_consent_count,
                  kakao_consent_count,
                  email_consent_count,
                  joined_customer_count
                FROM %s
                %s
                ORDER BY stat_date
                """.formatted(table(properties.getTables().getCustomerStat()), filter.sql()), filter.parameters(), this::customerStat));
    }

    @Override
    public Optional<List<CustomerChannelConsentSummaryVO>> selectCustomerChannelConsents(StatSearchRequest request) {
        Map<String, QueryParameterValue> parameters = new LinkedHashMap<>();
        String channelFilter = "";
        if (hasText(request.getChannel())) {
            channelFilter = "\n  AND c.channel_type = @channel";
            parameters.put("channel", QueryParameterValue.string(request.getChannel()));
        }
        String sql = """
                SELECT
                  ccc.channel_id,
                  COUNT(*) AS total_count,
                  SUM(CASE WHEN COALESCE(ccc.is_consented, 0) = 1 THEN 1 ELSE 0 END) AS consented_count,
                  SUM(CASE WHEN COALESCE(ccc.is_consented, 0) = 1 THEN 0 ELSE 1 END) AS rejected_count
                FROM %s ccc
                INNER JOIN %s c
                        ON c.id = ccc.channel_id
                WHERE COALESCE(ccc.is_deleted, 0) = 0
                  AND COALESCE(c.is_deleted, 0) = 0%s
                GROUP BY ccc.channel_id
                ORDER BY ccc.channel_id
                """.formatted(
                table(properties.getTables().getCustomerChannelConsent()),
                table(properties.getTables().getChannel()),
                channelFilter
        );
        return querySafely("customer channel consents", () -> client.query(sql, parameters, row -> CustomerChannelConsentSummaryVO.builder()
                .channelId(BigQueryRowParsers.longValue(row, "channel_id"))
                .totalCount(BigQueryRowParsers.longValue(row, "total_count"))
                .consentedCount(BigQueryRowParsers.longValue(row, "consented_count"))
                .rejectedCount(BigQueryRowParsers.longValue(row, "rejected_count"))
                .build()));
    }

    @Override
    public Optional<List<ClickStatVO>> selectPerformanceClickStats(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "cs.stat_date", """
                WHERE COALESCE(cs.is_deleted, 0) = 0
                  AND COALESCE(c.is_deleted, 0) = 0
                """);
        Map<String, QueryParameterValue> parameters = new LinkedHashMap<>(filter.parameters());
        String channelFilter = "";
        if (hasText(request.getChannel())) {
            channelFilter = "\n  AND c.channel_type = @channel";
            parameters.put("channel", QueryParameterValue.string(request.getChannel()));
        }
        String sql = """
                SELECT
                  cs.id,
                  cs.stat_date,
                  cs.channel_id,
                  cs.hour_00,
                  cs.hour_01,
                  cs.hour_02,
                  cs.hour_03,
                  cs.hour_04,
                  cs.hour_05,
                  cs.hour_06,
                  cs.hour_07,
                  cs.hour_08,
                  cs.hour_09,
                  cs.hour_10,
                  cs.hour_11,
                  cs.hour_12,
                  cs.hour_13,
                  cs.hour_14,
                  cs.hour_15,
                  cs.hour_16,
                  cs.hour_17,
                  cs.hour_18,
                  cs.hour_19,
                  cs.hour_20,
                  cs.hour_21,
                  cs.hour_22,
                  cs.hour_23
                FROM %s cs
                INNER JOIN %s c
                        ON c.id = cs.channel_id
                %s%s
                ORDER BY cs.stat_date, cs.channel_id
                """.formatted(table(properties.getTables().getClickStat()), table(properties.getTables().getChannel()), filter.sql(), channelFilter);
        return querySafely("performance click stats", () -> client.query(sql, parameters, this::clickStat));
    }

    @Override
    public Optional<MessageStatVO> selectMessageSummary(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "stat_date", "WHERE COALESCE(is_deleted, 0) = 0");
        return querySafely("message summary", () -> client.query("""
                SELECT
                  NULL AS stat_date,
                  SUM(COALESCE(total_send_count, 0)) AS total_send_count,
                  SUM(COALESCE(total_success_count, 0)) AS total_success_count,
                  SUM(COALESCE(billing_cost, 0)) AS billing_cost,
                  SUM(COALESCE(max_cost, 0)) AS max_cost
                FROM %s
                %s
                """.formatted(table(properties.getTables().getMessageStat()), filter.sql()), filter.parameters(), this::messageStat))
                .flatMap(rows -> rows.stream().findFirst());
    }

    @Override
    public Optional<List<MessageStatVO>> selectMessageTrend(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "stat_date", "WHERE COALESCE(is_deleted, 0) = 0");
        return querySafely("message trend", () -> client.query("""
                SELECT
                  stat_date,
                  total_send_count,
                  total_success_count,
                  billing_cost,
                  max_cost
                FROM %s
                %s
                ORDER BY stat_date DESC
                LIMIT 7
                """.formatted(table(properties.getTables().getMessageStat()), filter.sql()), filter.parameters(), this::messageStat));
    }

    @Override
    public Optional<List<MessageStatByDegreeVO>> selectChannelSendSummary(StatSearchRequest request) {
        return selectDeliveryMessageStatByDegrees(request)
                .map(rows -> rows.stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                MessageStatByDegreeVO::getChannelId,
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()
                        ))
                        .entrySet()
                        .stream()
                        .map(entry -> MessageStatByDegreeVO.builder()
                                .channelId(entry.getKey())
                                .sendCount(entry.getValue().stream().mapToInt(row -> n(row.getSendCount())).sum())
                                .successCount(entry.getValue().stream().mapToInt(row -> n(row.getSuccessCount())).sum())
                                .build())
                        .toList());
    }

    @Override
    public Optional<CustomerStatVO> selectLatestCustomerStat(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "stat_date", "WHERE COALESCE(is_deleted, 0) = 0");
        return querySafely("latest customer stat", () -> client.query("""
                SELECT
                  id,
                  stat_date,
                  total_customer_count,
                  normal_customer_count,
                  new_customer_count,
                  dormant_customer_count,
                  sms_consent_count,
                  kakao_consent_count,
                  email_consent_count,
                  joined_customer_count
                FROM %s
                %s
                ORDER BY stat_date DESC
                LIMIT 1
                """.formatted(table(properties.getTables().getCustomerStat()), filter.sql()), filter.parameters(), this::customerStat))
                .flatMap(rows -> rows.stream().findFirst());
    }

    @Override
    public Optional<List<DashboardSummaryResponse.TemplatePerformance>> selectTemplatePerformanceTop(StatSearchRequest request) {
        DateFilter filter = dateFilter(request, "ts.stat_date", """
                WHERE COALESCE(ts.is_deleted, 0) = 0
                """);
        String sql = """
                SELECT
                  COALESCE(t.title, CONCAT('템플릿 ', CAST(ts.template_id AS STRING))) AS name,
                  CASE
                    WHEN SUM(COALESCE(ts.click_target_count, 0)) = 0 THEN 0
                    ELSE ROUND((SUM(COALESCE(ts.click_count, 0)) / SUM(COALESCE(ts.click_target_count, 0))) * 100, 1)
                  END AS click,
                  CASE
                    WHEN SUM(COALESCE(ts.conversion_target_count, 0)) = 0 THEN NULL
                    ELSE ROUND((SUM(COALESCE(ts.conversion_count, 0)) / SUM(COALESCE(ts.conversion_target_count, 0))) * 100, 1)
                  END AS conversion,
                  CASE
                    WHEN COALESCE(t.is_ai_generated, 0) = 1 THEN 'AI 템플릿'
                    ELSE '기본 템플릿'
                  END AS source
                FROM %s ts
                LEFT JOIN %s t
                       ON t.id = ts.template_id
                      AND COALESCE(t.is_deleted, 0) = 0
                %s
                GROUP BY ts.template_id, t.title, t.is_ai_generated
                ORDER BY click DESC, SUM(COALESCE(ts.click_count, 0)) DESC
                LIMIT 5
                """.formatted(table(properties.getTables().getTemplateStat()), table(properties.getTables().getTemplate()), filter.sql());
        return querySafely("template performance top", () -> client.query(sql, filter.parameters(), row -> DashboardSummaryResponse.TemplatePerformance.builder()
                .name(BigQueryRowParsers.string(row, "name"))
                .click(n(BigQueryRowParsers.doubleValue(row, "click")))
                .conversion(BigQueryRowParsers.doubleValue(row, "conversion"))
                .source(BigQueryRowParsers.string(row, "source"))
                .build()));
    }

    private <T> Optional<List<T>> querySafely(String label, Supplier<List<T>> query) {
        if (!properties.isConfigured()) {
            log.warn("BigQuery DW {} skipped: projectId/dataset is not configured.", label);
            return Optional.empty();
        }
        try {
            List<T> result = query.get();
            return Optional.of(result);
        } catch (RuntimeException exception) {
            log.warn("BigQuery DW {} unavailable. Falling back to primary datasource: exceptionType={}",
                    label,
                    exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private String table(String tableName) {
        return properties.table(tableName);
    }

    private DateFilter dateFilter(StatSearchRequest request, String column, String baseSql) {
        Map<String, QueryParameterValue> parameters = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder(baseSql.stripTrailing());
        if (request.getFrom() != null) {
            sql.append("\n  AND ").append(column).append(" >= @from_date");
            parameters.put("from_date", QueryParameterValue.date(request.getFrom().toString()));
        }
        if (request.getTo() != null) {
            sql.append("\n  AND ").append(column).append(" <= @to_date");
            parameters.put("to_date", QueryParameterValue.date(request.getTo().toString()));
        }
        return new DateFilter(sql.toString(), parameters);
    }

    private MessageStatVO messageStat(com.google.cloud.bigquery.FieldValueList row) {
        return MessageStatVO.builder()
                .date(BigQueryRowParsers.date(row, "stat_date"))
                .totalSendCount(BigQueryRowParsers.intValue(row, "total_send_count"))
                .totalSuccessCount(BigQueryRowParsers.intValue(row, "total_success_count"))
                .billingCost(n(BigQueryRowParsers.decimal(row, "billing_cost")))
                .maxCost(n(BigQueryRowParsers.decimal(row, "max_cost")))
                .build();
    }

    private MessageStatByDegreeVO messageStatByDegree(com.google.cloud.bigquery.FieldValueList row) {
        return MessageStatByDegreeVO.builder()
                .id(BigQueryRowParsers.longValue(row, "id"))
                .degree(BigQueryRowParsers.intValue(row, "degree"))
                .sendCount(BigQueryRowParsers.intValue(row, "send_count"))
                .successCount(BigQueryRowParsers.intValue(row, "success_count"))
                .date(BigQueryRowParsers.date(row, "stat_date"))
                .cost(BigQueryRowParsers.decimal(row, "cost"))
                .channelId(BigQueryRowParsers.longValue(row, "channel_id"))
                .build();
    }

    private CustomerStatVO customerStat(com.google.cloud.bigquery.FieldValueList row) {
        return CustomerStatVO.builder()
                .id(BigQueryRowParsers.longValue(row, "id"))
                .date(BigQueryRowParsers.date(row, "stat_date"))
                .totalCustomerCount(BigQueryRowParsers.intValue(row, "total_customer_count"))
                .normalCustomerCount(BigQueryRowParsers.intValue(row, "normal_customer_count"))
                .newCustomerCount(BigQueryRowParsers.intValue(row, "new_customer_count"))
                .dormantCustomerCount(BigQueryRowParsers.intValue(row, "dormant_customer_count"))
                .smsConsentCount(BigQueryRowParsers.intValue(row, "sms_consent_count"))
                .kakaoConsentCount(BigQueryRowParsers.intValue(row, "kakao_consent_count"))
                .emailConsentCount(BigQueryRowParsers.intValue(row, "email_consent_count"))
                .joinedCustomerCount(BigQueryRowParsers.intValue(row, "joined_customer_count"))
                .build();
    }

    private ClickStatVO clickStat(com.google.cloud.bigquery.FieldValueList row) {
        return ClickStatVO.builder()
                .id(BigQueryRowParsers.longValue(row, "id"))
                .statDate(BigQueryRowParsers.date(row, "stat_date"))
                .channelId(BigQueryRowParsers.longValue(row, "channel_id"))
                .hour00(BigQueryRowParsers.intValue(row, "hour_00"))
                .hour01(BigQueryRowParsers.intValue(row, "hour_01"))
                .hour02(BigQueryRowParsers.intValue(row, "hour_02"))
                .hour03(BigQueryRowParsers.intValue(row, "hour_03"))
                .hour04(BigQueryRowParsers.intValue(row, "hour_04"))
                .hour05(BigQueryRowParsers.intValue(row, "hour_05"))
                .hour06(BigQueryRowParsers.intValue(row, "hour_06"))
                .hour07(BigQueryRowParsers.intValue(row, "hour_07"))
                .hour08(BigQueryRowParsers.intValue(row, "hour_08"))
                .hour09(BigQueryRowParsers.intValue(row, "hour_09"))
                .hour10(BigQueryRowParsers.intValue(row, "hour_10"))
                .hour11(BigQueryRowParsers.intValue(row, "hour_11"))
                .hour12(BigQueryRowParsers.intValue(row, "hour_12"))
                .hour13(BigQueryRowParsers.intValue(row, "hour_13"))
                .hour14(BigQueryRowParsers.intValue(row, "hour_14"))
                .hour15(BigQueryRowParsers.intValue(row, "hour_15"))
                .hour16(BigQueryRowParsers.intValue(row, "hour_16"))
                .hour17(BigQueryRowParsers.intValue(row, "hour_17"))
                .hour18(BigQueryRowParsers.intValue(row, "hour_18"))
                .hour19(BigQueryRowParsers.intValue(row, "hour_19"))
                .hour20(BigQueryRowParsers.intValue(row, "hour_20"))
                .hour21(BigQueryRowParsers.intValue(row, "hour_21"))
                .hour22(BigQueryRowParsers.intValue(row, "hour_22"))
                .hour23(BigQueryRowParsers.intValue(row, "hour_23"))
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int n(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal n(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private double n(Double value) {
        return value == null ? 0.0 : value;
    }

    private record DateFilter(
            String sql,
            Map<String, QueryParameterValue> parameters
    ) {
    }
}
