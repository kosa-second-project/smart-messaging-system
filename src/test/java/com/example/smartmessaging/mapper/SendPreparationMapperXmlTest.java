package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SendPreparationMapperXmlTest {

    private static final String RESOURCE = "mappers/SendPreparationMapper.xml";

    @Test
    void 발송준비_매퍼XML을_파싱하고_기존테이블_INSERT를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto");

        parseMapper(configuration, "mappers/common-mapper.xml");
        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.SendPreparationMapper.findRecipientCandidatesByCustomerIds"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.SendPreparationMapper.insertSendHistory"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.SendPreparationMapper.insertSendHistoryRouting"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.SendPreparationMapper.insertSendTarget"
        )).isTrue();
    }

    @Test
    void 새테이블을_만들지_않고_기존_발송이력_대상_라우팅_테이블만_사용한다() throws Exception {
        String mapperXml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("INSERT INTO send_history",
                        "#{templateId,jdbcType=NUMERIC}",
                        "INSERT INTO send_history_routing",
                        "INSERT INTO send_target",
                        "FROM customer_channel_consent")
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
