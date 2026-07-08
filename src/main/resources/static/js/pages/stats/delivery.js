$(function() {
    const state = {
        period: StatsRenderer.defaultPeriod(7)
    };

    bindEvents(state);
    loadDeliveryStats(state);
    RealtimeQueueStatus.start();
});

function bindEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = StatsRenderer.getPeriod(state.period);
        loadDeliveryStats(state);
    });
}

function loadDeliveryStats(state) {
    const period = StatsRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(StatsRenderer.periodLabel(period));

    ApiClient.get("/api/stats/delivery", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        StatsRenderer.renderCards("#statsCards", response.cards, {
            icons: ["send", "check", "activity", "target", "refresh"],
            colors: ["blue", "green", "violet", "amber", "green"]
        });
        StatsRenderer.renderCharts(response.charts, {
            channelTrend: "#channelTrendChart",
            sendSuccessTrend: "#sendTrendChart",
            fallbackSuccess: "#fallbackChart"
        }, {
            sendSuccessTrend: {
                kind: "composedTrend"
            },
            fallbackSuccess: {
                yMin: 0,
                yMax: 100,
                tooltipSuffix: "%",
                yFormatter: function(value) {
                    return `${value}%`;
                }
            }
        });
    });
}
