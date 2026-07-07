package com.example.smartmessaging.dw;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.LoadJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BigQueryBackfillRunner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<TableCopySpec> TABLES = List.of(
            spec("channel", "SELECT id, channel_type, cost_per_msg, max_length, is_active, is_deleted, created_at, updated_at FROM channel"),
            spec("message_stat", "SELECT id, total_send_count, total_success_count, billing_cost, max_cost, stat_date, is_deleted, created_at, updated_at FROM message_stat"),
            spec("message_stat_by_degree", "SELECT id, degree, send_count, success_count, stat_date, cost, channel_id, is_deleted, created_at, updated_at FROM message_stat_by_degree"),
            spec("channel_stat", "SELECT id, click_target_count, click_count, conversion_target_count, conversion_count, consent_target_count, consent_count, channel_id, is_deleted, created_at, updated_at FROM channel_stat"),
            spec("customer_stat", "SELECT id, stat_date, total_customer_count, normal_customer_count, new_customer_count, dormant_customer_count, sms_consent_count, kakao_consent_count, email_consent_count, joined_customer_count, is_deleted, created_at, updated_at FROM customer_stat"),
            spec("customer_channel_consent", "SELECT id, customer_id, channel_id, is_consented, is_deleted, created_at, updated_at FROM customer_channel_consent"),
            spec("click_stat", "SELECT id, stat_date, channel_id, hour_00, hour_01, hour_02, hour_03, hour_04, hour_05, hour_06, hour_07, hour_08, hour_09, hour_10, hour_11, hour_12, hour_13, hour_14, hour_15, hour_16, hour_17, hour_18, hour_19, hour_20, hour_21, hour_22, hour_23, is_deleted, created_at, updated_at FROM click_stat"),
            spec("template", "SELECT id, title, is_ai_generated, is_deleted, created_at, updated_at FROM template"),
            spec("template_stat", "SELECT id, template_id, stat_date, click_target_count, click_count, conversion_target_count, conversion_count, is_deleted, created_at, updated_at FROM template_stat")
    );

    public static void main(String[] args) throws Exception {
        String projectId = requiredEnv("BIGQUERY_PROJECT_ID");
        String dataset = requiredEnv("BIGQUERY_DATASET");
        OracleConfig oracleConfig = oracleConfig();
        BigQuery bigQuery = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .build()
                .getService();

        try (Connection connection = DriverManager.getConnection(
                oracleConfig.url(),
                oracleConfig.username(),
                oracleConfig.password()
        )) {
            for (TableCopySpec table : TABLES) {
                copyTable(connection, bigQuery, projectId, dataset, table);
            }
        }
    }

    private static void copyTable(
            Connection connection,
            BigQuery bigQuery,
            String projectId,
            String dataset,
            TableCopySpec table
    ) throws Exception {
        TableId tableId = TableId.of(projectId, dataset, table.name());
        long copied = 0;
        JobId jobId = JobId.of(projectId, "oracle-backfill-" + table.name() + "-" + UUID.randomUUID());
        WriteChannelConfiguration configuration = WriteChannelConfiguration.newBuilder(tableId)
                .setFormatOptions(FormatOptions.json())
                .setWriteDisposition(JobInfo.WriteDisposition.WRITE_TRUNCATE)
                .build();
        TableDataWriteChannel writer = bigQuery.writer(jobId, configuration);
        try (writer;
             OutputStream outputStream = Channels.newOutputStream(writer);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(table.sql())) {
            while (resultSet.next()) {
                outputStream.write(OBJECT_MAPPER.writeValueAsBytes(row(resultSet)));
                outputStream.write('\n');
                copied++;
            }
        }

        Job job = writer.getJob().waitFor();
        if (job == null || job.getStatus().getError() != null) {
            throw new IllegalStateException("BigQuery load failed for " + tableId + ": "
                    + (job == null ? "job not found" : job.getStatus().getError())
                    + ", executionErrors="
                    + (job == null ? List.of() : job.getStatus().getExecutionErrors()));
        }
        System.out.printf("Copied %d rows: %s%n", copied, table.name());
    }

    private static Map<String, Object> row(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            String columnName = metadata.getColumnLabel(index).toLowerCase(Locale.ROOT);
            Object value = columnValue(resultSet, metadata.getColumnType(index), columnName, index);
            row.put(columnName, toBigQueryValue(value));
        }
        return row;
    }

    private static Object columnValue(ResultSet resultSet, int columnType, String columnName, int index) throws SQLException {
        if (columnName.endsWith("_date")) {
            return resultSet.getDate(index);
        }
        return switch (columnType) {
            case Types.DATE -> resultSet.getDate(index);
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE + 1 -> resultSet.getTimestamp(index);
            default -> resultSet.getObject(index);
        };
    }

    private static Object toBigQueryValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return value;
    }

    private static OracleConfig oracleConfig() throws IOException {
        Map<String, Object> root;
        try (InputStream inputStream = Files.newInputStream(Path.of("src/main/resources/application-local.yml"))) {
            root = new Yaml().load(inputStream);
        }
        Map<?, ?> spring = map(root.get("spring"));
        Map<?, ?> datasource = map(spring.get("datasource"));
        return new OracleConfig(
                string(datasource.get("url")),
                string(datasource.get("username")),
                string(datasource.get("password"))
        );
    }

    private static Map<?, ?> map(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        throw new IllegalStateException("Expected YAML map but got " + value);
    }

    private static String string(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required.");
        }
        return value;
    }

    private static TableCopySpec spec(String name, String sql) {
        return new TableCopySpec(name, sql);
    }

    private record TableCopySpec(String name, String sql) {
    }

    private record OracleConfig(String url, String username, String password) {
    }
}
