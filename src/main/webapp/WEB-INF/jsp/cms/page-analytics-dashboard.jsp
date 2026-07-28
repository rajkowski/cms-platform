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
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="font" uri="/WEB-INF/tlds/font-functions.tld" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<%@ page import="com.fasterxml.jackson.databind.JsonNode" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.Iterator" %>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>

<web:script package="chartjs" file="chart.umd.js" />

<style>
  .page-analytics-shell { max-width: 1400px; margin: 0 auto; padding: 1.5rem; }
  .page-analytics-header, .page-analytics-toolbar, .page-analytics-chart-panel, .page-analytics-table-panel { background: #ffffff; border: 1px solid #e5e7eb; border-radius: 16px; box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06); }
  .page-analytics-header { padding: 1.25rem 1.5rem; margin-bottom: 1rem; }
  .page-analytics-top { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; align-items: flex-start; }
  .page-analytics-title h1 { margin: 0 0 0.35rem; font-size: 1.9rem; line-height: 1.1; }
  .page-analytics-title p { margin: 0; color: #6b7280; }
  .page-analytics-toolbar { padding: 1rem 1.25rem; margin-bottom: 1rem; display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.75rem; }
  .page-analytics-ranges { display: flex; gap: 0.5rem; flex-wrap: wrap; }
  .page-analytics-custom-range { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: end; }
  .page-analytics-custom-range label { display: flex; flex-direction: column; gap: 0.25rem; font-size: 0.85rem; color: #6b7280; }
  .page-analytics-custom-range input { min-width: 160px; }
  .page-analytics-range-link.active { background: #0f766e; color: #fff; border-color: #0f766e; }
  .page-analytics-section { padding: 1.25rem; }
  .page-analytics-section h2 { margin: 0 0 1rem; font-size: 1.1rem; text-align: center; }
  .page-analytics-section h2.page-analytics-members-heading { text-align: left; }
  .page-analytics-section h2.page-analytics-trend-heading { text-align: left; }
  .page-analytics-chart-panel { margin-bottom: 1rem; }
  .page-analytics-chart-wrap { position: relative; height: 240px; }
  .page-analytics-empty { padding: 2rem; text-align: center; color: #6b7280; }
  .page-analytics-warning { margin: 0 0 1rem; padding: 0.75rem 1rem; border-radius: 10px; background: #fffbeb; border: 1px solid #f59e0b; color: #92400e; text-align: center; }
  .page-analytics-table-panel { margin-bottom: 1rem; overflow: hidden; }
  .page-analytics-table { width: 100%; border-collapse: collapse; }
  .page-analytics-table th, .page-analytics-table td { padding: 0.8rem 1rem; border-bottom: 1px solid #e5e7eb; text-align: left; vertical-align: top; }
  .page-analytics-table th { background: #f9fafb; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.02em; color: #6b7280; text-align: center; }
  .page-analytics-table tr:last-child td { border-bottom: 0; }
  .page-analytics-table td { text-align: center; }
  .page-analytics-table .numeric { text-align: center; white-space: nowrap; }
</style>

<div class="page-analytics-shell">
  <%@include file="../page_messages.jspf" %>

  <c:set var="pageAnalyticsPath" value="${not empty param.webPage ? param.webPage : (not empty webPage ? webPage : analyticsPage.link)}" />
  <c:set var="pageAnalyticsTitle" value="${empty pageAnalyticsPath ? 'Analytics Dashboard' : pageAnalyticsPath}" />
  <c:set var="pageAnalyticsTitle" value="${fn:replace(fn:replace(fn:replace(pageAnalyticsTitle, '/', ' '), '-', ' '), '_', ' ')}" />
  <c:set var="pageAnalyticsTitle" value="${fn:trim(pageAnalyticsTitle)}" />
  <c:set var="pageAnalyticsTitle" value="${empty pageAnalyticsTitle ? 'Analytics Dashboard' : 'Analytics Summary - ' += pageAnalyticsTitle}" />
  <c:set var="pageAnalyticsTitle" value="${empty pageAnalyticsTitle ? pageAnalyticsTitle : fn:toUpperCase(fn:substring(pageAnalyticsTitle, 0, 1)) += fn:substring(pageAnalyticsTitle, 1, fn:length(pageAnalyticsTitle))}" />

  <div class="page-analytics-header">
    <div class="page-analytics-top">
      <div class="page-analytics-title">
        <h1><c:out value="${pageAnalyticsTitle}" /></h1>
      </div>
    </div>
  </div>

  <div id="page-analytics-warning" class="page-analytics-warning" style="display:none;">No page analytics data is available for this request.</div>

  <div class="page-analytics-toolbar">
    <div><span class="page-analytics-muted">Date range</span></div>
    <div class="page-analytics-custom-range">
      <label>
        <span>From</span>
        <input id="page-analytics-from-date" type="date" value="${not empty param.fromDate ? param.fromDate : pageAnalyticsFromDate}" />
      </label>
      <label>
        <span>To</span>
        <input id="page-analytics-to-date" type="date" value="${not empty param.toDate ? param.toDate : pageAnalyticsToDate}" />
      </label>
    </div>
    <div class="page-analytics-ranges">
      <c:set var="pageAnalyticsEncodedPath" value="<%= URLEncoder.encode(String.valueOf(pageContext.findAttribute(\"pageAnalyticsPath\")), StandardCharsets.UTF_8) %>" />
      <c:set var="pageAnalyticsBaseUrl" value="${pageContext.request.contextPath}/page-analytics?webPage=${pageAnalyticsEncodedPath}" />
      <c:set var="pageAnalyticsSelectedDays" value="${not empty param.days ? param.days : 7}" />
      <a class="button small hollow secondary page-analytics-range-link ${pageAnalyticsSelectedDays == 7 ? 'active' : ''}" href="${pageAnalyticsBaseUrl}&amp;days=7">7 Days</a>
      <a class="button small hollow secondary page-analytics-range-link ${pageAnalyticsSelectedDays == 30 ? 'active' : ''}" href="${pageAnalyticsBaseUrl}&amp;days=30">30 Days</a>
      <a class="button small hollow secondary page-analytics-range-link ${pageAnalyticsSelectedDays == 90 ? 'active' : ''}" href="${pageAnalyticsBaseUrl}&amp;days=90">90 Days</a>
      <a class="button small hollow secondary page-analytics-range-link ${pageAnalyticsSelectedDays == 180 ? 'active' : ''}" href="${pageAnalyticsBaseUrl}&amp;days=180">6 Months</a>
    </div>
  </div>

  <div class="page-analytics-chart-panel">
    <div class="page-analytics-section">
      <h2 class="page-analytics-trend-heading">Trend</h2>
      <div class="page-analytics-chart-wrap">
        <canvas id="page-analytics-chart"></canvas>
      </div>
    </div>
  </div>

  <div class="page-analytics-table-panel">
    <div class="page-analytics-section">
      <h2 class="page-analytics-members-heading">Members</h2>
      <div id="page-analytics-members-wrap" class="page-analytics-empty">No member visit data available.</div>
      <table id="page-analytics-members-table" class="page-analytics-table" style="display:none;">
        <thead>
          <tr>
            <th>Full name</th>
            <th>Username</th>
            <th class="numeric">Visit count</th>
            <th>First visit</th>
            <th>Last visit</th>
          </tr>
        </thead>
        <tbody></tbody>
      </table>
    </div>
  </div>
</div>

<%
  Object rawPageAnalytics = request.getAttribute("pageAnalyticsData");
  String serializedPageAnalytics = rawPageAnalytics != null ? rawPageAnalytics.toString() : "null";
  String selectedWebPage = request.getParameter("webPage");
  if (selectedWebPage == null || selectedWebPage.isBlank()) {
    Object requestWebPage = request.getAttribute("webPage");
    if (requestWebPage != null) {
      selectedWebPage = requestWebPage.toString();
    }
  }
  if (selectedWebPage == null || selectedWebPage.isBlank()) {
    selectedWebPage = "";
  }
%>
<script>
  (function() {
    const selectedWebPage = '<c:out value="${selectedWebPage}" />';
    const pageAnalyticsData = (() => {
      try {
        const bootstrapped = <%= serializedPageAnalytics %>;
        return bootstrapped && typeof bootstrapped === 'object' ? bootstrapped : null;
      } catch (e) {
        return null;
      }
    })();
    const analyticsSource = pageAnalyticsData && pageAnalyticsData.success !== false ? pageAnalyticsData : null;
    const hasAnalyticsData = !!(analyticsSource && ((Array.isArray(analyticsSource.trend) && analyticsSource.trend.length > 0) || (Array.isArray(analyticsSource.members) && analyticsSource.members.length > 0) || Number(analyticsSource.totalViews || 0) > 0 || Number(analyticsSource.anonymousVisits || 0) > 0 || Number(analyticsSource.memberCount || 0) > 0));
    const warning = document.getElementById('page-analytics-warning');
    const toolbar = document.querySelector('.page-analytics-toolbar');
    const chartPanel = document.querySelector('.page-analytics-chart-panel');
    const tablePanel = document.querySelector('.page-analytics-table-panel');
    const pageAnalyticsRequestBaseUrl = (() => {
      const url = new URL(window.location.href);
      url.search = '';
      url.searchParams.set('webPage', selectedWebPage || analyticsSource.pagePath || analyticsSource.requestedPagePath || '');
      return url;
    })();

    const fromDateInput = document.getElementById('page-analytics-from-date');
    const toDateInput = document.getElementById('page-analytics-to-date');
    const initialFromDate = fromDateInput ? fromDateInput.value : '';
    const initialToDate = toDateInput ? toDateInput.value : '';

    function submitDateRangeIfReady() {
      if (!fromDateInput || !toDateInput) {
        return;
      }
      if (!fromDateInput.value || !toDateInput.value) {
        return;
      }
      const url = new URL(pageAnalyticsRequestBaseUrl.toString());
      url.searchParams.set('fromDate', fromDateInput.value);
      url.searchParams.set('toDate', toDateInput.value);
      url.searchParams.delete('days');
      window.location.href = url.toString();
    }

    function clearActiveRangeLink() {
      document.querySelectorAll('.page-analytics-range-link.active').forEach(function(link) {
        link.classList.remove('active');
      });
    }

    if (fromDateInput && toDateInput) {
      fromDateInput.addEventListener('change', function() {
        clearActiveRangeLink();
        submitDateRangeIfReady();
      });
      toDateInput.addEventListener('change', function() {
        clearActiveRangeLink();
        submitDateRangeIfReady();
      });
      fromDateInput.addEventListener('blur', submitDateRangeIfReady);
      toDateInput.addEventListener('blur', submitDateRangeIfReady);
      if (initialFromDate && initialToDate) {
        fromDateInput.value = initialFromDate;
        toDateInput.value = initialToDate;
        clearActiveRangeLink();
      }
    }

    function formatMemberDate(value) {
      if (!value) {
        return '';
      }
      const normalized = String(value).replace('T', ' ').replace(/\.\d+$/, '');
      const date = new Date(normalized.replace(' ', 'T'));
      if (Number.isNaN(date.getTime())) {
        return normalized;
      }
      const day = String(date.getDate()).padStart(2, '0');
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const year = date.getFullYear();
      let hours = date.getHours();
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const ampm = hours >= 12 ? 'PM' : 'AM';
      hours = hours % 12;
      hours = hours === 0 ? 12 : hours;
      return day + '-' + month + '-' + year + ' ' + String(hours).padStart(2, '0') + ':' + minutes + ' ' + ampm;
    }

    function renderMembersTable(members) {
      const wrap = document.getElementById('page-analytics-members-wrap');
      const table = document.getElementById('page-analytics-members-table');
      const tbody = table ? table.querySelector('tbody') : null;
      if (!wrap || !table || !tbody) {
        return;
      }
      tbody.innerHTML = '';
      if (!Array.isArray(members) || members.length === 0) {
        wrap.style.display = 'block';
        table.style.display = 'none';
        return;
      }
      members.forEach(function(member) {
        const row = document.createElement('tr');
        row.innerHTML = '<td>' +
          (member.fullName || 'Unknown') +
          '</td><td>' +
          (member.username || '') +
          '</td><td class="numeric">' +
          (member.visitCount != null ? member.visitCount : 0) +
          '</td><td>' +
          formatMemberDate(member.firstVisit) +
          '</td><td>' +
          formatMemberDate(member.lastVisit) +
          '</td>';
        tbody.appendChild(row);
      });
      wrap.style.display = 'none';
      table.style.display = 'table';
    }

    function getRangeDays() {
      const params = new URL(window.location.href).searchParams;
      const hasFromDate = !!params.get('fromDate');
      const hasToDate = !!params.get('toDate');
      if (hasFromDate && hasToDate) {
        const fromDate = new Date(params.get('fromDate') + 'T00:00:00Z');
        const toDate = new Date(params.get('toDate') + 'T00:00:00Z');
        if (!Number.isNaN(fromDate.getTime()) && !Number.isNaN(toDate.getTime())) {
          return Math.max(1, Math.floor((toDate.getTime() - fromDate.getTime()) / 86400000) + 1);
        }
      }
      const days = parseInt(params.get('days') || '7', 10);
      return Number.isFinite(days) && days > 0 ? days : 7;
    }

    function buildRangeUrl(days) {
      const url = new URL(pageAnalyticsRequestBaseUrl.toString());
      url.searchParams.set('days', String(days));
      return url.toString();
    }

    function parseAnalyticsDate(label) {
      if (!label) {
        return null;
      }
      const date = new Date(label);
      if (!Number.isNaN(date.getTime())) {
        return date;
      }
      const parts = String(label).split(/[-\/]/);
      if (parts.length === 3) {
        const altDate = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));
        if (!Number.isNaN(altDate.getTime())) {
          return altDate;
        }
      }
      return null;
    }

    function normalizeTrend(trend) {
      return Array.isArray(trend) ? trend.map(function(item) {
        return {
          date: parseAnalyticsDate(item && item.label ? item.label : null),
          views: Number(item && item.views ? item.views : 0)
        };
      }).filter(function(item) { return item.date !== null; }) : [];
    }

    function groupTrendPoints(trend, rangeDays) {
      const points = normalizeTrend(trend);
      const params = new URL(window.location.href).searchParams;
      const fromParam = params.get('fromDate');
      const toParam = params.get('toDate');
      const isCustomRange = !!(fromParam && toParam);
      if (isCustomRange) {
        const startDate = new Date(fromParam + 'T00:00:00Z');
        const endDate = new Date(toParam + 'T00:00:00Z');
        if (!Number.isNaN(startDate.getTime()) && !Number.isNaN(endDate.getTime())) {
          const seriesMap = new Map();
          points.forEach(function(item) {
            seriesMap.set(item.date.toISOString().slice(0, 10), item.views);
          });
          const series = [];
          for (let cursor = new Date(startDate); cursor <= endDate; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
            const key = cursor.toISOString().slice(0, 10);
            series.push({
              label: key,
              views: seriesMap.has(key) ? seriesMap.get(key) : 0
            });
          }
          return series;
        }
      }
      if (points.length === 0) {
        return [];
      }
      if (rangeDays <= 30) {
        const byDay = new Map();
        points.forEach(function(item) {
          byDay.set(item.date.toISOString().slice(0, 10), item.views);
        });
        const series = [];
        const today = new Date();
        const end = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
        const start = new Date(end);
        start.setUTCDate(start.getUTCDate() - Math.max(0, rangeDays - 1));
        for (let cursor = new Date(start); cursor <= end; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
          const key = cursor.toISOString().slice(0, 10);
          series.push({
            label: key,
            views: byDay.has(key) ? byDay.get(key) : 0
          });
        }
        return series;
      }
      if (rangeDays <= 90) {
        const buckets = new Map();
        points.forEach(function(item) {
          const date = item.date;
          const bucketStart = new Date(date.getFullYear(), date.getMonth(), date.getDate() - (date.getDay() || 7) + 1);
          const key = bucketStart.toISOString().slice(0, 10);
          if (!buckets.has(key)) {
            buckets.set(key, { start: bucketStart, views: 0 });
          }
          buckets.get(key).views += item.views;
        });
        return Array.from(buckets.values()).map(function(bucket) {
          const end = new Date(bucket.start.getFullYear(), bucket.start.getMonth(), bucket.start.getDate() + 6);
          return { label: bucket.start.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) + ' - ' + end.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }), views: bucket.views };
        });
      }
      const buckets = new Map();
      points.forEach(function(item) {
        const date = item.date;
        const key = date.getFullYear() + '-' + date.getMonth();
        if (!buckets.has(key)) {
          buckets.set(key, { date: new Date(date.getFullYear(), date.getMonth(), 1), views: 0 });
        }
        buckets.get(key).views += item.views;
      });
      return Array.from(buckets.values()).map(function(bucket) {
        return { label: bucket.date.toLocaleDateString(undefined, { month: 'short', year: 'numeric' }), views: bucket.views };
      });
    }

    if (!hasAnalyticsData) {
      if (warning) {
        warning.style.display = 'block';
      }
      if (toolbar) {
        toolbar.style.display = 'none';
      }
      if (chartPanel) {
        chartPanel.style.display = 'none';
      }
      if (tablePanel) {
        tablePanel.style.display = 'none';
      }
      return;
    }
    if (warning) {
      warning.style.display = 'none';
    }

    renderMembersTable(analyticsSource.members);
    const rangeDays = getRangeDays();
    const trend = groupTrendPoints(analyticsSource.trend, rangeDays);
    const chartCanvas = document.getElementById('page-analytics-chart');
    if (chartCanvas && typeof Chart !== 'undefined' && trend.length > 0) {
      new Chart(chartCanvas.getContext('2d'), {
        type: 'line',
        data: {
          labels: trend.map(function(point) { return point.label; }),
          datasets: [{
            label: 'Views',
            data: trend.map(function(point) { return point.views; }),
            borderColor: '#0f766e',
            backgroundColor: 'rgba(15, 118, 110, 0.12)',
            tension: 0.25,
            fill: true,
            pointRadius: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          animation: false,
          plugins: {
            legend: {
              display: false
            }
          },
          scales: {
            x: {
              title: { display: true }
            },
            y: {
              beginAtZero: true,
              title: { display: true },
              ticks: { precision: 0 }
            }
          }
        }
      });
    }
  })();
</script>