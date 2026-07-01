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
    if (!canvas) {
        return;
    }

    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const padding = { top: 8, right: 18, bottom: 28, left: 90 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;
    const min = 96;
    const max = 100;
    const rowGap = 18;
    const barHeight = Math.min(14, (chartHeight - rowGap * (rows.length - 1)) / rows.length);

    clearCanvas(ctx, width, height);
    drawHorizontalGrid(ctx, padding, chartWidth, chartHeight, min, max);

    rows.forEach(function(row, index) {
        const y = padding.top + index * (barHeight + rowGap);
        const barWidth = ((row.successRate - min) / (max - min)) * chartWidth;
        drawText(ctx, row.channel, padding.left - 8, y + barHeight * 0.72, {
            align: "right",
            fill: "#0A0A0F",
            size: 10
        });
        ctx.fillStyle = "#1843FA";
        roundRect(ctx, padding.left, y, Math.max(2, barWidth), barHeight, 0, 3, 3, 0);
        ctx.fill();
        drawText(ctx, `${row.successRate}%`, padding.left + Math.min(chartWidth - 2, barWidth + 8), y + barHeight * 0.72, {
            align: barWidth > chartWidth - 34 ? "right" : "left",
            fill: barWidth > chartWidth - 34 ? "#FFFFFF" : "#6B6B80",
            size: 10,
            weight: 700
        });
    });

    [96, 97, 98, 99, 100].forEach(function(value) {
        const x = padding.left + ((value - min) / (max - min)) * chartWidth;
        drawText(ctx, `${value}%`, x, height - 8, {
            align: "center",
            fill: "#6B6B80",
            size: 10
        });
    });
}

function drawHorizontalGrid(ctx, padding, chartWidth, chartHeight, min, max) {
    ctx.save();
    ctx.strokeStyle = "#f0f0f5";
    ctx.setLineDash([3, 3]);
    ctx.lineWidth = 1;
    [96, 97, 98, 99, 100].forEach(function(value) {
        const x = padding.left + ((value - min) / (max - min)) * chartWidth;
        ctx.beginPath();
        ctx.moveTo(x, padding.top);
        ctx.lineTo(x, padding.top + chartHeight);
        ctx.stroke();
    });
    ctx.restore();
}

function drawLineChart(canvas, rows, options) {
    if (!canvas) {
        return;
    }

    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const padding = { top: 8, right: 18, bottom: 42, left: 46 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom - 18;
    const values = rows.flatMap(function(row) {
        return options.keys.map(function(key) {
            return Number(row[key]) || 0;
        });
    });
    const max = Math.max.apply(null, values) * 1.12;

    clearCanvas(ctx, width, height);
    drawGrid(ctx, padding, chartWidth, chartHeight, 0, max, 4, options.yFormatter);

    options.keys.forEach(function(key, index) {
        const points = getLinePoints(rows, key, padding, chartWidth, chartHeight, 0, max);
        drawLine(ctx, points, options.colors[index], 2);
    });

    rows.forEach(function(row, index) {
        const x = padding.left + (rows.length === 1 ? chartWidth / 2 : (chartWidth / (rows.length - 1)) * index);
        drawText(ctx, row.label, x, padding.top + chartHeight + 18, {
            align: "center",
            fill: "#6B6B80",
            size: 11
        });
    });

    drawLegend(ctx, options.labels, options.colors, padding.left, height - 8);
}

function drawDonutChart(canvas, rows) {
    if (!canvas) {
        return;
    }

    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const total = rows.reduce(function(sum, row) {
        return sum + row.value;
    }, 0);
    const radius = Math.min(width, height) * 0.29;
    const centerX = width / 2;
    const centerY = height / 2;
    let start = -Math.PI / 2;

    clearCanvas(ctx, width, height);

    rows.forEach(function(row) {
        const angle = total === 0 ? 0 : (row.value / total) * Math.PI * 2;
        ctx.save();
        ctx.strokeStyle = row.color;
        ctx.lineWidth = Math.max(18, radius * 0.34);
        ctx.lineCap = "butt";
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius, start, start + angle);
        ctx.stroke();
        ctx.restore();

        const labelAngle = start + angle / 2;
        const labelX = centerX + Math.cos(labelAngle) * (radius + 18);
        const labelY = centerY + Math.sin(labelAngle) * (radius + 18);
        drawText(ctx, `${row.value}%`, labelX, labelY + 3, {
            align: "center",
            fill: "#0A0A0F",
            size: 10,
            weight: 700
        });
        start += angle + 0.045;
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

function setupCanvas(canvas) {
    const dpr = window.devicePixelRatio || 1;
    const width = Math.max(1, canvas.clientWidth);
    const height = Math.max(1, canvas.clientHeight);
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    return ctx;
}

function clearCanvas(ctx, width, height) {
    ctx.clearRect(0, 0, width, height);
}

function drawGrid(ctx, padding, chartWidth, chartHeight, min, max, ticks, formatter) {
    ctx.save();
    ctx.strokeStyle = "#f0f0f5";
    ctx.setLineDash([3, 3]);
    ctx.lineWidth = 1;

    for (let index = 0; index <= ticks; index += 1) {
        const ratio = index / ticks;
        const y = padding.top + chartHeight - chartHeight * ratio;
        ctx.beginPath();
        ctx.moveTo(padding.left, y);
        ctx.lineTo(padding.left + chartWidth, y);
        ctx.stroke();
        drawText(ctx, formatter(min + (max - min) * ratio), padding.left - 8, y + 4, {
            align: "right",
            fill: "#6B6B80",
            size: 11
        });
    }
    ctx.restore();
}

function getLinePoints(rows, key, padding, chartWidth, chartHeight, min, max) {
    return rows.map(function(row, index) {
        const x = padding.left + (rows.length === 1 ? chartWidth / 2 : (chartWidth / (rows.length - 1)) * index);
        const value = Number(row[key]) || 0;
        const y = padding.top + chartHeight - ((value - min) / (max - min)) * chartHeight;
        return { x, y };
    });
}

function drawLine(ctx, points, color, lineWidth) {
    ctx.save();
    ctx.strokeStyle = color;
    ctx.lineWidth = lineWidth;
    ctx.beginPath();
    points.forEach(function(point, index) {
        if (index === 0) {
            ctx.moveTo(point.x, point.y);
        } else {
            ctx.lineTo(point.x, point.y);
        }
    });
    ctx.stroke();
    ctx.restore();
}

function drawLegend(ctx, labels, colors, x, y) {
    let cursor = x;
    labels.forEach(function(label, index) {
        ctx.fillStyle = colors[index];
        ctx.fillRect(cursor, y - 8, 8, 8);
        drawText(ctx, label, cursor + 14, y, {
            align: "left",
            fill: "#0A0A0F",
            size: 11
        });
        cursor += ctx.measureText(label).width + 52;
    });
}

function drawText(ctx, text, x, y, options) {
    ctx.save();
    ctx.fillStyle = options.fill || "#0A0A0F";
    ctx.font = `${options.weight || 400} ${options.size || 12}px "Pretendard Variable", "Pretendard", "Inter", sans-serif`;
    ctx.textAlign = options.align || "left";
    ctx.fillText(text, x, y);
    ctx.restore();
}

function roundRect(ctx, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft) {
    const radii = [topLeft, topRight, bottomRight, bottomLeft].map(function(radius) {
        return Math.min(radius || 0, width / 2, height / 2);
    });
    ctx.beginPath();
    ctx.moveTo(x + radii[0], y);
    ctx.lineTo(x + width - radii[1], y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radii[1]);
    ctx.lineTo(x + width, y + height - radii[2]);
    ctx.quadraticCurveTo(x + width, y + height, x + width - radii[2], y + height);
    ctx.lineTo(x + radii[3], y + height);
    ctx.quadraticCurveTo(x, y + height, x, y + height - radii[3]);
    ctx.lineTo(x, y + radii[0]);
    ctx.quadraticCurveTo(x, y, x + radii[0], y);
    ctx.closePath();
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
