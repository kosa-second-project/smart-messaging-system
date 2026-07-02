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
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.HistoryMapper.findHistoryDetailById"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.HistoryMapper.findAttemptFlowsByHistoryId"
        )).isTrue();
        assertThat(configuration.hasStatement(
                "com.example.smartmessaging.mapper.HistoryMapper.findStatusOptions"
        )).isFalse();
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

    @Test
    void 태그_필터와_목록_태그는_tag_id_오름차순으로_조회한다() throws Exception {
        String resource = "mappers/HistoryMapper.xml";
        String mapperXml;

        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("ORDER BY id ASC",
                        "ORDER BY tag_rows.send_history_id, tag_rows.tag_id ASC");
    }

    @Test
    void 상세조회는_soft_delete를_제외하고_차수별_성공실패를_집계한다() throws Exception {
        String resource = "mappers/HistoryMapper.xml";
        String mapperXml;

        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            mapperXml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(mapperXml)
                .contains("NVL(sh.is_deleted, 0) = 0",
                        "NVL(st.is_deleted, 0) = 0",
                        "NVL(sa.is_deleted, 0) = 0",
                        "NVL(c.is_deleted, 0) = 0",
                        "COUNT(CASE WHEN sa.is_succeeded = 1 THEN 1 END)",
                        "COUNT(CASE WHEN sa.is_succeeded = 0 THEN 1 END)",
                        "ORDER BY sa.attempt_order ASC")
                .doesNotContain("fail_reason");
    }
}
