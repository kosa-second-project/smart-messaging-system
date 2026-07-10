const RealtimeQueueStatus = (function() {
    const DEFAULT_REFRESH_MS = 1000;

    function start(options) {
        const settings = {
            endpoint: "/api/dashboard/queue-status",
            refreshMs: DEFAULT_REFRESH_MS,
            ...options
        };
        let isLoading = false;

        function tick() {
            if (isLoading) {
                return;
            }
            isLoading = true;
            load(settings).always(function() {
                isLoading = false;
            });
        }

        tick();
        const timer = window.setInterval(function() {
            tick();
        }, settings.refreshMs);

        $(window).on("beforeunload", function() {
            window.clearInterval(timer);
        });

        return timer;
    }

    function load(settings) {
        return ApiClient.get(settings.endpoint, {
            _: Date.now()
        })
            .done(function(response) {
                renderQueueState(response || {});
                renderQueueStatus(response?.queues || []);
            })
            .fail(function() {
                renderQueueState({
                    status: "NEEDS_ATTENTION",
                    statusLabel: "조회 실패",
                    statusMessage: "RabbitMQ 큐 상태를 불러오지 못했습니다.",
                    refreshedAt: new Date().toISOString()
                });
                renderQueueStatus([]);
            });
    }

    function renderQueueState(queueState) {
        const status = String(queueState.status || "IDLE").toLowerCase().replace(/_/g, "-");
        const label = queueState.statusLabel || "현재 대기 중";
        const message = queueState.statusMessage || "처리할 메시지가 없습니다.";

        $("#dashboardQueueStateBadge")
            .removeClass("dashboard-queue-state--idle dashboard-queue-state--processing dashboard-queue-state--needs-attention")
            .addClass(`dashboard-queue-state--${status}`)
            .text(label);
        $("#dashboardQueueMessage").text(message);
        $("#dashboardQueueRefreshedAt").text(formatQueueTime(queueState.refreshedAt));
    }

    function renderQueueStatus(queueStatus) {
        const displayItems = queueStatus || [];
        const total = getQueueTotal(displayItems);

        if (displayItems.length === 0) {
            $("#dashboardQueueItems").html(`
                <div class="stats-queue-item stats-queue-item--empty">
                    <div class="stats-queue-item__top">
                        <span class="stats-queue-item__label">큐 상태 없음</span>
                    </div>
                    <div class="stats-queue-item__count">-</div>
                </div>
            `);
            $("#dashboardQueueBar").empty();
            return;
        }

        $("#dashboardQueueItems").html(displayItems.map(function(item) {
            const count = getQueueCount(item);
            const rate = total === 0 ? 0 : (count / total) * 100;
            const available = item.available !== false;
            const metaText = getQueueMetaText(item);

            return `
                <div class="stats-queue-item ${available ? "" : "is-unavailable"}" style="--queue-color:${escapeHtml(item.color || "#94A3B8")}">
                    <div class="stats-queue-item__top">
                        <div class="stats-queue-item__label-wrap">
                            <span class="stats-queue-item__dot" style="background:${escapeHtml(item.color || "#94A3B8")}"></span>
                            <span class="stats-queue-item__label">${escapeHtml(item.label)}</span>
                        </div>
                        <span class="stats-queue-item__rate">${rate.toFixed(1)}%</span>
                    </div>
                    <div class="stats-queue-item__count">${count.toLocaleString()}건</div>
                    <div class="stats-queue-item__meta">${escapeHtml(metaText)}</div>
                </div>
            `;
        }).join(""));

        $("#dashboardQueueBar").html(displayItems.filter(function(item) {
            return getQueueCount(item) > 0;
        }).map(function(item) {
            const count = getQueueCount(item);
            const rate = total === 0 ? 0 : (count / total) * 100;
            const width = total === 0 ? 0 : Math.max(4, rate);
            return `<span class="stats-queue-bar__segment" title="${escapeHtml(item.label)} ${rate.toFixed(1)}%" style="width:${width}%;background:${escapeHtml(item.color || "#94A3B8")}"></span>`;
        }).join(""));
    }

    function getQueueMetaText(item) {
        if (item.available === false) {
            return "Management API 조회 실패";
        }

        const readyCount = Number(item.readyCount || 0);
        const unackedCount = Number(item.unackedCount || 0);
        const consumerCount = Number(item.consumerCount || 0);

        return `대기 ${readyCount.toLocaleString()}건 · 처리중 ${unackedCount.toLocaleString()}건 · 컨슈머 ${consumerCount.toLocaleString()}개`;
    }

    function getQueueCount(item) {
        return Number(item?.totalCount ?? item?.readyCount ?? item?.count ?? 0);
    }

    function getQueueTotal(queueStatus) {
        return queueStatus.reduce(function(sum, item) {
            return sum + getQueueCount(item);
        }, 0);
    }

    function formatQueueTime(value) {
        const date = value ? new Date(value) : new Date();
        if (Number.isNaN(date.getTime())) {
            return "-";
        }
        return date.toLocaleTimeString("ko-KR", {
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        });
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    return {
        getQueueTotal,
        start
    };
})();
