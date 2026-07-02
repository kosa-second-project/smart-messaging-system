$(function() {
    const state = {
        period: {
            start: "2026-06-01",
            end: "2026-06-30"
        },
        channel: "카카오톡"
    };

    bindPerformanceEvents(state);
    renderPerformancePage(state);
});

function bindPerformanceEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = {
            start: $("#statsStartDate").val(),
            end: $("#statsEndDate").val()
        };
        renderPerformancePage(state);
    });

    $("#performanceChannelSelect").on("change", function() {
        state.channel = $(this).val();
        renderPerformancePage(state);
    });

    $(window).on("resize", debounce(function() {
        renderPerformanceCharts(state);
    }, 120));
}

function renderPerformancePage(state) {
    const period = getOrderedPeriod(state.period);
    $("#statsPeriodLabel").text(getStatsPeriodLabel(period));

    renderMetricCards([
        { label: "선택 채널", value: state.channel, sub: "채널별 데이터 표시", color: "blue", icon: "message" },
        { label: "평균 클릭률", value: "21.3%", sub: "업계 평균 8.2%", trend: { value: "+0.8%p", label: "이전 기간 대비", up: true }, color: "green", icon: "target" },
        { label: "전환율", value: "6.2%", sub: "선택 기간 평균", trend: { value: "+0.6%p", label: "이전 기간 대비", up: true }, color: "violet", icon: "chart" },
        { label: "수신 거부율", value: "0.12%", sub: "업계 평균 0.41%", trend: { value: "-0.02%p", label: "이전 기간 대비", up: true }, color: "amber", icon: "check" }
    ]);

    renderPerformanceCharts(state);
}

function renderPerformanceCharts(state) {
    const period = getOrderedPeriod(state.period);
    const performanceRows = buildPerformanceSeriesData(period, state.channel);

    drawLineChart(document.getElementById("performanceTrendChart"), performanceRows, {
        keys: ["clickRate", "conversionRate"],
        labels: ["클릭률", "전환율"],
        colors: ["#10B981", "#F59E0B"],
        yFormatter: function(value) {
            return `${Math.round(value)}%`;
        }
    });

    drawGroupedBarChart(document.getElementById("weekdayClickChart"), WEEKDAY_CLICK_DATA, {
        keys: ["rate"],
        labels: ["클릭률"],
        colors: ["#1843FA"],
        yFormatter: function(value) {
            return `${Math.round(value)}%`;
        }
    });

    drawHourlyClickChart(document.getElementById("hourlyClickChart"), HOURLY_CLICK_DATA);
}

function drawHourlyClickChart(canvas, rows) {
    StatsChart.line(canvas, rows, {
        keys: ["rate"],
        labels: ["클릭률"],
        colors: ["#1843FA"],
        legend: false,
        yFormatter: function(value) {
            return `${Math.round(value)}%`;
        }
    });
}

function buildPerformanceSeriesData(period, channel) {
    const channelOffset = ["카카오톡", "SMS", "LMS", "이메일"].indexOf(channel) * 0.8;
    return createStatsBuckets(period).buckets.map(function(bucket) {
        const clickRate = Number(clamp(14.8 + channelOffset + bucket.index * 0.45 + (bucket.days > 7 ? 1.4 : 0), 8, 36).toFixed(1));
        return {
            label: bucket.label,
            clickRate,
            conversionRate: Number(clamp(clickRate * 0.28 + (bucket.index % 3) * 0.2, 2, 11).toFixed(1))
        };
    });
}

const WEEKDAY_CLICK_DATA = [
    { label: "월", rate: 12.1 },
    { label: "화", rate: 16.8 },
    { label: "수", rate: 18.2 },
    { label: "목", rate: 17.3 },
    { label: "금", rate: 15.9 },
    { label: "토", rate: 11.4 },
    { label: "일", rate: 10.1 }
];

const HOURLY_CLICK_DATA = [
    { label: "00시", rate: 2.1 },
    { label: "01시", rate: 1.4 },
    { label: "02시", rate: 0.9 },
    { label: "03시", rate: 0.7 },
    { label: "04시", rate: 0.8 },
    { label: "05시", rate: 1.2 },
    { label: "06시", rate: 2.6 },
    { label: "07시", rate: 5.3 },
    { label: "08시", rate: 8.4 },
    { label: "09시", rate: 13.8 },
    { label: "10시", rate: 18.7 },
    { label: "11시", rate: 16.9 },
    { label: "12시", rate: 14.2 },
    { label: "13시", rate: 15.6 },
    { label: "14시", rate: 17.9 },
    { label: "15시", rate: 19.4 },
    { label: "16시", rate: 21.3 },
    { label: "17시", rate: 18.1 },
    { label: "18시", rate: 13.6 },
    { label: "19시", rate: 10.2 },
    { label: "20시", rate: 8.7 },
    { label: "21시", rate: 6.1 },
    { label: "22시", rate: 4.3 },
    { label: "23시", rate: 3.0 }
];
