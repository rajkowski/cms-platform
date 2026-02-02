/**
 * Analytics Charts Library
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 */

const AnalyticsCharts = (function() {
  'use strict';

  let chartInstances = {};

  /**
   * Render trend chart
   */
  function renderTrendChart(canvasId, data) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    // Destroy existing chart if any
    if (chartInstances[canvasId]) {
      chartInstances[canvasId].destroy();
    }

    const ctx = canvas.getContext('2d');
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

    const labels = data.dates || [];
    const values = data.values || [];

    const textColor = isDark ? '#ffffff' : '#212529';
    const gridColor = isDark ? '#404040' : '#dee2e6';
    const backgroundColor = isDark ? 'rgba(74, 158, 255, 0.1)' : 'rgba(0, 123, 255, 0.1)';

    chartInstances[canvasId] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: data.label || 'Trend',
          data: values,
          borderColor: '#007bff',
          backgroundColor: backgroundColor,
          borderWidth: 2,
          tension: 0.4,
          fill: true,
          pointRadius: 4,
          pointBackgroundColor: '#007bff',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            ticks: {
              color: textColor
            },
            grid: {
              color: gridColor,
              drawBorder: false
            }
          },
          y: {
            ticks: {
              color: textColor
            },
            grid: {
              color: gridColor,
              drawBorder: false
            }
          }
        }
      }
    });
  }

  /**
   * Render pie chart
   */
  function renderPieChart(canvasId, data, label) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    if (chartInstances[canvasId]) {
      chartInstances[canvasId].destroy();
    }

    const ctx = canvas.getContext('2d');
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

    const labels = data.labels || [];
    const values = data.values || [];
    const colors = generateColors(values.length);
    const textColor = isDark ? '#ffffff' : '#212529';

    chartInstances[canvasId] = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: colors,
          borderColor: isDark ? '#2d2d2d' : '#ffffff',
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: textColor,
              padding: 15,
              font: {
                size: 12
              }
            }
          }
        }
      }
    });
  }

  /**
   * Render response time chart
   */
  function renderResponseTimeChart(canvasId, data) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;

    if (chartInstances[canvasId]) {
      chartInstances[canvasId].destroy();
    }

    const ctx = canvas.getContext('2d');
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

    const textColor = isDark ? '#ffffff' : '#212529';
    const gridColor = isDark ? '#404040' : '#dee2e6';

    chartInstances[canvasId] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['p50', 'p95', 'p99'],
        datasets: [{
          label: 'Response Time (ms)',
          data: [data.p50 || 0, data.p95 || 0, data.p99 || 0],
          backgroundColor: ['#28a745', '#ffc107', '#dc3545'],
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            ticks: {
              color: textColor
            },
            grid: {
              color: gridColor,
              drawBorder: false
            }
          },
          y: {
            ticks: {
              color: textColor
            },
            grid: {
              color: gridColor,
              drawBorder: false
            }
          }
        }
      }
    });
  }

  /**
   * Generate chart colors
   */
  function generateColors(count) {
    const colors = [
      '#007bff',
      '#28a745',
      '#dc3545',
      '#ffc107',
      '#17a2b8',
      '#6f42c1',
      '#e83e8c',
      '#fd7e14'
    ];

    const result = [];
    for (let i = 0; i < count; i++) {
      result.push(colors[i % colors.length]);
    }
    return result;
  }

  /**
   * Handle window resize to update chart dimensions
   */
  function handleWindowResize() {
    Object.values(chartInstances).forEach(function(chart) {
      if (chart && chart.resize) {
        chart.resize();
      }
    });
  }

  /**
   * Setup resize listener for responsive charts
   */
  function setupResizeListener() {
    let resizeTimeout;
    window.addEventListener('resize', function() {
      clearTimeout(resizeTimeout);
      resizeTimeout = setTimeout(handleWindowResize, 250); // Debounce resize events
    });
  }

  // Initialize resize listener when module loads
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupResizeListener);
  } else {
    setupResizeListener();
  }

  // Public API
  return {
    renderTrendChart: renderTrendChart,
    renderPieChart: renderPieChart,
    renderResponseTimeChart: renderResponseTimeChart
  };
})();
