$(function() {
    const page = $("#statsAnalysisPage").data("analysisPage");
    const builders = window.STATS_ANALYSIS_BUILDERS || {};
    const builder = builders[page];

    if (!builder) {
        return;
    }

    const state = {
        period: {
            start: "2026-06-23",
            end: "2026-06-29"
        }
    };

    bindAnalysisEvents(state, builder);
    renderAnalysisPage(state, builder);
});

function bindAnalysisEvents(state, builder) {
    $("#statsPeriodForm").on("change", "input", function() {
        state.period = {
            start: $("#statsStartDate").val(),
            end: $("#statsEndDate").val()
        };
        renderAnalysisPage(state, builder);
    });

    $(window).on("resize", debounce(function() {
        renderAnalysisCharts(builder(getOrderedPeriod(state.period)));
    }, 120));
}

function renderAnalysisPage(state, builder) {
    const period = getOrderedPeriod(state.period);
    const data = builder(period);

    $("#statsPeriodLabel").text(getStatsPeriodLabel(period));
    $("#statsCards").attr("aria-label", `${data.title} 주요 지표`);
    $("#analysisPrimaryTitle").text(data.primary.title);
    $("#analysisPrimaryMeta").text(data.primary.meta);
    $("#analysisSecondaryTitle").text(data.secondary.title);
    $("#analysisSecondaryMeta").text(data.secondary.meta);

    renderMetricCards(data.cards);
    if (data.table) {
        $("#analysisTableTitle").text(data.table.title);
        renderAnalysisTable(data.table);
    }
    if (data.insights) {
        $("#analysisInsightTitle").text(data.insights.title);
        renderInsights(data.insights);
    }
    renderAnalysisCharts(data);
}

function renderAnalysisCharts(data) {
    renderChart(document.getElementById("analysisPrimaryChart"), data.primary);
    renderChart(document.getElementById("analysisSecondaryChart"), data.secondary);
}

function renderChart(canvas, config) {
    if (!canvas || !config) {
        return;
    }

    if (config.type === "groupedBar") {
        drawGroupedBarChart(canvas, config.rows, config);
    } else if (config.type === "line") {
        drawLineChart(canvas, config.rows, config);
    } else if (config.type === "donut") {
        drawDonutChart(canvas, config.entries);
    } else if (config.type === "horizontalBar") {
        drawHorizontalBarChart(canvas, config.rows);
    }
}

function renderMetricCards(cards) {
    $("#statsCards").html(cards.map(function(card) {
        return `
            <article class="stats-card stats-metric-card">
                <div class="stats-metric-card__top">
                    <span class="stats-metric-card__label">${escapeHtml(card.label)}</span>
                    <span class="stats-metric-card__icon stats-metric-card__icon--${card.color}">${iconSvg(card.icon)}</span>
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

function renderAnalysisTable(table) {
    $("#analysisTable").html(`
        <thead>
            <tr>${table.columns.map(function(column) { return `<th>${escapeHtml(column)}</th>`; }).join("")}</tr>
        </thead>
        <tbody>
            ${table.rows.map(function(row) {
                return `<tr>${row.map(function(cell, index) {
                    return `<td>${index === row.length - 1 && table.statusLast ? `<span class="stats-table-status">${escapeHtml(cell)}</span>` : escapeHtml(cell)}</td>`;
                }).join("")}</tr>`;
            }).join("")}
        </tbody>
    `);
}

function renderInsights(insights) {
    if (insights.type === "progress") {
        $("#analysisInsightList").html(`
            <div class="stats-progress-list">
                ${insights.rows.map(function(row) {
                    return `
                        <div class="stats-progress-row">
                            <div class="stats-progress-row__top">
                                <span>${escapeHtml(row.label)}</span>
                                <span>${escapeHtml(row.value)}</span>
                            </div>
                            <div class="stats-progress-row__track">
                                <div class="stats-progress-row__bar" style="width:${clamp(row.rate, 2, 100)}%;background:${row.color}"></div>
                            </div>
                            <div class="stats-progress-row__sub">${escapeHtml(row.sub)}</div>
                        </div>
                    `;
                }).join("")}
            </div>
        `);
        return;
    }

    $("#analysisInsightList").html(insights.rows.map(function(row, index) {
        return `
            <div class="stats-analysis-item">
                <span class="stats-analysis-item__rank">${index + 1}</span>
                <span>
                    <span class="stats-analysis-item__title">${escapeHtml(row.title)}</span>
                    <span class="stats-analysis-item__sub">${escapeHtml(row.sub)}</span>
                </span>
                <span class="stats-analysis-item__value">${escapeHtml(row.value)}</span>
            </div>
        `;
    }).join(""));
}

function drawGroupedBarChart(canvas, rows, options) {
    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const padding = { top: 8, right: 16, bottom: 42, left: 48 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom - 24;
    const values = rows.flatMap(function(row) {
        return options.keys.map(function(key) { return Number(row[key]) || 0; });
    });
    const max = Math.max.apply(null, values) * 1.12;

    clearCanvas(ctx, width, height);
    drawGrid(ctx, padding, chartWidth, chartHeight, 0, max, 4, options.yFormatter);

    const groupWidth = chartWidth / rows.length;
    const barGap = 3;
    const barWidth = Math.max(4, Math.min(13, (groupWidth - 14 - barGap * (options.keys.length - 1)) / options.keys.length));

    rows.forEach(function(row, rowIndex) {
        const groupStart = padding.left + rowIndex * groupWidth + (groupWidth - (barWidth * options.keys.length + barGap * (options.keys.length - 1))) / 2;
        options.keys.forEach(function(key, keyIndex) {
            const value = Number(row[key]) || 0;
            const barHeight = Math.max(2, (value / max) * chartHeight);
            drawRoundedTopBar(ctx, groupStart + keyIndex * (barWidth + barGap), padding.top + chartHeight - barHeight, barWidth, barHeight, 3, options.colors[keyIndex]);
        });
        drawText(ctx, row.label, padding.left + rowIndex * groupWidth + groupWidth / 2, padding.top + chartHeight + 18, {
            align: "center",
            fill: "#6B6B80",
            size: 11
        });
    });

    drawLegend(ctx, options.labels, options.colors, padding.left, height - 14);
}

function drawLineChart(canvas, rows, options) {
    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const padding = { top: 8, right: 20, bottom: 42, left: 50 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom - 24;
    const values = rows.flatMap(function(row) {
        return options.keys.map(function(key) { return Number(row[key]) || 0; });
    });
    const max = Math.max.apply(null, values) * 1.12;

    clearCanvas(ctx, width, height);
    drawGrid(ctx, padding, chartWidth, chartHeight, 0, max, 4, options.yFormatter);

    options.keys.forEach(function(key, index) {
        const points = getLinePoints(rows, key, padding, chartWidth, chartHeight, 0, max);
        if (index === 0) {
            drawArea(ctx, points, padding.top + chartHeight, options.colors[index], 0.12);
        }
        drawLine(ctx, points, options.colors[index], index === 0 ? 2.5 : 2, index === 2 ? [4, 4] : []);
        drawDots(ctx, points, options.colors[index]);
    });

    rows.forEach(function(row, index) {
        const x = padding.left + (rows.length === 1 ? chartWidth / 2 : (chartWidth / (rows.length - 1)) * index);
        drawText(ctx, row.label, x, padding.top + chartHeight + 18, {
            align: "center",
            fill: "#6B6B80",
            size: 11
        });
    });

    drawLegend(ctx, options.labels, options.colors, padding.left, height - 14);
}

function drawDonutChart(canvas, entries) {
    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const total = entries.reduce(function(sum, entry) { return sum + entry.value; }, 0);
    const radius = Math.min(width, height) * 0.28;
    const centerX = width * 0.38;
    const centerY = height * 0.48;
    let start = -Math.PI / 2;

    clearCanvas(ctx, width, height);

    entries.forEach(function(entry) {
        const angle = total === 0 ? 0 : (entry.value / total) * Math.PI * 2;
        ctx.beginPath();
        ctx.strokeStyle = entry.color;
        ctx.lineWidth = Math.max(18, radius * 0.3);
        ctx.arc(centerX, centerY, radius, start, start + angle);
        ctx.stroke();
        start += angle;
    });

    drawText(ctx, "합계", centerX, centerY - 4, {
        align: "center",
        fill: "#6B6B80",
        size: 11
    });
    drawText(ctx, formatCompactNumber(total), centerX, centerY + 16, {
        align: "center",
        fill: "#0A0A0F",
        size: 16,
        weight: 700
    });

    let y = Math.max(26, centerY - entries.length * 15);
    entries.forEach(function(entry) {
        const rate = total === 0 ? 0 : (entry.value / total) * 100;
        ctx.fillStyle = entry.color;
        ctx.fillRect(width * 0.66, y - 9, 8, 8);
        drawText(ctx, entry.label, width * 0.66 + 14, y, {
            align: "left",
            fill: "#0A0A0F",
            size: 12,
            weight: 700
        });
        drawText(ctx, `${rate.toFixed(1)}%`, width - 18, y, {
            align: "right",
            fill: "#6B6B80",
            size: 12
        });
        y += 30;
    });
}

function drawHorizontalBarChart(canvas, rows) {
    const ctx = setupCanvas(canvas);
    const width = canvas.clientWidth;
    const height = canvas.clientHeight;
    const padding = { top: 16, right: 20, bottom: 20, left: 78 };
    const chartWidth = width - padding.left - padding.right;
    const rowGap = 18;
    const barHeight = Math.min(26, (height - padding.top - padding.bottom - rowGap * (rows.length - 1)) / rows.length);
    const max = Math.max.apply(null, rows.map(function(row) { return row.value; }));

    clearCanvas(ctx, width, height);

    rows.forEach(function(row, index) {
        const y = padding.top + index * (barHeight + rowGap);
        const barWidth = max === 0 ? 0 : (row.value / max) * chartWidth;
        drawText(ctx, row.label, padding.left - 10, y + barHeight * 0.66, {
            align: "right",
            fill: "#0A0A0F",
            size: 12,
            weight: 700
        });
        ctx.fillStyle = "#F0F0F5";
        roundRect(ctx, padding.left, y, chartWidth, barHeight, 6);
        ctx.fill();
        ctx.fillStyle = row.color;
        roundRect(ctx, padding.left, y, Math.max(4, barWidth), barHeight, 6);
        ctx.fill();
        drawText(ctx, formatCompactNumber(row.value), padding.left + Math.min(chartWidth - 4, barWidth + 8), y + barHeight * 0.66, {
            align: barWidth > chartWidth * 0.78 ? "right" : "left",
            fill: barWidth > chartWidth * 0.78 ? "#FFFFFF" : "#6B6B80",
            size: 11,
            weight: 700
        });
    });
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

        const value = min + (max - min) * ratio;
        drawText(ctx, formatter(Math.round(value)), padding.left - 8, y + 4, {
            align: "right",
            fill: "#6B6B80",
            size: 11
        });
    }
    ctx.restore();
}

function drawRoundedTopBar(ctx, x, y, width, height, radius, color) {
    ctx.save();
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(x, y + height);
    ctx.lineTo(x, y + radius);
    ctx.quadraticCurveTo(x, y, x + radius, y);
    ctx.lineTo(x + width - radius, y);
    ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
    ctx.lineTo(x + width, y + height);
    ctx.closePath();
    ctx.fill();
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

function drawLine(ctx, points, color, lineWidth, dash) {
    ctx.save();
    ctx.strokeStyle = color;
    ctx.lineWidth = lineWidth;
    ctx.setLineDash(dash || []);
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

function drawArea(ctx, points, bottom, color, alpha) {
    if (points.length === 0) {
        return;
    }

    ctx.save();
    const gradient = ctx.createLinearGradient(0, points[0].y, 0, bottom);
    gradient.addColorStop(0, rgba(color, alpha));
    gradient.addColorStop(1, rgba(color, 0));
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.moveTo(points[0].x, bottom);
    points.forEach(function(point) {
        ctx.lineTo(point.x, point.y);
    });
    ctx.lineTo(points[points.length - 1].x, bottom);
    ctx.closePath();
    ctx.fill();
    ctx.restore();
}

function drawDots(ctx, points, color) {
    ctx.save();
    ctx.fillStyle = color;
    points.forEach(function(point) {
        ctx.beginPath();
        ctx.arc(point.x, point.y, 3, 0, Math.PI * 2);
        ctx.fill();
    });
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

function roundRect(ctx, x, y, width, height, radius) {
    const r = Math.min(radius, width / 2, height / 2);
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + width, y, x + width, y + height, r);
    ctx.arcTo(x + width, y + height, x, y + height, r);
    ctx.arcTo(x, y + height, x, y, r);
    ctx.arcTo(x, y, x + width, y, r);
    ctx.closePath();
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

function sumBy(rows, key) {
    return rows.reduce(function(sum, row) {
        return sum + (Number(row[key]) || 0);
    }, 0);
}

function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
}

function rgba(hex, alpha) {
    const normalized = hex.replace("#", "");
    const r = parseInt(normalized.slice(0, 2), 16);
    const g = parseInt(normalized.slice(2, 4), 16);
    const b = parseInt(normalized.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function formatNumber(value) {
    return Math.round(value).toLocaleString();
}

function formatCompactNumber(value) {
    if (value >= 100000000) {
        return `${(value / 100000000).toFixed(1)}억`;
    }
    if (value >= 10000) {
        return `${(value / 10000).toFixed(1)}만`;
    }
    return formatNumber(value);
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

function iconSvg(name) {
    const icons = {
        check: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 12l2 2 4-4"></path><circle cx="12" cy="12" r="10"></circle></svg>',
        activity: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M22 12h-4l-3 9L9 3l-3 9H2"></path></svg>',
        target: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle></svg>',
        refresh: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path><path d="M3 21v-5h5"></path><path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path><path d="M21 3v5h-5"></path></svg>',
        message: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>',
        coin: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="8"></circle><path d="M12 6v12"></path><path d="M16 9a4 4 0 0 0-4-2H9.5a2.5 2.5 0 0 0 0 5H14a2.5 2.5 0 0 1 0 5h-2a4 4 0 0 1-4-2"></path></svg>',
        users: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>',
        clock: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><path d="M12 6v6l4 2"></path></svg>',
        chart: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3v18h18"></path><path d="m19 9-5 5-4-4-3 3"></path></svg>',
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
