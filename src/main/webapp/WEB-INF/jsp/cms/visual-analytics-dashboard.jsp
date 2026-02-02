<%--
  ~ Copyright 2026 Matt Rajkowski
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>

<web:script package="chartjs" file="chart.umd.js" />

<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-analytics-dashboard.css" />
</g:compress>

<div id="visual-analytics-dashboard-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Dashboard Toolbar -->
  <div id="dashboard-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <h2>Analytics Dashboard</h2>
    </div>

    <!-- Left Section: Time Range Picker -->
    <div class="toolbar-section left">
      <div id="time-range-picker" class="button-group">
        <button type="button" class="button tiny secondary no-gap" data-range="1d" title="Last 24 hours">1d</button>
        <button type="button" class="button tiny secondary no-gap active" data-range="7d" title="Last 7 Days">7d</button>
        <button type="button" class="button tiny secondary no-gap" data-range="30d" title="Last 30 Days">30d</button>
        <button type="button" class="button tiny secondary no-gap" data-range="12m" title="Last 12 Months">12m</button>
        <button type="button" class="button tiny secondary no-gap radius" id="custom-range-btn" title="Custom Range">...</button>
      </div>
    </div>

    <!-- Center Section: Filters -->
    <div class="toolbar-section center">
      <input type="text" id="filter-page" class="analytics-filter-input" placeholder="Filter by page..." />
      <select id="filter-device" class="analytics-filter-select">
        <option value="">All Devices</option>
        <option value="desktop">Desktop</option>
        <option value="mobile">Mobile</option>
        <option value="tablet">Tablet</option>
      </select>
      <button id="clear-filters-btn" class="button tiny radius secondary no-gap" title="No active filters" aria-label="No active filters">
        <i class="${font:far()} fa-times"></i> Clear
      </button>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
      <span id="data-freshness" class="data-freshness-indicator"></span>
    </div>

    <div class="titlebar-right">
      <button id="dark-mode-toggle" class="titlebar-btn" title="Toggle Dark Mode">
        <i class="${font:far()} fa-moon"></i>
      </button>
      <button id="refresh-btn" class="titlebar-btn" title="Refresh Data">
        <i class="${font:far()} fa-sync-alt"></i>
      </button>
    </div>
  </div>

  <!-- Dashboard Container -->
  <div id="dashboard-container">
    <!-- Left Navigation Panel -->
    <nav id="dashboard-nav" class="dashboard-nav">
      <ul class="dashboard-nav-list">
        <li><a href="javascript:void(0)" class="nav-item active" data-tab="overview"><i class="${font:far()} fa-chart-line"></i> Overview</a></li>
        <li><a href="javascript:void(0)" class="nav-item" data-tab="live"><i class="${font:far()} fa-wave-square"></i> Live</a></li>
        <li><a href="javascript:void(0)" class="nav-item" data-tab="content"><i class="${font:far()} fa-file-lines"></i> Content</a></li>
        <li><a href="javascript:void(0)" class="nav-item" data-tab="audience"><i class="${font:far()} fa-users"></i> Audience</a></li>
        <c:if test="${technicalMetricsEnabled}">
          <li><a href="javascript:void(0)" class="nav-item" data-tab="technical"><i class="${font:far()} fa-sliders"></i> Technical</a></li>
        </c:if>
      </ul>
    </nav>

    <!-- Main Content Area -->
    <div id="dashboard-content" class="dashboard-content">

      <!-- Overview Tab -->
      <section id="overview-tab" class="dashboard-tab active">
        <div class="tab-content active">

          <!-- Trend Chart -->
          <div id="trend-chart-container" class="chart-container skeleton">
            <h3><i class="fas fa-chart-line"></i> Page Views Trend</h3>
            <!-- Canvas will be created dynamically by JavaScript if needed -->
            <canvas id="trend-chart" style="max-height: 300px;"></canvas>
          </div>

          <!-- KPI Cards Grid -->
          <div id="kpi-cards" class="kpi-cards-grid">
            <div class="kpi-card skeleton">
              <div class="kpi-value"></div>
              <div class="kpi-label"></div>
              <div class="kpi-trend"></div>
            </div>
            <div class="kpi-card skeleton">
              <div class="kpi-value"></div>
              <div class="kpi-label"></div>
              <div class="kpi-trend"></div>
            </div>
            <div class="kpi-card skeleton">
              <div class="kpi-value"></div>
              <div class="kpi-label"></div>
              <div class="kpi-trend"></div>
            </div>
            <div class="kpi-card skeleton">
              <div class="kpi-value"></div>
              <div class="kpi-label"></div>
              <div class="kpi-trend"></div>
            </div>
          </div>

          <!-- Empty State -->
          <div id="overview-empty-state" class="empty-state" style="display: none;">
            <i class="${font:far()} fa-inbox"></i>
            <p>No data available for the selected time range.</p>
            <small>Try selecting a different time range or checking back later.</small>
          </div>
        </div>
      </section>

      <!-- Live Tab -->
      <section id="live-tab" class="dashboard-tab">
        <div class="tab-content">
          <!-- Active Sessions Table -->
          <div class="section-header">
            <h3><i class="${font:far()} fa-users"></i> Who's Online</h3>
          </div>
          <div id="active-sessions" class="data-table skeleton">
            <table>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Current Page</th>
                  <th>Device</th>
                  <th>Location</th>
                  <th>Duration</th>
                </tr>
              </thead>
              <tbody id="sessions-tbody">
              </tbody>
            </table>
          </div>

          <!-- Live Events Stream -->
          <div class="section-header" style="margin-top: 30px;">
            <h3><i class="${font:far()} fa-stream"></i> Recent Events</h3>
          </div>
          <div id="recent-events" class="events-list skeleton">
          </div>

          <!-- Empty State -->
          <div id="live-empty-state" class="empty-state" style="display: none;">
            <i class="${font:far()} fa-inbox"></i>
            <p>No active sessions or recent events.</p>
          </div>
        </div>
      </section>

      <!-- Content Tab -->
      <section id="content-tab" class="dashboard-tab">
        <div class="tab-content">
          <!-- Top Pages -->
          <div class="section-header">
            <h3><i class="${font:far()} fa-file-lines"></i> Top Pages</h3>
          </div>
          <div id="top-pages" class="data-table skeleton">
            <table>
              <thead>
                <tr>
                  <th>Page</th>
                  <th>Views</th>
                  <th>Unique Users</th>
                  <th>Avg Time</th>
                  <th>Bounce Rate</th>
                </tr>
              </thead>
              <tbody id="pages-tbody">
              </tbody>
            </table>
          </div>

          <!-- Top Assets -->
          <div class="section-header" style="margin-top: 30px;">
            <h3><i class="${font:far()} fa-image"></i> Top Assets</h3>
            <div class="filter-group">
              <label for="filter-asset-type">Asset Type:</label>
              <select id="filter-asset-type" class="filter-select">
                <option value="">All Types</option>
                <option value="PDF">PDF</option>
                <option value="Document">Document</option>
                <option value="Spreadsheet">Spreadsheet</option>
                <option value="Presentation">Presentation</option>
                <option value="Diagram">Diagram</option>
                <option value="Image">Image</option>
                <option value="Archive">Archive</option>
                <option value="Executable">Executable</option>
              </select>
            </div>
          </div>
          <div id="top-assets" class="data-table skeleton">
            <table>
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Type</th>
                  <th>Downloads</th>
                  <th>Views</th>
                </tr>
              </thead>
              <tbody id="assets-tbody">
              </tbody>
            </table>
          </div>

          <!-- Empty State -->
          <div id="content-empty-state" class="empty-state" style="display: none;">
            <i class="${font:far()} fa-inbox"></i>
            <p>No content data available for the selected time range.</p>
          </div>
        </div>
      </section>

      <!-- Audience Tab -->
      <section id="audience-tab" class="dashboard-tab">
        <div class="tab-content">
          <!-- Audience Segments -->
          <div class="section-header">
            <h3><i class="${font:far()} fa-users"></i> Audience Breakdown</h3>
          </div>
          <div id="audience-segments" class="audience-grid skeleton">
            <div class="segment-card">
              <div class="segment-value"></div>
              <div class="segment-label"></div>
            </div>
            <div class="segment-card">
              <div class="segment-value"></div>
              <div class="segment-label"></div>
            </div>
            <div class="segment-card">
              <div class="segment-value"></div>
              <div class="segment-label"></div>
            </div>
          </div>

          <!-- Device and Browser Distribution -->
          <div class="distribution-grid">
            <!-- Device Distribution -->
            <div>
              <div class="section-header">
                <h3><i class="${font:far()} fa-mobile"></i> Device Distribution</h3>
              </div>
              <div class="chart-container skeleton" style="height: 250px;">
                <canvas id="device-chart"></canvas>
              </div>
            </div>

            <!-- Browser Distribution -->
            <div>
              <div class="section-header">
                <h3><i class="${font:far()} fa-globe"></i> Browser Distribution</h3>
              </div>
              <div class="chart-container skeleton" style="height: 250px;">
                <canvas id="browser-chart"></canvas>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div id="audience-empty-state" class="empty-state" style="display: none;">
            <i class="${font:far()} fa-inbox"></i>
            <p>No audience data available for the selected time range.</p>
          </div>
        </div>
      </section>

      <!-- Technical Tab (conditional) -->
      <c:if test="${technicalMetricsEnabled}">
        <section id="technical-tab" class="dashboard-tab">
          <div class="tab-content">
            <!-- Performance Metrics -->
            <div class="section-header">
              <h3><i class="${font:far()} fa-gauge"></i> Performance Metrics</h3>
            </div>
            <div id="performance-metrics" class="metrics-grid skeleton">
              <div class="metric-card">
                <div class="metric-value"></div>
                <div class="metric-label"></div>
              </div>
              <div class="metric-card">
                <div class="metric-value"></div>
                <div class="metric-label"></div>
              </div>
              <div class="metric-card">
                <div class="metric-value"></div>
                <div class="metric-label"></div>
              </div>
            </div>

            <!-- Response Time Distribution -->
            <div class="section-header" style="margin-top: 30px;">
              <h3><i class="${font:far()} fa-hourglass"></i> Response Times</h3>
            </div>
            <div class="chart-container skeleton" style="height: 250px;">
              <canvas id="response-time-chart"></canvas>
            </div>

            <!-- Error Rate -->
            <div class="section-header" style="margin-top: 30px;">
              <h3><i class="${font:far()} fa-exclamation-triangle"></i> Errors</h3>
            </div>
            <div id="error-metrics" class="data-table skeleton">
              <table>
                <thead>
                  <tr>
                    <th>Status Code</th>
                    <th>Count</th>
                    <th>Percentage</th>
                  </tr>
                </thead>
                <tbody id="errors-tbody">
                </tbody>
              </table>
            </div>

            <!-- Empty State -->
            <div id="technical-empty-state" class="empty-state" style="display: none;">
              <i class="${font:far()} fa-inbox"></i>
              <p>No technical metrics available for the selected time range.</p>
            </div>
          </div>
        </section>
      </c:if>

    </div>

  </div>

  <!-- Custom Date Range Modal -->
  <div id="date-range-modal" class="modal" style="display: none;">
    <div class="modal-overlay" id="modal-overlay"></div>
    <div class="modal-content">
      <div class="modal-header">
        <h3>Select Date Range</h3>
        <button class="modal-close-btn" id="modal-close-btn">&times;</button>
      </div>
      <div class="modal-body">
        <label for="custom-start-date">Start Date:</label>
        <input type="date" id="custom-start-date" class="modal-input" />
        <label for="custom-end-date">End Date:</label>
        <input type="date" id="custom-end-date" class="modal-input" />
      </div>
      <div class="modal-footer">
        <button class="button secondary" id="modal-cancel-btn">Cancel</button>
        <button class="button primary" id="modal-apply-btn">Apply</button>
      </div>
    </div>
  </div>

</div>

<g:compress>
  <script src="/javascript/analytics/analytics-dashboard.js"></script>
  <script src="/javascript/analytics/analytics-api.js"></script>
  <script src="/javascript/analytics/analytics-charts.js"></script>
</g:compress>

<script>
  // Initialize the analytics dashboard
  document.addEventListener('DOMContentLoaded', function() {
    if (typeof AnalyticsDashboard !== 'undefined') {
      AnalyticsDashboard.init({
        containerId: 'visual-analytics-dashboard-wrapper',
        defaultTimeRange: '<c:out value="${defaultTimeRange}" />',
        liveEnabled: <c:out value="${liveEnabled}" default="true" />,
        technicalMetricsEnabled: <c:out value="${technicalMetricsEnabled}" default="true" />
      });
    }
  });
</script>
