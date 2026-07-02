const StatsRenderer = (function() {
    const palette = ["#1843FA", "#10B981", "#F59E0B", "#0EA5E9", "#8B5CF6", "#EF4444"];
    const iconNames = ["send", "check", "activity", "target", "refresh", "chart"];
    const colorNames = ["blue", "green", "violet", "amber", "green", "blue"];

    function renderCards(selector, cards, options) {
        const settings = options || {};
        const html = (cards || []).map(function(card, index) {
            const icon = settings.icons?.[index] || iconNames[index % iconNames.length];
            const color = settings.colors?.[index] || colorNames[index % colorNames.length];

            return `
                <article class="stats-card stats-metric-card">
                    <div class="stats-metric-card__top">
                        <span class="stats-metric-card__label">${escapeHtml(card.title)}</span>
                        <span class="stats-metric-card__icon stats-metric-card__icon--${color}">${iconSvg(icon)}</span>
                    </div>
                    <div class="stats-metric-card__value">${escapeHtml(card.value)}</div>
                    <div class="stats-metric-card__sub">${escapeHtml(card.subText)}</div>
                </article>
            `;
        }).join("");

        $(selector).html(html);
    }

    function renderCharts(charts, targets, optionsByChartId) {
        (charts || []).forEach(function(chart) {
            const selector = targets?.[chart.chartId] || `#${chart.chartId}`;
            const canvas = document.querySelector(selector);

            if (!canvas) {
                return;
            }

            renderChart(canvas, chart, optionsByChartId?.[chart.chartId] || {});
        });
    }

    function renderChart(canvas, chart, options) {
        const datasets = chart.datasets || [];
        const labels = chart.labels || [];
        const keys = datasets.map(function(dataset) {
            return dataset.label;
        });
        const colors = keys.map(function(label, index) {
            return colorForLabel(label, index);
        });

        if (options.kind === "composedTrend") {
            return StatsChart.composedTrend(canvas, toComposedRows(labels, datasets));
        }

        if (options.kind === "horizontalBar") {
            return StatsChart.horizontalBar(canvas, toSingleValueRows(labels, datasets[0]), {
                color: options.color || "#1843FA",
                label: datasets[0]?.label || chart.title,
                xMin: options.xMin,
                xMax: options.xMax,
                tooltipSuffix: options.tooltipSuffix || "",
                yFormatter: options.yFormatter || identity
            });
        }

        if (chart.type === "doughnut" || chart.type === "donut") {
            return StatsChart.doughnut(canvas, toEntries(labels, datasets[0], colors), {
                legend: options.legend,
                tooltipSuffix: options.tooltipSuffix || ""
            });
        }

        const chartOptions = {
            keys,
            labels: keys,
            colors,
            legend: options.legend,
            yMin: options.yMin,
            yMax: options.yMax,
            tooltipSuffix: options.tooltipSuffix || "",
            yFormatter: options.yFormatter || identity
        };
        const rows = toRows(labels, datasets);

        if (chart.type === "bar") {
            return StatsChart.groupedBar(canvas, rows, chartOptions);
        }

        return StatsChart.line(canvas, rows, {
            ...chartOptions,
            fill: chart.type === "area"
        });
    }

    function renderTable(tableSelector, table) {
        if (!table) {
            return;
        }

        $(tableSelector).html(`
            <thead>
                <tr>${(table.columns || []).map(function(column) {
                    return `<th>${escapeHtml(column)}</th>`;
                }).join("")}</tr>
            </thead>
            <tbody>
                ${(table.rows || []).map(function(row) {
                    return `<tr>${row.map(function(cell) {
                        return `<td>${escapeHtml(cell)}</td>`;
                    }).join("")}</tr>`;
                }).join("")}
            </tbody>
        `);
    }

    function renderProgressTable(selector, table) {
        if (!table) {
            return;
        }

        $(selector).html((table.rows || []).map(function(row, index) {
            const rate = parseFloat(String(row[3] || "0").replace("%", "")) || 0;
            const color = palette[index % palette.length];

            return `
                <div class="stats-consent-row">
                    <div class="stats-progress-row__top">
                        <span>${escapeHtml(row[0])}</span>
                        <span>${escapeHtml(row[3])}</span>
                    </div>
                    <div class="stats-progress-row__track stats-consent-row__track">
                        <div class="stats-progress-row__bar" style="width:${Math.max(2, rate)}%;background:${color}"></div>
                    </div>
                    <div class="stats-consent-row__meta">
                        <span>동의 ${escapeHtml(row[1])}</span>
                        <span>미동의 ${escapeHtml(row[2])}</span>
                    </div>
                </div>
            `;
        }).join(""));
    }

    function findById(items, id) {
        return (items || []).find(function(item) {
            return item.chartId === id || item.tableId === id;
        });
    }

    function getPeriod(defaultPeriod) {
        const start = $("#statsStartDate").val() || defaultPeriod.start;
        const end = $("#statsEndDate").val() || defaultPeriod.end;

        return orderPeriod({ start, end });
    }

    function periodLabel(period) {
        return `${period.start} ~ ${period.end} (${getPeriodDays(period)}일)`;
    }

    function toRows(labels, datasets) {
        return labels.map(function(label, labelIndex) {
            const row = { label };
            datasets.forEach(function(dataset) {
                row[dataset.label] = dataset.data[labelIndex];
            });
            return row;
        });
    }

    function toSingleValueRows(labels, dataset) {
        return labels.map(function(label, index) {
            return {
                label,
                value: dataset?.data?.[index] || 0
            };
        });
    }

    function toEntries(labels, dataset, colors) {
        return labels.map(function(label, index) {
            return {
                label,
                value: dataset?.data?.[index] || 0,
                color: colors[index] || palette[index % palette.length]
            };
        });
    }

    function toComposedRows(labels, datasets) {
        const sendDataset = datasets.find(function(dataset) { return dataset.label === "발송"; });
        const successDataset = datasets.find(function(dataset) { return dataset.label === "성공"; });

        return labels.map(function(label, index) {
            return {
                label,
                sends: sendDataset?.data?.[index] || 0,
                success: successDataset?.data?.[index] || 0
            };
        });
    }

    function colorForLabel(label, index) {
        const colorMap = {
            카카오톡: "#F7E600",
            카카오: "#F7E600",
            SMS: "#1843FA",
            LMS: "#10B981",
            이메일: "#0EA5E9",
            발송: "#1843FA",
            성공: "#10B981",
            성공률: "#F59E0B",
            "실제 청구 비용": "#1843FA",
            "최대 비용": "#EF4444",
            절감액: "#10B981",
            클릭률: "#10B981",
            전환율: "#F59E0B"
        };

        return colorMap[label] || palette[index % palette.length];
    }

    function orderPeriod(period) {
        if (!isValidDate(period.start) || !isValidDate(period.end)) {
            return period;
        }

        return parseDate(period.start) <= parseDate(period.end)
            ? period
            : { start: period.end, end: period.start };
    }

    function getPeriodDays(period) {
        return Math.max(1, Math.round((parseDate(period.end).getTime() - parseDate(period.start).getTime()) / 86400000) + 1);
    }

    function parseDate(value) {
        const parts = value.split("-").map(Number);
        return new Date(parts[0], parts[1] - 1, parts[2]);
    }

    function isValidDate(value) {
        return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(parseDate(value).getTime());
    }

    function identity(value) {
        return value;
    }

    function iconSvg(name) {
        const icons = {
            send: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m22 2-7 20-4-9-9-4Z"></path><path d="M22 2 11 13"></path></svg>',
            check: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 12l2 2 4-4"></path><circle cx="12" cy="12" r="10"></circle></svg>',
            activity: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M22 12h-4l-3 9L9 3l-3 9H2"></path></svg>',
            target: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10"></circle><circle cx="12" cy="12" r="6"></circle><circle cx="12" cy="12" r="2"></circle></svg>',
            refresh: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M21 12a9 9 0 0 1-15 6.7L3 16"></path><path d="M3 21v-5h5"></path><path d="M3 12a9 9 0 0 1 15-6.7L21 8"></path><path d="M21 3v5h-5"></path></svg>',
            chart: '<svg class="stats-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 3v18h18"></path><path d="m19 9-5 5-4-4-3 3"></path></svg>'
        };

        return icons[name] || icons.chart;
    }

    function escapeHtml(value) {
        return String(value ?? "")
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
    }

    return {
        findById,
        getPeriod,
        periodLabel,
        renderCards,
        renderCharts,
        renderChart,
        renderTable,
        renderProgressTable
    };
})();
