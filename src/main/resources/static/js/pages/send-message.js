// ==========================================
// 4. 2단계: 메시지 작성 및 채널 라우팅 기능 제어 객체
// ==========================================
const MessageComposer = {
    channels: [],
    draggedItem: null,
    smsMaxLength: 90, // DB 연동 전 기본값
    baseEstimatedCost: 0,
    smsCost: 18,
    lmsCost: 45,
    smsCount: 0,
    channelDistribution: {},
    templateFilters: {
        keyword: "",
        categories: [],
        channelTypes: []
    },
    templateCategories: [],
    templateChannels: [],
    searchTimer: null,
    linkSettingsSnapshot: null,
    previewActionCode: "Ab3dE5gH",
    previewUnsubscribeCode: "Qr7xK2Lm",

    init: function () {
        this.loadChannels();
        this.bindEvents();
        // 1단계에서 가져온 총 대상 수 렌더링
        $("#totalTargetCount").text(SendPage.state.draftTotalCount || 0);
        const shouldRestoreMessageDraft = sessionStorage.getItem("messageEntrySource") === "review";
        if (shouldRestoreMessageDraft) {
            $("#messageTitle").val(sessionStorage.getItem("messageTitle") || "");
            $("#messageContent").val(sessionStorage.getItem("messageContent") || "");
            $("#linkButtonName").val(sessionStorage.getItem("linkButtonName") || "");
            $("#linkUrl").val(sessionStorage.getItem("linkUrl") || "");
        } else {
            $("#messageTitle").val("");
            $("#messageContent").val("");
            $("#linkButtonName").val("");
            $("#linkUrl").val("");
        }
        this.restorePurposeState(shouldRestoreMessageDraft);
        this.restoreLinkPurposeState(shouldRestoreMessageDraft);
        this.loadTemplateOptions();
        this.loadTemplates();
        this.restoreSelectedTemplate();
        $("#messageTitle").trigger("input");
        $("#messageContent").trigger("input");
    },

    getByteLength: function (text) {
        if (!text) {
            return 0;
        }
        let byteCount = 0;
        for (let i = 0; i < text.length; i++) {
            byteCount += text.charCodeAt(i) <= 0x007F ? 1 : 2;
        }
        return byteCount;
    },

    getCharacterLength: function (text) {
        return text ? text.length : 0;
    },

    normalizeChannelType: function (channelType) {
        const normalized = (channelType || "").trim().toUpperCase();
        if (normalized.startsWith("KAKAO")) {
            return "KAKAO";
        }
        return normalized;
    },

    normalizeDistribution: function (distribution) {
        const normalized = {};
        Object.entries(distribution || {}).forEach(([channelType, count]) => {
            const key = this.normalizeChannelType(channelType);
            normalized[key] = (normalized[key] || 0) + (count || 0);
        });
        return normalized;
    },

    loadChannels: function () {
        const self = this;
        $.ajax({
            url: "/api/channels/active",
            type: "GET",
            success: function (data) {
                let channels = data || [];
                let sms = channels.find(c => c.channelType && c.channelType.toUpperCase() === 'SMS');
                let lms = channels.find(c => c.channelType && c.channelType.toUpperCase() === 'LMS');

                if (sms && lms) {
                    self.smsMaxLength = sms.maxLength || 90; // DB의 SMS 최대 길이값 저장
                    self.smsCost = sms.costPerMsg || 18;
                    self.lmsCost = lms.costPerMsg || 45;
                    sms.originalType = sms.channelType;
                    sms.channelType = "문자메시지(sms/lms)";
                    const smsCost = sms.costPerMsg ? sms.costPerMsg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : "0";
                    const lmsCost = lms.costPerMsg ? lms.costPerMsg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : "0";
                    sms.displayCost = `${smsCost}원 / ${lmsCost}원`;

                    // LMS 삭제
                    channels = channels.filter(c => c.id !== lms.id);
                } else if (sms) {
                    self.smsMaxLength = sms.maxLength || 90;
                    sms.originalType = sms.channelType;
                    sms.channelType = "문자메시지(sms/lms)";
                }

                self.channels = channels;
                self.renderChannels();

                // 채널 로드가 끝난 후 비용 산출을 호출하여 데이터 일관성 보장(레이스 컨디션 방지)
                if (SendPage.state.draftId) {
                    self.loadCostEstimation(SendPage.state.draftId);
                }
            },
            error: function () {
                alert("채널 정보를 불러오는 데 실패했습니다.");
            }
        });
    },

    loadTemplateOptions: function () {
        const self = this;
        $.ajax({
            url: "/api/templates/options",
            type: "GET",
            success: function (res) {
                self.templateCategories = res.categories || [];
                self.templateChannels = res.channels || [];
                self.renderTemplateFilterControls();
                self.renderPurposeButtons(res.purposes || []);
            },
            error: function () {
                console.error("템플릿 옵션을 불러오는 데 실패했습니다.");
                self.renderPurposeButtons([]);
            }
        });
    },

    renderTemplateFilterControls: function () {
        const $categorySelect = $("#templateCategorySelect");
        $categorySelect.empty().append('<option value="">전체 카테고리</option>');
        this.templateCategories.forEach(category => {
            const disabled = this.templateFilters.categories.includes(category.value) ? "disabled" : "";
            $categorySelect.append(`<option value="${escapeHtml(category.value)}" ${disabled}>${escapeHtml(category.label)}</option>`);
        });

        const $channelSelect = $("#templateChannelSelect");
        $channelSelect.empty().append('<option value="">전체 채널</option>');
        this.templateChannels.forEach(channel => {
            const value = channel.channelType || "";
            const label = this.getChannelLabel(value);
            const disabled = this.templateFilters.channelTypes.includes(value) ? "disabled" : "";
            $channelSelect.append(`<option value="${escapeHtml(value)}" ${disabled}>${escapeHtml(label)}</option>`);
        });

        $categorySelect.val("");
        $channelSelect.val("");
        this.renderSelectedFilterTags();
    },

    renderPurposeButtons: function () {
        const $container = $("#messagePurposeToggle");
        const options = [
            { value: "AD", label: "광고성" },
            { value: "INFO", label: "정보성" }
        ];

        $container.empty();
        options.forEach((purpose, idx) => {
            const activeClass = idx === 0 ? "active" : "";
            $container.append(`<button type="button" class="purpose-btn ${activeClass}" data-val="${escapeHtml(purpose.value)}">${escapeHtml(purpose.label)}</button>`);
        });

        this.restorePurposeState();
        this.updatePurposeNotice();
        this.updatePreviewContent();
        this.updateMessageMetrics();
    },

    renderSelectedFilterTags: function () {
        const $container = $("#templateCategoryTags");
        $container.empty();
        this.templateFilters.categories.forEach(value => {
            const category = this.templateCategories.find(item => item.value === value);
            const label = category ? category.label : value;
            $container.append(this.buildFilterChip("category", value, label));
        });
        this.templateFilters.channelTypes.forEach(value => {
            $container.append(this.buildFilterChip("channel", value, this.getChannelLabel(value)));
        });
    },

    buildFilterChip: function (type, value, label) {
        return `
            <span class="filter-chip" data-filter-type="${escapeHtml(type)}" data-val="${escapeHtml(value)}">
                ${escapeHtml(label)}
                <button type="button" class="filter-chip__remove" aria-label="${escapeHtml(label)} 필터 제거">x</button>
            </span>
        `;
    },

    loadTemplates: function () {
        const self = this;
        const params = new URLSearchParams({ size: "50" });
        if (this.templateFilters.keyword) {
            params.set("keyword", this.templateFilters.keyword);
        }
        this.templateFilters.categories.forEach(category => params.append("categories", category));
        this.templateFilters.channelTypes.forEach(channelType => params.append("channelTypes", channelType));
        const url = "/api/templates?" + params.toString();

        $.ajax({
            url: url,
            type: "GET",
            success: function(res) {
                const list = res.list || [];
                const $container = $(".template-list-container");
                $container.empty();

                if (list.length === 0) {
                    $container.append('<div class="template-empty">조건에 맞는 템플릿이 없습니다.</div>');
                    $(".template-search-footer").text("0건 검색됨");
                    return;
                }

                list.forEach(item => {
                    const title = item.title || "제목 없음";
                    const content = item.content || "";
                    const categoryLabel = item.categoryDisplayName || "일반";
                    const purposeStr = item.purpose === "AD" ? "광고성" : "정보성";

                    let channelHtml = "";
                    if (item.channels && item.channels.length > 0) {
                        const firstCh = item.channels[0].channelType;
                        const normalizedCh = self.normalizeChannelType(firstCh);
                        const chClass = normalizedCh === 'KAKAO' ? 'kakao' : (normalizedCh === 'EMAIL' ? 'email' : 'sms');
                        const chName = self.getChannelLabel(normalizedCh);
                        channelHtml = `<span class="card-channel ${chClass}">${chName}</span>`;
                    }

                    const html = `
                        <div class="template-card" data-template="${escapeHtml(content)}" data-title="${escapeHtml(title)}" data-purpose="${item.purpose}">
                            <div class="card-header">
                                <h4 class="card-title">${escapeHtml(title)}</h4>
                                ${channelHtml}
                            </div>
                            <div class="card-body">
                                ${escapeHtml(content)}
                            </div>
                            <div class="card-tags">
                                <span class="tag-badge">${escapeHtml(categoryLabel)}</span>
                                <span class="tag-badge">${purposeStr}</span>
                            </div>
                        </div>
                    `;
                    $container.append(html);
                });

                $(".template-search-footer").text(list.length + "건 검색됨");
            },
            error: function() {
                console.error("템플릿 목록 로드 실패");
            }
        });
    },

    getChannelLabel: function (channelType) {
        const normalized = this.normalizeChannelType(channelType);
        if (normalized === "KAKAO") {
            return "카카오톡";
        }
        if (normalized === "EMAIL") {
            return "이메일";
        }
        if (normalized === "SMS") {
            return "SMS";
        }
        if (normalized === "LMS") {
            return "LMS";
        }
        return channelType || "채널";
    },

    renderChannels: function () {
        const $list = $("#channelSortableList");
        $list.empty();

        if (this.channels.length === 0) {
            $list.append('<li class="channel-empty">활성화된 채널이 없습니다.</li>');
            return;
        }

        this.channels.forEach((ch, index) => {
            const safeName = escapeHtml(ch.channelType);
            const basicCost = ch.costPerMsg ? ch.costPerMsg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : "0";
            const safeCostStr = ch.displayCost ? `<b>${escapeHtml(ch.displayCost)}</b>` : `<b>${basicCost}</b>원`;

            // 우선순위 뱃지 (1순위, 2순위, 3순위 ...)
            let badgeClass = "priority-disabled";
            if (index === 0) badgeClass = "priority-1";
            else if (index === 1) badgeClass = "priority-2";
            else if (index === 2) badgeClass = "priority-3";

            const html = `
                <li class="channel-card" draggable="true" data-index="${index}" data-id="${ch.id}">
                    <div class="channel-info">
                        <div class="drag-handle">☰</div>
                        <div class="channel-name">${safeName}</div>
                        <div class="channel-meta">건당 ${safeCostStr}</div>
                    </div>
                    <div class="priority-badge ${badgeClass}">${index + 1}순위</div>
                </li>
            `;
            $list.append(html);
        });

        this.bindDragEvents();
    },

    bindDragEvents: function () {
        const self = this;
        const $cards = $(".channel-card");

        $cards.on("dragstart", function (e) {
            self.draggedItem = this;
            $(this).addClass("dragging");
            e.originalEvent.dataTransfer.effectAllowed = "move";
        });

        $cards.on("dragover", function (e) {
            e.preventDefault();
            e.originalEvent.dataTransfer.dropEffect = "move";

            const target = $(this).closest(".channel-card")[0];
            if (target && target !== self.draggedItem) {
                const bounding = target.getBoundingClientRect();
                const offset = bounding.y + (bounding.height / 2);
                if (e.clientY - offset > 0) {
                    $(target).after(self.draggedItem);
                } else {
                    $(target).before(self.draggedItem);
                }
            }
        });

        $cards.on("dragend", function () {
            $(this).removeClass("dragging");
            self.draggedItem = null;
            self.updateChannelOrder();
        });
    },

    updateChannelOrder: function () {
        const newOrderIds = [];
        $(".channel-card").each(function () {
            newOrderIds.push($(this).data("id"));
        });

        // channels 배열 재배치
        const reordered = [];
        const priorities = [];
        newOrderIds.forEach(id => {
            const found = this.channels.find(c => c.id === id);
            if (found) {
                reordered.push(found);
                priorities.push(found.originalType || found.channelType);
            }
        });

        this.channels = reordered;
        this.renderChannels(); // 뱃지 재렌더링

        if (SendPage.state.draftId) {
            this.loadCostEstimation(SendPage.state.draftId, priorities);
        }
    },

    bindEvents: function () {
        const self = this;

        // 최저가 자동정렬 버튼
        $("#btnSmartSort").on("click", function () {
            self.channels.sort((a, b) => (a.costPerMsg || 0) - (b.costPerMsg || 0));
            self.renderChannels();
            // 정렬된 순서를 반영하여 예상 비용 재계산 호출
            self.updateChannelOrder();
        });

        // 미리보기 탭 스위칭 이벤트
        $(".preview-tab-btn").on("click", function () {
            $(".preview-tab-btn").removeClass("active");
            $(this).addClass("active");

            const target = $(this).data("target"); // sms, kakao, email
            const $box = $("#phonePreviewBox");
            $box.removeClass("mode-sms mode-kakao mode-email");
            $box.addClass("mode-" + target);
        });

        // 제목 입력 실시간 미리보기 동기화
        $("#messageTitle").on("input", function () {
            const title = $(this).val() || "메시지 제목";
            $("#previewSmsTitle").text(title);
            $("#previewKakaoTitle").text(title);
            $("#previewEmailTitle").text(title);
            self.updateMessageMetrics();
        });

        // 텍스트 바이트 수 계산 및 미리보기 화면 텍스트 동기화
        $("#messageContent").on("input", function () {
            const actualText = $(this).val();

            self.updatePreviewContent();
            self.updateMessageMetrics();
        });

        // 템플릿 카드 클릭 시 제목과 내용 자동 완성 및 미리보기 연동
        $(document).on("click", ".template-card", function () {
            const title = $(this).attr("data-title") || "";
            const content = $(this).attr("data-template") || "";
            const purpose = $(this).attr("data-purpose");

            // 입력 필드 세팅
            $("#messageTitle").val(title);
            $("#messageContent").val(content);

            // 실시간 미리보기 업데이트
            $("#messageTitle").trigger("input");
            $("#messageContent").trigger("input");

            // 광고여부 버튼 연동
            if (purpose) {
                $(".purpose-btn").removeClass("active");
                $(`.purpose-btn[data-val="${purpose}"]`).addClass("active");
                self.updatePurposeNotice();
                self.updatePreviewContent();
                self.updateMessageMetrics();
            }

            // 시각적 피드백 (선택 효과)
            $(".template-card").removeClass("active-card");
            $(this).addClass("active-card");
            self.updateSelectedTemplate(title, content);
        });

        $("#templateSearchInput").on("input", function () {
            window.clearTimeout(self.searchTimer);
            self.searchTimer = window.setTimeout(function () {
                self.templateFilters.keyword = ($("#templateSearchInput").val() || "").trim();
                self.loadTemplates();
            }, 250);
        });

        $("#templateCategorySelect").on("change", function () {
            self.addTemplateFilter("categories", $(this).val() || "");
        });

        $("#templateChannelSelect").on("change", function () {
            self.addTemplateFilter("channelTypes", $(this).val() || "");
        });

        $(document).on("click", ".filter-chip__remove", function () {
            const $chip = $(this).closest(".filter-chip");
            self.removeTemplateFilter($chip.data("filter-type"), $chip.data("val"));
        });

        $(document).on("click", ".purpose-btn", function() {
            $(".purpose-btn").removeClass("active");
            $(this).addClass("active");
            self.updatePurposeNotice();
            self.updatePreviewContent();
            self.updateMessageMetrics();
        });

        $("#btnSaveTemplate").on("click", function () {
            self.saveCurrentTemplate();
        });

        $("#btnSendTestMessage").on("click", function () {
            self.sendTestMessageToMe();
        });

        $("#btnToggleLinkSettings").on("click", function (event) {
            event.stopPropagation();
            self.toggleLinkSettings();
        });

        $("#btnApplyLinkSettings").on("click", function () {
            self.commitLinkSettingsSnapshot();
            self.closeLinkSettings();
        });

        $("#btnCancelLinkSettings").on("click", function () {
            self.restoreLinkSettingsSnapshot();
            self.closeLinkSettings();
        });

        $("#linkSettingsPanel").on("click", function (event) {
            event.stopPropagation();
        });

        $(document).on("click", function () {
            self.restoreLinkSettingsSnapshot();
            self.closeLinkSettings();
        });

        $(document).on("keydown", function (event) {
            if (event.key === "Escape") {
                self.restoreLinkSettingsSnapshot();
                self.closeLinkSettings();
            }
        });

        $("#linkButtonName, #linkUrl").on("input", function () {
            self.updatePreviewContent();
            self.updateMessageMetrics();
        });

        $(".link-purpose-btn").on("click", function () {
            $(".link-purpose-btn").removeClass("active");
            $(this).addClass("active");
            self.updatePreviewContent();
            self.updateMessageMetrics();
        });

        $(document).on("click", ".js-insert-variable", function () {
            self.insertVariable($(this).data("variable") || "");
        });
    },

    addTemplateFilter: function (filterKey, value) {
        if (!value || !Array.isArray(this.templateFilters[filterKey])) {
            this.renderTemplateFilterControls();
            return;
        }
        if (!this.templateFilters[filterKey].includes(value)) {
            this.templateFilters[filterKey].push(value);
        }
        this.renderTemplateFilterControls();
        this.loadTemplates();
    },

    removeTemplateFilter: function (filterType, value) {
        const filterKey = filterType === "channel" ? "channelTypes" : "categories";
        if (!Array.isArray(this.templateFilters[filterKey])) {
            return;
        }
        this.templateFilters[filterKey] = this.templateFilters[filterKey].filter(item => item !== value);
        this.renderTemplateFilterControls();
        this.loadTemplates();
    },

    toggleLinkSettings: function () {
        const $panel = $("#linkSettingsPanel");
        const isOpen = $panel.hasClass("is-open");
        if (isOpen) {
            this.restoreLinkSettingsSnapshot();
            this.closeLinkSettings();
            return;
        }
        this.captureLinkSettingsSnapshot();
        $panel.addClass("is-open");
        $("#btnToggleLinkSettings").attr("aria-expanded", "true");
    },

    closeLinkSettings: function () {
        $("#linkSettingsPanel").removeClass("is-open");
        $("#btnToggleLinkSettings").attr("aria-expanded", "false");
    },

    captureLinkSettingsSnapshot: function () {
        this.linkSettingsSnapshot = {
            buttonName: $("#linkButtonName").val() || "",
            url: $("#linkUrl").val() || "",
            purpose: $(".link-purpose-btn.active").data("purpose") || "CLICK"
        };
    },

    commitLinkSettingsSnapshot: function () {
        this.captureLinkSettingsSnapshot();
        this.updatePreviewContent();
        this.updateMessageMetrics();
    },

    restoreLinkSettingsSnapshot: function () {
        if (!this.linkSettingsSnapshot) {
            return;
        }
        $("#linkButtonName").val(this.linkSettingsSnapshot.buttonName);
        $("#linkUrl").val(this.linkSettingsSnapshot.url);
        $(".link-purpose-btn").removeClass("active");
        $(`.link-purpose-btn[data-purpose="${this.linkSettingsSnapshot.purpose}"]`).addClass("active");
        this.updatePreviewContent();
        this.updateMessageMetrics();
    },

    loadCostEstimation: function (draftId, priorities = []) {
        const self = this;

        // 로딩 피드백 제공
        $("#estimatedTotalCost").text("계산 중...");
        $("#channelDistributionList").empty().append('<li class="cost-distribution-item cost-distribution-item--loading">재계산 중입니다...</li>');

        $.ajax({
            url: `/api/campaigns/draft/${draftId}/estimate-cost`,
            type: "GET",
            data: { priorities: priorities },
            traditional: true,
            success: function (res) {
                if (!res) return;

                // 1. 총 비용 표시 (동적 계산을 위해 변수에 저장)
                self.baseEstimatedCost = res.totalEstimatedCost || 0;
                self.channelDistribution = self.normalizeDistribution(res.channelDistribution || {});
                self.smsCount = self.channelDistribution["SMS"] || 0;

                self.updateCostUI(self.isCurrentLmsMessage());
            },
            error: function (err) {
                if (err.status === 404) {
                    alert("발송 세션이 만료되었거나 존재하지 않습니다. 수신자 선택 단계로 돌아갑니다.");
                    if (typeof SendPage !== "undefined") {
                        SendPage.clearSendSession();
                    }
                    window.location.href = "/send/recipients";
                } else {
                    console.error("예상 비용 산출 실패");
                }
            }
        });
    },

    updateCostUI: function (isLms) {
        let extraCost = 0;
        if (isLms) {
            extraCost = (this.lmsCost - this.smsCost) * this.smsCount;
        }
        const totalCost = this.baseEstimatedCost + extraCost;
        $("#estimatedTotalCost").text(totalCost.toLocaleString());

        // 2. 채널 분포 리스트 표시 및 SMS -> LMS 변환
        const $distList = $("#channelDistributionList");
        $distList.empty();

        const dist = { ...this.channelDistribution };

        if (isLms && dist["SMS"] > 0) {
            // SMS의 인원을 전부 LMS로 이동
            dist["LMS"] = (dist["LMS"] || 0) + dist["SMS"];
            dist["SMS"] = 0;
        }

        // 항상 표시할 주요 채널들
        const allKeys = ["EMAIL", "SMS", "LMS", "KAKAO", "UNASSIGNED"];
        const self = this;

        allKeys.forEach(channelType => {
            const count = dist[channelType] || 0;

            // 배정 제외는 0명이면 굳이 표시하지 않음
            if (channelType === "UNASSIGNED" && count === 0) return;

            let label = channelType;
            let costPerMsg = 0;

            if (channelType === "KAKAO") {
                label = "카카오톡";
                const ch = self.channels.find(c => self.normalizeChannelType(c.channelType) === "KAKAO");
                if (ch) costPerMsg = ch.costPerMsg || 0;
            }
            else if (channelType === "SMS") { label = "SMS"; costPerMsg = self.smsCost; }
            else if (channelType === "LMS") { label = "LMS"; costPerMsg = self.lmsCost; }
            else if (channelType === "EMAIL") {
                label = "이메일";
                const ch = self.channels.find(c => self.normalizeChannelType(c.channelType) === "EMAIL");
                if (ch) costPerMsg = ch.costPerMsg || 0;
            }
            else if (channelType === "UNASSIGNED") { label = "배정 불가"; }

            let totalChCost = count * costPerMsg;
            const formattedCost = totalChCost > 0 ? totalChCost.toLocaleString(undefined, { maximumFractionDigits: 1 }) + "원" : "0원";

            const itemHtml = `
                <li class="cost-distribution-item">
                    <span class="cost-distribution-label">${escapeHtml(label)}</span>
                    <span class="cost-distribution-value">${count}명</span>
                    <span class="cost-distribution-cost">${formattedCost}</span>
                </li>
            `;
            $distList.append(itemHtml);
        });
    },

    restorePurposeState: function (shouldRestore = true) {
        if (!shouldRestore) {
            sessionStorage.removeItem("messagePurpose");
            $(".purpose-btn").removeClass("active");
            $(".purpose-btn[data-val='AD']").addClass("active");
            return;
        }
        const savedPurpose = sessionStorage.getItem("messagePurpose");
        if (!savedPurpose) {
            return;
        }
        $(".purpose-btn").removeClass("active");
        $(`.purpose-btn[data-val="${savedPurpose}"]`).addClass("active");
    },

    restoreLinkPurposeState: function (shouldRestore = true) {
        if (!shouldRestore) {
            sessionStorage.removeItem("linkPurpose");
            $(".link-purpose-btn").removeClass("active");
            $(".link-purpose-btn[data-purpose='CLICK']").addClass("active");
            return;
        }
        const savedPurpose = sessionStorage.getItem("linkPurpose") || "CLICK";
        $(".link-purpose-btn").removeClass("active");
        $(`.link-purpose-btn[data-purpose="${savedPurpose}"]`).addClass("active");
    },

    updatePurposeNotice: function () {
        const purpose = $(".purpose-btn.active").data("val") || "INFO";
        const $notice = $("#messagePurposeNotice");
        if (purpose === "AD") {
            $notice
                .removeClass("message-purpose-notice--info")
                .addClass("message-purpose-notice--ad")
                .text("광고성 문자에는 고객별 수신거부 링크가 자동으로 추가됩니다.");
            $(".preview-ad-badge").text("광고성").removeClass("preview-ad-badge--info");
            return;
        }
        $notice
            .removeClass("message-purpose-notice--ad")
            .addClass("message-purpose-notice--info")
            .text("정보성 메시지에는 광고 문구 사용을 제한해주세요.");
        $(".preview-ad-badge").text("정보성").addClass("preview-ad-badge--info");
    },

    updatePreviewContent: function () {
        const preview = this.buildPreviewParts(true);

        $("#previewSmsContent").text(preview.body);
        $("#previewKakaoContent").text(preview.body);
        $("#previewSmsLinks").text(preview.actionLink);
        $("#previewKakaoLinks").text(preview.actionLink);
        $("#previewSmsUnsubscribe").text(preview.unsubscribe);
        $("#previewKakaoUnsubscribe").text(preview.unsubscribe);
        $("#previewEmailContent").text(this.buildPreviewMessageText(true));
    },

    updateMessageMetrics: function () {
        const characterCount = this.getCharacterLength(this.buildPreviewMetricText());
        $("#currentBytes").text(characterCount);

        const isLms = this.isCurrentLmsMessage();
        const $badge = $("#msgTypeBadge");
        if (isLms) {
            $badge.text("LMS")
                .removeClass("ds-badge--primary")
                .addClass("ds-badge--secondary msg-type-badge--lms");
        } else {
            $badge.text("SMS")
                .removeClass("ds-badge--secondary msg-type-badge--lms")
                .addClass("ds-badge--primary");
        }
        this.updateCostUI(isLms);
    },

    buildPreviewMessageText: function (usePlaceholder) {
        const parts = this.buildPreviewParts(usePlaceholder);
        let text = parts.body;
        if (parts.actionLink) {
            text += "\n\n" + parts.actionLink;
        }
        if (parts.unsubscribe) {
            text += "\n\n" + parts.unsubscribe;
        }
        return text;
    },

    buildPreviewMetricText: function () {
        const title = ($("#messageTitle").val() || "").trim();
        const bodyText = this.buildPreviewMessageText(false);
        if (!title) {
            return bodyText;
        }
        return title + bodyText;
    },

    buildPreviewParts: function (usePlaceholder) {
        const body = $("#messageContent").val() || (usePlaceholder ? "전송할 메시지 내용을 입력해주세요." : "");
        const buttonName = ($("#linkButtonName").val() || "자세히 보기").trim();
        const linkUrl = ($("#linkUrl").val() || "").trim();
        const purpose = $(".purpose-btn.active").data("val") || "INFO";
        const parts = {
            body: body,
            actionLink: "",
            unsubscribe: ""
        };

        if (linkUrl) {
            parts.actionLink = buttonName + "\n" + this.getPreviewShortUrl("r");
        }
        if (purpose === "AD") {
            parts.unsubscribe = "수신거부를 원하시면 아래 링크를 눌러주세요.\n" + this.getPreviewShortUrl("u");
        }
        return parts;
    },

    getPreviewShortUrl: function (path) {
        const code = path === "u" ? this.previewUnsubscribeCode : this.previewActionCode;
        return "https://kosa.kr/" + path + "/" + code;
    },

    hasMessageTitle: function () {
        return !!($("#messageTitle").val() || "").trim();
    },

    isCurrentLmsMessage: function () {
        return this.hasMessageTitle() || this.getByteLength(this.buildPreviewMessageText(false)) > this.smsMaxLength;
    },

    updateSelectedTemplate: function (title, content) {
        const safeTitle = title || "제목 없음";
        const safeContent = content || "";
        $("#selectedTemplateName").text(safeTitle);
        $("#selectedTemplatePreview").text(safeContent);
        $("#selectedTemplateAlert").removeClass("is-hidden");
        sessionStorage.setItem("selectedTemplateTitle", safeTitle);
        sessionStorage.setItem("selectedTemplateContent", safeContent);
    },

    restoreSelectedTemplate: function () {
        const title = sessionStorage.getItem("selectedTemplateTitle");
        const content = sessionStorage.getItem("selectedTemplateContent");
        if (!title) {
            return;
        }
        $("#selectedTemplateName").text(title);
        $("#selectedTemplatePreview").text(content || "");
        $("#selectedTemplateAlert").removeClass("is-hidden");
    },

    saveCurrentTemplate: function () {
        const title = ($("#messageTitle").val() || "").trim();
        const content = ($("#messageContent").val() || "").trim();
        const purpose = $(".purpose-btn.active").data("val") || "INFO";

        if (!title) {
            alert("템플릿 제목을 입력해주세요.");
            $("#messageTitle").focus();
            return;
        }
        if (!content) {
            alert("템플릿 내용을 입력해주세요.");
            $("#messageContent").focus();
            return;
        }

        const $button = $("#btnSaveTemplate");
        const originalText = $button.html();
        $button.prop("disabled", true).text("저장 중...");

        $.ajax({
            url: "/api/templates",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                title: title,
                content: content,
                isAiGenerated: false,
                category: "CRM",
                purpose: purpose,
                channelIds: this.getCurrentChannelIds()
            }),
            success: () => {
                alert("템플릿이 저장되었습니다.");
                this.loadTemplates();
            },
            error: (xhr) => {
                const response = xhr.responseJSON || {};
                alert(response.message || "템플릿 저장에 실패했습니다.");
            },
            complete: () => {
                $button.prop("disabled", false).html(originalText);
            }
        });
    },

    sendTestMessageToMe: function () {
        const title = ($("#messageTitle").val() || "").trim();
        const content = ($("#messageContent").val() || "").trim();
        const purpose = $(".purpose-btn.active").data("val") || "INFO";
        const linkButtonName = ($("#linkButtonName").val() || "").trim();
        const linkUrl = ($("#linkUrl").val() || "").trim();
        const linkPurpose = $(".link-purpose-btn.active").data("purpose") || "CLICK";

        if (!title) {
            alert("테스트 발송 전에 제목을 입력해주세요.");
            $("#messageTitle").focus();
            return;
        }
        if (!content) {
            alert("테스트 발송 전에 내용을 입력해주세요.");
            $("#messageContent").focus();
            return;
        }
        if (!linkUrl) {
            alert("클릭 확인을 위해 링크 URL을 입력해주세요.");
            $("#linkUrl").focus();
            this.openLinkSettings();
            return;
        }
        if (!/^https?:\/\//i.test(linkUrl)) {
            alert("링크 URL은 http 또는 https로 시작해야 합니다.");
            $("#linkUrl").focus();
            this.openLinkSettings();
            return;
        }
        if (!window.confirm("내 카카오 계정으로 테스트 메시지를 발송할까요?")) {
            return;
        }

        const $button = $("#btnSendTestMessage");
        const originalText = $button.text();
        $button.prop("disabled", true).text("테스트 발송 중...");

        $.ajax({
            url: "/api/dev/message-test",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                title: title,
                content: content,
                purpose: purpose,
                linkButtonName: linkButtonName,
                linkUrl: linkUrl,
                linkPurpose: linkPurpose
            }),
            success: function (res) {
                alert(MessageComposer.buildTestSendResultMessage(res));
            },
            error: function (xhr) {
                const response = xhr.responseJSON || {};
                alert(response.message || "테스트 발송에 실패했습니다.");
            },
            complete: function () {
                $button.prop("disabled", false).text(originalText);
            }
        });
    },

    openLinkSettings: function () {
        this.captureLinkSettingsSnapshot();
        $("#linkSettingsPanel").addClass("is-open");
        $("#btnToggleLinkSettings").attr("aria-expanded", "true");
    },

    buildTestSendResultMessage: function (res) {
        const lines = [
            "테스트 발송 요청이 완료되었습니다.",
            "대상: " + (res.customerName || "테스트 고객"),
            "",
            this.formatChannelResult("문자", res.smsResult, res.smsActionUrl),
            this.formatChannelResult("이메일", res.emailResult, res.emailActionUrl),
            this.formatChannelResult("카카오", res.kakaoResult, res.kakaoActionUrl)
        ];
        return lines.join("\n");
    },

    formatChannelResult: function (label, result, actionUrl) {
        if (!result) {
            return label + ": 결과 없음";
        }
        if (result.success) {
            return label + ": 성공\n" + actionUrl;
        }
        if (result.errorCode === "SKIPPED") {
            return label + ": 스킵 - " + (result.errorMessage || "이번 테스트 발송 대상이 아닙니다.");
        }
        return label + ": 실패 - " + (result.errorMessage || result.errorCode || "알 수 없는 오류");
    },

    getCurrentChannelIds: function () {
        return this.channels
            .map(channel => channel.id)
            .filter(Boolean);
    },

    insertVariable: function(val) {
        const $textarea = $("#messageContent");
        const dom = $textarea[0];
        if (!dom || !val) {
            return;
        }

        const start = dom.selectionStart;
        const end = dom.selectionEnd;
        const text = $textarea.val() || "";
        const newText = text.substring(0, start) + val + text.substring(end);
        $textarea.val(newText);

        dom.selectionStart = dom.selectionEnd = start + val.length;
        $textarea.focus();
        $textarea.trigger("input");
    }
};
