let templateCurrentPage = 1;
let templatePageSize = 10;
let templatePreviewMode = "message";
let templateOptions = {
    channels: [],
    categories: [],
    purposes: [],
    tags: []
};

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

    ["templateCategoryFilter", "templatePurposeFilter", "templateChannelFilter", "templateSortSelect"].forEach(id => {
        document.getElementById(id).addEventListener("change", function() {
            templateCurrentPage = 1;
            fetchTemplates();
            renderQuickFilters();
        });
    });

    ["templateTitle", "templateContent"].forEach(id => {
        document.getElementById(id).addEventListener("input", renderFormPreview);
    });

    document.getElementById("templateContent").addEventListener("input", function(event) {
        document.getElementById("templateContentCount").innerText = `${event.target.value.length}자`;
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
        });
    });

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
            renderQuickFilters();
            renderTemplateChannelCheckboxes();
            renderTemplateTagOptions();
        })
        .catch(err => console.error("Template options load fail:", err));
}

function renderTemplateFilters() {
    renderSelectOptions("templateCategoryFilter", templateOptions.categories || [], "전체 카테고리");
    renderSelectOptions("templateCategory", templateOptions.categories || [], "카테고리 선택");

    const purposeOptions = buildPurposeOptions(templateOptions.purposes || []);
    renderSelectOptions("templatePurposeFilter", purposeOptions, "전체 광고여부");

    const channelOptions = (templateOptions.channels || []).map(channel => ({
        value: channel.channelType,
        label: channel.channelType
    }));
    renderSelectOptions("templateChannelFilter", channelOptions, "전체 채널");
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

function renderQuickFilters() {
    const wrapper = document.getElementById("templateQuickFilters");
    const chips = [
        ...(templateOptions.categories || []).slice(0, 6).map(option => ({ type: "category", value: option.value, label: option.label })),
        ...buildPurposeOptions(templateOptions.purposes || []).slice(0, 4).map(option => ({ type: "purpose", value: option.value, label: option.label })),
        ...(templateOptions.channels || []).slice(0, 6).map(channel => ({ type: "channel", value: channel.channelType, label: channel.channelType }))
    ];

    wrapper.innerHTML = chips.map(chip => {
        const active = isQuickFilterActive(chip);
        return `<button type="button" class="template-chip ${active ? "is-active" : ""}" data-chip-type="${chip.type}" data-chip-value="${escapeHtml(chip.value)}">${escapeHtml(chip.label)}</button>`;
    }).join("");

    wrapper.querySelectorAll(".template-chip").forEach(button => {
        button.addEventListener("click", function() {
            const type = button.dataset.chipType;
            const value = button.dataset.chipValue;
            const selectId = type === "category" ? "templateCategoryFilter" : type === "purpose" ? "templatePurposeFilter" : "templateChannelFilter";
            const select = document.getElementById(selectId);
            select.value = select.value === value ? "" : value;
            templateCurrentPage = 1;
            fetchTemplates();
            renderQuickFilters();
        });
    });
}

function isQuickFilterActive(chip) {
    if (chip.type === "category") return document.getElementById("templateCategoryFilter").value === chip.value;
    if (chip.type === "purpose") return document.getElementById("templatePurposeFilter").value === chip.value;
    return document.getElementById("templateChannelFilter").value === chip.value;
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
}

function renderTemplateTagOptions() {
    const wrapper = document.getElementById("templateTagList");
    const tags = templateOptions.tags || [];

    if (!tags.length) {
        wrapper.innerHTML = `<span class="text-muted">등록된 태그가 없습니다.</span>`;
        return;
    }

    // 템플릿-태그 매핑 테이블이 확인되면 저장 로직을 별도로 연결한다.
    wrapper.innerHTML = tags
        .map(tag => `<span class="ds-badge" data-tag-id="${escapeHtml(tag.value)}">${escapeHtml(tag.label)}</span>`)
        .join("");
}

function fetchTemplates() {
    const tbody = document.getElementById("templateTableBody");
    tbody.innerHTML = `<tr><td colspan="7" class="template-empty-cell">데이터를 불러오는 중입니다...</td></tr>`;
    document.getElementById("templateMobileList").innerHTML = `<div class="template-empty-cell">데이터를 불러오는 중입니다...</div>`;

    const params = new URLSearchParams({
        page: templateCurrentPage,
        size: templatePageSize,
        sortOrder: document.getElementById("templateSortSelect").value
    });

    appendParam(params, "keyword", document.getElementById("templateKeywordInput").value.trim());
    appendParam(params, "category", document.getElementById("templateCategoryFilter").value);
    appendParam(params, "purpose", document.getElementById("templatePurposeFilter").value);
    appendParam(params, "channelType", document.getElementById("templateChannelFilter").value);

    fetch(`/api/templates?${params.toString()}`)
        .then(res => res.json())
        .then(data => {
            renderTemplateTable(data.list || []);
            renderTemplateMobileList(data.list || []);
            renderTemplatePagination(data);
        })
        .catch(err => {
            console.error("Template list load fail:", err);
            tbody.innerHTML = `<tr><td colspan="7" class="template-empty-cell">템플릿 목록을 불러오는 도중 오류가 발생했습니다.</td></tr>`;
            document.getElementById("templateMobileList").innerHTML = `<div class="template-empty-cell">템플릿 목록을 불러오는 도중 오류가 발생했습니다.</div>`;
        });
}

function appendParam(params, key, value) {
    if (value) {
        params.append(key, value);
    }
}

function renderTemplateTable(list) {
    const tbody = document.getElementById("templateTableBody");
    tbody.innerHTML = "";

    if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="template-empty-cell">조건에 맞는 템플릿이 없습니다.</td></tr>`;
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
            <td class="template-col-lg">${renderBadge(item.category || "-", "default")}</td>
            <td>${renderPurposeBadge(item.purpose)}</td>
            <td class="template-col-xl">${renderTagList(item)}</td>
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
                ${renderBadge(item.category || "-", "default")}
                ${renderTagList(item, 2)}
            </div>
            <div class="template-mobile-card__footer">
                <span>${(item.cnt || 0).toLocaleString()}회 사용</span>
                <span>${item.updatedAt || "-"}</span>
            </div>
        </button>
    `).join("");
}

function renderTagList(item, limit = 3) {
    const tags = buildTemplateTags(item).slice(0, limit);
    if (!tags.length) {
        return renderBadge("없음", "default");
    }
    return `<div class="template-tag-list">${tags.map(tag => `<span class="ds-badge">${escapeHtml(tag)}</span>`).join("")}</div>`;
}

function buildTemplateTags(item) {
    const tags = [];
    if (item.category) tags.push(item.category);
    if (item.purpose) tags.push(getPurposeLabel(item.purpose));
    (item.channels || []).forEach(channel => tags.push(channel.channelType));
    if (item.isAiGenerated) tags.push("AI");
    return [...new Set(tags.filter(Boolean))];
}

function renderChannelChips(channels) {
    if (!channels.length) {
        return renderBadge("미지정", "default");
    }

    return `<div class="template-channel-list">${channels.map(channel => renderBadge(channel.channelType, "blue")).join("")}</div>`;
}

function renderPurposeBadge(purpose) {
    const normalized = normalizePurpose(purpose);
    if (normalized === "advertising") {
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
    const totalPages = pageData.totalPages || 0;
    const from = total === 0 ? 0 : ((current - 1) * size) + 1;
    const to = Math.min(current * size, total);

    document.getElementById("templatePageInfo").innerText = `${total.toLocaleString()}건 중 ${from.toLocaleString()}-${to.toLocaleString()}`;

    const container = document.getElementById("templatePaginationButtons");
    container.innerHTML = "";

    container.appendChild(createPageButton("이전", current === 1, function() {
        templateCurrentPage = Math.max(1, templateCurrentPage - 1);
        fetchTemplates();
    }));

    const max = Math.max(1, totalPages);
    const base = Math.min(Math.max(current - 2, 1), Math.max(max - 4, 1));
    for (let page = base; page < base + 5 && page <= max; page++) {
        const button = createPageButton(page, false, function() {
            templateCurrentPage = page;
            fetchTemplates();
        });
        button.classList.toggle("is-active", page === current);
        container.appendChild(button);
    }

    container.appendChild(createPageButton("다음", current >= max, function() {
        templateCurrentPage = Math.min(max, templateCurrentPage + 1);
        fetchTemplates();
    }));
}

function createPageButton(label, disabled, onClick) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "ds-page-button";
    button.innerText = label;
    button.disabled = disabled;
    button.onclick = onClick;
    return button;
}

function openTemplateDetailModal(templateId) {
    const body = document.getElementById("templateDetailBody");
    body.innerHTML = `<div class="template-empty-cell">상세 정보를 불러오는 중입니다...</div>`;
    document.getElementById("templateDetailModal").classList.add("is-open");

    fetch(`/api/templates/${templateId}`)
        .then(res => res.json())
        .then(data => {
            body.innerHTML = renderTemplateDetail(data);
        })
        .catch(err => {
            console.error("Template detail load fail:", err);
            body.innerHTML = `<div class="template-empty-cell">상세 정보를 불러오는 도중 오류가 발생했습니다.</div>`;
        });
}

function renderTemplateDetail(item) {
    return `
        <div class="template-detail">
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
                            <span class="template-detail__value">${escapeHtml(item.category || "-")}</span>
                        </div>
                        <div class="template-detail__meta-item">
                            <span class="template-detail__label">광고여부</span>
                            ${renderPurposeBadge(item.purpose)}
                        </div>
                        <div class="template-detail__meta-item">
                            <span class="template-detail__label">태그</span>
                            ${renderTagList(item, 8)}
                        </div>
                    </div>
                </div>
            </div>

            <div class="template-detail__metrics">
                ${renderMetric("사용 횟수", `${(item.cnt || 0).toLocaleString()}회`)}
                ${renderMetric("최근 수정", item.updatedAt || "-")}
                ${renderMetric("광고여부", getPurposeLabel(item.purpose))}
                ${renderMetric("문자 길이", `${(item.content || "").length}자`)}
                ${renderMetric("카카오 상태", getKakaoStatusLabel(item.kakaoTemplateStatus))}
            </div>

            <div class="template-detail__content">
                <section>
                    <div class="template-detail__metric-label">메시지 내용</div>
                    <div class="template-message-box">${escapeHtml(item.content || "")}</div>
                </section>
                <section>
                    <div class="template-preview-header">
                        <span>미리보기</span>
                    </div>
                    ${renderMessagePreview(item.title, item.content, inferPreviewMode(item))}
                </section>
            </div>
        </div>
    `;
}

function renderMetric(label, value) {
    return `
        <div>
            <div class="template-detail__metric-label">${escapeHtml(label)}</div>
            <div class="template-detail__metric-value">${escapeHtml(value)}</div>
        </div>
    `;
}

function openAddTemplateModal() {
    resetTemplateForm();
    document.getElementById("templateAddModal").classList.add("is-open");
    renderFormPreview();
}

function closeAddTemplateModal() {
    document.getElementById("templateAddModal").classList.remove("is-open");
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

function resetTemplateForm() {
    document.getElementById("templateForm").reset();
    document.getElementById("templatePurpose").value = "advertising";
    document.querySelectorAll("[data-purpose-value]").forEach(button => {
        button.classList.toggle("is-active", button.dataset.purposeValue === "advertising");
    });
    document.getElementById("templateContentCount").innerText = "0자";
    document.querySelectorAll("input[name='templateChannel']").forEach(input => input.checked = false);
}

function saveTemplate() {
    const body = {
        title: document.getElementById("templateTitle").value.trim(),
        content: document.getElementById("templateContent").value.trim(),
        category: document.getElementById("templateCategory").value,
        purpose: document.getElementById("templatePurpose").value,
        kakaoTemplateStatus: document.getElementById("templateKakaoStatus").value,
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

function renderFormPreview() {
    const title = document.getElementById("templateTitle")?.value || "";
    const content = document.getElementById("templateContent")?.value || "";
    document.getElementById("templateFormPreview").innerHTML = renderMessagePreview(title, content, templatePreviewMode, true);
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

function buildPurposeOptions(options) {
    const defaults = [
        { value: "advertising", label: "광고" },
        { value: "informational", label: "정보성" }
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
    if (value === "advertising" || value === "ad" || value.includes("광고")) return "advertising";
    return "informational";
}

function getPurposeLabel(purpose) {
    return normalizePurpose(purpose) === "advertising" ? "광고" : "정보성";
}

function getKakaoStatusLabel(status) {
    if (status === "APPROVED") return "승인";
    if (status === "REJECTED") return "반려";
    return "대기";
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
