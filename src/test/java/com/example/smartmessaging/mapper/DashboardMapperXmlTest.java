package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardMapperXmlTest {

    private static final String RESOURCE = "mappers/DashboardMapper.xml";

    @Test
    void 대시보드_매퍼XML을_파싱하고_요약쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto.request");
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto.response");
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto.vo");

        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.DashboardMapper.selectMessageSummary"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.DashboardMapper.selectMessageTrend"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.DashboardMapper.selectChannelSendSummary"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.DashboardMapper.selectLatestCustomerStat"
        )).isTrue();
    }

    @Test
    void 대시보드는_전체통계목록_대신_집계와_제한조회만_사용한다() throws Exception {
        String mapperXml = readMapperXml();

        assertThat(mapperXml)
                .contains(
                        "<select id=\"selectMessageSummary\"",
                        "SUM(COALESCE(ms.total_send_count, 0)) AS total_send_count",
                        "<select id=\"selectMessageTrend\"",
                        "FETCH FIRST 7 ROWS ONLY",
                        "<select id=\"selectChannelSendSummary\"",
                        "GROUP BY msbd.channel_id",
                        "<select id=\"selectLatestCustomerStat\"",
                        "FETCH FIRST 1 ROW ONLY",
                        "ORDER BY sh.completed_at DESC NULLS LAST",
                        "FETCH FIRST 5 ROWS ONLY"
                )
                .doesNotContain("ORDER BY COALESCE(sh.completed_at, sh.scheduled_at, sh.created_at) DESC");
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
}
