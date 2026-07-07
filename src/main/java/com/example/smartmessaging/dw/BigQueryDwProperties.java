package com.example.smartmessaging.dw;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dw.bigquery")
public class BigQueryDwProperties {

    private boolean enabled = false;
    private String projectId = "";
    private String dataset = "";
    private boolean useQueryCache = true;
    private Long maximumBytesBilled;
    private Tables tables = new Tables();

    public boolean isConfigured() {
        return hasText(projectId) && hasText(dataset);
    }

    public String table(String tableName) {
        if (!isConfigured()) {
            throw new IllegalStateException("BigQuery projectId and dataset must be configured.");
        }
        return "`%s.%s.%s`".formatted(projectId, dataset, tableName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @Getter
    @Setter
    public static class Tables {
        private String channel = "channel";
        private String messageStat = "message_stat";
        private String messageStatByDegree = "message_stat_by_degree";
        private String channelStat = "channel_stat";
        private String customerStat = "customer_stat";
        private String customerChannelConsent = "customer_channel_consent";
        private String clickStat = "click_stat";
        private String template = "template";
        private String templateStat = "template_stat";
        private String aiMarketingInsight = "ai_marketing_insight";

        public List<String> all() {
            return List.of(
                    channel,
                    messageStat,
                    messageStatByDegree,
                    channelStat,
                    customerStat,
                    customerChannelConsent,
                    clickStat,
                    template,
                    templateStat,
                    aiMarketingInsight
            );
        }
    }
}
