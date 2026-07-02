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
    StatsChart.groupedBar(canvas, rows, options);
}

function drawLineChart(canvas, rows, options) {
    StatsChart.line(canvas, rows, options);
}

function drawDonutChart(canvas, entries) {
    StatsChart.doughnut(canvas, entries, {});
}

function drawHorizontalBarChart(canvas, rows) {
    StatsChart.horizontalBar(canvas, rows, {
        label: "발송량",
        yFormatter: formatCompactNumber
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

function sumBy(rows, key) {
    return rows.reduce(function(sum, row) {
        return sum + (Number(row[key]) || 0);
    }, 0);
}

function clamp(value, min, max) {
    return Math.min(max, Math.max(min, value));
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
