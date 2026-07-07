package com.example.smartmessaging.dw;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class BigQueryDwConfig {

    private final BigQueryDwProperties properties;

    @Bean
    @ConditionalOnProperty(prefix = "dw.bigquery", name = "enabled", havingValue = "true")
    public BigQuery bigQuery() {
        BigQueryOptions.Builder builder = BigQueryOptions.newBuilder();
        if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
            builder.setProjectId(properties.getProjectId());
        }
        return builder.build().getService();
    }
}
