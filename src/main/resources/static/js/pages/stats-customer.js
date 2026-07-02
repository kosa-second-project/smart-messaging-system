$(function() {
    const state = {
        period: {
            start: "2026-06-01",
            end: "2026-06-30"
        }
    };

    renderMetricCards([
        { label: "전체 고객", value: "307,998", sub: "분석 가능 고객", color: "amber", icon: "users" },
        { label: "일반 고객", value: "198,351", sub: "주요 발송 대상", color: "blue", icon: "users" },
        { label: "신규 고객", value: "1,471", sub: "30일 누적 가입", trend: { value: "+3.9%", label: "이전 기간 대비", up: true }, color: "green", icon: "chart" },
        { label: "휴면 고객", value: "23,420", sub: "최신 상태 · 6개월 이상 미활동", color: "violet", icon: "clock" }
    ]);

    renderCustomerConsent();
    bindCustomerEvents(state);
    renderNewCustomerChart(state);
});

function bindCustomerEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = {
            start: $("#statsStartDate").val(),
            end: $("#statsEndDate").val()
        };
        renderNewCustomerChart(state);
    });

    $(window).on("resize", debounce(function() {
        renderNewCustomerChart(state);
    }, 120));
}

function renderNewCustomerChart(state) {
    const period = getOrderedPeriod(state.period);
    $("#statsPeriodLabel").text(getStatsPeriodLabel(period));
    drawNewCustomerBarChart(document.getElementById("newCustomerChart"), buildNewCustomerSeriesData(period));
}

function renderCustomerConsent() {
    const latestSnapshotMembers = 306527;
    const rows = [
        { label: "메시지", agreed: Math.round(latestSnapshotMembers * 0.645), total: latestSnapshotMembers, color: "#3B82F6" },
        { label: "카카오톡", agreed: Math.round(latestSnapshotMembers * 0.784), total: latestSnapshotMembers, color: "#FBBF24" },
        { label: "이메일", agreed: Math.round(latestSnapshotMembers * 0.665), total: latestSnapshotMembers, color: "#10B981" }
    ].map(function(row) {
        return {
            ...row,
            rate: Number(((row.agreed / row.total) * 100).toFixed(1)),
            declined: row.total - row.agreed
        };
    });

    $("#customerConsentList").html(rows.map(function(row) {
        return `
            <div class="stats-consent-row">
                <div class="stats-progress-row__top">
                    <span>${escapeHtml(row.label)}</span>
                    <span>${row.rate}%</span>
                </div>
                <div class="stats-progress-row__track stats-consent-row__track">
                    <div class="stats-progress-row__bar" style="width:${row.rate}%;background:${row.color}"></div>
                </div>
                <div class="stats-consent-row__meta">
                    <span>동의 ${formatNumber(row.agreed)}명</span>
                    <span>미동의 ${formatNumber(row.declined)}명</span>
                </div>
            </div>
        `;
    }).join(""));
}

function drawNewCustomerBarChart(canvas, rows) {
    StatsChart.groupedBar(canvas, rows, {
        keys: ["count"],
        labels: ["신규 고객"],
        colors: ["#10B981"],
        legend: false,
        yFormatter: function(value) {
            return Math.round(value).toLocaleString();
        }
    });
}

function buildNewCustomerSeriesData(period) {
    return createStatsBuckets(period).buckets.map(function(bucket) {
        return {
            label: bucket.label,
            count: Math.round((34 + ((bucket.index * 17) % 31)) * bucket.days)
        };
    });
}
