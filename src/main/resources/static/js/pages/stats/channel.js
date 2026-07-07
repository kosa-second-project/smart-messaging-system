$(function() {
    const state = {
        period: StatsRenderer.defaultPeriod(7)
    };

    bindChannelEvents(state);
    loadChannelStats(state);
});

function bindChannelEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = StatsRenderer.getPeriod(state.period);
        loadChannelStats(state);
    });
}

function loadChannelStats(state) {
    const period = StatsRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(StatsRenderer.periodLabel(period));

    ApiClient.get("/api/stats/channel", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        StatsRenderer.renderCharts(response.charts, {
            channelSuccessRate: "#channelSuccessChart",
            channelTrend: "#channelTrendChart",
            channelShare: "#channelShareChart"
        }, {
            channelSuccessRate: {
                kind: "horizontalBar",
                xMin: 0,
                xMax: 100,
                tooltipSuffix: "%",
                yFormatter: function(value) {
                    return `${value}%`;
                }
            },
            channelShare: {
                legend: false,
                tooltipSuffix: "%"
            }
        });
        renderChannelShareLegend(StatsRenderer.findById(response.charts, "channelShare"));
        renderChannelCost(StatsRenderer.findById(response.tables, "channelCost"));
    });
}

function renderChannelCost(table) {
    if (!table) {
        return;
    }

    $("#channelCostCards").html(table.rows.map(function(row) {
        return `
            <div class="stats-channel-mobile-card">
                <div class="stats-channel-mobile-card__top">
                    <div class="stats-channel-mobile-card__title">${escapeHtml(row[0])}</div>
                    <div class="stats-channel-mobile-card__rate">${escapeHtml(row[2])} 성공</div>
                </div>
                <div class="stats-channel-mobile-card__grid">
                    <div>
                        <div class="stats-channel-mobile-card__label">발송량</div>
                        <div class="stats-channel-mobile-card__value">${escapeHtml(row[1])}</div>
                    </div>
                    <div>
                        <div class="stats-channel-mobile-card__label">총 비용</div>
                        <div class="stats-channel-mobile-card__value">${escapeHtml(row[3])}</div>
                    </div>
                </div>
            </div>
        `;
    }).join(""));

    $("#channelCostRows").html(table.rows.map(function(row) {
        return `
            <tr>
                <td class="stats-channel-table__channel">${escapeHtml(row[0])}</td>
                <td>${escapeHtml(row[1])}</td>
                <td class="stats-channel-table__success">${escapeHtml(row[2])}</td>
                <td class="stats-channel-table__cost">${escapeHtml(row[3])}</td>
            </tr>
        `;
    }).join(""));
}

function renderChannelShareLegend(chart) {
    if (!chart || !chart.datasets?.length) {
        return;
    }

    const values = chart.datasets[0].data || [];
    const colors = ["#F7E600", "#1843FA", "#10B981", "#0EA5E9"];

    $("#channelShareLegend").html((chart.labels || []).map(function(label, index) {
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

function escapeHtml(value) {
    return String(value ?? "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
}
