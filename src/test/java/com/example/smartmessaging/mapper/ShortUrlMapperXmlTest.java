package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlMapperXmlTest {

    private static final String RESOURCE = "mappers/ShortUrlMapper.xml";

    @Test
    void 단축URL_매퍼XML을_파싱하고_클릭추적_쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto");

        parseMapper(configuration, "mappers/common-mapper.xml");
        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement("com.example.smartmessaging.service.repository.ShortUrlMapper.findClickTargetById")).isTrue();
        assertThat(configuration.hasStatement("com.example.smartmessaging.service.repository.ShortUrlMapper.markFirstClicked")).isTrue();
        assertThat(configuration.hasStatement("com.example.smartmessaging.service.repository.ShortUrlMapper.incrementChannelClickCount")).isTrue();
        assertThat(configuration.hasStatement("com.example.smartmessaging.service.repository.ShortUrlMapper.incrementHourlyClickCount")).isTrue();
    }

    @Test
    void 클릭추적은_sendTarget과_채널통계_테이블을_사용한다() throws Exception {
        String mapperXml;
        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("JOIN send_target st",
                        "st.user_uuid",
                        "st.final_channel_id AS channel_id",
                        "MERGE INTO channel_stat",
                        "MERGE INTO click_stat")
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
