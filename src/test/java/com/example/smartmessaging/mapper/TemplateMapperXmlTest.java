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
                "com.example.smartmessaging.service.repository.TemplateMapper.selectTemplateDetail"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.TemplateMapper.selectCategoryOptions"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.service.repository.TemplateMapper.selectTagOptions"
        )).isFalse();
    }

    @Test
    void default_sort_orders_by_updated_at_then_created_at() throws Exception {
        String mapper = new String(
                Resources.getResourceAsStream(RESOURCE).readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(mapper)
                .contains("t.updated_at DESC NULLS LAST, t.created_at DESC NULLS LAST, t.id DESC");
    }

    @Test
    void detail_query_calculates_click_and_conversion_rates() throws Exception {
        String mapper = new String(
                Resources.getResourceAsStream(RESOURCE).readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(mapper)
                .contains("AS click_rate",
                        "AS conversion_rate",
                        "SUM(NVL(click_count, 0)) / SUM(NVL(click_target_count, 0))",
                        "SUM(NVL(conversion_count, 0)) / SUM(NVL(conversion_target_count, 0))");
    }

    @Test
    void template_read_queries_are_not_limited_to_current_user() throws Exception {
        String mapper = new String(
                Resources.getResourceAsStream(RESOURCE).readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(mapper)
                .doesNotContain("AND t.user_id = #{userId}")
                .doesNotContain("AND user_id = #{userId}");
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
