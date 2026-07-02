$(function() {
    const state = {
        period: {
            start: "2026-06-23",
            end: "2026-06-29"
        }
    };

    bindChannelEvents(state);
    renderChannelPage(state);
});

function bindChannelEvents(state) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = {
            start: $("#statsStartDate").val(),
            end: $("#statsEndDate").val()
        };
        renderChannelPage(state);
    });

    $(window).on("resize", debounce(function() {
        renderChannelCharts(getOrderedPeriod(state.period));
    }, 120));
}

function renderChannelPage(state) {
    const period = getOrderedPeriod(state.period);
    const costRows = buildChannelCostData(period);
    const shareRows = buildChannelShareData(costRows);

    $("#statsPeriodLabel").text(getStatsPeriodLabel(period));
    renderChannelCost(costRows);
    renderChannelShareLegend(shareRows);
    renderChannelCharts(period);
}

function renderChannelCharts(period) {
    const costRows = buildChannelCostData(period);
    const trendRows = buildChannelTrendData(period, 8);
    const shareRows = buildChannelShareData(costRows);

    drawHorizontalSuccessChart(document.getElementById("channelSuccessChart"), costRows);
    drawLineChart(document.getElementById("channelTrendChart"), trendRows, {
        keys: ["kakao", "sms", "lms", "email"],
        labels: ["카카오톡", "SMS", "LMS", "이메일"],
        colors: ["#F7E600", "#1843FA", "#10B981", "#0EA5E9"],
        yFormatter: function(value) {
            return `${(value / 10000).toFixed(0)}만`;
        },
        valueFormatter: function(value) {
            return `${Number(value).toLocaleString()}건`;
        }
    });
    drawDonutChart(document.getElementById("channelShareChart"), shareRows);
}

function renderChannelCost(rows) {
    $("#channelCostCards").html(rows.map(function(row) {
        return `
            <div class="stats-channel-mobile-card">
                <div class="stats-channel-mobile-card__top">
                    <div class="stats-channel-mobile-card__title">${escapeHtml(row.channel)}</div>
                    <div class="stats-channel-mobile-card__rate">${row.successRate}% 성공</div>
                </div>
                <div class="stats-channel-mobile-card__grid">
                    <div>
                        <div class="stats-channel-mobile-card__label">발송량</div>
                        <div class="stats-channel-mobile-card__value">${formatNumber(row.sends)}건</div>
                    </div>
                    <div>
                        <div class="stats-channel-mobile-card__label">총 비용</div>
                        <div class="stats-channel-mobile-card__value">${formatWon(row.cost)}</div>
                    </div>
                </div>
            </div>
        `;
    }).join(""));

    $("#channelCostRows").html(rows.map(function(row) {
        return `
            <tr>
                <td class="stats-channel-table__channel">${escapeHtml(row.channel)}</td>
                <td>${formatNumber(row.sends)}건</td>
                <td class="stats-channel-table__success">${row.successRate}%</td>
                <td class="stats-channel-table__cost">${formatWon(row.cost)}</td>
            </tr>
        `;
    }).join(""));
}

function renderChannelShareLegend(rows) {
    $("#channelShareLegend").html(rows.map(function(row) {
        return `
            <div class="stats-channel-share__legend-row">
                <span class="stats-channel-share__swatch" style="background:${row.color}"></span>
                <span>${escapeHtml(row.name)}</span>
                <span class="stats-channel-share__value">${row.value}%</span>
            </div>
        `;
    }).join(""));
}

function drawHorizontalSuccessChart(canvas, rows) {
    StatsChart.horizontalBar(canvas, rows, {
        color: "#1843FA",
        label: "성공률",
        xMin: 96,
        xMax: 100,
        tooltipSuffix: "%",
        yFormatter: function(value) {
            return `${value}%`;
        }
    });
}

function drawLineChart(canvas, rows, options) {
    StatsChart.line(canvas, rows, options);
}

function drawDonutChart(canvas, rows) {
    StatsChart.doughnut(canvas, rows, {
        legend: false,
        tooltipSuffix: "%"
    });
}

function buildChannelCostData(period) {
    const days = getStatsPeriodDays(period);
    const scale = days / 30;
    const baseRows = [
        { channel: "카카오톡", sends: 535279, successRate: 99.1, failRate: 0.9, cost: 3586503, unit: 7 },
        { channel: "SMS", sends: 249886, successRate: 99.1, failRate: 0.9, cost: 2498860, unit: 10 },
        { channel: "LMS", sends: 80241, successRate: 98.2, failRate: 1.8, cost: 2407230, unit: 30 },
        { channel: "이메일", sends: 26739, successRate: 97.8, failRate: 2.2, cost: 80217, unit: 3 }
    ];

    return baseRows.map(function(channel, index) {
        const sends = Math.round(channel.sends * scale * (0.94 + index * 0.025));
        const successRate = Number(clamp(channel.successRate + ((days + index) % 5 - 2) * 0.08, 96, 99.8).toFixed(1));
        return {
            ...channel,
            sends,
            successRate,
            failRate: Number((100 - successRate).toFixed(1)),
            cost: sends * channel.unit
        };
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
            email: Math.round(base * 0.16)
        };
    });
}

function buildChannelShareData(rows) {
    const colors = {
        "카카오톡": "#F7E600",
        SMS: "#1843FA",
        LMS: "#10B981",
        "이메일": "#0EA5E9"
    };
    const total = rows.reduce(function(sum, row) {
        return sum + row.sends;
    }, 0) || 1;
    const grouped = rows.reduce(function(acc, row) {
        const name = row.channel.includes("카카오") ? "카카오톡" : row.channel;
        acc[name] = (acc[name] || 0) + row.sends;
        return acc;
    }, {});

    return Object.keys(grouped).map(function(name) {
        return {
            name,
            value: Math.round((grouped[name] / total) * 100),
            color: colors[name] || "#64748B"
        };
    }).sort(function(a, b) {
        return b.value - a.value;
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

function parseStatDate(value) {
    const parts = value.split("-").map(Number);
    return new Date(parts[0], parts[1] - 1, parts[2]);
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

function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
}

function formatNumber(value) {
    return Math.round(value).toLocaleString();
}

function formatWon(value) {
    return `${formatNumber(value)}원`;
}

function debounce(callback, delay) {
    let timer;
    return function() {
        clearTimeout(timer);
        timer = setTimeout(callback, delay);
    };
}

function escapeHtml(value) {
    return String(value ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
