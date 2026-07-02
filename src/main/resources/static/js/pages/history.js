$(function() {
    const $form = $("#historySearchForm");
    const modal = document.getElementById("historyDetailModal");
    const dialog = modal?.querySelector(".history-modal__dialog");
    const loading = document.getElementById("historyDetailLoading");
    const error = document.getElementById("historyDetailError");
    const content = document.getElementById("historyDetailContent");
    let lastFocusedElement = null;
    let detailRequest = null;

    $form.find("[data-auto-submit]").on("change", function() {
        $form.find("input[name='page']").remove();
        $form.trigger("submit");
    });

    $form.find("[data-tag-submit]").on("change", function() {
        $form.find("input[name='page']").remove();
        $form.trigger("submit");
    });

    $(".history-table").on("click", ".history-table__row[data-history-id]", function() {
        openHistoryDetail(this.dataset.historyId, this);
    });

    $(".history-table").on("keydown", ".history-table__row[data-history-id]", function(event) {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            openHistoryDetail(this.dataset.historyId, this);
        }
    });

    $(modal).on("click", "[data-modal-close]", closeHistoryDetail);

    $(document).on("keydown", function(event) {
        if (event.key === "Escape" && modal && !modal.hidden) {
            closeHistoryDetail();
        }
    });

    $("#historyRetryButton").on("click", function() {
        const icon = this.querySelector(".history-retry-button__icon");
        icon.classList.remove("is-spinning");
        void icon.offsetWidth;
        icon.classList.add("is-spinning");
    });

    $("#historyRetryButton .history-retry-button__icon").on("animationend", function() {
        this.classList.remove("is-spinning");
    });

    async function openHistoryDetail(sendHistoryId, triggerElement) {
        if (!modal || !sendHistoryId) {
            return;
        }

        detailRequest?.abort();
        detailRequest = new AbortController();
        lastFocusedElement = triggerElement;
        modal.hidden = false;
        document.body.classList.add("history-modal-open");
        showLoading();
        dialog.focus();

        try {
            const response = await fetch(`/history/${encodeURIComponent(sendHistoryId)}`, {
                headers: { "Accept": "application/json" },
                signal: detailRequest.signal
            });
            const responseBody = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(responseBody.message || "상세 정보를 불러오지 못했습니다.");
            }

            renderHistoryDetail(responseBody);
            loading.hidden = true;
            content.hidden = false;
        } catch (requestError) {
            if (requestError.name === "AbortError") {
                return;
            }
            showError(requestError.message || "상세 정보를 불러오지 못했습니다.");
        }
    }

    function closeHistoryDetail() {
        if (!modal || modal.hidden) {
            return;
        }
        detailRequest?.abort();
        detailRequest = null;
        modal.hidden = true;
        document.body.classList.remove("history-modal-open");
        lastFocusedElement?.focus();
    }

    function showLoading() {
        loading.hidden = false;
        error.hidden = true;
        error.textContent = "";
        content.hidden = true;
    }

    function showError(message) {
        loading.hidden = true;
        content.hidden = true;
        error.textContent = message;
        error.hidden = false;
    }

    function renderHistoryDetail(detail) {
        setText("historyDetailTitle", detail.title || "-");
        setText("historyDetailScheduledAt", formatDateTime(detail.scheduledAt));
        setText("historyDetailTotalCount", `${formatNumber(detail.totalTargetCount)}건`);
        setText("historyDetailSuccessCount", `${formatNumber(detail.successCount)}건`);
        setText("historyDetailFailCount", `${formatNumber(detail.failCount)}건`);
        setText("historyDetailActualCost", formatWon(detail.actualCost));
        setText("historyDetailSaving", formatWon(detail.estimatedSaving));
        setText("historyDetailSuccessRate", `${formatRate(detail.displaySuccessRate)}%`);
        setText("historyDetailMessage", detail.content || "-");
        renderBadges(detail);
        renderTags(detail.tags || []);
        renderAttemptFlows(detail.attemptFlows || []);
    }

    function renderBadges(detail) {
        const container = document.getElementById("historyDetailBadges");
        container.replaceChildren();

        const channels = detail.channels || [];
        if (channels.length === 0) {
            appendBadge(container, "-");
        } else {
            channels.forEach(channel => appendBadge(container, channel, "ds-badge--primary"));
        }
        appendBadge(container, detail.purposeLabel || "-");
        appendBadge(container, detail.statusLabel || "-", statusClassOf(detail.status));
    }

    function renderTags(tags) {
        const container = document.getElementById("historyDetailTags");
        container.replaceChildren();
        if (tags.length === 0) {
            container.textContent = "-";
            return;
        }
        tags.forEach(tag => appendBadge(container, tag, "history-tag-badge"));
    }

    function renderAttemptFlows(attemptFlows) {
        const container = document.getElementById("historyAttemptFlows");
        container.replaceChildren();

        if (attemptFlows.length === 0) {
            const empty = document.createElement("p");
            empty.className = "history-attempt-flows__empty";
            empty.textContent = "-";
            container.appendChild(empty);
            return;
        }

        attemptFlows.forEach(flow => {
            const item = document.createElement("article");
            item.className = "history-attempt-flow";

            const order = document.createElement("span");
            order.className = "history-attempt-flow__order";
            order.textContent = flow.attemptOrder ?? "-";

            const flowContent = document.createElement("div");
            flowContent.className = "history-attempt-flow__content";
            const title = document.createElement("strong");
            title.textContent = `${flow.attemptOrder ?? "-"}차 ${flow.channelName || "-"}`;
            const counts = document.createElement("div");
            counts.className = "history-attempt-flow__counts";
            counts.append(
                createCount(`요청 ${formatNumber(flow.requestCount)}건`),
                createCount(`성공 ${formatNumber(flow.successCount)}건`),
                createCount(`실패 ${formatNumber(flow.failCount)}건`)
            );
            flowContent.append(title, counts);
            item.append(order, flowContent);
            container.appendChild(item);
        });
    }

    function appendBadge(container, label, extraClass) {
        const badge = document.createElement("span");
        badge.className = `ds-badge${extraClass ? ` ${extraClass}` : ""}`;
        badge.textContent = label;
        container.appendChild(badge);
    }

    function createCount(text) {
        const count = document.createElement("span");
        count.textContent = text;
        return count;
    }

    function setText(id, value) {
        document.getElementById(id).textContent = value;
    }

    function formatNumber(value) {
        const number = Number(value ?? 0);
        return Number.isFinite(number) ? number.toLocaleString("ko-KR") : "0";
    }

    function formatWon(value) {
        return `${formatNumber(value)}원`;
    }

    function formatRate(value) {
        const rate = Number(value ?? 0);
        return Number.isFinite(rate) ? rate.toFixed(1) : "0.0";
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }
        const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
        return match ? `${match[1]}.${match[2]}.${match[3]} ${match[4]}:${match[5]}` : String(value);
    }

    function statusClassOf(status) {
        const statusClasses = {
            SCHEDULED: "history-status history-status--scheduled",
            SENDING: "history-status history-status--sending",
            SENT: "history-status history-status--completed",
            FAILED: "history-status history-status--failed"
        };
        return statusClasses[String(status || "").trim().toUpperCase()]
            || "history-status history-status--default";
    }
});
