$(function() {
    const state = {
        period: StatsRenderer.defaultPeriod(7)
    };

    bindCustomerEvents(state);
    loadCustomerStats(state);
});

function bindCustomerEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = StatsRenderer.getPeriod(state.period);
        loadCustomerStats(state);
    });
}

function loadCustomerStats(state) {
    const period = StatsRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(StatsRenderer.periodLabel(period));

    ApiClient.get("/api/stats/customer", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        StatsRenderer.renderCards("#statsCards", response.cards, {
            icons: ["chart", "send", "check", "activity"],
            colors: ["amber", "blue", "green", "violet"]
        });
        StatsRenderer.renderCharts(response.charts, {
            newCustomerTrend: "#newCustomerChart"
        }, {
            newCustomerTrend: {
                legend: false
            }
        });
        StatsRenderer.renderProgressTable(
                "#customerConsentList",
                StatsRenderer.findById(response.tables, "customerConsent")
        );
    });
}
