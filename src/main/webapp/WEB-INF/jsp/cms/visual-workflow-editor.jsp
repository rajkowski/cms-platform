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
<c:set var="viewMode" value="${viewMode}" scope="request"/>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-workflow-editor.css" />
</g:compress>
<div id="visual-workflow-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="Workflow Editor" />
      <c:set var="activeApp" value="workflow" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="reload-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-sync"></i> Reload</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <span id="workflow-editor-title" style="font-weight: 600; font-size: 0.9rem;"></span>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
    </div>

    <div class="titlebar-right">
      <a href="#" id="dark-mode-toggle-menu" class="titlebar-btn apps-btn" title="Toggle Dark Mode">
        <i class="${font:far()} fa-moon"></i>
      </a>
      <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="titlebar-btn apps-btn confirm-exit" title="Exit back to site">
            <i class="${font:far()} fa-arrow-right-from-bracket"></i>
          </a>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/" class="titlebar-btn apps-btn confirm-exit" title="Exit back to site">
            <i class="${font:far()} fa-arrow-right-from-bracket"></i>
          </a>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <!-- Main Workflow Editor Container - 3 Panel Layout -->
  <div id="visual-workflow-editor-container">

    <!-- Left Panel: Task/Event Browser -->
    <div id="workflow-library-panel">
      <div class="library-tabs-container">
        <ul class="tabs-nav">
          <li><a href="#tasks-tab" class="${viewMode == 'tasks' ? 'active' : ''}">Tasks <span id="tasks-count" class="tab-count"></span></a></li>
          <li><a href="#events-tab" class="${viewMode == 'events' ? 'active' : ''}">Events <span id="events-count" class="tab-count"></span></a></li>
        </ul>

        <!-- Tasks Tab -->
        <div id="tasks-tab" class="tab-content ${viewMode == 'tasks' ? 'active' : ''}">
          <div id="tasks-error" style="display: none;"></div>
          <div id="tasks-empty" style="display: none;">
            <p>No scheduled tasks available</p>
          </div>
          <ul id="tasks-list" class="workflow-list">
            <!-- Tasks will be dynamically inserted here -->
          </ul>
        </div>

        <!-- Events Tab -->
        <div id="events-tab" class="tab-content ${viewMode == 'events' ? 'active' : ''}">
          <div id="events-error" style="display: none;"></div>
          <div id="events-empty" style="display: none;">
            <p>No event workflows available</p>
          </div>
          <ul id="events-list" class="workflow-list">
            <!-- Events will be dynamically inserted here -->
          </ul>
        </div>
      </div>
    </div>

    <!-- Center Panel: Main View -->
    <div id="workflow-editor-canvas">
      <!-- Tasks Overview (shown when Tasks tab active, no task selected) -->
      <div id="tasks-overview" style="display: none;">
        <!-- Stats Cards -->
        <div class="workflow-stats-cards" id="stats-cards">
          <div class="workflow-stat-card">
            <div class="stat-value" id="stat-total">-</div>
            <div class="stat-label"># of Jobs</div>
          </div>
          <div class="workflow-stat-card">
            <div class="stat-value" id="stat-enqueued">-</div>
            <div class="stat-label">Enqueued</div>
          </div>
          <div class="workflow-stat-card success-card">
            <div class="stat-value" id="stat-succeeded">-</div>
            <div class="stat-label">Succeeded</div>
          </div>
          <div class="workflow-stat-card danger-card">
            <div class="stat-value" id="stat-failed">-</div>
            <div class="stat-label">Failed</div>
          </div>
        </div>

        <!-- Jobs Table -->
        <div class="workflow-table-container">
          <table class="workflow-table" id="jobs-table">
            <thead>
              <tr>
                <th>Job ID</th>
                <th>Job Name</th>
                <th>Schedule (Cron)</th>
                <th>Next Run</th>
              </tr>
            </thead>
            <tbody id="jobs-table-body">
              <!-- Rows inserted dynamically -->
            </tbody>
          </table>
        </div>
      </div>

      <!-- Task Detail (shown when a specific task is selected) -->
      <div id="task-detail" style="display: none;">
        <div class="detail-header">
          <h5 id="task-detail-name"></h5>
        </div>
        <div class="detail-section">
          <label class="detail-label">Job ID</label>
          <div class="detail-value" id="task-detail-id"></div>
        </div>
        <div class="detail-section">
          <label class="detail-label">Schedule (Cron)</label>
          <div class="detail-value" id="task-detail-schedule"></div>
        </div>
        <div class="detail-section">
          <label class="detail-label">Next Run</label>
          <div class="detail-value" id="task-detail-next-run"></div>
        </div>
      </div>

      <!-- Events Overview (shown when Events tab active, no event selected) -->
      <div id="events-overview" style="display: none;">
        <div class="workflow-stats-cards">
          <div class="workflow-stat-card">
            <div class="stat-value" id="stat-workflows">-</div>
            <div class="stat-label"># of Workflows</div>
          </div>
        </div>
        <div class="workflow-table-container">
          <table class="workflow-table">
            <thead>
              <tr>
                <th>Event ID</th>
                <th>Source File</th>
                <th>Variables</th>
                <th>Steps</th>
              </tr>
            </thead>
            <tbody id="events-table-body">
              <!-- Rows inserted dynamically -->
            </tbody>
          </table>
        </div>
      </div>

      <!-- Event / Workflow Flowchart (shown when a specific event is selected) -->
      <div id="event-flowchart" style="display: none;">
        <div class="detail-header">
          <h5 id="event-detail-name"></h5>
          <span class="detail-meta" id="event-detail-file"></span>
        </div>
        <div id="workflow-flowchart-container">
          <!-- Flowchart nodes rendered dynamically -->
        </div>
      </div>

      <!-- Empty state -->
      <div class="empty-canvas" id="workflow-empty-canvas">
        <i class="${font:far()} fa-diagram-project fa-3x margin-bottom-10"></i>
        <h5>Welcome to the Workflow Editor</h5>
        <p>Select a task or event from the left panel to view details</p>
      </div>
    </div>

    <!-- Right Panel: Properties -->
    <div id="workflow-properties-panel">
      <div id="properties-panel-resize-handle"></div>

      <!-- Right Panel Tabs -->
      <div class="right-panel-tabs-container">
        <ul class="tabs-nav right-panel-tabs-nav">
          <li><a href="#wf-info-tab" class="active" data-tab="wf-info">Info</a></li>
          <li><a href="#wf-history-tab" data-tab="wf-history">History</a></li>
        </ul>

        <!-- Info Tab -->
        <div id="wf-info-tab" class="tab-content right-panel-tab-content active" data-tab="wf-info">
          <div id="wf-info-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select a task or event to view its details</p>
          </div>
        </div>

        <!-- History Tab -->
        <div id="wf-history-tab" class="tab-content right-panel-tab-content" data-tab="wf-history">
          <div id="wf-history-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select a scheduled task to view recent run history</p>
          </div>
        </div>
      </div>
    </div>

  </div>
</div>

<!-- JavaScript for Workflow Editor -->
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/widgets/editor/visual-workflow-editor.js"></script>
</g:compress>

<script>
  document.addEventListener('DOMContentLoaded', function() {
    setupAppsMenu();
    setupEditorAppSwitcher();

    if (typeof VisualWorkflowEditor !== 'undefined') {
      const editor = new VisualWorkflowEditor({
        token: '<c:out value="${userSession.formToken}" />',
        viewMode: '<c:out value="${viewMode}"/>',
        selectedId: '<c:out value="${selectedId}"/>'
      });
      editor.init();
    }
  });
</script>
