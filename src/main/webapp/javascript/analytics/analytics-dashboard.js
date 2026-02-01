/**
 * Analytics Dashboard Main Application
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 */

const AnalyticsDashboard = (function() {
  'use strict';

  let config = {
    containerId: 'visual-analytics-dashboard-wrapper',
    defaultTimeRange: '7d',
    liveEnabled: true,
    technicalMetricsEnabled: true
  };

  let state = {
    currentTab: 'overview',
    timeRange: '7d',
    filters: {
      page: '',
      pageId: '',
      device: ''
    },
    cachedData: {},
    lastFetch: {},
    pageOptions: {} // Map of page names/links to IDs
  };

  const CACHE_DURATION = 60000; // 60 seconds

  /**
   * Initialize the dashboard
   */
  function init(options) {
    config = Object.assign({}, config, options);
    state.timeRange = config.defaultTimeRange;

    // Load saved state from local storage
    loadState();

    // Setup event listeners
    setupEventListeners();

    // Load filter options
    loadAndPopulateFilterOptions();

    // Sync UI with loaded state
    syncUIWithState();

    // Load initial data for the restored tab
    loadTabData(state.currentTab);
  }

  /**
   * Setup all event listeners
   */
  function setupEventListeners() {
    // Tab navigation
    document.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', handleTabClick);
    });

    // Time range buttons
    document.querySelectorAll('[data-range]').forEach(btn => {
      btn.addEventListener('click', handleTimeRangeChange);
    });

    // Custom range modal
    const customRangeBtn = document.getElementById('custom-range-btn');
    if (customRangeBtn) {
      customRangeBtn.addEventListener('click', openCustomRangeModal);
    }

    // Modal controls
    const modalOverlay = document.getElementById('modal-overlay');
    const modalCloseBtn = document.getElementById('modal-close-btn');
    const modalCancelBtn = document.getElementById('modal-cancel-btn');
    const modalApplyBtn = document.getElementById('modal-apply-btn');

    if (modalOverlay) modalOverlay.addEventListener('click', closeCustomRangeModal);
    if (modalCloseBtn) modalCloseBtn.addEventListener('click', closeCustomRangeModal);
    if (modalCancelBtn) modalCancelBtn.addEventListener('click', closeCustomRangeModal);
    if (modalApplyBtn) modalApplyBtn.addEventListener('click', applyCustomRange);

    // Filters
    const filterPage = document.getElementById('filter-page');
    const filterDevice = document.getElementById('filter-device');
    const clearFiltersBtn = document.getElementById('clear-filters-btn');

    if (filterPage) {
      filterPage.addEventListener('change', handleFilterChange);
      filterPage.addEventListener('blur', handlePageFilterBlur);
      filterPage.addEventListener('keyup', debounce(handleFilterChange, 500));
    }
    if (filterDevice) filterDevice.addEventListener('change', handleFilterChange);
    if (clearFiltersBtn) clearFiltersBtn.addEventListener('click', clearFilters);

    // Refresh button
    const refreshBtn = document.getElementById('refresh-btn');
    if (refreshBtn) refreshBtn.addEventListener('click', refreshData);

    // Dark mode toggle
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    if (darkModeToggle) darkModeToggle.addEventListener('click', toggleDarkMode);
  }

  /**
   * Load filter options and populate filter dropdowns
   */
  function loadAndPopulateFilterOptions() {
    AnalyticsAPI.getFilterOptions()
      .then(data => {
        if (data && data.success !== false) {
          populateFilterDropdowns(data);
        }
      })
      .catch(error => {
        console.warn('Failed to load filter options:', error);
        // Continue without filter options populated
      });
  }

  /**
   * Populate filter dropdowns with options from the API
   */
  function populateFilterDropdowns(filterData) {
    // Populate page filter
    const filterPageInput = document.getElementById('filter-page');
    if (filterPageInput && filterData.pages && Array.isArray(filterData.pages)) {
      // Create a datalist for autocomplete
      let datalist = document.getElementById('pages-list');
      if (!datalist) {
        datalist = document.createElement('datalist');
        datalist.id = 'pages-list';
        document.body.appendChild(datalist);
        filterPageInput.setAttribute('list', 'pages-list');
      }
      datalist.innerHTML = '';
      filterData.pages.forEach(page => {
        const option = document.createElement('option');
        const displayValue = page.title || page.name || page.link || '';
        option.value = displayValue;
        option.dataset.id = page.id || '';
        datalist.appendChild(option);
        // Store page ID mapping for quick lookup
        state.pageOptions[displayValue] = page.id || '';
        if (page.link) {
          state.pageOptions[page.link] = page.id || '';
        }
      });
    }

    // Populate device filter
    const filterDeviceSelect = document.getElementById('filter-device');
    if (filterDeviceSelect && filterData.devices && Array.isArray(filterData.devices)) {
      // Keep the existing "All Devices" option
      const currentValue = filterDeviceSelect.value;
      filterDeviceSelect.innerHTML = '<option value="">All Devices</option>';
      filterData.devices.forEach(device => {
        const option = document.createElement('option');
        option.value = device.toLowerCase();
        option.textContent = device;
        filterDeviceSelect.appendChild(option);
      });
      filterDeviceSelect.value = currentValue;
    }
  }

  /**
   * Handle tab navigation
   */
  function handleTabClick(event) {
    event.preventDefault();
    const tab = event.currentTarget.getAttribute('data-tab');
    switchTab(tab);
  }

  /**
   * Switch to a different tab
   */
  function switchTab(tabName) {
    // Update UI
    document.querySelectorAll('.nav-item').forEach(item => {
      item.classList.toggle('active', item.getAttribute('data-tab') === tabName);
    });

    document.querySelectorAll('.dashboard-tab').forEach(tab => {
      const isActive = tab.id === tabName + '-tab';
      tab.classList.toggle('active', isActive);
      // Also toggle active class on child tab-content
      const tabContent = tab.querySelector('.tab-content');
      if (tabContent) {
        tabContent.classList.toggle('active', isActive);
      }
    });

    state.currentTab = tabName;
    saveState();

    // Load data for the tab
    loadTabData(tabName);
  }

  /**
   * Sync UI with current state (used after loading state from storage)
   */
  function syncUIWithState() {
    // Sync tab UI with state
    document.querySelectorAll('.nav-item').forEach(item => {
      item.classList.toggle('active', item.getAttribute('data-tab') === state.currentTab);
    });

    document.querySelectorAll('.dashboard-tab').forEach(tab => {
      const isActive = tab.id === state.currentTab + '-tab';
      tab.classList.toggle('active', isActive);
      const tabContent = tab.querySelector('.tab-content');
      if (tabContent) {
        tabContent.classList.toggle('active', isActive);
      }
    });

    // Sync time range buttons
    document.querySelectorAll('[data-range]').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-range') === state.timeRange);
    });

    // Sync filter inputs
    const filterPage = document.getElementById('filter-page');
    const filterDevice = document.getElementById('filter-device');
    if (filterPage) filterPage.value = state.filters.page || '';
    if (filterDevice) filterDevice.value = state.filters.device || '';

    // Update clear button state
    updateClearButtonState();
  }

  /**
   * Load data for a specific tab
   */
  function loadTabData(tabName) {
    if (!shouldFetchData(tabName)) {
      return; // Use cached data
    }

    showLoadingIndicator(true);

    const params = {
      range: state.timeRange,
      ...state.filters
    };

    let endpoint = '';
    switch (tabName) {
      case 'overview':
        endpoint = '/json/analyticsOverviewLoad';
        break;
      case 'live':
        endpoint = '/json/analyticsLiveLoad';
        break;
      case 'content':
        endpoint = '/json/analyticsContentLoad';
        break;
      case 'audience':
        endpoint = '/json/analyticsAudienceLoad';
        break;
      case 'technical':
        endpoint = '/json/analyticsTechnicalLoad';
        break;
      default:
        return;
    }

    AnalyticsAPI.fetchData(endpoint, params)
      .then(data => {
        state.cachedData[tabName] = data;
        state.lastFetch[tabName] = Date.now();
        renderTabData(tabName, data);
        updateDataFreshness();
      })
      .catch(error => {
        console.error('Error loading tab data:', error);
        showErrorMessage(`Failed to load ${tabName} data`);
      })
      .finally(() => {
        showLoadingIndicator(false);
      });
  }

  /**
   * Check if we need to fetch fresh data
   */
  function shouldFetchData(tabName) {
    const lastFetch = state.lastFetch[tabName];
    if (!lastFetch) return true;
    return Date.now() - lastFetch > CACHE_DURATION;
  }

  /**
   * Render data for a specific tab
   */
  function renderTabData(tabName, data) {
    if (!data) {
      showEmptyState(tabName);
      return;
    }

    hideEmptyState(tabName);

    switch (tabName) {
      case 'overview':
        renderOverviewTab(data);
        break;
      case 'live':
        renderLiveTab(data);
        break;
      case 'content':
        renderContentTab(data);
        break;
      case 'audience':
        renderAudienceTab(data);
        break;
      case 'technical':
        renderTechnicalTab(data);
        break;
    }
  }

  /**
   * Render Overview Tab
   */
  function renderOverviewTab(data) {
    // Render KPI Cards
    const kpiCardsContainer = document.getElementById('kpi-cards');
    if (kpiCardsContainer && data.kpis) {
      kpiCardsContainer.classList.remove('skeleton');
      kpiCardsContainer.innerHTML = '';
      const kpis = [
        { key: 'activeUsers', label: 'Active Users', icon: 'fa-users' },
        { key: 'sessions', label: 'Sessions', icon: 'fa-link' },
        { key: 'pageViews', label: 'Page Views', icon: 'fa-file' },
        { key: 'avgSessionDuration', label: 'Avg Duration', icon: 'fa-clock' },
        { key: 'bounceRate', label: 'Bounce Rate', icon: 'fa-bounce' },
        { key: 'newUsers', label: 'New Users', icon: 'fa-user-plus' }
      ];

      kpis.forEach(kpi => {
        const value = data.kpis[kpi.key];
        const trend = data.kpis[kpi.key + 'Trend'] || 0;
        let displayValue = value;
        let tooltip = '';
        
        // Format avgSessionDuration in human-readable format
        if (kpi.key === 'avgSessionDuration') {
          displayValue = formatDuration(value);
          tooltip = `${value} seconds`;
        }
        // Format bounceRate as percentage
        else if (kpi.key === 'bounceRate') {
          displayValue = formatPercent(value);
        }
        
        const card = createKPICard(kpi.label, displayValue, trend, tooltip);
        kpiCardsContainer.appendChild(card);
      });
    }

    // Render Trends Section
    renderTrendsSummary(data);

    // Render Trend Chart
    renderTrendChart(data);
  }

  /**
   * Render Trends Summary Section
   */
  function renderTrendsSummary(data) {
    const trendsContainer = document.getElementById('trends-summary');
    if (!trendsContainer) return;

    trendsContainer.classList.remove('skeleton');
    trendsContainer.innerHTML = '';

    if (!data.trends || data.trends.length === 0) {
      trendsContainer.innerHTML = '<p class="text-muted">No trend data available</p>';
      return;
    }

    // Group trends by metric
    const metricTrends = {};
    data.trends.forEach(trend => {
      if (!metricTrends[trend.metric]) {
        metricTrends[trend.metric] = [];
      }
      metricTrends[trend.metric].push(trend);
    });

    // Render each trend metric
    Object.keys(metricTrends).forEach(metric => {
      const trendGroup = metricTrends[metric];
      const trendCard = document.createElement('div');
      trendCard.className = 'trend-card';

      let trendContent = `<div class="trend-metric">${escapeHtml(metric)}</div>`;

      trendGroup.forEach(trend => {
        const trendValue = normalizeNumber(trend.value) || 0;
        const trendClass = trendValue >= 0 ? 'positive' : 'negative';
        const trendIcon = trendValue >= 0 ? 'fa-arrow-up' : 'fa-arrow-down';
        const trendText = trendValue >= 0 ? `+${trendValue.toFixed(1)}%` : `${trendValue.toFixed(1)}%`;
        const label = trend.label || trend.period || 'Trend';

        trendContent += `
          <div class="trend-item">
            <span class="trend-label">${escapeHtml(label)}</span>
            <span class="trend-value ${trendClass}">
              <i class="fas ${trendIcon}"></i>
              ${trendText}
            </span>
          </div>
        `;
      });

      trendCard.innerHTML = trendContent;
      trendsContainer.appendChild(trendCard);
    });
  }

  /**
   * Render Trend Chart
   */
  function renderTrendChart(data) {
    const trendChartContainer = document.querySelector('#trend-chart-container');
    if (!trendChartContainer) {
      console.warn('Trend chart container (#trend-chart-container) not found in DOM');
      return;
    }

    try {
      trendChartContainer.classList.remove('skeleton');

      // Ensure content is cleared before rendering
      const existingChart = trendChartContainer.querySelector('canvas#trend-chart');
      if (!existingChart) {
        trendChartContainer.innerHTML = '<canvas id="trend-chart" style="max-height: 300px;"></canvas>';
      }

      // Get or verify canvas element
      const canvas = document.getElementById('trend-chart');
      if (!canvas) {
        console.error('Failed to create or find trend chart canvas');
        trendChartContainer.innerHTML = '<p class="text-muted">Error rendering trend chart</p>';
        return;
      }

      // Priority 1: Handle trendSeries as array of label/value objects
      if (data && Array.isArray(data.trendSeries) && data.trendSeries.length > 0) {
        const trendSeries = {
          dates: data.trendSeries.map(item => item.label),
          values: data.trendSeries.map(item => Number.parseFloat(item.value))
        };
        console.log('Rendering trend chart with API data:', trendSeries);
        AnalyticsCharts.renderTrendChart('trend-chart', trendSeries);
        return;
      }

      // Priority 2: Handle trendSeries with dates and values properties
      if (data && data.trendSeries && data.trendSeries.dates && data.trendSeries.values) {
        console.log('Rendering trend chart with API data:', data.trendSeries);
        AnalyticsCharts.renderTrendChart('trend-chart', data.trendSeries);
        return;
      }

      // Priority 3: Generate data from available metrics
      // const trendSeries = generateTrendData(data);
      // if (trendSeries && trendSeries.dates && trendSeries.values && trendSeries.dates.length > 0) {
      //   console.log('Rendering trend chart with generated data:', trendSeries);
      //   AnalyticsCharts.renderTrendChart('trend-chart', trendSeries);
      //   return;
      // }

      // Priority 4: Show placeholder message
      console.warn('No trend data available for chart rendering');
      trendChartContainer.innerHTML = '<p class="text-muted">No trend data available</p>';
    } catch (error) {
      console.error('Error rendering trend chart:', error);
      trendChartContainer.innerHTML = '<p class="text-muted">Error loading trend chart</p>';
    }
  }

  /**
   * Generate trend data from available metrics
   */
  function generateTrendData(data) {
    if (!data) {
      console.warn('No data provided to generateTrendData, using default fallback');
      return [];
    }

    // Try to use trendData if available
    if (data.trendData && Array.isArray(data.trendData) && data.trendData.length > 0) {
      const dates = [];
      const values = [];
      data.trendData.forEach(point => {
        if (point.date) dates.push(point.date);
        if (point.value !== undefined) values.push(point.value);
      });
      if (dates.length > 0 && values.length > 0) {
        return {
          label: data.label || 'Trend',
          dates: dates,
          values: values
        };
      }
    }

    // Try to use kpis data if available
    if (data.kpis && data.kpis.pageViews) {
      // Generate synthetic data based on time range
      const dates = generateDateLabels(state.timeRange);
      const values = generateRandomValues(dates.length, 100, 500);
      return {
        label: 'Page Views Trend',
        dates: dates,
        values: values
      };
    }

    // Generate synthetic data from time range if no other data available
    if (state.timeRange) {
      const dates = generateDateLabels(state.timeRange);
      const values = generateRandomValues(dates.length, 100, 500);
      return {
        label: 'Page Views Trend',
        dates: dates,
        values: values
      };
    }

    // Ultimate fallback
    return [];
  }

  /**
   * Generate date labels based on time range
   */
  function generateDateLabels(timeRange) {
    const dates = [];
    const today = new Date();
    let dayCount = 7; // default

    if (timeRange === '1d') {
      dayCount = 1;
    } else if (timeRange === '30d') {
      dayCount = 30;
    } else if (timeRange === '12m') {
      dayCount = 365;
    } else if (timeRange.includes(',')) {
      // Custom date range - estimate days
      const parts = timeRange.split(',');
      const startDate = new Date(parts[0]);
      const endDate = new Date(parts[1]);
      dayCount = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1;
    }

    const step = Math.max(1, Math.floor(dayCount / 14)); // Show max 14 data points
    for (let i = dayCount - 1; i >= 0; i -= step) {
      const date = new Date(today);
      date.setDate(date.getDate() - i);
      dates.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
    }

    return dates;
  }

  /**
   * Generate random values for demonstration
   */
  function generateRandomValues(count, min, max) {
    const values = [];
    let baseValue = (min + max) / 2;
    for (let i = 0; i < count; i++) {
      const variation = (Math.random() - 0.5) * (max - min) * 0.2;
      baseValue = Math.max(min, Math.min(max, baseValue + variation));
      values.push(Math.round(baseValue));
    }
    return values;
  }

  /**
   * Render Live Tab
   */
  function renderLiveTab(data) {
    const sessionsContainer = document.getElementById('sessions-tbody');
    const sessionsTable = document.getElementById('active-sessions');
    if (sessionsTable) {
      sessionsTable.classList.remove('skeleton');
    }
    if (sessionsContainer && data.activeSessions) {
      sessionsContainer.innerHTML = '';
      data.activeSessions.forEach(session => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${escapeHtml(session.userType || 'Guest')}</td>
          <td>${escapeHtml(session.page || '-')}</td>
          <td>${escapeHtml(session.device || 'Unknown')}</td>
          <td>${escapeHtml(session.location || 'Unknown')}</td>
          <td>${formatDuration(session.duration)}</td>
        `;
        sessionsContainer.appendChild(row);
      });
    }

    const eventsContainer = document.getElementById('recent-events');
    if (eventsContainer) {
      eventsContainer.classList.remove('skeleton');
      if (data.recentEvents) {
        eventsContainer.innerHTML = '';
        data.recentEvents.forEach(event => {
          const eventEl = createEventElement(event);
          eventsContainer.appendChild(eventEl);
        });
      }
    }
  }

  /**
   * Render Content Tab
   */
  function renderContentTab(data) {
    const pagesContainer = document.getElementById('pages-tbody');
    const pagesTable = document.getElementById('top-pages');
    if (pagesTable) {
      pagesTable.classList.remove('skeleton');
    }
    if (pagesContainer && data.topPages) {
      pagesContainer.innerHTML = '';
      data.topPages.forEach(page => {
        const row = document.createElement('tr');
        const pageLabel = page.pagePath || page.label || page.title || page.pageId || '-';
        const views = page.views || page.value || 0;
        const uniqueUsers = page.uniqueUsers || page.unique_users || page.users || 0;
        const avgTime = page.avgTime || page.avg_time_on_page || 0;
        const bounceRate = page.bounceRate || page.bounce_rate || 0;
        
        row.innerHTML = `
          <td><a href="${escapeHtml(pageLabel)}" target="_blank">${escapeHtml(pageLabel)}</a></td>
          <td>${formatNumber(views)}</td>
          <td>${formatNumber(uniqueUsers)}</td>
          <td>${formatDuration(avgTime)}</td>
          <td>${formatPercent(bounceRate)}</td>
        `;
        pagesContainer.appendChild(row);
      });
    }

    const assetsContainer = document.getElementById('assets-tbody');
    const assetsTable = document.getElementById('top-assets');
    if (assetsTable) {
      assetsTable.classList.remove('skeleton');
    }
    if (assetsContainer && data.topAssets) {
      assetsContainer.innerHTML = '';
      data.topAssets.forEach(asset => {
        const row = document.createElement('tr');
        const assetName = asset.assetName || asset.name || '-';
        const assetType = asset.assetType || asset.type || 'Unknown';
        const downloads = asset.downloads || 0;
        const views = asset.views || 0;
        
        row.innerHTML = `
          <td><a href="${escapeHtml(asset.assetPath || '')}" target="_blank">${escapeHtml(assetName)}</a></td>
          <td>${escapeHtml(assetType)}</td>
          <td>${formatNumber(downloads)}</td>
          <td>${formatNumber(views)}</td>
        `;
        assetsContainer.appendChild(row);
      });
    }
  }

  /**
   * Render Audience Tab
   */
  function renderAudienceTab(data) {
    const segmentsContainer = document.getElementById('audience-segments');
    if (segmentsContainer) {
      segmentsContainer.classList.remove('skeleton');
      segmentsContainer.innerHTML = '';
      // Show average session duration as primary metric
      const durationCard = createSegmentCard({
        name: 'Avg Session Duration',
        count: data.avgSessionDuration || 0,
        type: 'duration'
      });
      segmentsContainer.appendChild(durationCard);
    }

    // Render Device Distribution Chart
    const deviceChartContainer = document.querySelector('#device-chart');
    if (deviceChartContainer) {
      deviceChartContainer.closest('.chart-container')?.classList.remove('skeleton');
    }
    if (data.devices && Array.isArray(data.devices)) {
      const deviceData = {
        labels: data.devices.map(d => d.label),
        values: data.devices.map(d => Number.parseInt(d.value))
      };
      AnalyticsCharts.renderPieChart('device-chart', deviceData, 'Devices');
    }

    // Render Browser Distribution Chart
    const browserChartContainer = document.querySelector('#browser-chart');
    if (browserChartContainer) {
      browserChartContainer.closest('.chart-container')?.classList.remove('skeleton');
    }
    if (data.browsers && Array.isArray(data.browsers)) {
      const browserData = {
        labels: data.browsers.map(b => b.label),
        values: data.browsers.map(b => Number.parseInt(b.value))
      };
      AnalyticsCharts.renderPieChart('browser-chart', browserData, 'Browsers');
    }
  }

  /**
   * Render Technical Tab
   */
  function renderTechnicalTab(data) {
    const metricsContainer = document.getElementById('performance-metrics');
    if (metricsContainer) {
      metricsContainer.classList.remove('skeleton');
      if (data.performance) {
        metricsContainer.innerHTML = '';
        const metrics = [
          { key: 'p50', label: 'p50 Response Time' },
          { key: 'p95', label: 'p95 Response Time' },
          { key: 'p99', label: 'p99 Response Time' }
        ];

        metrics.forEach(metric => {
          const value = data.performance[metric.key];
          const card = createMetricCard(metric.label, value + 'ms');
          metricsContainer.appendChild(card);
        });
      }
    }

    const responseChartContainer = document.querySelector('#response-time-chart');
    if (responseChartContainer) {
      responseChartContainer.closest('.chart-container')?.classList.remove('skeleton');
    }
    if (data.performance) {
      AnalyticsCharts.renderResponseTimeChart('response-time-chart', data.performance);
    }

    const errorsContainer = document.getElementById('errors-tbody');
    const errorsTable = document.getElementById('error-metrics');
    if (errorsTable) {
      errorsTable.classList.remove('skeleton');
    }
    if (errorsContainer && data.errors) {
      errorsContainer.innerHTML = '';
      data.errors.forEach(error => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${error.statusCode}</td>
          <td>${formatNumber(error.count)}</td>
          <td>${formatPercent(error.percentage)}</td>
        `;
        errorsContainer.appendChild(row);
      });
    }
  }

  /**
   * Create KPI Card Element
   */
  function createKPICard(label, value, trend, tooltip = '') {
    const card = document.createElement('div');
    card.className = 'kpi-card';
    const trendClass = trend >= 0 ? 'positive' : 'negative';
    const trendIcon = trend >= 0 ? 'fa-arrow-up' : 'fa-arrow-down';
    const trendText = trend >= 0 ? `+${trend}%` : `${trend}%`;
    const tooltipAttr = tooltip ? `title="${escapeHtml(tooltip)}"` : '';
    
    // Check if value is already formatted (contains letters like m, s, or %)
    const displayValue = typeof value === 'string' && /[a-z%]/i.test(value) 
      ? escapeHtml(value) 
      : formatNumber(value);

    card.innerHTML = `
      <div class="kpi-value" ${tooltipAttr}>${displayValue}</div>
      <div class="kpi-label">${escapeHtml(label)}</div>
      <div class="kpi-trend ${trendClass}">
        <i class="fas ${trendIcon}"></i>
        <span>${trendText}</span>
      </div>
    `;
    return card;
  }

  /**
   * Create Segment Card Element
   */
  function createSegmentCard(segment) {
    const card = document.createElement('div');
    card.className = 'segment-card';
    card.innerHTML = `
      <div class="segment-value">${formatNumber(segment.count)}</div>
      <div class="segment-label">${escapeHtml(segment.name)}</div>
    `;
    return card;
  }

  /**
   * Create Metric Card Element
   */
  function createMetricCard(label, value) {
    const card = document.createElement('div');
    card.className = 'metric-card';
    card.innerHTML = `
      <div class="metric-value">${escapeHtml(value)}</div>
      <div class="metric-label">${escapeHtml(label)}</div>
    `;
    return card;
  }

  /**
   * Create Event Element
   */
  function createEventElement(event) {
    const el = document.createElement('div');
    el.className = 'event-item';
    const icons = {
      'page-view': 'fa-file',
      'download': 'fa-download',
      'video-play': 'fa-play',
      'form-submit': 'fa-check'
    };
    const icon = icons[event.type] || 'fa-circle';

    el.innerHTML = `
      <div class="event-icon">
        <i class="fas ${icon}"></i>
      </div>
      <div class="event-content">
        <div class="event-type">${escapeHtml(event.type)}</div>
        <div class="event-details">${escapeHtml(event.page || 'Unknown page')}</div>
      </div>
      <div class="event-time">${formatTime(event.timestamp)}</div>
    `;
    return el;
  }

  /**
   * Handle time range change
   */
  function handleTimeRangeChange(event) {
    const range = event.currentTarget.getAttribute('data-range');
    state.timeRange = range;
    state.lastFetch = {}; // Clear cache to force refresh
    saveState();

    // Update button states
    document.querySelectorAll('[data-range]').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-range') === range);
    });

    // Reload current tab
    loadTabData(state.currentTab);
  }

  /**
   * Handle page filter blur to resolve page ID
   */
  function handlePageFilterBlur() {
    const filterPage = document.getElementById('filter-page');
    if (!filterPage) return;

    const pageValue = filterPage.value;
    if (pageValue && state.pageOptions[pageValue]) {
      // Valid page was selected, store the ID
      state.filters.pageId = state.pageOptions[pageValue];
    } else if (!pageValue) {
      // Filter was cleared
      state.filters.pageId = '';
    }
    // If pageValue exists but no ID mapping, we keep the page name as-is
    updateClearButtonState();
  }

  /**
   * Handle filter changes
   */
  function handleFilterChange() {
    const filterPage = document.getElementById('filter-page');
    const filterDevice = document.getElementById('filter-device');

    state.filters.page = filterPage ? filterPage.value : '';
    state.filters.device = filterDevice ? filterDevice.value : '';
    state.lastFetch = {}; // Clear cache
    saveState();

    // Update clear button state
    updateClearButtonState();

    loadTabData(state.currentTab);
  }

  /**
   * Update clear button visibility and styling based on active filters
   */
  function updateClearButtonState() {
    const clearBtn = document.getElementById('clear-filters-btn');
    if (!clearBtn) return;

    const hasActiveFilters = state.filters.page || state.filters.device;
    if (hasActiveFilters) {
      clearBtn.classList.add('active');
      clearBtn.setAttribute('aria-label', 'Clear all active filters');
      clearBtn.title = 'Clear all active filters';
    } else {
      clearBtn.classList.remove('active');
      clearBtn.setAttribute('aria-label', 'No active filters');
      clearBtn.title = 'No active filters';
    }
  }

  /**
   * Clear all filters
   */
  function clearFilters() {
    const filterPage = document.getElementById('filter-page');
    const filterDevice = document.getElementById('filter-device');

    if (filterPage) filterPage.value = '';
    if (filterDevice) filterDevice.value = '';

    state.filters = { page: '', pageId: '', device: '' };
    state.lastFetch = {};
    saveState();

    // Update clear button state
    updateClearButtonState();

    loadTabData(state.currentTab);
  }

  /**
   * Open custom date range modal
   */
  function openCustomRangeModal() {
    const modal = document.getElementById('date-range-modal');
    if (modal) modal.style.display = 'flex';
  }

  /**
   * Close custom date range modal
   */
  function closeCustomRangeModal() {
    const modal = document.getElementById('date-range-modal');
    if (modal) modal.style.display = 'none';
  }

  /**
   * Apply custom date range
   */
  function applyCustomRange() {
    const startDate = document.getElementById('custom-start-date').value;
    const endDate = document.getElementById('custom-end-date').value;

    if (!startDate || !endDate) {
      alert('Please select both start and end dates');
      return;
    }

    state.timeRange = startDate + ',' + endDate;
    state.lastFetch = {};
    saveState();
    closeCustomRangeModal();

    loadTabData(state.currentTab);
  }

  /**
   * Refresh data
   */
  function refreshData() {
    state.lastFetch = {};
    loadTabData(state.currentTab);
  }

  /**
   * Toggle dark mode
   */
  function toggleDarkMode() {
    const wrapper = document.getElementById(config.containerId);
    const isDark = wrapper.getAttribute('data-theme') === 'dark';
    const newTheme = isDark ? 'light' : 'dark';

    wrapper.setAttribute('data-theme', newTheme);
    localStorage.setItem('analyticsDashboard.theme', newTheme);
  }

  /**
   * Show/hide loading indicator
   */
  function showLoadingIndicator(show) {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) {
      indicator.style.display = show ? 'flex' : 'none';
    }
  }

  /**
   * Update data freshness indicator
   */
  function updateDataFreshness() {
    const indicator = document.getElementById('data-freshness');
    if (indicator) {
      indicator.textContent = 'Updated: ' + new Date().toLocaleTimeString();
    }
  }

  /**
   * Show empty state message
   */
  function showEmptyState(tabName) {
    const emptyState = document.getElementById(tabName + '-empty-state');
    if (emptyState) emptyState.style.display = 'block';
  }

  /**
   * Hide empty state message
   */
  function hideEmptyState(tabName) {
    const emptyState = document.getElementById(tabName + '-empty-state');
    if (emptyState) emptyState.style.display = 'none';
  }

  /**
   * Show error message
   */
  function showErrorMessage(message) {
    // This can be enhanced with a proper notification system
    console.error(message);
  }

  /**
   * Save state to local storage
   */
  function saveState() {
    localStorage.setItem('analyticsDashboard.state', JSON.stringify({
      currentTab: state.currentTab,
      timeRange: state.timeRange,
      filters: state.filters
    }));
  }

  /**
   * Load state from local storage
   */
  function loadState() {
    const saved = localStorage.getItem('analyticsDashboard.state');
    if (saved) {
      const data = JSON.parse(saved);
      state.currentTab = data.currentTab || 'overview';
      state.timeRange = data.timeRange || config.defaultTimeRange;
      state.filters = data.filters || { page: '', pageId: '', device: '' };
    }

    // Load theme preference
    const theme = localStorage.getItem('analyticsDashboard.theme') || 'light';
    const wrapper = document.getElementById(config.containerId);
    wrapper.setAttribute('data-theme', theme);
  }

  /**
   * Utility: Debounce function
   */
  function debounce(func, delay) {
    let timeoutId;
    return function(...args) {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => func.apply(this, args), delay);
    };
  }

  /**
   * Utility: Escape HTML
   */
  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Utility: Format number
   */
  function formatNumber(num) {
    const normalized = normalizeNumber(num);
    if (normalized === null) return '0';
    if (normalized >= 1000000) {
      return (normalized / 1000000).toFixed(1) + 'M';
    } else if (normalized >= 1000) {
      return (normalized / 1000).toFixed(1) + 'K';
    }
    return normalized.toString();
  }

  /**
   * Utility: Format percent
   */
  function formatPercent(value) {
    const normalized = normalizeNumber(value);
    if (normalized === null) return '0%';
    return normalized.toFixed(1) + '%';
  }

  /**
   * Utility: Format duration
   */
  function formatDuration(seconds) {
    const normalized = normalizeNumber(seconds);
    if (normalized === null) return '0s';
    if (normalized < 60) return Math.floor(normalized) + 's';
    if (normalized < 3600) return (normalized / 60).toFixed(1) + 'm';
    return (normalized / 3600).toFixed(1) + 'h';
  }

  /**
   * Utility: Normalize numeric inputs (numbers or numeric strings)
   */
  function normalizeNumber(value) {
    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }
    if (typeof value === 'string') {
      const parsed = Number.parseFloat(value);
      if (Number.isFinite(parsed)) {
        return parsed;
      }
    }
    return null;
  }

  /**
   * Utility: Format time
   */
  function formatTime(timestamp) {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
  }

  // Public API
  return {
    init: init
  };
})();
