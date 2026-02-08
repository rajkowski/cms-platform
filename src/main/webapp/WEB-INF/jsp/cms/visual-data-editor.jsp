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
<web:stylesheet package="spectrum" file="spectrum.css" />
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-data-editor.css" />
</g:compress>
<div id="visual-data-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="Data Editor" />
      <c:set var="activeApp" value="data" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="new-collection-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-folder-plus"></i> New Collection</button>
      <button id="new-dataset-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-file-import"></i> New Dataset</button>
      <button id="reload-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-sync"></i> Reload</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <button id="save-btn" class="button tiny no-gap radius" disabled><i class="${font:far()} fa-save"></i> Save</button>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
    </div>

    <div class="titlebar-right">
      <!-- Apps Dropdown -->
      <div class="apps-menu">
        <button id="apps-btn" class="apps-btn" title="Apps">
          <i class="${font:far()} fa-th"></i>
          <i class="fas fa-chevron-down" style="font-size: 0.75rem; margin-left: 0.35rem;"></i>
        </button>
        <div class="apps-dropdown">
          <!-- Actions Section -->
          <div class="apps-menu-section">
            <a href="#" id="dark-mode-toggle-menu" class="apps-menu-item">
              <i class="${font:far()} fa-moon"></i>
              <span>Dark Mode</span>
            </a>
          </div>

          <!-- Exit Section -->
          <div class="apps-menu-section">
            <c:choose>
              <c:when test="${!empty returnPage}">
                <a href="${returnPage}" class="apps-menu-item confirm-exit">
                  <i class="${font:far()} fa-arrow-right-from-bracket"></i>
                  <span>Exit back to site</span>
                </a>
              </c:when>
              <c:otherwise>
                <a href="${ctx}/" class="apps-menu-item confirm-exit">
                  <i class="${font:far()} fa-arrow-right-from-bracket"></i>
                  <span>Exit back to site</span>
                </a>
              </c:otherwise>
            </c:choose>
          </div>
        </div>
      </div>
    </div>
  </div>
  
  <!-- Main Data Editor Container - 3 Panel Layout -->
  <div id="visual-data-editor-container">
    
    <!-- Left Panel: Library Browser -->
    <div id="data-library-panel">
      <div class="library-tabs-container">
        <ul class="tabs-nav">
          <li><a href="#collections-tab" class="active">Collections <span id="collections-count" class="tab-count"></span></a></li>
          <li><a href="#datasets-tab">Datasets <span id="datasets-count" class="tab-count"></span></a></li>
        </ul>

        <!-- Collections Tab -->
        <div id="collections-tab" class="tab-content active">
          <div style="margin-bottom: 15px;">
            <input type="text" id="collections-search" class="property-input" placeholder="Search collections..." style="width: 100%; padding: 10px; font-size: 14px;" />
          </div>
          <div id="collections-error" style="display: none;"></div>
          <div id="collections-empty" style="display: none;">
            <p>No collections available</p>
            <button id="create-first-collection-btn" class="button tiny expanded success radius">Create Your First Collection</button>
          </div>
          <ul id="collections-list" class="data-list">
            <!-- Collections will be dynamically inserted here -->
          </ul>
        </div>

        <!-- Datasets Tab -->
        <div id="datasets-tab" class="tab-content">
          <div style="margin-bottom: 15px;">
            <input type="text" id="datasets-search" class="property-input" placeholder="Search datasets..." style="width: 100%; padding: 10px; font-size: 14px;" />
          </div>
          <div id="datasets-error" style="display: none;"></div>
          <div id="datasets-empty" style="display: none;">
            <p>No datasets available</p>
            <button id="create-first-dataset-btn" class="button tiny expanded primary radius">Import Your First Dataset</button>
          </div>
          <ul id="datasets-list" class="data-list">
            <!-- Datasets will be dynamically inserted here -->
          </ul>
        </div>
      </div>
    </div>
    
    <!-- Center Panel: Editor Canvas / Preview -->
    <div id="data-editor-canvas">
      <div class="empty-canvas">
        <i class="${font:far()} fa-database fa-3x margin-bottom-10"></i>
        <h5>Welcome to the Data Editor</h5>
        <p>Select a collection or dataset from the left panel to get started</p>
        <p style="margin-top: 20px;">Or create something new:</p>
        <div style="margin-top: 15px;">
          <button class="button radius success new-collection-action">
            <i class="${font:far()} fa-folder-plus"></i> Create Collection
          </button>
          <button class="button radius primary import-dataset-action">
            <i class="${font:far()} fa-file-import"></i> Import Dataset
          </button>
        </div>
      </div>
    </div>
    
    <!-- Right Panel: Properties and Options -->
    <div id="data-properties-panel">
      <div id="properties-panel-resize-handle"></div>
      
      <!-- Right Panel Tabs -->
      <div class="right-panel-tabs-container">
        <ul class="tabs-nav right-panel-tabs-nav">
          <li><a href="#info-tab" class="active" data-tab="info">Info</a></li>
          <li><a href="#config-tab" data-tab="config">Configuration</a></li>
          <li><a href="#records-tab" data-tab="records">Records</a></li>
        </ul>
        
        <!-- Info Tab Content -->
        <div id="info-tab" class="tab-content right-panel-tab-content active" data-tab="info">
          <div id="info-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select a collection or dataset to view its information</p>
          </div>
        </div>
        
        <!-- Configuration Tab Content -->
        <div id="config-tab" class="tab-content right-panel-tab-content" data-tab="config">
          <div id="config-tab-content">
            <!-- For Collections: Permissions, Theme, Tabs, Categories, Table Columns, Custom Fields -->
            <!-- For Datasets: Source, Scheduling, Field Mapping, Data Transformation, Properties -->
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select an item to edit its configuration</p>
          </div>
        </div>
        
        <!-- Records Tab Content -->
        <div id="records-tab" class="tab-content right-panel-tab-content" data-tab="records">
          <div id="records-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Record list and preview will appear here</p>
          </div>
        </div>
      </div>
    </div>
    
  </div>
</div>

<!-- New Collection Modal -->
<div id="new-collection-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 600px;">
    <button class="modal-close" aria-label="Close">&times;</button>
    <h4>Create New Collection</h4>
    <form id="new-collection-form">
      <div class="property-group">
        <label class="property-label" for="collection-name">Collection Name <span class="required">*</span></label>
        <input type="text" id="collection-name" class="property-input" placeholder="Enter collection name" required />
        <small class="help-text">A descriptive name for your collection</small>
      </div>
      <div class="property-group">
        <label class="property-label" for="collection-unique-id">Unique ID</label>
        <input type="text" id="collection-unique-id" class="property-input" placeholder="auto-generated" />
        <small class="help-text">Leave blank to auto-generate from name</small>
      </div>
      <div class="property-group">
        <label class="property-label" for="collection-description">Description</label>
        <textarea id="collection-description" class="property-input" rows="3" placeholder="Describe this collection"></textarea>
      </div>
      <div class="property-group">
        <label class="property-label">
          <input type="checkbox" id="collection-allows-guests" />
          Allow guest access
        </label>
      </div>
      <div>
        <button type="submit" class="button success radius">Create Collection</button>
        <button type="button" class="button secondary radius modal-cancel">Cancel</button>
      </div>
    </form>
  </div>
</div>

<!-- Import Dataset Modal -->
<div id="import-dataset-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 600px;">
    <button class="modal-close" aria-label="Close">&times;</button>
    <h4>Import Dataset</h4>
    <form id="import-dataset-form">
      <div class="property-group">
        <label class="property-label" for="dataset-name">Dataset Name <span class="required">*</span></label>
        <input type="text" id="dataset-name" class="property-input" placeholder="Enter dataset name" required />
      </div>
      <div class="property-group">
        <label class="property-label">Import Source</label>
        <div class="import-source-options">
          <label class="radio-label">
            <input type="radio" name="import-source" value="file" checked />
            <i class="${font:far()} fa-file-upload"></i> Upload File
          </label>
          <label class="radio-label">
            <input type="radio" name="import-source" value="url" />
            <i class="${font:far()} fa-link"></i> Remote URL
          </label>
          <label class="radio-label">
            <input type="radio" name="import-source" value="stock" />
            <i class="${font:far()} fa-database"></i> Free Stock Data
          </label>
        </div>
      </div>
      <div id="file-upload-section" class="property-group">
        <label class="property-label" for="dataset-file">Select File <span class="required">*</span></label>
        <input type="file" id="dataset-file" class="property-input" accept=".csv,.json,.geojson,.xml" />
        <small class="help-text">Supported formats: CSV, JSON, GeoJSON, XML</small>
      </div>
      <div id="url-section" class="property-group" style="display:none;">
        <label class="property-label" for="dataset-url">Data Source URL <span class="required">*</span></label>
        <input type="url" id="dataset-url" class="property-input" placeholder="https://example.com/data.csv" />
        <small class="help-text">URL to a publicly accessible data file</small>
      </div>
      <div id="stock-data-section" class="property-group" style="display:none;">
        <label class="property-label" for="stock-data-source">Select Data Source</label>
        <select id="stock-data-source" class="property-input">
          <option value="">Choose a free dataset...</option>
          <option value="census">US Census Data</option>
          <option value="weather">Weather Data</option>
          <option value="covid">COVID-19 Statistics</option>
          <option value="geo">Geographic Data</option>
        </select>
        <small class="help-text">Sample datasets provided for quick import</small>
      </div>
      <div>
        <button type="submit" class="button success radius">Import Dataset</button>
        <button type="button" class="button secondary radius modal-cancel">Cancel</button>
      </div>
    </form>
  </div>
</div>

<!-- JavaScript for Data Editor -->
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/widgets/editor/visual-data-editor.js"></script>
</g:compress>

<script>
  // Initialize the Visual Data Editor
  document.addEventListener('DOMContentLoaded', function() {
    setupAppsMenu();
    setupEditorAppSwitcher();

    if (typeof VisualDataEditor !== 'undefined') {
      const editor = new VisualDataEditor({
        token: '<c:out value="${userSession.formToken}" />',
        viewMode: '<c:out value="${viewMode}"/>',
        uniqueId: '<c:out value="${uniqueId}"/>'
      });
      editor.init();
    }
  });
</script>
