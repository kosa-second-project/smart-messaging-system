package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateMapperXmlTest {

    private static final String RESOURCE = "mappers/TemplateMapper.xml";

    @Test
    void 템플릿_매퍼XML을_파싱하고_필수쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAliases("com.example.smartmessaging.dto");

        parseMapper(configuration, "mappers/common-mapper.xml");
        parseMapper(configuration, RESOURCE);

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.TemplateMapper.selectTemplateDetail"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.TemplateMapper.selectTagOptions"
        )).isTrue();
    }

    @Test
    void 태그_옵션은_tag_테이블에서_삭제되지_않은_태그를_id순으로_조회한다() throws Exception {
        String mapperXml;

        try (InputStream inputStream = Resources.getResourceAsStream(RESOURCE)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("<select id=\"selectTagOptions\"",
                        "FROM tag",
                        "WHERE NVL(is_deleted, 0) = 0",
                        "ORDER BY id ASC");
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
