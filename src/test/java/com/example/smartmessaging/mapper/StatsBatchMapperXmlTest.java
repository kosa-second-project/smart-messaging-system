package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StatsBatchMapperXmlTest {

    private static final String RESOURCE = "mappers/StatsBatchMapper.xml";

    @Test
    void 통계배치_매퍼XML을_파싱하고_필수쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();

        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.StatsBatchMapper.insertMessageStat"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.StatsBatchMapper.insertMessageStatByDegree"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.StatsBatchMapper.insertCustomerStat"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.StatsBatchMapper.insertClickStat"
        )).isTrue();
    }

    @Test
    void 날짜_조건은_컬럼_TRUNC_대신_범위_비교를_사용한다() throws Exception {
        String mapperXml = readMapperXml();

        assertThat(mapperXml)
                .doesNotContain("TRUNC(sh.completed_at)")
                .doesNotContain("TRUNC(su.clicked_at)")
                .doesNotContain("TRUNC(c.joined_at)")
                .contains(
                        "sh.completed_at &gt;= CAST(#{statDate} AS TIMESTAMP)",
                        "sh.completed_at &lt;  CAST(#{statDate} AS TIMESTAMP) + INTERVAL '1' DAY",
                        "su.clicked_at &gt;= CAST(#{statDate} AS TIMESTAMP)",
                        "su.clicked_at &lt;  CAST(#{statDate} AS TIMESTAMP) + INTERVAL '1' DAY",
                        "c.joined_at &lt; CAST(#{statDate} AS TIMESTAMP) + INTERVAL '1' DAY",
                        "c.joined_at &gt;= CAST(#{statDate} AS TIMESTAMP)"
                );
    }

    @Test
    void 고객통계_배치는_고객과_동의테이블을_각각_한번씩_집계한다() throws Exception {
        String mapperXml = readMapperXml();

        assertThat(mapperXml)
                .contains(
                        "WITH customer_counts AS",
                        "consent_counts AS",
                        "WHEN c.joined_at &lt; CAST(#{statDate} AS TIMESTAMP) - INTERVAL '7' DAY",
                        "COUNT(DISTINCT CASE WHEN ch.channel_type = 'SMS' THEN ccc.customer_id END) AS sms_consent_count",
                        "CROSS JOIN consent_counts consent_counts"
                );
        assertThat(countOccurrences(mapperXml, "FROM customer c")).isLessThanOrEqualTo(2);
        assertThat(countOccurrences(mapperXml, "FROM customer_channel_consent ccc")).isLessThanOrEqualTo(2);
    }

    private void parseMapper(Configuration configuration, String resource) throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            mapperParser.parse();
        }
    }

    private String readMapperXml() throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) >= 0) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}

