$(function() {
    const state = {
        period: {
            start: "2026-06-23",
            end: "2026-06-29"
        }
    };

    bindEvents(state);
    loadDeliveryReport(state);
});

function bindEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = ReportRenderer.getPeriod(state.period);
        loadDeliveryReport(state);
    });
}

function loadDeliveryReport(state) {
    const period = ReportRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(ReportRenderer.periodLabel(period));

    ApiClient.get("/api/reports/delivery", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        ReportRenderer.renderCards("#statsCards", response.cards, {
            icons: ["send", "check", "activity", "target", "refresh"],
            colors: ["blue", "green", "violet", "amber", "green"]
        });
        ReportRenderer.renderCharts(response.charts, {
            channelTrend: "#channelTrendChart",
            sendSuccessTrend: "#sendTrendChart",
            fallbackSuccess: "#fallbackChart"
        }, {
            sendSuccessTrend: {
                kind: "composedTrend"
            },
            fallbackSuccess: {
                yMin: 90,
                yMax: 100,
                tooltipSuffix: "%",
                yFormatter: function(value) {
                    return `${value}%`;
                }
            }
        });
        renderQueueStatus();
    });
}

function renderQueueStatus() {
    const queueStatus = [
        { label: "대기", count: 0, color: "#94A3B8" },
        { label: "발송 중", count: 2500, color: "#3B82F6" },
        { label: "완료", count: 12847, color: "#10B981" },
        { label: "실패", count: 165, color: "#EF4444" }
    ];
    const total = queueStatus.reduce(function(sum, item) {
        return sum + item.count;
    }, 0);

    $("#statsQueueItems").html(queueStatus.map(function(item) {
        const rate = total === 0 ? 0 : (item.count / total) * 100;
        return `
            <div class="stats-queue-item">
                <div class="stats-queue-item__top">
                    <div class="stats-queue-item__label-wrap">
                        <span class="stats-queue-item__dot" style="background:${item.color}"></span>
                        <span class="stats-queue-item__label">${item.label}</span>
                    </div>
                    <span class="stats-queue-item__rate">${rate.toFixed(1)}%</span>
                </div>
                <div class="stats-queue-item__count">${item.count.toLocaleString()}건</div>
            </div>
        `;
    }).join(""));

    $("#statsQueueBar").html(queueStatus.map(function(item) {
        const rate = total === 0 ? 0 : (item.count / total) * 100;
        return `<span class="stats-queue-bar__segment" title="${item.label} ${rate.toFixed(1)}%" style="width:${Math.max(item.count === 0 ? 2 : 4, rate)}%;background:${item.color}"></span>`;
    }).join(""));
}
