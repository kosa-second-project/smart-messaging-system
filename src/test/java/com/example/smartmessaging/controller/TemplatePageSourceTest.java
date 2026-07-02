package com.example.smartmessaging.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatePageSourceTest {

    @Test
    void 상세_렌더링은_DB응답_필드를_보여주고_목_미리보기를_사용하지_않는다() throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );
        String detailFunction = script.substring(
                script.indexOf("function renderTemplateDetail"),
                script.indexOf("function renderMetric")
        );

        assertThat(detailFunction)
                .contains("item.id",
                        "item.createdAt",
                        "item.updatedAt",
                        "item.content");
        assertThat(detailFunction)
                .doesNotContain("renderMessagePreview",
                        "010-0000-0000",
                        "Gmail");
    }

    @Test
    void 새_템플릿_모달은_미리보기_기본값을_메시지로_초기화한다() throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );
        String resetFunction = script.substring(
                script.indexOf("function resetTemplateForm"),
                script.indexOf("function saveTemplate")
        );

        assertThat(resetFunction)
                .contains("templatePreviewMode = \"message\"",
                        "button.dataset.previewMode === \"message\"");
    }
}
