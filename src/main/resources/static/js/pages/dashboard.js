const DASHBOARD_QUEUE_REFRESH_MS = 5000;
let dashboardQueueTimer = null;

$(function() {
    bindModal();
    loadDashboardSummary();
    loadDashboardQueueStatus();
    dashboardQueueTimer = window.setInterval(loadDashboardQueueStatus, DASHBOARD_QUEUE_REFRESH_MS);
    $(window).on("beforeunload", function() {
        window.clearInterval(dashboardQueueTimer);
    });
});

function loadDashboardSummary() {
    ApiClient.get("/api/dashboard/summary").done(function(response) {
        const charts = response.charts || [];
        const channelShare = findChart(charts, "channelShare");

        renderMetricCards(response.cards || []);
        renderCharts(charts);
        renderChannelLegend(toChartEntries(channelShare));
        renderHistory(response.recentSends || []);
        renderTemplates(response.templatePerformance || []);
    });
}


function loadDashboardQueueStatus() {
    ApiClient.get("/api/dashboard/queue-status")
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
function renderMetricCards(cards) {
    StatsRenderer.renderCards("#dashboardStatsCards", cards, {
        icons: ["send", "check", "target", "chart", "activity"],
        colors: ["blue", "green", "violet", "amber", "green"]
    });
}

function renderQueueStatus(queueStatus) {
    const total = getQueueTotal(queueStatus);

    if (!queueStatus || queueStatus.length === 0) {
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

    $("#dashboardQueueItems").html(queueStatus.map(function(item) {
        const count = getQueueCount(item);
        const rate = total === 0 ? 0 : (count / total) * 100;
        const readyCount = Number(item.readyCount || 0);
        const unackedCount = Number(item.unackedCount || 0);
        const consumerCount = Number(item.consumerCount || 0);
        const available = item.available !== false;
        const metaText = available
            ? `대기 ${readyCount.toLocaleString()}건 · 처리중 ${unackedCount.toLocaleString()}건 · 소비자 ${consumerCount.toLocaleString()}개`
            : "Management API 조회 실패";
        return `
            <div class="stats-queue-item ${available ? "" : "is-unavailable"}">
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

    $("#dashboardQueueBar").html(queueStatus.map(function(item) {
        const count = getQueueCount(item);
        const rate = total === 0 ? 0 : (count / total) * 100;
        const width = total === 0 ? 0 : Math.max(count === 0 ? 2 : 4, rate);
        return `<span class="stats-queue-bar__segment" title="${escapeHtml(item.label)} ${rate.toFixed(1)}%" style="width:${width}%;background:${escapeHtml(item.color || "#94A3B8")}"></span>`;
    }).join(""));
}
function renderCharts(charts) {
    const costChart = findChart(charts, "costComparison");
    const channelChart = findChart(charts, "channelShare");
    const dailyChart = findChart(charts, "dailySendTrend");

    if (costChart) {
        StatsRenderer.renderChart(document.querySelector("#dashboardCostChart"), costChart, {
            yFormatter: StatsRenderer.formatWon,
            tooltipSuffix: "원"
        });
    }

    if (channelChart) {
        StatsChart.doughnut(document.querySelector("#dashboardChannelChart"), toChartEntries(channelChart), {
            legend: false,
            tooltipSuffix: "%"
        });
    }

    if (dailyChart) {
        StatsChart.composedTrend(document.querySelector("#dashboardDailyChart"), toDailyRows(dailyChart));
    }
}

function renderChannelLegend(channelRows) {
    $("#dashboardChannelLegend").html(channelRows.map(function(channel) {
        return `
            <div class="dashboard-channel-legend__item">
                <span class="dashboard-channel-legend__swatch" style="background:${escapeHtml(channel.color)}"></span>
                <span class="dashboard-channel-legend__label">${escapeHtml(channel.label)}</span>
                <span class="dashboard-channel-legend__value">${Number(channel.value || 0).toLocaleString()}%</span>
            </div>
        `;
    }).join(""));
}

function renderHistory(historyRows) {
    $("#dashboardHistoryList").html(historyRows.map(function(record) {
        const variant = statusVariant(record.status);
        return `
            <div class="dashboard-history-item">
                <div class="min-w-0">
                    <div class="dashboard-history-item__title" title="${escapeHtml(record.template)}">${escapeHtml(record.template)}</div>
                    <div class="dashboard-history-item__meta">${escapeHtml(record.sentAt)} · ${escapeHtml(record.targetType)}</div>
                </div>
                <div class="dashboard-history-item__side">
                    <div>${Number(record.count || 0).toLocaleString()}건</div>
                    <span class="dashboard-status-badge dashboard-status-badge--${variant}">${escapeHtml(record.status)}</span>
                </div>
            </div>
        `;
    }).join(""));
}

function renderTemplates(templateRows) {
    $("#dashboardTemplateList").html(templateRows.map(function(template, index) {
        const conversion = template.conversion === null || template.conversion === undefined ? "-" : `${template.conversion}%`;
        const sourceBadge = template.source === "AI 템플릿"
            ? '<span class="dashboard-source-badge">AI</span>'
            : "";

        return `
            <div class="dashboard-template-item">
                <span class="dashboard-template-item__rank">${index + 1}</span>
                <div class="dashboard-template-item__main">
                    <div class="dashboard-template-item__title-row">
                        <span class="dashboard-template-item__title" title="${escapeHtml(template.name)}">${escapeHtml(template.name)}</span>
                        ${sourceBadge}
                    </div>
                </div>
                <div class="dashboard-template-item__metrics">
                    <div><span>클릭률</span>${Number(template.click || 0).toLocaleString()}%</div>
                    <div><span>전환율</span>${conversion}</div>
                </div>
            </div>
        `;
    }).join(""));
}

function renderModal(queueStatus, queueJobs, processSteps) {
    const total = getQueueTotal(queueStatus);
    $("#dashboardModalSummary").html(queueStatus.map(function(item) {
        const count = Number(item.count || 0);
        const rate = total === 0 ? 0 : (count / total) * 100;
        return `
            <div class="dashboard-modal-summary__item">
                <div class="dashboard-modal-summary__top">
                    <span class="dashboard-modal-summary__label">${escapeHtml(item.label)}</span>
                    <span class="dashboard-modal-summary__dot" style="background:${escapeHtml(item.color)}"></span>
                </div>
                <div class="dashboard-modal-summary__count">${count.toLocaleString()}건</div>
                <div class="dashboard-modal-summary__rate">${rate.toFixed(1)}%</div>
            </div>
        `;
    }).join(""));

    $("#dashboardProcessSteps").html(processSteps.map(function(step, index) {
        return `
            <div class="dashboard-process-step">
                <span class="dashboard-process-step__index ${step.state ? `is-${escapeHtml(step.state)}` : ""}">${index + 1}</span>
                <div>
                    <div class="dashboard-process-step__title">${escapeHtml(step.title)}</div>
                    <div class="dashboard-process-step__status">${escapeHtml(step.status)}</div>
                </div>
            </div>
        `;
    }).join(""));

    $("#dashboardQueueJobs").html(queueJobs.map(function(job) {
        const variant = statusVariant(job.status);
        return `
            <div class="dashboard-queue-job">
                <div class="dashboard-queue-job__head">
                    <div class="min-w-0">
                        <div class="dashboard-queue-job__title" title="${escapeHtml(job.title)}">${escapeHtml(job.title)}</div>
                        <div class="dashboard-queue-job__meta">${escapeHtml(job.id)} · ${escapeHtml(job.channel)} · ${Number(job.requested || 0).toLocaleString()}건</div>
                    </div>
                    <span class="dashboard-status-badge dashboard-status-badge--${variant}">${escapeHtml(job.status)}</span>
                </div>
                <div class="dashboard-queue-job__progress">
                    <div class="dashboard-queue-job__bar" style="width:${Number(job.progress || 0)}%"></div>
                </div>
                <div class="dashboard-queue-job__foot">
                    <span>${Number(job.processed || 0).toLocaleString()}건 처리</span>
                    <span>${Number(job.progress || 0)}%</span>
                </div>
            </div>
        `;
    }).join(""));
}

function bindModal() {
    const $modal = $("[data-dashboard-modal]");

    $("[data-dashboard-modal-open]").on("click", function() {
        $modal.addClass("is-open").attr("aria-hidden", "false");
    });

    $("[data-dashboard-modal-close]").on("click", function() {
        $modal.removeClass("is-open").attr("aria-hidden", "true");
    });

    $(document).on("keydown", function(event) {
        if (event.key === "Escape") {
            $modal.removeClass("is-open").attr("aria-hidden", "true");
        }
    });
}

function findChart(charts, chartId) {
    return (charts || []).find(function(chart) {
        return chart.chartId === chartId;
    });
}

function toChartEntries(chart) {
    const colors = ["#F7E600", "#1843FA", "#10B981", "#0EA5E9", "#8B5CF6", "#EF4444"];
    const dataset = chart?.datasets?.[0] || {};
    return (chart?.labels || []).map(function(label, index) {
        return {
            label,
            value: dataset.data?.[index] || 0,
            color: colors[index % colors.length]
        };
    });
}

function toDailyRows(chart) {
    const sendDataset = chart?.datasets?.[0] || {};
    const successDataset = chart?.datasets?.[1] || {};
    return (chart?.labels || []).map(function(label, index) {
        return {
            label,
            sends: sendDataset.data?.[index] || 0,
            success: successDataset.data?.[index] || 0
        };
    });
}

function statusVariant(status) {
    if (status === "완료") {
        return "green";
    }
    if (status === "실패") {
        return "red";
    }
    return "amber";
}

function getQueueCount(item) {
    return Number(item?.totalCount ?? item?.readyCount ?? item?.count ?? 0);
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
function getQueueTotal(queueStatus) {
    return queueStatus.reduce(function(sum, item) {
        return sum + getQueueCount(item);
    }, 0);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
