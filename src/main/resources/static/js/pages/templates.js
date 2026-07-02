let templateCurrentPage = 1;
let templatePageSize = 10;
let templateOptions = {
    channels: [],
    categories: [],
    purposes: []
};

document.addEventListener("DOMContentLoaded", function() {
    bindTemplateEvents();
    fetchTemplateOptions();
    fetchTemplateStats();
    fetchTemplates();
});

function bindTemplateEvents() {
    document.getElementById("templateKeywordInput").addEventListener("keyup", function() {
        templateCurrentPage = 1;
        fetchTemplates();
    });

    ["templateCategoryFilter", "templatePurposeFilter", "templateChannelFilter", "templateSortSelect"].forEach(id => {
        document.getElementById(id).addEventListener("change", function() {
            templateCurrentPage = 1;
            fetchTemplates();
        });
    });

    document.getElementById("templateForm").addEventListener("submit", function(event) {
        event.preventDefault();
        saveTemplate();
    });
}

function fetchTemplateStats() {
    fetch("/api/templates/stats")
        .then(res => res.json())
        .then(data => {
            document.getElementById("statTotalCount").innerText = `${(data.totalCount || 0).toLocaleString()}개`;
            document.getElementById("statAiCount").innerText = `${(data.aiGeneratedCount || 0).toLocaleString()}개`;
            document.getElementById("statApprovedCount").innerText = `${(data.kakaoApprovedCount || 0).toLocaleString()}개`;
            document.getElementById("statUseCount").innerText = `${(data.totalUseCount || 0).toLocaleString()}회`;
        })
        .catch(err => console.error("Template stats load fail:", err));
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
    renderSelectOptions("templateCategoryFilter", templateOptions.categories || [], "전체 카테고리");
    renderSelectOptions("templatePurposeFilter", templateOptions.purposes || [], "전체 목적");

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

function fetchTemplates() {
    const tbody = document.getElementById("templateTableBody");
    tbody.innerHTML = `<tr><td colspan="8" class="template-empty-cell">데이터를 불러오는 중입니다...</td></tr>`;

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
            renderTemplatePagination(data);
        })
        .catch(err => {
            console.error("Template list load fail:", err);
            tbody.innerHTML = `<tr><td colspan="8" class="template-empty-cell">템플릿 목록을 불러오는 도중 오류가 발생했습니다.</td></tr>`;
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
        tbody.innerHTML = `<tr><td colspan="8" class="template-empty-cell">조건에 맞는 템플릿이 없습니다.</td></tr>`;
        return;
    }

    list.forEach(item => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td class="template-title-cell">
                <span class="template-title">${escapeHtml(item.title)}</span>
                <span class="template-preview">${escapeHtml(item.content)}</span>
            </td>
            <td>${renderTextBadge(item.category || "-", "gray")}</td>
            <td>${escapeHtml(item.purpose || "-")}</td>
            <td>${renderChannelChips(item.channels || [])}</td>
            <td>${renderKakaoStatus(item.kakaoTemplateStatus)}</td>
            <td>${(item.cnt || 0).toLocaleString()}회</td>
            <td>${item.updatedAt || "-"}</td>
            <td class="template-table__actions">
                <button type="button" class="template-action-button" onclick="openTemplateModal(${item.id})">수정</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function renderTextBadge(text, color) {
    return `<span class="template-badge template-badge--${color}">${escapeHtml(text)}</span>`;
}

function renderChannelChips(channels) {
    if (!channels.length) {
        return `<span class="template-badge template-badge--gray">미지정</span>`;
    }

    return `
        <div class="template-channel-list-inline">
            ${channels.map(channel => `<span class="template-channel-chip">${escapeHtml(channel.channelType)}</span>`).join("")}
        </div>
    `;
}

function renderKakaoStatus(status) {
    if (status === "APPROVED") {
        return renderTextBadge("승인", "green");
    }
    if (status === "REJECTED") {
        return renderTextBadge("반려", "red");
    }
    return renderTextBadge("대기", "yellow");
}

function renderTemplatePagination(pageData) {
    const total = pageData.totalCount || 0;
    const current = pageData.page || 1;
    const size = pageData.size || templatePageSize;
    const totalPages = pageData.totalPages || 0;
    const from = total === 0 ? 0 : ((current - 1) * size) + 1;
    const to = Math.min(current * size, total);

    document.getElementById("templatePageInfo").innerText = `총 ${total.toLocaleString()}개 중 ${from} - ${to}개 노출`;

    const container = document.getElementById("templatePaginationButtons");
    container.innerHTML = "";

    container.appendChild(createPageButton("‹", current === 1, function() {
        templateCurrentPage--;
        fetchTemplates();
    }));

    let startPage = Math.max(1, current - 2);
    let endPage = Math.min(totalPages, startPage + 4);
    if (endPage - startPage < 4) {
        startPage = Math.max(1, endPage - 4);
    }

    for (let page = startPage; page <= endPage; page++) {
        const button = createPageButton(page, false, function() {
            templateCurrentPage = page;
            fetchTemplates();
        });
        button.classList.toggle("active", page === current);
        container.appendChild(button);
    }

    container.appendChild(createPageButton("›", current >= totalPages || totalPages === 0, function() {
        templateCurrentPage++;
        fetchTemplates();
    }));
}

function createPageButton(label, disabled, onClick) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "template-page-button";
    button.innerText = label;
    button.disabled = disabled;
    button.onclick = onClick;
    return button;
}

function openTemplateModal(templateId) {
    resetTemplateForm();

    if (templateId) {
        fetch(`/api/templates/${templateId}`)
            .then(res => res.json())
            .then(data => fillTemplateForm(data))
            .catch(err => console.error("Template detail load fail:", err));
    }

    document.getElementById("templateModalTitle").innerText = templateId ? "템플릿 수정" : "템플릿 등록";
    document.getElementById("templateDeleteButton").style.display = templateId ? "inline-flex" : "none";
    document.getElementById("templateModal").classList.add("open");
}

function resetTemplateForm() {
    document.getElementById("templateForm").reset();
    document.getElementById("templateId").value = "";
    document.querySelectorAll("input[name='templateChannel']").forEach(input => input.checked = false);
}

function fillTemplateForm(data) {
    document.getElementById("templateId").value = data.id;
    document.getElementById("templateTitle").value = data.title || "";
    document.getElementById("templateCategory").value = data.category || "";
    document.getElementById("templatePurpose").value = data.purpose || "";
    document.getElementById("templateKakaoStatus").value = data.kakaoTemplateStatus || "PENDING";
    document.getElementById("templateKakaoCode").value = data.kakaoTemplateCode || "";
    document.getElementById("templateAiGenerated").checked = !!data.isAiGenerated;
    document.getElementById("templateContent").value = data.content || "";

    const selectedIds = (data.channels || []).map(channel => String(channel.channelId));
    document.querySelectorAll("input[name='templateChannel']").forEach(input => {
        input.checked = selectedIds.includes(input.value);
    });
}

function saveTemplate() {
    const templateId = document.getElementById("templateId").value;
    const body = {
        title: document.getElementById("templateTitle").value.trim(),
        content: document.getElementById("templateContent").value.trim(),
        category: document.getElementById("templateCategory").value.trim(),
        purpose: document.getElementById("templatePurpose").value.trim(),
        kakaoTemplateCode: document.getElementById("templateKakaoCode").value.trim(),
        kakaoTemplateStatus: document.getElementById("templateKakaoStatus").value,
        isAiGenerated: document.getElementById("templateAiGenerated").checked,
        channelIds: Array.from(document.querySelectorAll("input[name='templateChannel']:checked"))
            .map(input => Number(input.value))
    };

    const url = templateId ? `/api/templates/${templateId}` : "/api/templates";
    const method = templateId ? "PUT" : "POST";

    fetch(url, withJsonBody(method, body))
        .then(res => {
            if (!res.ok) throw new Error("save failed");
            closeTemplateModal();
            fetchTemplateOptions();
            fetchTemplateStats();
            fetchTemplates();
        })
        .catch(err => {
            console.error("Template save fail:", err);
            alert("템플릿 저장 중 오류가 발생했습니다.");
        });
}

function deleteCurrentTemplate() {
    const templateId = document.getElementById("templateId").value;
    if (!templateId || !confirm("선택한 템플릿을 삭제하시겠습니까?")) {
        return;
    }

    fetch(`/api/templates/${templateId}`, withJsonBody("DELETE"))
        .then(res => {
            if (!res.ok) throw new Error("delete failed");
            closeTemplateModal();
            fetchTemplateOptions();
            fetchTemplateStats();
            fetchTemplates();
        })
        .catch(err => {
            console.error("Template delete fail:", err);
            alert("템플릿 삭제 중 오류가 발생했습니다.");
        });
}

function closeTemplateModal() {
    document.getElementById("templateModal").classList.remove("open");
}

function closeTemplateModalOnBackdrop(event) {
    if (event.target.id === "templateModal") {
        closeTemplateModal();
    }
}

function withJsonBody(method, body) {
    const headers = { "Content-Type": "application/json" };
    const token = document.querySelector("meta[name='_csrf']")?.getAttribute("content");
    const header = document.querySelector("meta[name='_csrf_header']")?.getAttribute("content");

    if (token && header) {
        headers[header] = token;
    }

    const options = { method, headers };
    if (body) {
        options.body = JSON.stringify(body);
    }
    return options;
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
