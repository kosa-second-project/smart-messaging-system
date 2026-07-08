const DASHBOARD_CARDS_REFRESH_MS = 5000;
let dashboardCardsTimer = null;

$(function() {
    bindModal();
    loadDashboardSummary();
    loadDashboardChannelShare();
    dashboardCardsTimer = window.setInterval(loadRealtimeCards, DASHBOARD_CARDS_REFRESH_MS);
    $(window).on("beforeunload", function() {
        window.clearInterval(dashboardCardsTimer);
    });
    RealtimeQueueStatus.start();
});

function loadDashboardSummary() {
    ApiClient.get("/api/dashboard/summary").done(function(response) {
        const charts = response.charts || [];

        renderMetricCards(response.cards || []);
        renderCharts(charts);
        renderHistory(response.recentSends || []);
        renderTemplates(response.templatePerformance || []);
    });
}

function loadDashboardChannelShare() {
    const period = StatsRenderer.defaultPeriod(7);

    ApiClient.get("/api/stats/channel", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        StatsRenderer.renderCharts(response.charts, {
            channelShare: "#dashboardChannelShareChart"
        }, {
            channelShare: {
                legend: false,
                tooltipSuffix: "%"
            }
        });
        renderDashboardChannelShareLegend(StatsRenderer.findById(response.charts, "channelShare"));
    });
}

function loadRealtimeCards() {
    ApiClient.get("/api/dashboard/realtime-cards").done(function(cards) {
        renderMetricCards(cards || []);
    });
}

function renderMetricCards(cards) {
    StatsRenderer.renderCards("#dashboardStatsCards", cards, {
        icons: ["send", "check", "target", "chart", "activity"],
        colors: ["blue", "green", "violet", "amber", "green"]
    });
}

function renderCharts(charts) {
    const costChart = findChart(charts, "costComparison");
    const dailyChart = findChart(charts, "dailySendTrend");

    if (costChart) {
        StatsRenderer.renderChart(document.querySelector("#dashboardCostChart"), costChart, {
            yFormatter: StatsRenderer.formatWon,
            tooltipSuffix: "원"
        });
    }

    if (dailyChart) {
        StatsChart.composedTrend(document.querySelector("#dashboardDailyChart"), toDailyRows(dailyChart));
    }
}

function renderDashboardChannelShareLegend(chart) {
    if (!chart || !chart.datasets?.length) {
        return;
    }

    const values = chart.datasets[0].data || [];
    const colors = ["#F7E600", "#1843FA", "#10B981", "#0EA5E9"];

    $("#dashboardChannelShareLegend").html((chart.labels || []).map(function(label, index) {
        const color = colors[index % colors.length];

        return `
            <div class="stats-channel-share__legend-row">
                <span class="stats-channel-share__swatch" style="background:${color}"></span>
                <span>${escapeHtml(label)}</span>
                <span class="stats-channel-share__value">${escapeHtml(values[index])}%</span>
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
    const total = RealtimeQueueStatus.getQueueTotal(queueStatus);
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

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
