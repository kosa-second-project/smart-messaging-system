let templateCurrentPage = 1;
let templatePageSize = 10;
let templatePreviewMode = "message";
let templateIsAiGenerated = false;
let templateReviewSignature = null;
let templateReviewStatus = null;
let templateGenerating = false;
let templateReviewing = false;
let templateLastValidFields = {
    title: "",
    content: ""
};
let templateLastValidChannelIds = [];
let templateOptions = {
    channels: [],
    categories: [],
    purposes: []
};

const TEMPLATE_AVAILABLE_VARIABLES = ["#{고객명}"];
const TEMPLATE_SMS_MAX_BYTES = 90;
const TEMPLATE_EXTENDED_MAX_BYTES = 1000;

document.addEventListener("DOMContentLoaded", function() {
    bindTemplateEvents();
    fetchTemplateOptions();
    fetchTemplates();
    renderFormPreview();
});

function bindTemplateEvents() {
    document.getElementById("templateKeywordInput").addEventListener("input", function() {
        templateCurrentPage = 1;
        fetchTemplates();
    });

    document.getElementById("templateSortSelect").addEventListener("change", function() {
        templateCurrentPage = 1;
        fetchTemplates();
    });

    document.querySelector(".template-toolbar").addEventListener("multiselect:change", function() {
        templateCurrentPage = 1;
        fetchTemplates();
    });

    ["templateTitle", "templateContent"].forEach(id => {
        document.getElementById(id).addEventListener("input", function() {
            templateIsAiGenerated = false;
            renderTemplateByteCount();
            renderFormPreview();
            if (!enforceTemplateLength()) {
                return;
            }
            invalidateTemplateReview();
        });
    });

    document.getElementById("templateCategory").addEventListener("change", invalidateTemplateReview);
    document.getElementById("templateChannelCheckboxes").addEventListener("change", function() {
        renderTemplateByteCount();
        if (!enforceTemplateLength()) {
            restoreTemplateChannelSelection();
            renderTemplateByteCount();
            renderFormPreview();
            return;
        }
        syncTemplateChannelSelection();
        invalidateTemplateReview();
    });

    document.querySelectorAll("[data-preview-mode]").forEach(button => {
        button.addEventListener("click", function() {
            templatePreviewMode = button.dataset.previewMode;
            document.querySelectorAll("[data-preview-mode]").forEach(item => {
                item.classList.toggle("is-active", item.dataset.previewMode === templatePreviewMode);
            });
            renderFormPreview();
        });
    });

    document.querySelectorAll("[data-purpose-value]").forEach(button => {
        button.addEventListener("click", function() {
            document.getElementById("templatePurpose").value = button.dataset.purposeValue;
            document.querySelectorAll("[data-purpose-value]").forEach(item => {
                item.classList.toggle("is-active", item.dataset.purposeValue === button.dataset.purposeValue);
            });
            invalidateTemplateReview();
        });
    });

    document.getElementById("templateAiPanelButton").addEventListener("click", openTemplateAiPanel);
    document.getElementById("templateAiPanelClose").addEventListener("click", closeTemplateAiPanel);
    document.getElementById("templateAiGenerateButton").addEventListener("click", generateTemplateSuggestions);
    document.getElementById("templateReviewButton").addEventListener("click", reviewTemplate);
    document.getElementById("templateReviewAcknowledge").addEventListener("change", updateTemplateSaveState);

    document.getElementById("templateForm").addEventListener("submit", function(event) {
        event.preventDefault();
        saveTemplate();
    });
}

function fetchTemplateOptions() {
    fetch("/api/templates/options")
        .then(res => res.json())
        .then(data => {
            templateOptions = data || templateOptions;
            renderTemplateFilters();
            renderTemplateChannelCheckboxes();
        })
        .catch(err => console.error("Template options load fail:", err));
}

function renderTemplateFilters() {
    const categoryOptions = (templateOptions.categories || []).map(option => ({
        value: option.value,
        label: getCategoryLabel(option.value || option.label)
    }));
    renderMultiSelectOptions("templateCategoryFilter", categoryOptions, "categories");
    renderSelectOptions("templateCategory", categoryOptions, "카테고리 선택");

    const purposeOptions = buildPurposeOptions(templateOptions.purposes || []);
    renderMultiSelectOptions("templatePurposeFilter", purposeOptions, "purposes");

    const channelOptions = (templateOptions.channels || []).map(channel => ({
        value: channel.channelType,
        label: channel.channelType
    }));
    renderMultiSelectOptions("templateChannelFilter", channelOptions, "channelTypes");
}

function renderMultiSelectOptions(rootId, options, inputName) {
    const root = document.getElementById(rootId);
    const menu = root.querySelector("[data-multiselect-menu]");
    const selected = new Set(getMultiSelectValues(rootId));

    menu.innerHTML = options.map(option => `
        <label class="ds-multiselect__option">
            <input type="checkbox" name="${inputName}" value="${escapeHtml(option.value)}" data-label="${escapeHtml(option.label)}" ${selected.has(String(option.value)) ? "checked" : ""}>
            <span>${escapeHtml(option.label)}</span>
        </label>
    `).join("");

    if (window.DsMultiselect) {
        window.DsMultiselect.initAll(root.parentElement);
        window.DsMultiselect.refresh(root);
    }
}

function renderSelectOptions(selectId, options, firstLabel) {
    const select = document.getElementById(selectId);
    const currentValue = select.value;
    select.innerHTML = `<option value="">${firstLabel}</option>`;

    options.forEach(option => {
        const opt = document.createElement("option");
        opt.value = option.value;
        opt.innerText = option.label;
        select.appendChild(opt);
    });

    select.value = currentValue;
}

function renderTemplateChannelCheckboxes() {
    const wrapper = document.getElementById("templateChannelCheckboxes");
    wrapper.innerHTML = "";

    (templateOptions.channels || []).forEach(channel => {
        const label = document.createElement("label");
        label.className = "template-channel-option";
        label.innerHTML = `
            <input type="checkbox" name="templateChannel" value="${channel.channelId}">
            <span>${escapeHtml(channel.channelType)}</span>
        `;
        wrapper.appendChild(label);
    });
    syncTemplateChannelSelection();
    renderTemplateByteCount();
}

function getKoreanByteLength(text) {
    if (!text) {
        return 0;
    }
    let byteCount = 0;
    for (let i = 0; i < text.length; i++) {
        byteCount += text.charCodeAt(i) <= 0x007F ? 1 : 2;
    }
    return byteCount;
}

function buildTemplateMetricText() {
    const title = (document.getElementById("templateTitle")?.value || "").trim();
    const content = document.getElementById("templateContent")?.value || "";
    if (!title) {
        return content;
    }
    return title + "\n" + content;
}

function normalizeTemplateChannelType(channelType) {
    const value = (channelType || "").trim().toUpperCase();
    if (value.startsWith("KAKAO")) {
        return "KAKAO";
    }
    return value;
}

function getTemplateLimitInfo() {
    const channels = getSelectedChannelTypes().map(normalizeTemplateChannelType);
    const hasSms = channels.includes("SMS");
    const limit = hasSms ? TEMPLATE_SMS_MAX_BYTES : TEMPLATE_EXTENDED_MAX_BYTES;
    const totalBytes = getKoreanByteLength(buildTemplateMetricText());
    return {
        totalBytes,
        limit,
        isOverLimit: totalBytes > limit
    };
}

function renderTemplateByteCount() {
    const counter = document.getElementById("templateContentCount");
    if (!counter) {
        return;
    }
    const info = getTemplateLimitInfo();
    counter.innerText = `${info.totalBytes} / 최대 ${info.limit}byte`;
    counter.classList.toggle("is-error", info.isOverLimit);
}

function validateTemplateLength() {
    const info = getTemplateLimitInfo();
    if (!info.isOverLimit) {
        return true;
    }
    alert(`템플릿은 제목과 내용을 포함해 ${info.limit}byte까지 입력할 수 있습니다. 현재 ${info.totalBytes}byte입니다.`);
    document.getElementById("templateContent").focus();
    return false;
}

function getCurrentTemplateFields() {
    return {
        title: document.getElementById("templateTitle")?.value || "",
        content: document.getElementById("templateContent")?.value || ""
    };
}

function syncTemplateFields() {
    templateLastValidFields = getCurrentTemplateFields();
}

function restoreTemplateFields() {
    document.getElementById("templateTitle").value = templateLastValidFields.title || "";
    document.getElementById("templateContent").value = templateLastValidFields.content || "";
    renderTemplateByteCount();
    renderFormPreview();
}

function getSelectedTemplateChannelIds() {
    return Array.from(document.querySelectorAll("input[name='templateChannel']:checked"))
        .map(input => String(input.value));
}

function syncTemplateChannelSelection() {
    templateLastValidChannelIds = getSelectedTemplateChannelIds();
}

function restoreTemplateChannelSelection() {
    const selected = new Set(templateLastValidChannelIds);
    document.querySelectorAll("input[name='templateChannel']").forEach(input => {
        input.checked = selected.has(String(input.value));
    });
}

function enforceTemplateLength() {
    const info = getTemplateLimitInfo();
    if (!info.isOverLimit) {
        syncTemplateFields();
        return true;
    }
    alert(`템플릿은 제목과 내용을 포함해 ${info.limit}byte까지 입력할 수 있습니다. 현재 ${info.totalBytes}byte입니다.`);
    restoreTemplateFields();
    return false;
}

function fetchTemplates() {
    const tbody = document.getElementById("templateTableBody");
    tbody.innerHTML = `<tr><td colspan="6" class="template-empty-cell">데이터를 불러오는 중입니다...</td></tr>`;
    document.getElementById("templateMobileList").innerHTML = `<div class="template-empty-cell">데이터를 불러오는 중입니다...</div>`;

    const params = new URLSearchParams({
        page: templateCurrentPage,
        size: templatePageSize,
        sortOrder: document.getElementById("templateSortSelect").value
    });

    appendParam(params, "keyword", document.getElementById("templateKeywordInput").value.trim());
    appendParams(params, "categories", getMultiSelectValues("templateCategoryFilter"));
    appendParams(params, "purposes", getMultiSelectValues("templatePurposeFilter"));
    appendParams(params, "channelTypes", getMultiSelectValues("templateChannelFilter"));

    fetch(`/api/templates?${params.toString()}`)
        .then(res => res.json())
        .then(data => {
            renderTemplateTable(data.list || []);
            renderTemplateMobileList(data.list || []);
            renderTemplatePagination(data);
        })
        .catch(err => {
            console.error("Template list load fail:", err);
            tbody.innerHTML = `<tr><td colspan="6" class="template-empty-cell">템플릿 목록을 불러오는 도중 오류가 발생했습니다.</td></tr>`;
            document.getElementById("templateMobileList").innerHTML = `<div class="template-empty-cell">템플릿 목록을 불러오는 도중 오류가 발생했습니다.</div>`;
        });
}

function appendParam(params, key, value) {
    if (value) {
        params.append(key, value);
    }
}

function appendParams(params, key, values) {
    values.forEach(value => appendParam(params, key, value));
}

function getMultiSelectValues(rootId) {
    return Array.from(document.querySelectorAll(`#${rootId} input[type='checkbox']:checked`))
        .map(input => input.value)
        .filter(Boolean);
}

function renderTemplateTable(list) {
    const tbody = document.getElementById("templateTableBody");
    tbody.innerHTML = "";

    if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="template-empty-cell">조건에 맞는 템플릿이 없습니다.</td></tr>`;
        return;
    }

    list.forEach(item => {
        const tr = document.createElement("tr");
        tr.onclick = function() {
            openTemplateDetailModal(item.id);
        };
        tr.innerHTML = `
            <td class="template-title-cell">
                <div class="template-title">${escapeHtml(item.title)}</div>
                <div class="template-snippet">${escapeHtml(flattenText(item.content))}</div>
            </td>
            <td class="template-col-lg">${renderBadge(getCategoryLabel(item.category), "default")}</td>
            <td>${renderPurposeBadge(item.purpose)}</td>
            <td>${renderChannelChips(item.channels || [])}</td>
            <td class="template-col-lg"><span class="template-title">${(item.cnt || 0).toLocaleString()}회</span></td>
            <td><span class="text-muted">${item.updatedAt || "-"}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

function renderTemplateMobileList(list) {
    const wrapper = document.getElementById("templateMobileList");

    if (list.length === 0) {
        wrapper.innerHTML = `<div class="template-empty-cell">조건에 맞는 템플릿이 없습니다.</div>`;
        return;
    }

    wrapper.innerHTML = list.map(item => `
        <button type="button" class="template-mobile-card" onclick="openTemplateDetailModal(${item.id})">
            <div class="template-mobile-card__top">
                <div class="template-title-cell">
                    <div class="template-title">${escapeHtml(item.title)}</div>
                    <div class="template-mobile-card__body">${escapeHtml(flattenText(item.content))}</div>
                </div>
                ${renderPurposeBadge(item.purpose)}
            </div>
            <div class="template-mobile-card__chips">
                ${renderChannelChips(item.channels || [])}
                ${renderBadge(getCategoryLabel(item.category), "default")}
            </div>
            <div class="template-mobile-card__footer">
                <span>${(item.cnt || 0).toLocaleString()}회 사용</span>
                <span>${item.updatedAt || "-"}</span>
            </div>
        </button>
    `).join("");
}

function renderChannelChips(channels) {
    if (!channels.length) {
        return renderBadge("미지정", "default");
    }

    return `<div class="template-channel-list">${channels.map(channel => renderBadge(channel.channelType, "blue")).join("")}</div>`;
}

function renderPurposeBadge(purpose) {
    const normalized = normalizePurpose(purpose);
    if (normalized === "AD") {
        return renderBadge("광고", "amber");
    }
    return renderBadge("정보성", "green");
}

function renderBadge(text, variant) {
    const className = variant === "blue" ? "ds-badge ds-badge--primary"
        : variant === "green" ? "ds-badge ds-badge--success"
        : variant === "amber" ? "ds-badge ds-badge--warning"
        : "ds-badge";
    return `<span class="${className}">${escapeHtml(text)}</span>`;
}

function renderTemplatePagination(pageData) {
    const total = pageData.totalCount || 0;
    const current = pageData.page || 1;
    const size = pageData.size || templatePageSize;

    window.DsPagination?.renderOffset("#templatePagination", {
        total,
        page: current,
        size,
        totalPages: pageData.totalPages || 0,
        onPageChange: function(page) {
            templateCurrentPage = page;
            fetchTemplates();
        },
        onPageSizeChange: function(nextSize) {
            templatePageSize = nextSize;
            templateCurrentPage = 1;
            fetchTemplates();
        }
    });
}

let currentDetailItem = null;
let currentDetailPreviewMode = null;

function openTemplateDetailModal(templateId) {
    currentDetailPreviewMode = null; // 초기화
    const body = document.getElementById("templateDetailBody");
    body.innerHTML = `<div class="template-empty-cell">상세 정보를 불러오는 중입니다...</div>`;
    document.getElementById("templateDetailModal").classList.add("is-open");

    fetch(`/api/templates/${templateId}`)
        .then(res => res.json())
        .then(data => {
            currentDetailItem = data;
            body.innerHTML = renderTemplateDetail(data);
            renderTemplateDetailPreview(data);
        })
        .catch(err => {
            console.error("Template detail load fail:", err);
            body.innerHTML = `<div class="template-empty-cell">상세 정보를 불러오는 도중 오류가 발생했습니다.</div>`;
        });
}

function renderTemplateDetail(item) {
    if (!currentDetailPreviewMode) {
        currentDetailPreviewMode = inferPreviewMode(item);
    }

    return `
        <div class="template-modal-layout template-modal-layout--detail template-detail">
            <section class="template-modal-main">
                <div class="template-detail__summary">
                    <div>
                        <div class="template-detail__title">${escapeHtml(item.title)}</div>
                        <div class="template-detail__meta">
                            <div class="template-detail__meta-item">
                                <span class="template-detail__label">채널</span>
                                <span class="template-detail__value">${renderChannelChips(item.channels || [])}</span>
                            </div>
                            <div class="template-detail__meta-item">
                                <span class="template-detail__label">카테고리</span>
                                <span class="template-detail__value">${escapeHtml(item.categoryDisplayName || getCategoryLabel(item.category))}</span>
                            </div>
                            <div class="template-detail__meta-item">
                                <span class="template-detail__label">광고여부</span>
                                ${renderPurposeBadge(item.purpose)}
                            </div>
                        </div>
                    </div>
                </div>

                <div class="template-detail__metrics">
                    ${renderMetric("템플릿 ID", item.id || "-")}
                    ${renderMetric("사용 횟수", `${(item.cnt || 0).toLocaleString()}회`)}
                    ${renderMetric("클릭률", formatPercent(item.clickRate))}
                    ${renderMetric("전환률", formatPercent(item.conversionRate))}
                    ${renderMetric("생성일", item.createdAt || "-")}
                    ${renderMetric("최근 수정", item.updatedAt || "-")}
                    ${renderMetric("광고여부", getPurposeLabel(item.purpose))}
                </div>

                <section class="template-detail__content">
                    <div class="template-detail__metric-label">메시지 내용</div>
                    <div class="template-message-box" style="white-space: pre-wrap; word-break: break-all;">${escapeHtml(item.content || "")}</div>
                </section>
            </section>

            <aside class="template-modal-aside template-detail-preview">
                <div class="template-panel-header">
                    <span>미리보기</span>
                    <div class="ds-segment">
                        <button type="button" class="ds-segment__item ${currentDetailPreviewMode === 'message' ? 'is-active' : ''}" onclick="switchDetailPreviewMode('message')">메시지</button>
                        <button type="button" class="ds-segment__item ${currentDetailPreviewMode === 'kakao' ? 'is-active' : ''}" onclick="switchDetailPreviewMode('kakao')">카카오톡</button>
                        <button type="button" class="ds-segment__item ${currentDetailPreviewMode === 'email' ? 'is-active' : ''}" onclick="switchDetailPreviewMode('email')">이메일</button>
                    </div>
                </div>
                <div id="templateDetailPreview"></div>
            </aside>
        </div>
    `;
}

function switchDetailPreviewMode(mode) {
    currentDetailPreviewMode = mode;
    
    // 세그먼트 버튼 활성화 클래스 토글
    const buttons = document.querySelectorAll(".template-detail-preview .ds-segment__item");
    buttons.forEach(btn => {
        const btnMode = btn.getAttribute("onclick").match(/'([^']+)'/)[1];
        btn.classList.toggle("is-active", btnMode === mode);
    });

    // 미리보기 화면 갱신
    const previewDiv = document.getElementById("templateDetailPreview");
    if (previewDiv && currentDetailItem) {
        renderTemplateDetailPreview(currentDetailItem);
    }
}

function renderTemplateDetailPreview(item) {
    const previewDiv = document.getElementById("templateDetailPreview");
    if (!previewDiv) {
        return;
    }

    const component = createCommonMessagePreview(item.title, item.content, currentDetailPreviewMode);
    previewDiv.innerHTML = "";
    if (component) {
        previewDiv.appendChild(component);
    }
}

function createCommonMessagePreview(title, content, mode) {
    const template = document.getElementById("messagePreviewComponentTemplate");
    if (!template) {
        return null;
    }

    const fragment = template.content.cloneNode(true);
    const preview = fragment.querySelector("#phonePreviewBox");
    if (!preview) {
        return null;
    }

    const normalizedMode = mode === "message" ? "sms" : mode;
    preview.classList.remove("mode-sms", "mode-kakao", "mode-email");
    preview.classList.add(`mode-${normalizedMode}`);

    setPreviewText(preview, "#previewSmsTitle", title || "메시지 제목");
    setPreviewText(preview, "#previewKakaoTitle", title || "메시지 제목");
    setPreviewText(preview, "#previewEmailTitle", title || "메시지 제목");
    setPreviewText(preview, "#previewSmsContent", content || "발송할 메시지 내용을 입력해주세요.");
    setPreviewText(preview, "#previewKakaoContent", content || "발송할 메시지 내용을 입력해주세요.");
    setPreviewText(preview, "#previewEmailContent", content || "발송할 메시지 내용을 입력해주세요.");
    setPreviewText(preview, "#previewSmsLinks", "");
    setPreviewText(preview, "#previewKakaoLinks", "");
    setPreviewText(preview, "#previewSmsUnsubscribe", "");
    setPreviewText(preview, "#previewKakaoUnsubscribe", "");

    const frame = document.createElement("div");
    frame.className = "preview-phone-frame";
    frame.appendChild(preview);
    return frame;
}

function setPreviewText(root, selector, text) {
    const target = root.querySelector(selector);
    if (target) {
        target.textContent = text;
    }
}

function renderMetric(label, value) {
    return `
        <div>
            <div class="template-detail__metric-label">${escapeHtml(label)}</div>
            <div class="template-detail__metric-value">${escapeHtml(value)}</div>
        </div>
    `;
}

function formatPercent(value) {
    const number = Number(value);
    if (!Number.isFinite(number)) {
        return "0%";
    }
    return `${number.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 1
    })}%`;
}

function openAddTemplateModal() {
    resetTemplateForm();
    document.getElementById("templateAddModal").classList.add("is-open");
    renderFormPreview();
}

function closeAddTemplateModal() {
    document.getElementById("templateAddModal").classList.remove("is-open");
    closeTemplateAiPanel();
    closeTemplateReviewModal();
}

function closeAddTemplateOnBackdrop(event) {
    if (event.target.id === "templateAddModal" || event.target.classList.contains("ds-modal__backdrop")) {
        closeAddTemplateModal();
    }
}

function closeTemplateDetailModal() {
    document.getElementById("templateDetailModal").classList.remove("is-open");
}

function closeTemplateDetailOnBackdrop(event) {
    if (event.target.id === "templateDetailModal" || event.target.classList.contains("ds-modal__backdrop")) {
        closeTemplateDetailModal();
    }
}

function closeTemplateAiOnBackdrop(event) {
    if (event.target.id === "templateAiModal" || event.target.classList.contains("ds-modal__backdrop")) {
        closeTemplateAiPanel();
    }
}

function closeTemplateReviewOnBackdrop(event) {
    if (event.target.id === "templateReviewModal" || event.target.classList.contains("ds-modal__backdrop")) {
        closeTemplateReviewModal();
    }
}

function resetTemplateForm() {
    document.getElementById("templateForm").reset();
    templateIsAiGenerated = false;
    templateReviewSignature = null;
    templateReviewStatus = null;
    templateGenerating = false;
    templateReviewing = false;
    document.getElementById("templatePurpose").value = "AD";
    document.querySelectorAll("[data-purpose-value]").forEach(button => {
        button.classList.toggle("is-active", button.dataset.purposeValue === "AD");
    });
    // 새 템플릿 작성 시 미리보기는 항상 메시지 탭에서 시작한다.
    templatePreviewMode = "message";
    document.querySelectorAll("[data-preview-mode]").forEach(button => {
        button.classList.toggle("is-active", button.dataset.previewMode === "message");
    });
    document.querySelectorAll("input[name='templateChannel']").forEach(input => input.checked = false);
    syncTemplateFields();
    syncTemplateChannelSelection();
    renderTemplateByteCount();
    document.getElementById("templateAiDirection").value = "";
    document.getElementById("templateAiMessage").innerText = "";
    document.getElementById("templateAiSuggestions").innerHTML = "";
    document.getElementById("templateReviewResult").innerHTML = "";
    document.getElementById("templateReviewPanel").hidden = true;
    document.getElementById("templateReviewAcknowledgeRow").hidden = true;
    document.getElementById("templateReviewAcknowledge").checked = false;
    document.getElementById("templateReviewButton").disabled = false;
    document.getElementById("templateReviewButton").innerText = "AI 검사";
    document.getElementById("templateAiGenerateButton").disabled = false;
    document.getElementById("templateAiGenerateButton").innerText = "추천 문구 생성";
    closeTemplateAiPanel();
    updateTemplateSaveState();
}

function saveTemplate() {
    if (!validateTemplateLength()) {
        return;
    }
    if (templateReviewSignature !== getTemplateReviewSignature()) {
        invalidateTemplateReview();
        alert("현재 내용으로 AI 검사를 먼저 완료해 주세요.");
        return;
    }
    if (templateReviewStatus !== "PASS" && !document.getElementById("templateReviewAcknowledge").checked) {
        alert("검사 결과 확인에 동의해 주세요.");
        return;
    }

    const body = {
        title: document.getElementById("templateTitle").value.trim(),
        content: document.getElementById("templateContent").value.trim(),
        isAiGenerated: templateIsAiGenerated,
        category: document.getElementById("templateCategory").value,
        purpose: document.getElementById("templatePurpose").value,
        channelIds: Array.from(document.querySelectorAll("input[name='templateChannel']:checked"))
            .map(input => Number(input.value))
    };

    fetch("/api/templates", withJsonBody("POST", body))
        .then(res => {
            if (!res.ok) throw new Error("save failed");
            closeAddTemplateModal();
            fetchTemplateOptions();
            fetchTemplates();
        })
        .catch(err => {
            console.error("Template save fail:", err);
            alert("템플릿 저장 중 오류가 발생했습니다.");
        });
}

function openTemplateAiPanel() {
    document.getElementById("templateAiModal").classList.add("is-open");
    document.getElementById("templateAiPanel").hidden = false;
    document.getElementById("templateAiDirection").focus();
}

function closeTemplateAiPanel() {
    document.getElementById("templateAiModal").classList.remove("is-open");
    document.getElementById("templateAiPanel").hidden = true;
}

function openTemplateReviewModal() {
    document.getElementById("templateReviewModal").classList.add("is-open");
    document.getElementById("templateReviewPanel").hidden = false;
}

function closeTemplateReviewModal() {
    document.getElementById("templateReviewModal").classList.remove("is-open");
}

function generateTemplateSuggestions() {
    if (templateGenerating) return;

    const message = document.getElementById("templateAiMessage");
    message.classList.remove("is-error");
    const direction = document.getElementById("templateAiDirection").value.trim();
    const missing = [];
    if (!getSelectedChannelTypes().length) missing.push("채널");
    if (!document.getElementById("templateCategory").value) missing.push("카테고리");
    if (!document.getElementById("templatePurpose").value) missing.push("광고 여부");
    if (!direction) missing.push("원하는 문구 방향");
    if (missing.length) {
        message.innerText = `${missing.join(", ")} 항목을 입력해 주세요.`;
        message.classList.add("is-error");
        return;
    }

    const body = buildAiRequestBase();
    body.direction = direction;
    const generationSignature = JSON.stringify(body);
    templateGenerating = true;
    setGeneratingState(true);
    message.innerText = "조건에 맞는 문구를 생성하고 있습니다...";
    document.getElementById("templateAiSuggestions").innerHTML = "";

    fetch("/api/ai/suggestions", withJsonBody("POST", body))
        .then(async res => {
            if (!res.ok) throw new Error(await readErrorMessage(res, "AI 문구 생성에 실패했습니다."));
            return res.json();
        })
        .then(data => {
            const currentRequest = buildAiRequestBase();
            currentRequest.direction = document.getElementById("templateAiDirection").value.trim();
            if (generationSignature !== JSON.stringify(currentRequest)) {
                throw new Error("생성 중 조건이 변경되었습니다. 다시 생성해 주세요.");
            }
            const suggestions = (data.suggestions || []).slice(0, 2);
            if (!suggestions.length) throw new Error("추천 가능한 문구가 없습니다. 입력 방향을 바꿔 다시 시도해 주세요.");
            message.innerText = `${suggestions.length}개의 문구를 생성했습니다.`;
            renderTemplateSuggestions(suggestions);
        })
        .catch(err => {
            console.error("AI suggestion fail:", err);
            message.innerText = err.message || "AI 문구 생성 중 오류가 발생했습니다.";
            message.classList.add("is-error");
        })
        .finally(() => {
            templateGenerating = false;
            setGeneratingState(false);
        });
}

function setGeneratingState(loading) {
    const button = document.getElementById("templateAiGenerateButton");
    button.disabled = loading;
    button.innerText = loading ? "생성 중..." : "추천 문구 생성";
}

function renderTemplateSuggestions(suggestions) {
    const wrapper = document.getElementById("templateAiSuggestions");
    wrapper.innerHTML = "";
    suggestions.forEach((suggestion, index) => {
        const card = document.createElement("article");
        card.className = "template-ai-suggestion";
        card.innerHTML = `
            <strong class="template-ai-suggestion__title">${index + 1}. ${escapeHtml(suggestion.title)}</strong>
            <p class="template-ai-suggestion__content">${escapeHtml(suggestion.content)}</p>
            <button type="button" class="ds-button ds-button--outline">이 문구 적용</button>
        `;
        card.querySelector("button").addEventListener("click", function() {
            applyTemplateSuggestion(suggestion);
        });
        wrapper.appendChild(card);
    });
}

function applyTemplateSuggestion(suggestion) {
    document.getElementById("templateTitle").value = suggestion.title || "";
    document.getElementById("templateContent").value = suggestion.content || "";
    renderTemplateByteCount();
    if (!enforceTemplateLength()) {
        return;
    }
    templateIsAiGenerated = true;
    invalidateTemplateReview();
    renderFormPreview();
    closeTemplateAiPanel();
}

function reviewTemplate() {
    if (templateReviewing || !validateTemplateForReview()) return;

    const requestSignature = getTemplateReviewSignature();
    templateReviewing = true;
    templateReviewSignature = null;
    templateReviewStatus = null;
    updateTemplateSaveState();
    const button = document.getElementById("templateReviewButton");
    button.disabled = true;
    button.innerText = "검사 중...";
    const panel = document.getElementById("templateReviewPanel");
    openTemplateReviewModal();
    panel.hidden = false;
    document.getElementById("templateReviewResult").innerHTML = `<p class="template-review-summary">문구를 검사하고 있습니다...</p>`;
    document.getElementById("templateReviewAcknowledgeRow").hidden = true;

    fetch("/api/ai/messages/review", withJsonBody("POST", buildAiReviewRequest()))
        .then(async res => {
            if (!res.ok) throw new Error(await readErrorMessage(res, "AI 검사에 실패했습니다."));
            return res.json();
        })
        .then(data => {
            if (requestSignature !== getTemplateReviewSignature()) {
                throw new Error("검사 중 입력 내용이 변경되었습니다. 다시 검사해 주세요.");
            }
            templateReviewSignature = requestSignature;
            templateReviewStatus = data.status || "NOTICE";
            renderTemplateReview(data);
        })
        .catch(err => {
            console.error("AI review fail:", err);
            templateReviewSignature = null;
            templateReviewStatus = null;
            document.getElementById("templateReviewResult").innerHTML = `
                <div class="template-review-header"><strong>검사를 완료하지 못했습니다.</strong></div>
                <p class="template-review-summary">${escapeHtml(err.message || "잠시 후 다시 시도해 주세요.")}</p>
            `;
        })
        .finally(() => {
            templateReviewing = false;
            button.disabled = false;
            button.innerText = "다시 검사";
            updateTemplateSaveState();
        });
}

function validateTemplateForReview() {
    const form = document.getElementById("templateForm");
    if (!form.reportValidity()) return false;
    if (!getSelectedChannelTypes().length) {
        alert("하나 이상의 채널을 선택해 주세요.");
        return false;
    }
    if (!validateTemplateLength()) {
        return false;
    }
    return true;
}

function buildAiRequestBase() {
    return {
        contextType: "TEMPLATE_CREATE",
        messageType: document.getElementById("templatePurpose").value,
        channels: getSelectedChannelTypes(),
        customerTags: [],
        category: document.getElementById("templateCategory").value,
        availableVariables: TEMPLATE_AVAILABLE_VARIABLES
    };
}

function buildAiReviewRequest() {
    return {
        ...buildAiRequestBase(),
        title: document.getElementById("templateTitle").value.trim(),
        content: document.getElementById("templateContent").value.trim(),
        templateId: null,
        userId: null
    };
}

function getSelectedChannelTypes() {
    const selectedIds = new Set(Array.from(document.querySelectorAll("input[name='templateChannel']:checked"))
        .map(input => Number(input.value)));
    return (templateOptions.channels || [])
        .filter(channel => selectedIds.has(Number(channel.channelId)))
        .map(channel => channel.channelType);
}

function getTemplateReviewSignature() {
    return JSON.stringify({
        title: document.getElementById("templateTitle").value.trim(),
        content: document.getElementById("templateContent").value.trim(),
        category: document.getElementById("templateCategory").value,
        purpose: document.getElementById("templatePurpose").value,
        channelTypes: getSelectedChannelTypes().slice().sort()
    });
}

function invalidateTemplateReview() {
    templateReviewSignature = null;
    templateReviewStatus = null;
    document.getElementById("templateReviewAcknowledge").checked = false;
    document.getElementById("templateReviewAcknowledgeRow").hidden = true;
    const panel = document.getElementById("templateReviewPanel");
    if (!panel.hidden) {
        document.getElementById("templateReviewResult").innerHTML = `<p class="template-review-summary">입력 내용이 변경되었습니다. 다시 AI 검사를 실행해 주세요.</p>`;
    }
    updateTemplateSaveState();
}

function updateTemplateSaveState() {
    const currentReview = templateReviewSignature !== null
        && templateReviewSignature === getTemplateReviewSignature();
    const acknowledged = templateReviewStatus === "PASS"
        || document.getElementById("templateReviewAcknowledge").checked;
    const isLengthValid = !getTemplateLimitInfo().isOverLimit;
    document.getElementById("templateSaveButton").disabled = !(currentReview && acknowledged && isLengthValid);
}

function renderTemplateReview(data) {
    const status = data.status || "NOTICE";
    const issues = data.issues || [];
    const issueHtml = issues.length
        ? issues.map(issue => `
            <article class="template-review-issue">
                <div class="template-review-issue__header">
                    <strong>${escapeHtml(issue.message || issue.ruleId || "검토 항목")}</strong>
                    <span class="template-review-status template-review-status--${escapeHtml(issue.status || status)}">${escapeHtml(issue.status || status)}</span>
                </div>
                <div class="template-review-meta">
                    ${issue.source ? renderBadge(issue.source, "default") : ""}
                    ${issue.severity ? renderBadge(issue.severity, issue.severity === "HIGH" ? "amber" : "default") : ""}
                    ${issue.field ? renderBadge(issue.field, "blue") : ""}
                </div>
                ${issue.targetText ? `<p><strong>대상:</strong> ${escapeHtml(issue.targetText)}</p>` : ""}
                ${issue.suggestion ? `<p><strong>수정 제안:</strong> ${escapeHtml(issue.suggestion)}</p>` : ""}
                ${(issue.detail || []).length ? `<p><strong>상세:</strong> ${escapeHtml(issue.detail.join(", "))}</p>` : ""}
            </article>
        `).join("")
        : `<p class="template-review-summary">발견된 이슈가 없습니다.</p>`;

    document.getElementById("templateReviewResult").innerHTML = `
        <div class="template-review-header">
            <strong>AI 검사 결과</strong>
            <span class="template-review-status template-review-status--${escapeHtml(status)}">${escapeHtml(status)}</span>
        </div>
        <p class="template-review-summary">${escapeHtml(data.summary || "검사가 완료되었습니다.")}</p>
        <div class="template-review-issues">${issueHtml}</div>
        ${data.suggestedRewrite ? `
            <div class="template-review-rewrite">
                <strong>AI 수정 문구</strong>
                <p>${escapeHtml(data.suggestedRewrite)}</p>
                <button id="templateApplyRewriteButton" type="button" class="ds-button ds-button--outline">수정 문구 적용</button>
            </div>
        ` : ""}
    `;

    if (data.suggestedRewrite) {
        document.getElementById("templateApplyRewriteButton").addEventListener("click", function() {
            document.getElementById("templateContent").value = data.suggestedRewrite;
            renderTemplateByteCount();
            if (!enforceTemplateLength()) {
                return;
            }
            renderFormPreview();
            invalidateTemplateReview();
        });
    }

    const needsAcknowledge = status !== "PASS";
    document.getElementById("templateReviewAcknowledgeRow").hidden = !needsAcknowledge;
    document.getElementById("templateReviewAcknowledge").checked = false;
    updateTemplateSaveState();
}

async function readErrorMessage(response, fallback) {
    try {
        const data = await response.json();
        return data.message || fallback;
    } catch (error) {
        return fallback;
    }
}

function renderFormPreview() {
    const title = document.getElementById("templateTitle")?.value || "";
    const content = document.getElementById("templateContent")?.value || "";
    const previewDiv = document.getElementById("templateFormPreview");
    if (!previewDiv) {
        return;
    }

    const component = createCommonMessagePreview(title, content, templatePreviewMode);
    previewDiv.innerHTML = "";
    if (component) {
        previewDiv.appendChild(component);
    }
}

function renderMessagePreview(title, content, mode, compact = false) {
    const modeClass = mode === "kakao" ? "message-preview--kakao" : mode === "email" ? "message-preview--email" : "";
    const safeTitle = escapeHtml(title || "메시지 제목");
    const safeContent = escapeHtml(content || "메시지 내용을 입력하세요.");

    if (mode === "email") {
        return `
            <div class="message-preview ${modeClass}">
                <div class="message-preview__screen">
                    <div class="message-preview__sensor"></div>
                    <div class="message-preview__status"><span>9:41</span><span>▭ ▰</span></div>
                    <div class="message-preview__title">Mail</div>
                    <div class="message-preview__body">
                        <div class="message-preview__mail-header"><span class="message-preview__mail-logo">M</span><strong>Gmail</strong></div>
                        <div class="message-preview__mail-title">${safeTitle}</div>
                        <div class="message-preview__mail-meta">
                            <span class="message-preview__mail-avatar">현</span>
                            <span><strong>현대퓨처넷</strong><small>to me</small></span>
                        </div>
                        <div class="message-preview__bubble">${safeContent}</div>
                    </div>
                    <div class="message-preview__home"></div>
                </div>
            </div>
        `;
    }

    return `
        <div class="message-preview ${modeClass}">
            <div class="message-preview__screen">
                <div class="message-preview__sensor"></div>
                <div class="message-preview__status"><span>9:41</span><span>▭ ▰</span></div>
                <div class="message-preview__title">${mode === "kakao" ? "카카오톡" : "메시지"}</div>
                <div class="message-preview__body">
                    <div class="message-preview__sender">${mode === "message" ? "010-0000-0000" : "현대퓨처넷"}</div>
                    <div class="message-preview__bubble-group">
                        <div class="message-preview__bubble">
                            <div class="message-preview__bubble-title">${safeTitle}</div>
                            <span class="message-preview__bubble-text">${safeContent}</span>
                        </div>
                        ${mode === "kakao" ? `<div class="message-preview__kakao-action">자세히 보기</div>` : ""}
                    </div>
                </div>
                <div class="message-preview__home"></div>
            </div>
        </div>
    `;
}

function inferPreviewMode(item) {
    const channels = (item.channels || []).map(channel => channel.channelType.toUpperCase()).join(",");
    if (channels.includes("EMAIL")) return "email";
    if (channels.includes("KAKAO") || channels.includes("알림") || channels.includes("친구")) return "kakao";
    return "message";
}

function getCategoryLabel(category) {
    if (!category) return "-";

    const key = String(category).trim().toUpperCase();
    const labels = {
        BENEFIT: "혜택",
        EVENT: "이벤트",
        NOTICE: "공지",
        CRM: "고객관리"
    };

    return labels[key] || category;
}

function buildPurposeOptions(options) {
    const defaults = [
        { value: "AD", label: "광고" },
        { value: "INFO", label: "정보성" }
    ];
    const rows = options.map(option => ({
        value: option.value,
        label: getPurposeLabel(option.value || option.label)
    }));
    const merged = [...defaults, ...rows];
    return merged.filter((option, index) => merged.findIndex(item => item.value === option.value) === index);
}

function normalizePurpose(purpose) {
    const value = (purpose || "").toLowerCase();
    if (value === "advertising" || value === "ad" || value.includes("광고")) return "AD";
    return "INFO";
}

function getPurposeLabel(purpose) {
    return normalizePurpose(purpose) === "AD" ? "광고" : "정보성";
}

function flattenText(text) {
    return (text || "").replace(/\s+/g, " ").trim();
}

function withJsonBody(method, body) {
    const headers = { "Content-Type": "application/json" };
    const token = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const header = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

    if (token && header) {
        headers[header] = token;
    }

    return {
        method,
        headers,
        body: JSON.stringify(body)
    };
}

function escapeHtml(text) {
    if (text === null || text === undefined) return "";
    return String(text).replace(/[&<>"']/g, function(match) {
        return {
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            "\"": "&quot;",
            "'": "&#039;"
        }[match];
    });
}
