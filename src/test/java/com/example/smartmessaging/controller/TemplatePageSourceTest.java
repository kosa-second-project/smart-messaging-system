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
                        "item.content",
                        "renderMessagePreview(item.title, item.content");
        assertThat(detailFunction)
                .doesNotContain("010-0000-0000",
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
                        "button.dataset.previewMode === \"message\"",
                        "document.getElementById(\"templatePurpose\").value = \"AD\"");
    }

    @Test
    void 새_템플릿_모달은_태그와_카카오값을_전송하지_않는다() throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );
        String template = Files.readString(
                Path.of("src/main/resources/templates/pages/templates.html"),
                StandardCharsets.UTF_8
        );
        String saveFunction = script.substring(
                script.indexOf("function saveTemplate"),
                script.indexOf("function renderFormPreview")
        );

        assertThat(template).doesNotContain("태그", "templateTagList", "카카오 상태", "templateKakaoStatus", "kakaoTemplateCode");
        assertThat(script).contains("getCategoryLabel");
        assertThat(saveFunction).doesNotContain("tagIds", "templateTag", "kakaoTemplateStatus", "kakaoTemplateCode");
    }

    @Test
    void 채널_선택은_한_행으로_표시한다() throws Exception {
        String style = Files.readString(
                Path.of("src/main/resources/static/css/pages/templates.css"),
                StandardCharsets.UTF_8
        );
        String channelGridStyle = style.substring(
                style.indexOf(".template-channel-grid"),
                style.indexOf(".template-channel-option")
        );

        assertThat(channelGridStyle)
                .contains("display: flex",
                        "flex-wrap: wrap");
    }
}
