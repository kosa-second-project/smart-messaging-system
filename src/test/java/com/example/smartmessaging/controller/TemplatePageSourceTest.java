package com.example.smartmessaging.controller;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TemplatePageSourceTest {

    @Test
    void 상세_렌더링은_DB응답_필드로_메시지_미리보기를_구성한다() throws Exception {
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
                        "item.clickRate",
                        "item.conversionRate",
                        "templateDetailPreview");
        assertThat(detailFunction)
                .doesNotContain("010-0000-0000",
                        "Gmail");
    }

    @Test
    void 템플릿_상세_미리보기는_메시지작성_공통_컴포넌트를_사용한다() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/pages/templates.html"),
                StandardCharsets.UTF_8
        );
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );

        assertThat(template)
                .contains("messagePreviewComponentTemplate",
                        "fragments/components/message-preview :: preview");
        assertThat(script)
                .contains("createCommonMessagePreview",
                        "templateFormPreview",
                        "templateDetailPreview",
                        "phonePreviewBox",
                        "mode-sms",
                        "mode-kakao",
                        "mode-email");
    }

    @Test
    void 템플릿_상세는_클릭률과_전환률을_표시한다() throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );

        assertThat(script)
                .contains("클릭률",
                        "전환률",
                        "formatPercent(item.clickRate)",
                        "formatPercent(item.conversionRate)");
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
    void 새_템플릿_모달은_AI_검사완료상태만_저장요청에_반영한다() throws Exception {
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

        assertThat(template).contains("templateReviewButton", "templateSaveButton");
        assertThat(template).contains("AI 문구 추천", "template-ai-open-button", "template-review-action-button");
        assertThat(template).doesNotContain("AI로 작성");
        assertThat(template).doesNotContain("templateTagCheckboxes", "고객 태그");
        assertThat(script).contains("getCategoryLabel", "invalidateTemplateReview");
        assertThat(saveFunction).contains("templateReviewSignature");
        assertThat(saveFunction).doesNotContain("tagIds", "templateTag", "templateKakaoStatus", "kakaoTemplateCode");
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

    @Test
    void AI_추천_적용_후_사용자가_문구를_수정하면_AI_생성_상태를_해제한다() throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );
        String inputListener = script.substring(
                script.indexOf("[\"templateTitle\", \"templateContent\"].forEach"),
                script.indexOf("document.getElementById(\"templateCategory\")")
        );
        String applySuggestion = script.substring(
                script.indexOf("function applyTemplateSuggestion"),
                script.indexOf("function reviewTemplate")
        );

        assertThat(inputListener).contains("templateIsAiGenerated = false");
        assertThat(applySuggestion).contains("templateIsAiGenerated = true");
    }

    @Test
    void AI_추천_요청은_고객명_변수만_허용한다() throws Exception {
        String script = Files.readString(
                Path.of("src/main/resources/static/js/pages/templates.js"),
                StandardCharsets.UTF_8
        );

        assertThat(script)
                .contains("const TEMPLATE_AVAILABLE_VARIABLES = [\"#{고객명}\"]")
                .doesNotContain("const TEMPLATE_AVAILABLE_VARIABLES = [\"#{고객명}\", \"#{주문번호}\", \"#{쿠폰명}\"]");
    }
}
