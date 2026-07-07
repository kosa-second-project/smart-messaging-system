const CustomerPage = {
    currentTab: "members",
    currentPage: 1,
    pageSize: 20,
    searchKeyword: "",
    sortOrder: "latest",
    conditionMode: "OR",
    tags: [],
    selectedTagIds: [],

    init() {
        this.bindEvents();
        this.renderHeader();
        this.updateTagPanelState();
        this.fetchStats();
        this.loadTags();
    },

    bindEvents() {
        const searchInput = document.getElementById("customerSearchInput");
        let searchTimer;
        searchInput?.addEventListener("input", (event) => {
            this.searchKeyword = event.target.value.trim();
            clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                this.currentPage = 1;
                this.fetchData();
            }, 250);
        });

        document.getElementById("btnResetTags")?.addEventListener("click", () => {
            if (this.currentTab === "rejects") return;
            this.selectedTagIds = [];
            this.currentPage = 1;
            this.renderTagFilters();
            this.fetchData();
        });

        document.getElementById("btnConditionOr")?.addEventListener("click", () => {
            if (this.currentTab === "rejects") return;
            this.conditionMode = "OR";
            document.getElementById("btnConditionOr")?.classList.add("active");
            document.getElementById("btnConditionAnd")?.classList.remove("active");
            this.currentPage = 1;
            this.fetchData();
        });

        document.getElementById("btnConditionAnd")?.addEventListener("click", () => {
            if (this.currentTab === "rejects") return;
            this.conditionMode = "AND";
            document.getElementById("btnConditionAnd")?.classList.add("active");
            document.getElementById("btnConditionOr")?.classList.remove("active");
            this.currentPage = 1;
            this.fetchData();
        });
    },

    loadTags() {
        fetch("/api/customers/tags")
            .then(res => res.json())
            .then(tags => {
                this.tags = Array.isArray(tags) ? tags.filter(tag => tag && tag.id && tag.name) : [];
                this.renderTagFilters();
            })
            .catch(err => {
                console.error("Tag fetch failed:", err);
                this.tags = [];
                this.renderTagFilters();
            })
            .finally(() => this.fetchData());
    },

    fetchStats() {
        fetch("/api/customers/stats")
            .then(res => res.json())
            .then(data => {
                setText("statTotal", `${formatNumber(data.totalCount)}명`);
                setText("statRegular", `${formatNumber(data.regularCount)}명`);
                setText("statNew", `${formatNumber(data.newCount)}명`);
                setText("statDormant", `${formatNumber(data.dormantCount)}명`);
            })
            .catch(err => console.error("Stats fetch failed:", err));
    },

    renderTagFilters() {
        const root = document.getElementById("customerTagFilters");
        if (!root) return;

        const groups = this.groupTags(this.tags);
        root.replaceChildren();

        if (this.tags.length === 0) {
            const empty = document.createElement("div");
            empty.className = "selector-empty-cell";
            empty.textContent = "표시할 태그가 없습니다.";
            root.appendChild(empty);
            this.renderSelectedTagSummary();
            this.updateTagPanelState();
            return;
        }

        groups.forEach(group => {
            if (group.tags.length === 0) return;

            const section = document.createElement("div");
            section.className = "selector-panel-section";
            section.dataset.group = group.key;

            const title = document.createElement("h4");
            title.className = "selector-section-title";
            title.textContent = group.title;

            const cloud = document.createElement("div");
            cloud.className = "selector-tag-cloud";

            group.tags.forEach(tag => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "selector-tag-button";
                button.textContent = tag.name;
                button.dataset.tagId = tag.id;
                if (this.selectedTagIds.includes(Number(tag.id))) {
                    button.classList.add("selected");
                }
                button.addEventListener("click", () => this.toggleTag(Number(tag.id)));
                cloud.appendChild(button);
            });

            section.append(title, cloud);
            root.appendChild(section);
        });

        this.renderSelectedTagSummary();
        this.updateTagPanelState();
    },

    groupTags(tags) {
        const used = new Set();
        const take = (predicate) => tags.filter(tag => {
            const name = String(tag.name || "").trim();
            if (used.has(tag.id) || !predicate(name)) return false;
            used.add(tag.id);
            return true;
        });

        const customerTypes = take(name => ["일반", "신규", "휴면"].includes(name));
        const genders = take(name => ["남자", "여자"].includes(name));
        const ages = take(name => /^\d{2,3}대$/.test(name));
        const consents = take(name => name.toLowerCase().includes("동의"));
        const others = tags.filter(tag => !used.has(tag.id));

        return [
            { key: "type", title: "고객 유형", tags: customerTypes },
            { key: "gender", title: "성별", tags: genders },
            { key: "age", title: "나이", tags: ages },
            { key: "consent", title: "수신 동의", tags: consents },
            { key: "etc", title: "기타 태그", tags: others }
        ];
    },

    toggleTag(tagId) {
        if (this.currentTab === "rejects") return;

        if (this.selectedTagIds.includes(tagId)) {
            this.selectedTagIds = this.selectedTagIds.filter(id => id !== tagId);
        } else {
            this.selectedTagIds = [...this.selectedTagIds, tagId];
        }
        this.currentPage = 1;
        this.renderTagFilters();
        this.fetchData();
    },

    renderSelectedTagSummary() {
        const summary = document.getElementById("selectedTagSummary");
        if (!summary) return;

        const names = this.tags
            .filter(tag => this.selectedTagIds.includes(Number(tag.id)))
            .map(tag => tag.name);

        summary.textContent = names.length === 0 ? "태그 전체" : names.join(", ");
    },

    updateTagPanelState() {
        const isRejectsTab = this.currentTab === "rejects";
        const panel = document.getElementById("customerTagPanel");
        panel?.classList.toggle("is-disabled", isRejectsTab);
        panel?.setAttribute("aria-disabled", String(isRejectsTab));

        ["btnResetTags", "btnConditionOr", "btnConditionAnd"].forEach(id => {
            document.getElementById(id)?.toggleAttribute("disabled", isRejectsTab);
        });

        document.querySelectorAll("#customerTagFilters .selector-tag-button").forEach(button => {
            button.toggleAttribute("disabled", isRejectsTab);
        });

        if (isRejectsTab) {
            setText("selectedTagSummary", "수신거부 목록에서는 태그 선택 비활성");
        } else {
            this.renderSelectedTagSummary();
        }
    },

    renderHeader() {
        const header = document.getElementById("tableHeader");
        if (!header) return;

        if (this.currentTab === "members") {
            header.innerHTML = `
                <tr>
                    <th>고객명</th>
                    <th>번호</th>
                    <th>유형</th>
                    <th>성별</th>
                    <th>나이</th>
                    <th>생일 대상자</th>
                    <th>문자</th>
                    <th>카카오</th>
                    <th>이메일</th>
                    <th>가입일</th>
                </tr>
            `;
        } else {
            header.innerHTML = `
                <tr>
                    <th>고객명</th>
                    <th>번호</th>
                    <th>수신 거부 일시</th>
                </tr>
            `;
        }
    },

    fetchData() {
        const tbody = document.getElementById("tableBody");
        if (!tbody) return;

        const colSpan = this.currentTab === "members" ? 10 : 3;
        tbody.innerHTML = `<tr><td colspan="${colSpan}" class="selector-empty-cell">데이터를 불러오는 중입니다.</td></tr>`;

        const params = this.buildParams();
        const url = this.currentTab === "members" ? "/api/customers/manage" : "/api/customers/rejects";

        fetch(`${url}?${params.toString()}`)
            .then(res => res.json())
            .then(data => {
                const list = data.list || [];
                this.currentPage = data.page || this.currentPage;
                setText("lblCustomerCount", formatNumber(data.totalCount));
                this.renderTable(list);
                this.renderPagination(data);
            })
            .catch(err => {
                console.error("Customer fetch failed:", err);
                tbody.innerHTML = `<tr><td colspan="${colSpan}" class="selector-empty-cell">데이터를 불러오는 중 오류가 발생했습니다.</td></tr>`;
            });
    },

    buildParams() {
        const params = new URLSearchParams();
        params.set("page", this.currentPage);
        params.set("size", this.pageSize);
        params.set("sortOrder", this.sortOrder);
        params.set("matchType", this.conditionMode);

        if (this.searchKeyword) {
            params.set("name", this.searchKeyword);
        }

        if (this.currentTab === "members") {
            this.selectedTagIds.forEach(id => params.append("tagIds", id));
        }
        return params;
    },

    renderTable(list) {
        const tbody = document.getElementById("tableBody");
        tbody.replaceChildren();

        if (list.length === 0) {
            const colSpan = this.currentTab === "members" ? 10 : 3;
            tbody.innerHTML = `<tr><td colspan="${colSpan}" class="selector-empty-cell">조건에 맞는 고객이 없습니다.</td></tr>`;
            return;
        }

        list.forEach(item => {
            const tr = document.createElement("tr");
            tr.addEventListener("click", () => openDetailModal(item.customerId));

            if (this.currentTab === "members") {
                const tags = normalizeTags(item.tags);
                const gender = findTag(tags, tag => tag === "남자" || tag === "여자") || "-";
                const age = findTag(tags, tag => /^\d{2,3}대$/.test(tag)) || "-";
                const birthday = tags.includes("생일 대상자");

                tr.innerHTML = `
                    <td><strong>${escapeHtml(item.name)}</strong></td>
                    <td class="font-mono">${escapeHtml(maskPhoneNumber(item.phone))}</td>
                    <td>${renderTypeBadge(item.customerType)}</td>
                    <td>${escapeHtml(gender)}</td>
                    <td>${escapeHtml(age)}</td>
                    <td>${birthday ? '<span class="ds-badge ds-badge--primary">대상</span>' : '-'}</td>
                    <td>${renderConsent(item.smsConsent)}</td>
                    <td>${renderConsent(item.kakaoConsent)}</td>
                    <td>${renderConsent(item.emailConsent)}</td>
                    <td>${escapeHtml(item.joinedAt || "-")}</td>
                `;
            } else {
                tr.innerHTML = `
                    <td><strong>${escapeHtml(item.name)}</strong></td>
                    <td class="font-mono">${escapeHtml(maskPhoneNumber(item.phone))}</td>
                    <td>${escapeHtml(item.rejectedAt || "-")}</td>
                `;
            }

            tbody.appendChild(tr);
        });
    },

    renderPagination(pageData) {
        window.DsPagination?.renderOffset("#customerPagination", {
            total: pageData.totalCount || 0,
            page: pageData.page || 1,
            size: pageData.size || this.pageSize,
            totalPages: pageData.totalPages || 0,
            summary: `총 ${formatNumber(pageData.totalCount)}명`,
            onPageChange: (page) => {
                this.currentPage = page;
                this.fetchData();
            },
            onPageSizeChange: (size) => {
                this.pageSize = size;
                this.currentPage = 1;
                this.fetchData();
            }
        });
    }
};

document.addEventListener("DOMContentLoaded", () => CustomerPage.init());

function switchTab(tab) {
    CustomerPage.currentTab = tab;
    CustomerPage.currentPage = 1;
    document.getElementById("tabMembersBtn")?.classList.toggle("active", tab === "members");
    document.getElementById("tabRejectsBtn")?.classList.toggle("active", tab === "rejects");
    CustomerPage.renderHeader();
    CustomerPage.updateTagPanelState();
    CustomerPage.fetchData();
}

function changeSortOrder(value) {
    CustomerPage.sortOrder = value;
    CustomerPage.currentPage = 1;
    CustomerPage.fetchData();
}

function openDetailModal(customerId) {
    const modal = document.getElementById("customerDetailModal");
    const body = document.getElementById("modalBodyContent");
    if (!modal || !body) return;

    modal.classList.add("open");
    body.innerHTML = `<div class="selector-empty-cell">상세 데이터를 불러오는 중입니다.</div>`;

    Promise.all([
        fetch(`/api/customers/${customerId}`).then(res => res.json()),
        fetch(`/api/customers/${customerId}/history`).then(res => res.json())
    ])
        .then(([customer, history]) => renderModalBody(customer, history || []))
        .catch(err => {
            console.error("Customer detail fetch failed:", err);
            body.innerHTML = `<div class="selector-empty-cell">상세 데이터를 불러오지 못했습니다.</div>`;
        });
}

function closeDetailModal() {
    document.getElementById("customerDetailModal")?.classList.remove("open");
}

function closeModalOnBackdrop(event) {
    if (event.target.id === "customerDetailModal") {
        closeDetailModal();
    }
}

function renderModalBody(customer, history) {
    const body = document.getElementById("modalBodyContent");
    if (!body || !customer) return;

    const historyRows = history.length === 0
        ? `<tr><td colspan="4" class="selector-empty-cell">최근 메시지 이력이 없습니다.</td></tr>`
        : history.map(row => `
            <tr>
                <td>${escapeHtml(row.sentAt || "-")}</td>
                <td>${escapeHtml(row.templateName || "-")}</td>
                <td>${escapeHtml(row.channel || "-")}</td>
                <td>${escapeHtml(row.status || "-")}</td>
            </tr>
        `).join("");

    body.innerHTML = `
        <div class="customer-detail-profile">
            <div class="customer-detail-identity">
                <div class="customer-detail-avatar">${escapeHtml((customer.name || "?").charAt(0))}</div>
                <div>
                    <div class="customer-detail-name">
                        <span>${escapeHtml(customer.name || "-")}</span>
                        ${renderTypeBadge(customer.customerType)}
                    </div>
                    <div class="customer-detail-phone">${escapeHtml(maskPhoneNumber(customer.phone))}</div>
                </div>
            </div>
            <div class="customer-detail-dates">
                <div>
                    <div class="customer-detail-label">가입일</div>
                    <div class="customer-detail-value">${escapeHtml(customer.joinedAt || "-")}</div>
                </div>
                <div>
                    <div class="customer-detail-label">마지막 발송</div>
                    <div class="customer-detail-value">${escapeHtml(customer.lastSend || "기록 없음")}</div>
                </div>
            </div>
        </div>

        <div class="customer-detail-section">
            <div class="customer-detail-section-header">
                <span class="customer-detail-section-title">수신 동의</span>
            </div>
            <div class="customer-tag-list">
                <span class="ds-badge ${customer.smsConsent ? "ds-badge--success" : "ds-badge--danger"}">SMS ${customer.smsConsent ? "동의" : "거부"}</span>
                <span class="ds-badge ${customer.kakaoConsent ? "ds-badge--success" : "ds-badge--danger"}">카카오 ${customer.kakaoConsent ? "동의" : "거부"}</span>
                <span class="ds-badge ${customer.emailConsent ? "ds-badge--success" : "ds-badge--danger"}">이메일 ${customer.emailConsent ? "동의" : "거부"}</span>
            </div>
        </div>

        <div class="customer-detail-section">
            <div class="customer-detail-section-header">
                <span class="customer-detail-section-title">태그</span>
                <span class="ds-badge">${(customer.tags || []).length}개</span>
            </div>
            <div class="customer-tag-list">${renderTags(customer.tags)}</div>
        </div>

        <div class="customer-detail-section">
            <div class="customer-detail-section-header">
                <span class="customer-detail-section-title">최근 메시지 이력</span>
                <span class="ds-badge">${history.length}건</span>
            </div>
            <table class="customer-history-table">
                <thead>
                    <tr>
                        <th>발송일시</th>
                        <th>제목</th>
                        <th>채널</th>
                        <th>결과</th>
                    </tr>
                </thead>
                <tbody>${historyRows}</tbody>
            </table>
        </div>
    `;
}

function renderTags(tags) {
    const normalized = normalizeTags(tags);
    if (normalized.length === 0) {
        return `<span class="ds-badge">태그 없음</span>`;
    }
    return normalized.slice(0, 6).map(tag => `<span class="ds-badge">${escapeHtml(tag)}</span>`).join("");
}

function renderTypeBadge(type) {
    const value = type || "-";
    let className = "ds-badge";
    if (value === "신규") className += " ds-badge--success";
    else if (value === "휴면") className += " ds-badge--violet";
    else if (value === "일반") className += " ds-badge--primary";
    return `<span class="${className}">${escapeHtml(value)}</span>`;
}

function renderConsent(value) {
    const isAllowed = Boolean(value);
    return `<span class="selector-status-badge ${isAllowed ? "selector-status-badge--allow" : "selector-status-badge--deny"}">${isAllowed ? "✓" : "X"}</span>`;
}

function normalizeTags(tags) {
    return Array.isArray(tags) ? tags.map(tag => String(tag).trim()).filter(Boolean) : [];
}

function findTag(tags, predicate) {
    return tags.find(tag => predicate(tag));
}

function setText(id, value) {
    const element = document.getElementById(id);
    if (element) element.textContent = value;
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString();
}

function escapeHtml(value) {
    const text = value == null ? "" : String(value);
    return text.replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#039;"
    }[char]));
}

function maskPhoneNumber(phone) {
    if (!phone) return "";
    const clean = String(phone).replace(/[^0-9]/g, "");
    if (clean.length === 11) return `${clean.slice(0, 3)}-****-${clean.slice(7)}`;
    if (clean.length === 10) return `${clean.slice(0, 3)}-***-${clean.slice(6)}`;
    return String(phone).replace(/(\d{3})-?(\d{3,4})-?(\d{4})/, "$1-****-$3");
}
