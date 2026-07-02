$(function() {
    const state = {
        period: {
            start: "2026-06-01",
            end: "2026-06-30"
        }
    };

    bindCustomerEvents(state);
    loadCustomerReport(state);
});

function bindCustomerEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = ReportRenderer.getPeriod(state.period);
        loadCustomerReport(state);
    });
}

function loadCustomerReport(state) {
    const period = ReportRenderer.getPeriod(state.period);
    state.period = period;
    $("#statsPeriodLabel").text(ReportRenderer.periodLabel(period));

    ApiClient.get("/api/reports/customer", {
        from: period.start,
        to: period.end
    }).done(function(response) {
        ReportRenderer.renderCards("#statsCards", response.cards, {
            icons: ["chart", "send", "check", "activity"],
            colors: ["amber", "blue", "green", "violet"]
        });
        ReportRenderer.renderCharts(response.charts, {
            newCustomerTrend: "#newCustomerChart"
        }, {
            newCustomerTrend: {
                legend: false
            }
        });
        ReportRenderer.renderProgressTable(
                "#customerConsentList",
                ReportRenderer.findById(response.tables, "customerConsent")
        );
    });
}
