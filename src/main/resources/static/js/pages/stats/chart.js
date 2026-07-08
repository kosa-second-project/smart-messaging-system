const StatsChart = (function() {
    const instances = new WeakMap();
    const fontFamily = '"Pretendard Variable", "Pretendard", "Inter", sans-serif';
    const gridColor = "#F0F0F5";
    const labelColor = "#6B6B80";
    const chartAnimation = {
        duration: 800,
        easing: "easeOutQuart"
    };

    function render(canvas, config) {
        if (!canvas || typeof Chart === "undefined") {
            return null;
        }

        const previous = instances.get(canvas);
        if (previous) {
            previous.destroy();
        }

        const chart = new Chart(canvas, config);
        instances.set(canvas, chart);
        return chart;
    }

    function toDatasets(rows, options) {
        return options.keys.map(function(key, index) {
            const color = options.colors[index] || "#1843FA";
            return {
                label: options.labels[index] || key,
                data: rows.map(function(row) {
                    return Number(row[key]) || 0;
                }),
                borderColor: color,
                backgroundColor: options.fill ? transparent(color, 0.14) : color,
                borderWidth: 2,
                borderRadius: 4,
                tension: 0.35,
                fill: Boolean(options.fill && index === 0),
                pointRadius: options.pointRadius ?? 2.5,
                pointHoverRadius: 4
            };
        });
    }

    function baseOptions(options) {
        const yFormatter = options.yFormatter || function(value) { return value; };

        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: chartAnimation,
            plugins: {
                legend: {
                    display: options.legend !== false,
                    position: "bottom",
                    align: "start",
                    labels: {
                        boxWidth: 8,
                        boxHeight: 8,
                        color: "#0A0A0F",
                        font: { family: fontFamily, size: 11, weight: "600" }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const raw = context.raw;
                            const suffix = options.tooltipSuffix || "";
                            return `${context.dataset.label}: ${Number(raw).toLocaleString()}${suffix}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    grid: { display: false },
                    ticks: {
                        color: labelColor,
                        font: { family: fontFamily, size: 11 }
                    },
                    border: { display: false }
                },
                y: {
                    min: options.yMin,
                    max: options.yMax,
                    grid: { color: gridColor, borderDash: [3, 3] },
                    ticks: {
                        precision: options.yTickPrecision,
                        stepSize: options.yStepSize,
                        color: labelColor,
                        font: { family: fontFamily, size: 11 },
                        callback: function(value) {
                            return yFormatter(value);
                        }
                    },
                    border: { display: false }
                }
            }
        };
    }

    function groupedBar(canvas, rows, options) {
        return render(canvas, {
            type: "bar",
            data: {
                labels: rows.map(function(row) { return row.label || row.stage; }),
                datasets: toDatasets(rows, options)
            },
            options: baseOptions(options)
        });
    }

    function horizontalBar(canvas, rows, options) {
        const xFormatter = options.yFormatter || function(value) { return value; };

        return render(canvas, {
            type: "bar",
            data: {
                labels: rows.map(function(row) { return row.label || row.channel; }),
                datasets: [{
                    label: options.label || "",
                    data: rows.map(function(row) { return Number(row.value ?? row.successRate) || 0; }),
                    backgroundColor: rows.map(function(row) { return row.color || options.color || "#1843FA"; }),
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: chartAnimation,
                indexAxis: "y",
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const suffix = options.tooltipSuffix || "";
                                return `${context.dataset.label}: ${Number(context.raw).toLocaleString()}${suffix}`;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        min: options.xMin,
                        max: options.xMax,
                        grid: { color: gridColor, borderDash: [3, 3] },
                        ticks: {
                            color: labelColor,
                            font: { family: fontFamily, size: 11 },
                            callback: function(value) {
                                return xFormatter(value);
                            }
                        },
                        border: { display: false }
                    },
                    y: {
                        grid: { display: false },
                        ticks: {
                            color: "#0A0A0F",
                            font: { family: fontFamily, size: 11, weight: "600" }
                        },
                        border: { display: false }
                    }
                }
            }
        });
    }

    function line(canvas, rows, options) {
        return render(canvas, {
            type: "line",
            data: {
                labels: rows.map(function(row) { return row.label; }),
                datasets: toDatasets(rows, { ...options, fill: options.fill !== false })
            },
            options: baseOptions(options)
        });
    }

    function doughnut(canvas, rows, options) {
        return render(canvas, {
            type: "doughnut",
            data: {
                labels: rows.map(function(row) { return row.label || row.name; }),
                datasets: [{
                    data: rows.map(function(row) { return Number(row.value) || 0; }),
                    backgroundColor: rows.map(function(row, index) {
                        return row.color || (options.colors || [])[index] || "#1843FA";
                    }),
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: chartAnimation,
                cutout: options.cutout || "62%",
                plugins: {
                    legend: {
                        display: options.legend !== false,
                        position: "right",
                        labels: {
                            boxWidth: 8,
                            boxHeight: 8,
                            color: "#0A0A0F",
                            font: { family: fontFamily, size: 11, weight: "600" }
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return `${context.label}: ${Number(context.raw).toLocaleString()}${options.tooltipSuffix || ""}`;
                            }
                        }
                    }
                }
            }
        });
    }

    function composedTrend(canvas, rows) {
        const labels = rows.map(function(row) { return row.label; });
        const rates = rows.map(function(row) {
            return row.sends > 0 ? Number(((row.success / row.sends) * 100).toFixed(1)) : 0;
        });

        return render(canvas, {
            type: "line",
            data: {
                labels,
                datasets: [
                    {
                        label: "발송",
                        data: rows.map(function(row) { return row.sends; }),
                        borderColor: "#1843FA",
                        backgroundColor: transparent("#1843FA", 0.14),
                        fill: true,
                        tension: 0.35,
                        yAxisID: "y",
                        pointRadius: 2.5
                    },
                    {
                        label: "성공",
                        data: rows.map(function(row) { return row.success; }),
                        borderColor: "#10B981",
                        borderDash: [4, 3],
                        fill: false,
                        tension: 0.35,
                        yAxisID: "y",
                        pointRadius: 2.5
                    },
                    {
                        label: "성공률",
                        data: rates,
                        borderColor: "#F59E0B",
                        fill: false,
                        tension: 0.35,
                        yAxisID: "rate",
                        pointRadius: 2.5
                    }
                ]
            },
            options: {
                ...baseOptions({
                    yFormatter: function(value) { return Number(value).toLocaleString(); }
                }),
                scales: {
                    x: baseOptions({}).scales.x,
                    y: {
                        grid: { color: gridColor, borderDash: [3, 3] },
                        ticks: {
                            precision: 0,
                            color: labelColor,
                            font: { family: fontFamily, size: 11 },
                            callback: function(value) { return Number(value).toLocaleString(); }
                        },
                        border: { display: false }
                    },
                    rate: {
                        position: "right",
                        grid: { display: false },
                        ticks: {
                            color: labelColor,
                            font: { family: fontFamily, size: 11 },
                            callback: function(value) { return `${value}%`; }
                        },
                        border: { display: false }
                    }
                }
            }
        });
    }

    function transparent(hex, alpha) {
        const normalized = hex.replace("#", "");
        const r = parseInt(normalized.slice(0, 2), 16);
        const g = parseInt(normalized.slice(2, 4), 16);
        const b = parseInt(normalized.slice(4, 6), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }

    return {
        groupedBar,
        horizontalBar,
        line,
        doughnut,
        composedTrend
    };
})();
