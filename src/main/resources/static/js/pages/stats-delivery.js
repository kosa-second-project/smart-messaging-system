$(function() {
    const state = {
        period: {
            start: "2026-06-23",
            end: "2026-06-29"
        }
    };

    bindEvents(state);
    renderStatsOverview(state);
});

function bindEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = {
            start: $("#statsStartDate").val(),
            end: $("#statsEndDate").val()
        };
        renderStatsOverview(state);
    });

    $(window).on("resize", debounce(function() {
        renderCharts(state.period);
    }, 120));
}

function renderStatsOverview(state) {
    const period = getOrderedPeriod(state.period);
    const sendTrendData = buildSendTrendData(period, 2);
    const channelTrendData = buildChannelTrendData(period, 4);
    const fallbackData = buildFallbackStageData(period);
    const routingData = buildRoutingSeriesData(period);
    const totalSends = sumBy(sendTrendData, "sends");
    const totalSuccess = sumBy(sendTrendData, "success");
    const totalCost = sumBy(routingData, "actual");
    const totalSaved = sumBy(routingData, "saved");
    const days = getStatsPeriodDays(period);

    $("#statsPeriodLabel").text(getStatsPeriodLabel(period));
    renderMetricCards({
        totalSends,
        totalSuccess,
        totalCost,
        totalSaved,
        days,
        period
    });
    renderQueueStatus();
    renderCharts(period);
}

function renderMetricCards(summary) {
    const failCount = summary.totalSends - summary.totalSuccess;
    const cards = [
        {
            label: "총 발송",
            value: summary.totalSends.toLocaleString(),
            sub: getStatsPeriodLabel(summary.period),
            trend: { value: "+12.4%", label: "이전 기간 대비", up: true },
            color: "blue",
            icon: iconSvg("send")
        },
        {
            label: "평균 성공률",
            value: `${((summary.totalSuccess / summary.totalSends) * 100).toFixed(1)}%`,
            sub: `실패 ${failCount.toLocaleString()}건`,
            trend: { value: "+0.2%p", label: "이전 기간 대비", up: true },
            color: "green",
            icon: iconSvg("check")
        },
        {
            label: "기간 평균 발송",
            value: Math.round(summary.totalSends / summary.days).toLocaleString(),
            sub: "일 평균 기준",
            color: "violet",
            icon: iconSvg("activity")
        },
        {
            label: "실제 청구 비용",
            value: formatWon(summary.totalCost),
            sub: "선택 기간 누적",
            trend: { value: "+8.1%", label: "이전 기간 대비", up: false },
            color: "amber",
            icon: iconSvg("target")
        },
        {
            label: "스마트 라우팅 절감",
            value: formatWon(summary.totalSaved),
            sub: "최대 비용 대비",
            color: "green",
            icon: iconSvg("refresh")
        }
    ];

    $("#statsCards").html(cards.map(function(card) {
        return `
            <article class="stats-card stats-metric-card">
                <div class="stats-metric-card__top">
                    <span class="stats-metric-card__label">${escapeHtml(card.label)}</span>
                    <span class="stats-metric-card__icon stats-metric-card__icon--${card.color}">${card.icon}</span>
                </div>
                <div class="stats-metric-card__value">${escapeHtml(card.value)}</div>
                <div class="stats-metric-card__sub">${escapeHtml(card.sub)}</div>
                ${renderTrend(card.trend)}
            </article>
        `;
    }).join(""));
}

function renderTrend(trend) {
    if (!trend) {
        return "";
    }

    return `
        <div class="stats-metric-card__trend ${trend.up ? "" : "stats-metric-card__trend--down"}">
            ${trend.up ? iconSvg("arrow-up") : iconSvg("arrow-down")}
            ${escapeHtml(trend.value)} ${escapeHtml(trend.label)}
        </div>
    `;
}

function renderQueueStatus() {
    const colors = {
        "대기": "#94A3B8",
        "발송 중": "#3B82F6",
        "완료": "#10B981",
        "실패": "#EF4444"
    };
    const total = QUEUE_STATUS.reduce(function(sum, item) {
        return sum + item.count;
    }, 0);

    $("#statsQueueItems").html(QUEUE_STATUS.map(function(item) {
        const rate = total === 0 ? 0 : (item.count / total) * 100;
        return `
            <div class="stats-queue-item">
                <div class="stats-queue-item__top">
                    <div class="stats-queue-item__label-wrap">
                        <span class="stats-queue-item__dot" style="background:${colors[item.label]}"></span>
                        <span class="stats-queue-item__label">${escapeHtml(item.label)}</span>
                    </div>
                    <span class="stats-queue-item__rate">${rate.toFixed(1)}%</span>
                </div>
                <div class="stats-queue-item__count">${item.count.toLocaleString()}건</div>
            </div>
        `;
    }).join(""));

    $("#statsQueueBar").html(QUEUE_STATUS.map(function(item) {
        const rate = total === 0 ? 0 : (item.count / total) * 100;
        return `<span class="stats-queue-bar__segment" title="${escapeHtml(item.label)} ${rate.toFixed(1)}%" style="width:${Math.max(item.count === 0 ? 2 : 4, rate)}%;background:${colors[item.label]}"></span>`;
    }).join(""));
}

function renderCharts(period) {
    drawGroupedBarChart(document.getElementById("channelTrendChart"), buildChannelTrendData(period, 4), {
        keys: ["kakao", "sms", "lms", "rcs"],
        labels: ["카카오톡", "SMS", "LMS", "이메일"],
        colors: ["#F7E600", "#1843FA", "#10B981", "#0EA5E9"],
        yFormatter: function(value) {
            return `${(value / 10000).toFixed(0)}만`;
        },
        tooltipSuffix: "건"
    });

    drawComposedTrendChart(document.getElementById("sendTrendChart"), buildSendTrendData(period, 2));

    drawGroupedBarChart(document.getElementById("fallbackChart"), buildFallbackStageData(period), {
        keys: ["kakao", "sms", "lms"],
        labels: ["카카오", "SMS", "LMS"],
        colors: ["#F7E600", "#1843FA", "#10B981"],
        yMin: 90,
        yMax: 100,
        yFormatter: function(value) {
            return `${value}%`;
        },
        tooltipSuffix: "%"
    });
}

function drawGroupedBarChart(canvas, rows, options) {
    StatsChart.groupedBar(canvas, rows, options);
}

function drawComposedTrendChart(canvas, rows) {
    StatsChart.composedTrend(canvas, rows);
}

function buildSendTrendData(period, seed) {
    return createStatsBuckets(period).buckets.map(function(bucket) {
        const daily = 43000 + ((bucket.index * 13817 + seed * 7919) % 72000);
        const spike = bucket.index % 5 === 2 ? 1.45 : 1;
        const sends = Math.round(daily * bucket.days * spike);
        const successRate = 0.973 + ((bucket.index + seed) % 6) * 0.003;
        return { label: bucket.label, sends, success: Math.round(sends * successRate) };
    });
}

function buildChannelTrendData(period, seed) {
    return createStatsBuckets(period).buckets.map(function(bucket) {
        const base = (26000 + ((bucket.index * 9631 + seed * 5443) % 42000)) * bucket.days;
        return {
            label: bucket.label,
            kakao: Math.round(base * 1.72),
            sms: Math.round(base * 0.92),
            lms: Math.round(base * 0.34),
            rcs: Math.round(base * 0.16)
        };
    });
}

function buildRoutingSeriesData(period) {
    return createStatsBuckets(period).buckets.map(function(bucket) {
        const actual = Math.round((470000 + ((bucket.index * 182000) % 360000)) * bucket.days);
        const baseline = Math.round(actual * (1.22 + (bucket.index % 4) * 0.035));
        return { label: bucket.label, actual, baseline, saved: baseline - actual };
    });
}

function buildFallbackStageData(period) {
    const modifier = Math.min(1.2, getStatsPeriodDays(period) / 45);
    return FALLBACK_STAGE_DATA.map(function(stage, index) {
        return {
            stage: stage.stage,
            label: stage.stage,
            kakao: Number(clamp(stage.kakao - index * 0.18 + modifier * 0.12, 90, 100).toFixed(1)),
            sms: Number(clamp(stage.sms - index * 0.14 + modifier * 0.1, 90, 100).toFixed(1)),
            lms: Number(clamp(stage.lms - index * 0.16 + modifier * 0.08, 90, 100).toFixed(1))
        };
    });
}

function createStatsBuckets(period) {
    const ordered = getOrderedPeriod(period);
    const days = getStatsPeriodDays(ordered);
    const grain = days <= 31 ? "day" : days <= 120 ? "week" : "month";
    const end = parseStatDate(ordered.end);
    const buckets = [];
    let cursor = parseStatDate(ordered.start);

    while (cursor <= end) {
        const start = new Date(cursor);
        let bucketEnd = new Date(cursor);

        if (grain === "week") {
            bucketEnd.setDate(bucketEnd.getDate() + 6);
        } else if (grain === "month") {
            bucketEnd = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0);
        }

        if (bucketEnd > end) {
            bucketEnd = new Date(end);
        }

        const bucketDays = Math.round((bucketEnd.getTime() - start.getTime()) / 86400000) + 1;
        const label = grain === "day"
            ? `${start.getMonth() + 1}/${start.getDate()}`
            : grain === "week"
                ? `${formatCompactDate(start)}-${formatCompactDate(bucketEnd)}`
                : `${start.getMonth() + 1}월`;

        buckets.push({ label, days: bucketDays, index: buckets.length });
        cursor = new Date(bucketEnd);
        cursor.setDate(cursor.getDate() + 1);
    }

    return { grain, buckets };
}

const QUEUE_STATUS = [
    { label: "대기", count: 0 },
    { label: "발송 중", count: 2500 },
    { label: "완료", count: 12847 },
    { label: "실패", count: 165 }
];

const FALLBACK_STAGE_DATA = [
    { stage: "1차", kakao: 99.2, sms: 98.7, lms: 97.9 },
    { stage: "2차", kakao: 98.8, sms: 98.2, lms: 97.3 },
    { stage: "3차", kakao: 98.1, sms: 97.6, lms: 96.8 }
];

function parseStatDate(value) {
    const parts = value.split("-").map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2]);
}

function formatStatDate(date) {
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${date.getFullYear()}-${month}-${day}`;
}

function formatCompactDate(date) {
    return `${date.getMonth() + 1}.${String(date.getDate()).padStart(2, "0")}`;
}

function getOrderedPeriod(period) {
    if (!isValidStatDate(period.start) || !isValidStatDate(period.end)) {
        return { start: "2026-06-23", end: "2026-06-29" };
    }

    return parseStatDate(period.start) <= parseStatDate(period.end)
        ? period
        : { start: period.end, end: period.start };
}

function getStatsPeriodDays(period) {
    const ordered = getOrderedPeriod(period);
    return Math.max(1, Math.round((parseStatDate(ordered.end).getTime() - parseStatDate(ordered.start).getTime()) / 86400000) + 1);
}

function getStatsPeriodLabel(period) {
    const ordered = getOrderedPeriod(period);
    return `${ordered.start} ~ ${ordered.end} (${getStatsPeriodDays(ordered)}일)`;
}

function isValidStatDate(value) {
    return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(parseStatDate(value).getTime());
}

function formatWon(value) {
    return `${value.toLocaleString()}원`;
}

function sumBy(rows, key) {
    return rows.reduce(function(sum, row) {
        return sum + (Number(row[key]) || 0);
    }, 0);
}

function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
}

function debounce(callback, delay) {
    let timer;
    return function() {
        clearTimeout(timer);
        timer = setTimeout(callback, delay);
    };
}

function iconSvg(name) {
    const icons = {
        send: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m22 2-7 20-4-9-9-4Z"></path><path d="M22 2 11 13"></path></svg>',
        check: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 12l2 2 4-4"></path><circle cx="12" cy="12" r="10"></circle></svg>',
        activity: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M22 12h-4l-3 9L9 3l-3 9H2"></path></svg>',
        target: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle></svg>',
        refresh: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path><path d="M3 21v-5h5"></path><path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path><path d="M21 3v5h-5"></path></svg>',
        "arrow-up": '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M7 17 17 7"></path><path d="M7 7h10v10"></path></svg>',
        "arrow-down": '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m7 7 10 10"></path><path d="M17 7v10H7"></path></svg>'
    };

    return icons[name] || "";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
