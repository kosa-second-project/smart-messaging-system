$(function() {
    const state = {
        period: {
            start: "2026-06-23",
            end: "2026-06-29"
        }
    };

    bindCostEvents(state);
    loadCostReport(state);
});

function bindCostEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = ReportRenderer.getPeriod(state.period);
        loadCostReport(state);
    });
}

function loadCostReport(state) {
    const period = ReportRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(ReportRenderer.periodLabel(period));

    ApiClient.get("/api/reports/cost", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        ReportRenderer.renderCards("#statsCards", response.cards, {
            icons: ["target", "chart", "refresh", "send"],
            colors: ["amber", "violet", "green", "blue"]
        });
        renderCostChartTitles(response.charts);
        ReportRenderer.renderCharts(response.charts, {
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
    const primary = ReportRenderer.findById(charts, "costComparison");
    const secondary = ReportRenderer.findById(charts, "costSavings");

    $("#analysisPrimaryTitle").text(primary?.title || "");
    $("#analysisPrimaryMeta").text("");
    $("#analysisSecondaryTitle").text(secondary?.title || "");
    $("#analysisSecondaryMeta").text("");
}

function formatWon(value) {
    return `${Math.round(value).toLocaleString()}원`;
}
