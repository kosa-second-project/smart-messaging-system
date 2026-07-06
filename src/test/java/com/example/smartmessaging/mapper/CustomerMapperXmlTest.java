package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperXmlTest {

    private static final String RESOURCE = "mappers/CustomerMapper.xml";

    @Test
    void 고객매퍼XML을_파싱하고_테스트고객_관련_쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto");

        parseMapper(configuration, "mappers/common-mapper.xml");
        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement("com.example.smartmessaging.mapper.CustomerMapper.findByPhone")).isTrue();
        assertThat(configuration.hasStatement("com.example.smartmessaging.mapper.CustomerMapper.insertTestCustomer")).isTrue();
        assertThat(configuration.hasStatement("com.example.smartmessaging.mapper.CustomerMapper.updateTestCustomer")).isTrue();
        assertThat(configuration.hasStatement("com.example.smartmessaging.mapper.CustomerMapper.upsertChannelConsent")).isTrue();
    }

    @Test
    void 테스트고객_동의정보는_기존_고객_동의_테이블에_MERGE한다() throws Exception {
        String mapperXml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("MERGE INTO customer_channel_consent",
                        "WHEN MATCHED THEN",
                        "WHEN NOT MATCHED THEN",
                        "INSERT INTO customer",
                        "birth_date",
                        "#{birthDate,jdbcType=DATE}")
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
