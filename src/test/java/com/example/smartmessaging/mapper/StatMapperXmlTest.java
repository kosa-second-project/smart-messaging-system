package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class StatMapperXmlTest {

    private static final String RESOURCE = "mappers/StatMapper.xml";

    @Test
    void 통계_매퍼XML을_파싱하고_필수쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto.request");
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto.vo");

        parseMapper(configuration, "mappers/common-mapper.xml");
        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.StatMapper.selectCustomerChannelConsents"
        )).isTrue();
    }

    @Test
    void 고객_채널동의_조회는_원본목록이_아니라_채널별_집계를_반환한다() throws Exception {
        String mapperXml = readMapperXml();

        assertThat(mapperXml)
                .contains(
                        "COUNT(*) AS total_count",
                        "SUM(CASE WHEN NVL(ccc.is_consented, 0) = 1 THEN 1 ELSE 0 END) AS consented_count",
                        "GROUP BY ccc.channel_id"
                )
                .doesNotContain(
                        "ccc.customer_id,",
                        "ORDER BY ccc.channel_id, ccc.customer_id"
                );
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
