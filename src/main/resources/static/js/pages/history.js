$(function() {
    const $form = $("#historySearchForm");
    const modal = document.getElementById("historyDetailModal");
    const dialog = modal?.querySelector(".history-modal__dialog");
    const loading = document.getElementById("historyDetailLoading");
    const error = document.getElementById("historyDetailError");
    const content = document.getElementById("historyDetailContent");
    let lastFocusedElement = null;
    let detailRequest = null;

    // 검색 조건이 바뀌면 검색 폼을 자동으로 submit
    $form.find("[data-auto-submit]").on("change", function() {
        // 검색 조건이 바뀌면 첫 페이지부터 조회하도록 기존 page 값을 제거
        $form.find("input[name='page']").remove();
        $form.trigger("submit");
    });

    // 태그 조건이 바뀌면 검색 폼을 자동으로 submit
    $form.find("[data-tag-submit]").on("change", function() {
        $form.find("input[name='page']").remove();
        $form.trigger("submit");
    });

    // 목록 row 선택 시 해당 행의 상세 조회 모달을 엶
    $(".history-table").on("click", ".history-table__row[data-history-id]", function() {
        openHistoryDetail(this.dataset.historyId, this);
    });

    // 엔터나 스페이스로도 해당 row 열기 가능
    $(".history-table").on("keydown", ".history-table__row[data-history-id]", function(event) {
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault(); // 스페이스바를 눌렀을 때 페이지 스크롤이 아니라 지정한 동작을 하도록
            openHistoryDetail(this.dataset.historyId, this);
        }
    });

    $(modal).on("click", "[data-modal-close]", closeHistoryDetail);

    $(document).on("keydown", function(event) {
        if (!modal || modal.hidden) {
            return;
        }

        if (event.key === "Escape") {
            closeHistoryDetail();
            return;
        }

        // tab키 누를 경우 모달 안에서 포커스가 돌도록 함
        if (event.key === "Tab") {
            trapModalFocus(event);
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

    // 상세 조회 모달을 열고 서버에서 상세 데이터를 가져와서 모달에 채워넣음
    async function openHistoryDetail(sendHistoryId, triggerElement) {
        if (!modal || !sendHistoryId) {
            return;
        }

        // 새로운 요청을 보내기 전에 이전 요청을 취소함
        // 여러 row가 거의 동시에 클릭될때 데이터가 덮어씌워지는 상황 방지
        detailRequest?.abort();
        detailRequest = new AbortController();

        // 모달을 열었던 row 저장
        // 모달을 닫았을 때 다시 그 row로 포커스를 돌려주기 위함
        lastFocusedElement = triggerElement;

        modal.hidden = false;
        document.body.classList.add("history-modal-open");
        showLoading();
        dialog.focus();

        try {
            // sendHistoryId값에 따라 서버에 상세 데이터 요청
            const response = await fetch(`/history/${encodeURIComponent(sendHistoryId)}`, {
                headers: { "Accept": "application/json" },
                signal: detailRequest.signal // 필요시 요청을 취소할 수 있도록 함
            });

            // 서버 응답을 JSON으로 읽음, 실패할 경우 빈 객체로 처리
            const responseBody = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(responseBody.message || "상세 정보를 불러오지 못했습니다.");
            }

            renderHistoryDetail(responseBody);
            loading.hidden = true;
            content.hidden = false;
        } catch (requestError) {
            if (requestError.name === "AbortError") {  // 요청이 취소된 경우
                return;
            }
            showError(requestError.message || "상세 정보를 불러오지 못했습니다.");
        }
    }

    // 상세조회 모달을 닫는 메서드
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

    // 모달이 열려있을 때 Tab 포커스가 해당 모달 밖으로 빠지지 않도록 함
    function trapModalFocus(event) {
        // 모달 내에서 포커스가 가능한 요소들을 찾음, disabled나 보이지 않는 요소는 제외
        const focusableElements = Array.from(dialog.querySelectorAll(
            'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), ' +
            'textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter(element => element.getClientRects().length > 0);

        // 포커스 가능한 게 없으면 tab 기본 동작을 막고 dialog(모달 박스)에 포커스를 둠
        if (focusableElements.length === 0) {
            event.preventDefault();
            dialog.focus();
            return;
        }

        // 모달 안에서 포커스를 어디로 보낼지 판단하기 위한 값들
        const firstFocusable = focusableElements[0]; // 첫 번째 요소
        const lastFocusable = focusableElements[focusableElements.length - 1]; // 마지막 요소
        const activeElement = document.activeElement; // 현재 포커스가 잡혀있는 요소

        // 포커스가 모달 밖에 있으면 다시 모달 안으로
        if (!dialog.contains(activeElement) || activeElement === dialog) {
            event.preventDefault();
            (event.shiftKey ? lastFocusable : firstFocusable).focus();
            return;
        }

        if (event.shiftKey && activeElement === firstFocusable) {
            event.preventDefault();
            lastFocusable.focus();
        } else if (!event.shiftKey && activeElement === lastFocusable) {
            event.preventDefault();
            firstFocusable.focus();
        }
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

    // 서버에서 받은 상세 JSON을 모달에 넣는 메서드
    function renderHistoryDetail(detail) {
        setText("historyDetailTitle", detail.title || "-");
        setText("historyDetailScheduledAt", formatDateTime(detail.scheduledAt));
        setText("historyDetailTotalCount", `${formatNumber(detail.totalTargetCount)}건`);
        setText("historyDetailSuccessCount", `${formatNumber(detail.successCount)}건`);
        setText("historyDetailFailCount", `${formatNumber(detail.failCount)}건`);
        setText("historyDetailActualCost", formatWon(detail.actualCost));
        const savingElement = document.getElementById("historyDetailSaving");
        savingElement.textContent = formatWon(detail.estimatedSaving);
        savingElement.classList.toggle("is-positive", Number(detail.estimatedSaving ?? 0) >= 1);
        setText("historyDetailSuccessRate", `${formatRate(detail.displaySuccessRate)}%`);
        setText("historyDetailMessage", detail.content || "-");
        renderBadges(detail);
        renderTags(detail.tags || []);
        renderAttemptFlows(detail.attemptFlows || []);
    }

    // 모달 상단 배지 렌더링
    function renderBadges(detail) {
        const container = document.getElementById("historyDetailBadges");

        // 기존 배지를 비움
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

    // 태그 렌더링
    function renderTags(tags) {
        const container = document.getElementById("historyDetailTags");
        container.replaceChildren();
        if (tags.length === 0) {
            container.textContent = "-";
            return;
        }
        tags.forEach(tag => appendBadge(container, tag, "history-tag-badge"));
    }

    // 대체 발송 흐름 렌더링
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
