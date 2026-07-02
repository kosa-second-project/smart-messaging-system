package com.example.smartmessaging.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryMapperXmlTest {

    @Test
    void 발송기록_매퍼XML을_파싱하고_필수쿼리를_등록한다() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mappers/HistoryMapper.xml";

        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(
                    inputStream,
                    configuration,
                    resource,
                    configuration.getSqlFragments()
            );
            mapperParser.parse();
        }

        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.HistoryMapper.findHistories"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.HistoryMapper.countHistories"
        )).isTrue();
    }

    @Test
    void 통합검색은_제목_내용_태그에_이스케이프된_LIKE_패턴을_사용한다() throws Exception {
        String resource = "mappers/HistoryMapper.xml";
        String mapperXml;

        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("LOWER(sh.title) LIKE LOWER(#{keywordLikePattern}) ESCAPE '!'",
                        "LOWER(sh.content) LIKE LOWER(#{keywordLikePattern}) ESCAPE '!'",
                        "LOWER(t.name) LIKE LOWER(#{keywordLikePattern}) ESCAPE '!'")
                .doesNotContain("LOWER(#{keyword})");
    }
}
