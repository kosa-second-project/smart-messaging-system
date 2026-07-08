package com.example.smartmessaging.service.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StatServiceImplSourceTest {

    @Test
    void 차수별_통계는_반복_스트림_헬퍼_대신_사전집계_요약을_사용한다() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/example/smartmessaging/service/impl/StatServiceImpl.java"
        ));

        assertThat(source)
                .contains("private DegreeStatsSummary summarizeDegreeStats")
                .contains("private record DateChannelKey")
                .contains("private record DegreeChannelKey")
                .doesNotContain("sumSendByDateAndChannel(List<MessageStatByDegreeVO>")
                .doesNotContain("successRateByChannel(List<MessageStatByDegreeVO>")
                .doesNotContain("successRateByDegreeAndChannel(List<MessageStatByDegreeVO>");
    }

    @Test
    void fallback_성공률_차트는_낮은_성공률도_보이도록_축을_0부터_사용한다() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/resources/static/js/pages/stats/delivery.js"
        ));

        assertThat(source)
                .contains("yMin: 0")
                .doesNotContain("yMin: 90");
    }
}
