package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

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
                "com.example.smartmessaging.service.repository.TemplateMapper.selectTemplateDetail"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.TemplateMapper.selectCategoryOptions"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.TemplateMapper.selectTagOptions"
        )).isFalse();
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
