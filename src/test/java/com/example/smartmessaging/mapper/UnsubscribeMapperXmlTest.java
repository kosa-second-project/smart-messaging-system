package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UnsubscribeMapperXmlTest {

    private static final String RESOURCE = "mappers/UnsubscribeMapper.xml";

    @Test
    void 수신거부_매퍼XML을_파싱하고_중복방지_INSERT를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto");

        parseMapper(configuration, "mappers/common-mapper.xml");
        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.UnsubscribeMapper.insertRejectHistoryIfAbsent"
        )).isTrue();
    }

    @Test
    void 수신거부_이력은_NOT_EXISTS로_중복_삽입을_방지한다() throws Exception {
        String mapperXml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("INSERT INTO reject_history")
                .contains("WHERE NOT EXISTS")
                .doesNotContain("CREATE TABLE")
                .doesNotContain("ALTER TABLE");
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
}
