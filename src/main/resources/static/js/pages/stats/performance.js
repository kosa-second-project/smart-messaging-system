$(function() {
    const state = {
        period: StatsRenderer.defaultPeriod(7),
        channel: "KAKAO"
    };

    bindPerformanceEvents(state);
    loadPerformanceStats(state);
});

function bindPerformanceEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = StatsRenderer.getPeriod(state.period);
        loadPerformanceStats(state);
    });

    $("#performanceChannelSelect").on("change", function() {
        state.channel = $(this).val();
        loadPerformanceStats(state);
    });
}

function loadPerformanceStats(state) {
    const period = StatsRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(StatsRenderer.periodLabel(period));

    ApiClient.get("/api/stats/performance", {
        from: period.start,
        to: period.end,
        channel: state.channel
    }).done(function(response) {
        StatsRenderer.renderCards("#statsCards", response.cards, {
            icons: ["send", "target", "chart", "check"],
            colors: ["blue", "green", "violet", "amber"]
        });
        StatsRenderer.renderCharts(response.charts, {
            performanceTrend: "#performanceTrendChart",
            weekdayClick: "#weekdayClickChart",
            hourlyClick: "#hourlyClickChart"
        }, {
            performanceTrend: {
                tooltipSuffix: "건",
                yFormatter: countFormatter
            },
            weekdayClick: {
                legend: false,
                tooltipSuffix: "건",
                yFormatter: countFormatter
            },
            hourlyClick: {
                legend: false,
                tooltipSuffix: "건",
                yFormatter: countFormatter
            }
        });
    });
}

function countFormatter(value) {
    return Number(value).toLocaleString();
}
