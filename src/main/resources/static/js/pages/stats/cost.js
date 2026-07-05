$(function() {
    const state = {
        period: StatsRenderer.defaultPeriod(7)
    };

    bindCostEvents(state);
    loadCostStats(state);
});

function bindCostEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = StatsRenderer.getPeriod(state.period);
        loadCostStats(state);
    });
}

function loadCostStats(state) {
    const period = StatsRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(StatsRenderer.periodLabel(period));

    ApiClient.get("/api/stats/cost", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        StatsRenderer.renderCards("#statsCards", response.cards, {
            icons: ["target", "chart", "refresh", "send"],
            colors: ["amber", "violet", "green", "blue"]
        });
        renderCostChartTitles(response.charts);
        StatsRenderer.renderCharts(response.charts, {
            costComparison: "#analysisPrimaryChart",
            costSavings: "#analysisSecondaryChart"
        }, {
            costComparison: {
                yFormatter: formatWon
            },
            costSavings: {
                yFormatter: formatWon
            }
        });
    });
}

function renderCostChartTitles(charts) {
    const primary = StatsRenderer.findById(charts, "costComparison");
    const secondary = StatsRenderer.findById(charts, "costSavings");

    $("#analysisPrimaryTitle").text(primary?.title || "");
    $("#analysisPrimaryMeta").text("");
    $("#analysisSecondaryTitle").text(secondary?.title || "");
    $("#analysisSecondaryMeta").text("");
}

function formatWon(value) {
    return `${Math.round(value).toLocaleString()}원`;
}
