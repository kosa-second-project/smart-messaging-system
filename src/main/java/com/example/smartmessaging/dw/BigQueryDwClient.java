package com.example.smartmessaging.dw;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "dw.bigquery", name = "enabled", havingValue = "true")
public class BigQueryDwClient {

    private final BigQuery bigQuery;
    private final BigQueryDwProperties properties;

    public <T> List<T> query(
            String sql,
            Map<String, QueryParameterValue> parameters,
            Function<FieldValueList, T> parser
    ) {
        try {
            QueryJobConfiguration.Builder builder = QueryJobConfiguration.newBuilder(sql)
                    .setUseLegacySql(false)
                    .setUseQueryCache(properties.isUseQueryCache());
            if (properties.getMaximumBytesBilled() != null) {
                builder.setMaximumBytesBilled(properties.getMaximumBytesBilled());
            }
            parameters.forEach(builder::addNamedParameter);

            TableResult result = bigQuery.query(builder.build());
            List<T> rows = new ArrayList<>();
            for (FieldValueList row : result.iterateAll()) {
                rows.add(parser.apply(row));
            }
            return List.copyOf(rows);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BigQueryDwException("BigQuery query was interrupted.", exception);
        } catch (BigQueryException | IllegalArgumentException exception) {
            throw new BigQueryDwException("BigQuery query failed.", exception);
        }
    }
}
