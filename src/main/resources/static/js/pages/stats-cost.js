window.STATS_ANALYSIS_BUILDERS = window.STATS_ANALYSIS_BUILDERS || {};
window.STATS_ANALYSIS_BUILDERS.cost = function(period) {
    const rows = createStatsBuckets(period).buckets.map(function(bucket) {
        const actual = Math.round((520000 + ((bucket.index * 173000 + 47000) % 420000)) * bucket.days);
        const baseline = Math.round(actual * (1.24 + (bucket.index % 3) * 0.04));
        return { label: bucket.label, actual, baseline, saved: baseline - actual };
    });
    const totalActual = sumBy(rows, "actual");
    const totalBaseline = sumBy(rows, "baseline");
    const saved = totalBaseline - totalActual;
    const days = getStatsPeriodDays(period);

    return {
        title: "비용 분석",
        cards: [
            { label: "실제 청구 비용", value: "16,962,000원", sub: "선택 기간 누적", color: "amber", icon: "target" },
            { label: "최대 비용", value: "21,603,080원", sub: "동일 물량 기준", color: "violet", icon: "chart" },
            { label: "기간 절감액", value: "4,641,080원", sub: "절감률 21.5%", trend: { value: "+22.1%", label: "이전 기간 대비", up: true }, color: "green", icon: "coin" },
            { label: "대체 발송 전환", value: "5,365", sub: "전환율 1.9%", color: "blue", icon: "refresh" }
        ],
        primary: {
            type: "line",
            title: "비용 비교 현황",
            meta: getStatsPeriodLabel(period),
            rows,
            keys: ["actual", "baseline"],
            labels: ["실제 청구 비용", "최대 비용"],
            colors: ["#1843FA", "#EF4444"],
            yFormatter: function(value) { return formatWon(value); }
        },
        secondary: {
            type: "line",
            title: "절감액 추이",
            meta: "스마트 라우팅 기준",
            rows,
            keys: ["saved"],
            labels: ["절감액"],
            colors: ["#10B981"],
            yFormatter: function(value) { return formatWon(value); }
        }
    };
};
