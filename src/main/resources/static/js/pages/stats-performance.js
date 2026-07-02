$(function() {
    const state = {
        period: {
            start: "2026-06-01",
            end: "2026-06-30"
        },
        channel: "카카오톡"
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
        to: period.end
    }).done(function(response) {
        StatsRenderer.renderCards("#statsCards", applySelectedChannel(response.cards, state.channel), {
            icons: ["send", "target", "chart", "check"],
            colors: ["blue", "green", "violet", "amber"]
        });
        StatsRenderer.renderCharts(response.charts, {
            performanceTrend: "#performanceTrendChart",
            weekdayClick: "#weekdayClickChart",
            hourlyClick: "#hourlyClickChart"
        }, {
            performanceTrend: {
                yFormatter: percentFormatter
            },
            weekdayClick: {
                legend: false,
                tooltipSuffix: "%",
                yFormatter: percentFormatter
            },
            hourlyClick: {
                legend: false,
                tooltipSuffix: "%",
                yFormatter: percentFormatter
            }
        });
    });
}

function applySelectedChannel(cards, channel) {
    return (cards || []).map(function(card) {
        if (card.title !== "선택 채널") {
            return card;
        }

        return {
            ...card,
            value: channel
        };
    });
}

function percentFormatter(value) {
    return `${Math.round(value)}%`;
}
