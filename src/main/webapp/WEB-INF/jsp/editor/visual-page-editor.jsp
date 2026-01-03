<%--
  ~ Copyright 2025 Matt Rajkowski
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
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="webPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<link href="${ctx}/css/spectrum-1.8.1/spectrum.css" rel="stylesheet">
<script src="${ctx}/javascript/spectrum-1.8.1/spectrum.js"></script>
<style>
  /* Dark Mode Variables */
  :root {
    --editor-bg: #ffffff;
    --editor-panel-bg: #f8f9fa;
    --editor-border: #dee2e6;
    --editor-text: #212529;
    --editor-text-muted: #6c757d;
    --editor-hover-bg: #f8f9fa;
    --editor-selected-bg: #f0f7ff;
    --editor-selected-border: #007bff;
    --editor-shadow: rgba(0,0,0,0.1);
  }
  
  [data-theme="dark"] {
    --editor-bg: #1a1a1a;
    --editor-panel-bg: #2d2d2d;
    --editor-border: #404040;
    --editor-text: #e0e0e0;
    --editor-text-muted: #a0a0a0;
    --editor-hover-bg: #353535;
    --editor-selected-bg: #1e3a5f;
    --editor-selected-border: #4a9eff;
    --editor-shadow: rgba(0,0,0,0.3);
  }
  
  #visual-page-editor-container {
    display: flex;
    height: 600px; /* Will be calculated dynamically by JavaScript */
    min-height: 300px;
  }
  
  #editor-toolbar {
    background: var(--editor-panel-bg);
    border-bottom: 1px solid var(--editor-border);
    padding: 10px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 20px;
    transition: background 0.3s ease, border-color 0.3s ease;
  }

  .toolbar-section {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .toolbar-section.middle {
    flex-grow: 1;
    justify-content: center;
  }
  
  #widget-palette {
    width: 250px;
    background: var(--editor-panel-bg);
    border-right: 1px solid var(--editor-border);
    overflow-y: auto;
    padding: 15px;
    transition: background 0.3s ease, border-color 0.3s ease;
  }
  
  #editor-canvas {
    flex: 1;
    background: var(--editor-bg);
    overflow-y: auto;
    padding: 20px;
    position: relative;
    transition: background 0.3s ease;
  }
  
  #properties-panel {
    width: 300px;
    background: var(--editor-panel-bg);
    border-left: 1px solid var(--editor-border);
    color: var(--editor-text);
    overflow-y: auto;
    padding: 15px;
    position: relative;
    transition: background 0.3s ease, border-color 0.3s ease;
    min-width: 200px;
    max-width: 600px;
  }
  
  /* Properties Panel Resize Handle */
  #properties-panel-resize-handle {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    cursor: col-resize;
    background: transparent;
    z-index: 10;
    transition: background 0.2s;
  }
  
  #properties-panel-resize-handle:hover {
    background: var(--editor-selected-border);
  }
  
  #properties-panel-resize-handle.resizing {
    background: var(--editor-selected-border);
  }
  
  .widget-palette-item {
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    color: var(--editor-text);
    padding: 10px;
    margin-bottom: 10px;
    cursor: move;
    transition: all 0.2s;
  }
  
  .widget-palette-item:hover {
    box-shadow: 0 2px 8px var(--editor-shadow);
    border-color: var(--editor-selected-border);
  }
  
  .widget-palette-category {
    font-weight: bold;
    font-size: 14px;
    color: var(--editor-text);
    margin: 15px 0 10px 0;
    padding-bottom: 5px;
    border-bottom: 2px solid var(--editor-border);
  }
  
  .canvas-row {
    border: 2px dashed var(--editor-border);
    padding: 15px;
    margin-bottom: 15px;
    min-height: 80px;
    position: relative;
    transition: all 0.2s;
    background: var(--editor-bg);
  }
  
  .canvas-row:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
  }
  
  .canvas-row.selected {
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
    background: var(--editor-selected-bg);
  }
  
  .canvas-row.drag-over {
    background: var(--editor-selected-bg);
    border-color: var(--editor-selected-border);
  }
  
  .canvas-column {
    border: 1px solid var(--editor-border);
    padding: 10px;
    min-height: 60px;
    position: relative;
    background: var(--editor-bg);
    transition: all 0.2s;
  }
  
  .canvas-column:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
  }
  
  .canvas-column.selected {
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
    background: var(--editor-selected-bg);
  }
  
  .canvas-column.drag-over {
    background: var(--editor-selected-bg);
    border-color: var(--editor-selected-border);
  }
  
  /* Column Resize Handle */
  .column-resize-handle {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 6px;
    cursor: col-resize;
    background: transparent;
    z-index: 100;
    transition: background 0.2s;
    user-select: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
  }
  
  .column-resize-handle.left {
    left: -3px;
  }
  
  .column-resize-handle.right {
    right: -3px;
  }
  
  .column-resize-handle:hover,
  .column-resize-handle.resizing {
    background: var(--editor-selected-border);
    width: 8px;
  }
  
  .column-resize-handle.resizing {
    background: var(--editor-selected-border);
    opacity: 0.8;
  }
  
  .canvas-widget {
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    color: var(--editor-text);
    padding: 15px;
    margin-bottom: 10px;
    position: relative;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .canvas-widget:hover {
    border-color: var(--editor-selected-border);
    box-shadow: 0 2px 8px var(--editor-shadow);
  }
  
  .canvas-widget.selected {
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 3px rgba(74, 158, 255, 0.25);
  }
  
  .widget-controls {
    position: absolute;
    top: 15px;
    right: 5px;
    display: none;
  }
  
  .canvas-widget:hover .widget-controls {
    display: block;
  }

  .widget-drag-placeholder {
    background: #e9f4ff;
    border: 2px dashed #007bff;
    height: 50px;
    margin-bottom: 10px;
    transition: all 0.2s;
  }
  
  .row-drag-placeholder {
    background: #e9f4ff;
    border: 2px dashed #007bff;
    height: 80px;
    margin-bottom: 15px;
    transition: all 0.2s;
  }
  
  .row-controls {
    position: absolute;
    top: 0;
    right: 5px;
    display: none;
  }
  
  .canvas-row:hover .row-controls {
    display: block;
    z-index: 1;
  }
  
  .control-btn {
    background: #007bff;
    color: white;
    border: none;
    border-radius: 3px;
    padding: 4px 8px;
    margin-left: 4px;
    cursor: pointer;
    font-size: 12px;
  }
  
  .control-btn:hover {
    background: #0056b3;
  }
  
  .control-btn.danger {
    background: #dc3545;
  }
  
  .control-btn.danger:hover {
    background: #c82333;
  }
  
  .empty-canvas {
    text-align: center;
    padding: 60px 20px;
    color: var(--editor-text-muted);
  }
  
  .property-group {
    margin-bottom: 10px;
  }

  .property-group label {
    color: var(--editor-text);
  }
  
  .property-label {
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 5px;
    color: var(--editor-text);
  }
  
  .property-input {
    width: 100%;
    padding: 8px;
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    font-size: 14px;
    background: var(--editor-bg);
    color: var(--editor-text);
    transition: all 0.2s;
  }
  
  .property-input:focus {
    outline: none;
    border-color: var(--editor-selected-border);
    box-shadow: 0 0 0 2px rgba(74, 158, 255, 0.25);
  }
  
  select.property-input {
    background: var(--editor-bg);
    color: var(--editor-text);
  }

  /* Column Layout Picker Modal */
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0,0,0,0.5);
    display: none;
    z-index: 10000;
    justify-content: center;
    align-items: center;
  }

  .modal-overlay.active {
    display: flex !important;
  }

  .modal-content {
    background: var(--editor-bg);
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 5px 15px var(--editor-shadow);
    width: 90%;
    max-width: 600px;
    color: var(--editor-text);
  }

  .layout-picker {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 20px;
  }

  .layout-option {
    border: 2px solid var(--editor-border);
    border-radius: 4px;
    padding: 15px;
    cursor: pointer;
    transition: all 0.2s;
    text-align: center;
    background: var(--editor-bg);
  }
  
  .layout-option:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
  }

  .layout-preview {
    display: flex;
    gap: 5px;
    height: 40px;
    margin-bottom: 10px;
  }

  .layout-preview-col {
    background: #ced4da;
    border-radius: 2px;
    flex-grow: 1;
  }

  .layout-palette-item {
    position: relative; /* Needed for positioning the button */
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    padding: 10px;
    margin-bottom: 10px;
    cursor: move;
    transition: all 0.2s;
  }

  .add-layout-btn {
    position: absolute;
    top: 5px;
    right: 5px;
    background: #007bff;
    color: white;
    border: none;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    font-size: 16px;
    line-height: 24px;
    text-align: center;
    cursor: pointer;
    display: none; /* Hidden by default */
    z-index: 2;
  }

  .layout-palette-item:hover .add-layout-btn {
    display: block; /* Show on hover */
  }

  .add-layout-btn:hover {
    background: #0056b3;
  }

  .layout-palette-item:hover {
    box-shadow: 0 2px 8px var(--editor-shadow);
    border-color: var(--editor-selected-border);
  }
  
  .layout-label {
    font-size: 11px;
    color: var(--editor-text-muted);
    text-align: center;
  }

  /* Pre-Designed Page Template Styles */
  #pre-designed-page-list {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 15px;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  #pre-designed-page-list li {
    display: block;
  }

  #pre-designed-page-list a {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 15px;
    border: 2px solid var(--editor-border);
    border-radius: 6px;
    text-decoration: none;
    color: var(--editor-text);
    background: var(--editor-bg);
    cursor: pointer;
    transition: all 0.2s;
    height: 100%;
    min-height: 180px;
  }
  
  #pre-designed-page-list a:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
    box-shadow: 0 2px 8px var(--editor-shadow);
  }

  .template-preview {
    width: 100%;
    height: 80px;
    background: var(--editor-bg);
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    padding: 3px;
    margin-bottom: 10px;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .template-preview-row {
    display: flex;
    gap: 2px;
    height: 8px;
    flex-shrink: 0;
    min-height: 8px;
  }

  .template-preview-col {
    background: #ced4da;
    border-radius: 1px;
    flex: 1;
  }

  .template-label {
    font-size: 12px;
    color: var(--editor-text);
    font-weight: 500;
    text-align: center;
    word-wrap: break-word;
  }

  .palette-section {
    margin-bottom: 10px;
  }

  .palette-section-header {
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    user-select: none; /* Prevent text selection on click */
  }

  .palette-section-header:hover {
    color: #007bff;
  }

  .palette-section-header .toggle-icon {
    transition: transform 0.2s;
    font-size: 12px;
  }

  .palette-section-header.collapsed .toggle-icon {
    transform: rotate(-90deg);
  }

  .palette-section-content {
    padding-top: 10px;
  }

  /* Palette Tabs */
  #widget-palette {
    padding: 0; /* Remove padding to allow tabs to span full width */
  }

  .palette-tabs-container {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .tabs-nav {
    display: flex;
    list-style: none;
    margin: 0;
    padding: 0;
    border-bottom: 1px solid var(--editor-border);
    background-color: var(--editor-panel-bg);
    flex-shrink: 0;
  }

  .tabs-nav li {
    flex-grow: 1;
    text-align: center;
  }

  .tabs-nav a {
    display: block;
    padding: 8px 10px;
    text-decoration: none;
    color: var(--editor-text);
    border-bottom: 3px solid transparent;
    transition: all 0.2s;
    font-weight: 600;
    font-size: 12px;
    white-space: nowrap;
  }
  
  .tabs-nav a:hover {
    background-color: var(--editor-hover-bg);
  }
  
  .tabs-nav a.active {
    color: var(--editor-selected-border);
    border-bottom-color: var(--editor-selected-border);
  }

  .tab-content {
    display: none;
    padding: 15px;
    overflow-y: auto;
    flex-grow: 1;
  }

  .tab-content.active {
    display: block;
  }

  /* Pages Tab Styles */
  #web-page-list {
    list-style: none;
    margin: 0;
    padding: 0;
  }

  #web-page-list li {
    margin-bottom: 10px;
  }

  .web-page-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    border: 1px solid var(--editor-border);
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    background: var(--editor-bg);
  }
  
  .web-page-item:hover {
    border-color: var(--editor-selected-border);
    background: var(--editor-hover-bg);
    box-shadow: 0 2px 8px var(--editor-shadow);
  }
  
  .web-page-item.selected {
    border-color: var(--editor-selected-border);
    background: var(--editor-selected-bg);
  }
  
  .web-page-info {
    flex-grow: 1;
  }
  
  .web-page-title {
    font-weight: 600;
    color: var(--editor-text);
    margin-bottom: 3px;
  }
  
  .web-page-link {
    font-size: 12px;
    color: var(--editor-text-muted);
  }
  
  #pages-loading {
    text-align: center;
    padding: 20px;
    color: var(--editor-text-muted);
  }

  #pages-error {
    padding: 15px;
    background: #f8d7da;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
    color: #721c24;
    margin-bottom: 15px;
  }

  #pages-empty {
    text-align: center;
    padding: 40px 20px;
    color: var(--editor-text-muted);
  }
  
  /* Dark Mode Toggle */
  #dark-mode-toggle {
    border: 1px solid var(--editor-border);
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 14px;
  }
  
  /* Preview Toggle */
  #preview-container {
    display: none;
    flex: 1;
    background: var(--editor-bg);
    overflow-y: auto;
    padding: 20px;
    position: relative;
  }

  #preview-container.active {
    display: block !important;
  }

  #editor-canvas.hidden {
    display: none;
  }

  #preview-loading {
    text-align: center;
    padding: 60px 20px;
    color: var(--editor-text-muted);
  }
  
  #preview-error {
    padding: 20px;
    background: rgba(220, 53, 69, 0.1);
    border: 1px solid rgba(220, 53, 69, 0.3);
    color: #dc3545;
    border-radius: 4px;
  }

  #preview-iframe {
    width: 100%;
    height: 100%;
    border: none;
    display: none;
  }

  #preview-iframe.active {
    display: block !important;
  }

  #preview-loading {
    text-align: center;
    padding: 60px 20px;
    color: #6c757d;
  }

  #preview-error {
    padding: 20px;
    background: #f8d7da;
    border: 1px solid #f5c6cb;
    border-radius: 4px;
    color: #721c24;
    margin: 20px;
  }

  /* Toggle Switch Styles */
  #toggle-preview-btn {
    /* border: 2px solid #dee2e6; */
    /* padding: 6px 12px; */
    /* font-size: 13px; */
    /* font-weight: 600; */
    transition: all 0.3s ease;
    display: inline-flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    position: relative;
  }

  #toggle-preview-btn::after {
    content: '';
    display: inline-block;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: #dee2e6;
    transition: all 0.3s ease;
  }

  #toggle-preview-btn.active {
    color: #ffffff;
    background-color: #43AC6A;
  }

  #toggle-preview-btn.active::after {
    background: #0dd757;
    box-shadow: 0 0 8px rgba(23, 162, 184, 0.5);
  }
</style>
<link href="${ctx}/css/platform.css" rel="stylesheet">

<div id="visual-page-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>
  
  <!-- Toolbar -->
  <div id="editor-toolbar">
    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="add-row-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-plus"></i> Add Row</button>
      <button id="undo-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-undo"></i> Undo</button>
      <button id="redo-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-redo"></i> Redo</button>
      <button id="pre-designed-page-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-magic"></i> Templates</button>
    </div>

    <!-- Middle Section -->
    <div class="toolbar-section middle">
      <button id="toggle-preview-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-eye"></i> Preview</button>
      <button id="save-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-save"></i> Save</button>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <a id="web-page-info-btn" href="#" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-info-circle"></i> Page Info</a>
      <a id="web-page-css-btn" href="#" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-palette"></i> CSS</a>
      <a id="web-page-xml-editor-btn" href="#" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-code"></i> XML</a>
      <button id="preview-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-database"></i> Raw Data</button>
      <button id="dark-mode-toggle" class="button tiny secondary no-gap radius" title="Toggle Dark Mode"><i class="${font:far()} fa-moon"></i></button>
      <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="button tiny no-gap radius">Exit</a>
        </c:when>
        <c:when test="${!empty webPage.link}">
          <a href="${ctx}${webPage.link}" class="button tiny no-gap radius">Exit</a>
        </c:when>
      </c:choose>
    </div>
  </div>
  
  <!-- Main Editor Container -->
  <div id="visual-page-editor-container">
    
    <!-- Widget Palette -->
    <div id="widget-palette">
      <div class="palette-tabs-container">
        <ul class="tabs-nav">
          <li><a href="#pages-tab" class="active">Pages</a></li>
          <li><a href="#layouts-tab">Layouts</a></li>
          <li><a href="#widgets-tab">Widgets</a></li>
        </ul>

        <div id="widgets-tab" class="tab-content">
          <input type="text" id="widget-search" placeholder="Search widgets..." class="property-input" style="margin-bottom: 15px;" />
          <div id="widget-list-container">
            <!-- Widgets will be dynamically inserted here -->
          </div>
        </div>

        <div id="layouts-tab" class="tab-content">
          <div class="layout-palette-item" draggable="true" data-layout="small-12">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 100%;"></div>
            </div>
            <div class="layout-label">1 Column</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-6,small-6">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 50%;"></div>
              <div class="layout-preview-col" style="flex-basis: 50%;"></div>
            </div>
            <div class="layout-label">2 Columns</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-4,small-4,small-4">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
            </div>
            <div class="layout-label">3 Columns</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-3,small-3,small-3,small-3">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
            </div>
            <div class="layout-label">4 Columns</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-4,small-8">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
              <div class="layout-preview-col" style="flex-basis: 66.67%;"></div>
            </div>
            <div class="layout-label">33 / 67</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-8,small-4">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 66.67%;"></div>
              <div class="layout-preview-col" style="flex-basis: 33.33%;"></div>
            </div>
            <div class="layout-label">67 / 33</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-3,small-9">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
              <div class="layout-preview-col" style="flex-basis: 75%;"></div>
            </div>
            <div class="layout-label">25 / 75</div>
          </div>
          <div class="layout-palette-item" draggable="true" data-layout="small-9,small-3">
            <button class="add-layout-btn" title="Add row to page">+</button>
            <div class="layout-preview">
              <div class="layout-preview-col" style="flex-basis: 75%;"></div>
              <div class="layout-preview-col" style="flex-basis: 25%;"></div>
            </div>
            <div class="layout-label">75 / 25</div>
          </div>
        </div>

        <div id="pages-tab" class="tab-content active">
          <div id="pages-loading" style="display: none;">
            <i class="${font:far()} fa-spinner fa-spin"></i> Loading pages...
          </div>
          <div id="pages-error" style="display: none;"></div>
          <div id="pages-empty" style="display: none;">
            <p>No pages available</p>
          </div>
          <ul id="web-page-list">
            <!-- Web pages will be dynamically inserted here -->
          </ul>
        </div>

      </div>
    </div>
    
    <!-- Preview Container -->
    <div id="preview-container">
      <div id="preview-loading" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i> Loading preview...
      </div>
      <div id="preview-error" style="display: none;"></div>
      <iframe id="preview-iframe"></iframe>
    </div>
    
    <!-- Editor Canvas -->
    <div id="editor-canvas">
      <c:choose>
        <c:when test="${hasExistingLayout}">
          <!-- Existing layout will be rendered here via JavaScript -->
        </c:when>
        <c:otherwise>
          <div class="empty-canvas" style="cursor: pointer;">
            <i class="${font:far()} fa-plus-circle fa-3x margin-bottom-10"></i>
            <h5>Start Building Your Page</h5>
            <p>Click "Add Row" to begin or drag widgets from the palette</p>
          </div>
        </c:otherwise>
      </c:choose>
    </div>
    
    <!-- Properties Panel -->
    <div id="properties-panel">
      <div id="properties-panel-resize-handle"></div>
      <h5 style="color: var(--editor-text);">Properties</h5>
      <div id="properties-content">
        <p style="color: var(--editor-text-muted); font-size: 14px;">Select an element to edit its properties</p>
      </div>
    </div>
    
  </div>
</div>

<!-- Pre-Designed Page Modal -->
<div id="pre-designed-page-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 800px;">
    <h4>Apply a Pre-Designed Page Layout</h4>
    <ul id="pre-designed-page-list">
      <!-- Pre-designed page items will be inserted here by JavaScript -->
    </ul>
    <script>
      // Dynamically populate the pre-designed page list from preDesignedTemplates
      document.addEventListener('DOMContentLoaded', function() {
        var list = document.getElementById('pre-designed-page-list');
        if (window.preDesignedTemplates && window.preDesignedTemplateLabels) {
          // Use the order from window.preDesignedTemplateLabels keys
          Object.keys(window.preDesignedTemplateLabels).forEach(function(key) {
            console.log("Checking template key:", key);
            if (window.preDesignedTemplates[key]) {
              var label = window.preDesignedTemplateLabels[key] || key.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
              var li = document.createElement('li');
              var a = document.createElement('a');
              a.href = "#";
              a.setAttribute('data-template', key);
              
              // Create the template preview
              var preview = document.createElement('div');
              preview.className = 'template-preview';
              
              // Generate rows based on the template structure
              var template = window.preDesignedTemplates[key];
              if (Array.isArray(template) && template.length > 0) {
                // Show all rows in the preview
                for (var i = 0; i < template.length; i++) {
                  var row = template[i];
                  if (row.layout) {
                    var rowDiv = document.createElement('div');
                    rowDiv.className = 'template-preview-row';
                    
                    // Create columns based on layout
                    row.layout.forEach(function(colClass) {
                      var col = document.createElement('div');
                      col.className = 'template-preview-col';
                      var match = colClass.match(/small-(\d+)/);
                      if (match) {
                        var width = (parseInt(match[1]) / 12) * 100;
                        col.style.flex = width;
                      }
                      rowDiv.appendChild(col);
                    });
                    
                    preview.appendChild(rowDiv);
                  }
                }
              }
              
              // Create the label
              var labelDiv = document.createElement('div');
              labelDiv.className = 'template-label';
              labelDiv.textContent = label;
              
              a.appendChild(preview);
              a.appendChild(labelDiv);
              li.appendChild(a);
              list.appendChild(li);
            }
          });
        }
      });
    </script>
    <div style="text-align: right; margin-top: 20px;">
      <button id="close-pre-designed-page-modal" class="button tiny secondary">Cancel</button>
    </div>
  </div>
</div>

<!-- Hidden form for submission -->
<form id="editor-form" method="post" style="display: none;">
  <input type="hidden" name="widget" value="/json/saveWebPage1"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <input type="hidden" name="webPage" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="webPageLink" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="returnPage" value="${returnPage}" />
  <input type="hidden" id="designer-data" name="designerData" value=""/>
</form>

<!-- Store existing XML safely for JS -->
<script id="existing-xml-data" type="text/plain"><c:out value="${webPage.pageXml}" escapeXml="true"/></script>

<!-- Load JavaScript modules -->
<g:compress>
  <script src="${ctx}/javascript/widgets/editor/widget-registry.js"></script>
  <script src="${ctx}/javascript/widgets/editor/pre-designed-templates.js"></script>
  <script src="${ctx}/javascript/widgets/editor/editor-main.js"></script>
  <script src="${ctx}/javascript/widgets/editor/drag-drop-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/layout-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/canvas-controller.js"></script>
  <script src="${ctx}/javascript/widgets/editor/properties-panel.js"></script>
  <script src="${ctx}/javascript/widgets/editor/pages-tab-manager.js"></script>
</g:compress>

<script>
  // Initialize the editor
  document.addEventListener('DOMContentLoaded', function() {
    const editorConfig = {
      webPageLink: '<c:out value="${webPage.link}" />',
      existingXml: document.getElementById('existing-xml-data').textContent
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&amp;/g, '&')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&#034;/g, '"'),
      hasExistingLayout: <c:out value="${hasExistingLayout ? 'true' : 'false'}" default="false"/>
    };
    
    window.pageEditor = new PageEditor(editorConfig);
    
    // Set up preview update listener BEFORE initializing the page editor
    let isPreviewMode = false;
    const togglePreviewBtn = document.getElementById('toggle-preview-btn');
    const previewContainer = document.getElementById('preview-container');
    const editorCanvas = document.getElementById('editor-canvas');
    const previewIframe = document.getElementById('preview-iframe');
    const previewLoading = document.getElementById('preview-loading');
    const previewError = document.getElementById('preview-error');

    // Function to refresh the preview
    function refreshPreview() {
      previewIframe.style.display = 'none';
      previewLoading.style.display = 'block';
      previewError.style.display = 'none';
      
      // Get the current editor data as XML
      const layoutManager = window.pageEditor.getLayoutManager();
      const designerData = layoutManager.toXML();
      
      // Get the selected page link from the pages tab manager
      const webPageLink = window.pageEditor.pagesTabManager.getSelectedPageLink();
      
      // Send to server for rendering
      const formData = new FormData();
      // formData.append('widget', '<c:out value="${widgetContext.uniqueId}" />');
      formData.append('token', '<c:out value="${userSession.formToken}" />');
      formData.append('webPageLink', webPageLink);
      formData.append('designerData', designerData);
      formData.append('containerPreview', 'true');
      
      fetch(webPageLink, {
        method: 'POST',
        body: formData
      })
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to load preview: ' + response.statusText);
        }
        return response.text();
      })
      .then(html => {
        previewLoading.style.display = 'none';
        // Write the complete HTML response to the iframe
        const iframeDoc = previewIframe.contentDocument || previewIframe.contentWindow.document;
        iframeDoc.open();
        iframeDoc.write(html);
        iframeDoc.close();
        previewIframe.style.display = 'block';
      })
      .catch(error => {
        previewLoading.style.display = 'none';
        previewError.style.display = 'block';
        previewError.textContent = 'Error loading preview: ' + error.message;
      });
    }

    // Listen for page changes
    document.addEventListener('pageChanged', function(e) {
      console.log('pageChanged event fired, isPreviewMode:', isPreviewMode);
      // Reset the properties panel (unselect any previously selected widget)
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      
      // If in preview mode, refresh the preview when page is switched
      if (isPreviewMode) {
        console.log('Refreshing preview due to page change');
        // Add a small delay to ensure the layout is fully processed
        setTimeout(() => {
          refreshPreview();
        }, 100);
      }
    });

    togglePreviewBtn.addEventListener('click', function() {
      isPreviewMode = !isPreviewMode;
      
      if (isPreviewMode) {
        // Switch to preview mode
        editorCanvas.classList.add('hidden');
        previewContainer.classList.add('active');
        togglePreviewBtn.classList.add('active');
        
        // Load preview
        refreshPreview();
      } else {
        // Switch back to editor mode
        editorCanvas.classList.remove('hidden');
        previewContainer.classList.remove('active');
        togglePreviewBtn.classList.remove('active');
      }
    });

    // Now initialize the page editor
    window.pageEditor.init();

    // Set up middle section button handlers
    const returnPage = '<c:out value="${returnPage}" />';
    
    // Web Page Info button
    document.getElementById('web-page-info-btn').addEventListener('click', function(e) {
      e.preventDefault();
      // Reset the properties panel
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      const webPageLink = window.pageEditor.pagesTabManager.getSelectedPageLink();
      const link = '/admin/web-page?webPage=' + encodeURIComponent(webPageLink) + '&returnPage=' + encodeURIComponent(returnPage || webPageLink);
      previewIframe.src = link;
      previewIframe.classList.add('active');
      document.getElementById('preview-container').classList.add('active');
      editorCanvas.classList.add('hidden');
      togglePreviewBtn.classList.add('active');
      isPreviewMode = true;
    });
    
    // Web Page XML Editor button
    document.getElementById('web-page-xml-editor-btn').addEventListener('click', function(e) {
      e.preventDefault();
      // Reset the properties panel
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      const webPageLink = window.pageEditor.pagesTabManager.getSelectedPageLink();
      const link = '/admin/web-page-designer?webPage=' + encodeURIComponent(webPageLink);
      previewIframe.src = link;
      previewIframe.classList.add('active');
      document.getElementById('preview-container').classList.add('active');
      editorCanvas.classList.add('hidden');
      togglePreviewBtn.classList.add('active');
      isPreviewMode = true;
    });
    
    // Web Page CSS button
    document.getElementById('web-page-css-btn').addEventListener('click', function(e) {
      e.preventDefault();
      // Reset the properties panel
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      const webPageLink = window.pageEditor.pagesTabManager.getSelectedPageLink();
      const link = '/admin/css-editor?webPage=' + encodeURIComponent(webPageLink) + '&returnPage=' + encodeURIComponent(returnPage || webPageLink);
      previewIframe.src = link;
      previewIframe.classList.add('active');
      document.getElementById('preview-container').classList.add('active');
      editorCanvas.classList.add('hidden');
      togglePreviewBtn.classList.add('active');
      isPreviewMode = true;
    });

    // Helper function to close modal
    function closePreDesignedPageModal() {
      document.getElementById('pre-designed-page-modal').classList.remove('active');
    }

    // Show modal
    document.getElementById('pre-designed-page-btn').addEventListener('click', function() {
      document.getElementById('pre-designed-page-modal').classList.add('active');
    });
    
    // Hide modal
    document.getElementById('close-pre-designed-page-modal').addEventListener('click', function() {
      closePreDesignedPageModal();
    });
    
    // Close modal on ESC key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        const modal = document.getElementById('pre-designed-page-modal');
        if (modal && modal.classList.contains('active')) {
          closePreDesignedPageModal();
        }
      }
    });
    
    // Handle template selection
    document.getElementById('pre-designed-page-list').addEventListener('click', function(e) {
      // Find the closest anchor tag to handle clicks on child elements
      const link = e.target.closest('a');
      if (link) {
        e.preventDefault();
        const templateKey = link.getAttribute('data-template');
        const template = preDesignedTemplates[templateKey];
        if (template && window.pageEditor) {
          const layoutManager = window.pageEditor.getLayoutManager();
          // Remove all existing rows
          layoutManager.structure.rows = [];
          // Add new rows/widgets from template
          template.forEach(row => {
            const rowId = layoutManager.addRow(row.layout);
            const rowObj = layoutManager.getRow(rowId);
            if (Array.isArray(row.widgets)) {
              row.widgets.forEach((widget, colIdx) => {
                if (rowObj && rowObj.columns[colIdx]) {
                  const widgetId = layoutManager.addWidget(rowId, rowObj.columns[colIdx].id, widget.type);
                  // Set widget properties
                  const widgetObj = layoutManager.getWidget(widgetId);
                  if (widgetObj && widget.properties) {
                    Object.keys(widget.properties).forEach(propKey => {
                      widgetObj.properties[propKey] = widget.properties[propKey];
                    });
                  }
                }
              });
            }
          });
          // Re-render the layout after adding all rows/widgets
          window.pageEditor.getCanvasController().renderLayout(layoutManager.getStructure());
          window.pageEditor.saveToHistory();
        }
        closePreDesignedPageModal();
      }
    });

    // Set up palette tabs
    const tabs = document.querySelectorAll('.tabs-nav a');
    const tabContents = document.querySelectorAll('.tab-content');

    tabs.forEach(tab => {
      tab.addEventListener('click', e => {
        e.preventDefault();

        tabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        const target = document.querySelector(tab.getAttribute('href'));
        tabContents.forEach(tc => tc.classList.remove('active'));
        target.classList.add('active');
      });
    });

    // Handle clicking the add layout button
    document.querySelectorAll('.add-layout-btn').forEach(button => {
      button.addEventListener('click', (e) => {
        e.stopPropagation(); // Prevent triggering drag-and-drop
        const layoutItem = e.target.closest('.layout-palette-item');
        const layout = layoutItem.dataset.layout.split(',');
        window.pageEditor.addRow(layout);
      });
    });

    // Calculate the container height dynamically
    function calculateContainerHeight() {
      const wrapper = document.getElementById('visual-page-editor-wrapper');
      const toolbar = document.getElementById('editor-toolbar');
      const container = document.getElementById('visual-page-editor-container');
      
      if (!wrapper || !toolbar || !container) return;
      
      // Get the viewport height
      const viewportHeight = window.innerHeight;
      
      // Calculate the height used by elements above the container
      const wrapperTop = wrapper.getBoundingClientRect().top;
      const toolbarHeight = toolbar.offsetHeight;
      
      // Get any title element if it exists
      const titleElement = wrapper.querySelector('h4');
      const titleHeight = titleElement ? titleElement.offsetHeight : 0;
      
      // Get the messages container if it exists
      const messagesElement = wrapper.querySelector('[role="alert"], .messages-container');
      const messagesHeight = messagesElement ? messagesElement.offsetHeight : 0;
      
      // Calculate available height: viewport height - space from top - title - messages - toolbar
      const availableHeight = viewportHeight - wrapperTop - titleHeight - messagesHeight - toolbarHeight;
      
      // Set the container height with a minimum
      const finalHeight = Math.max(availableHeight, 300);
      container.style.height = finalHeight + 'px';
    }
    
    // Calculate height on load
    calculateContainerHeight();
    
    // Recalculate on window resize
    window.addEventListener('resize', calculateContainerHeight);
    
    // Dark Mode Toggle
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    const editorWrapper = document.getElementById('visual-page-editor-wrapper');
    
    // Check for saved theme preference
    const savedTheme = localStorage.getItem('editor-theme') || 'light';
    if (savedTheme === 'dark') {
      editorWrapper.setAttribute('data-theme', 'dark');
      darkModeToggle.innerHTML = '<i class="${font:far()} fa-sun"></i>';
    }
    
    darkModeToggle.addEventListener('click', function() {
      const currentTheme = editorWrapper.getAttribute('data-theme');
      if (currentTheme === 'dark') {
        editorWrapper.removeAttribute('data-theme');
        darkModeToggle.innerHTML = '<i class="${font:far()} fa-moon"></i>';
        localStorage.setItem('editor-theme', 'light');
      } else {
        editorWrapper.setAttribute('data-theme', 'dark');
        darkModeToggle.innerHTML = '<i class="${font:far()} fa-sun"></i>';
        localStorage.setItem('editor-theme', 'dark');
      }
    });
    
    // Properties Panel Resizing
    const propertiesPanel = document.getElementById('properties-panel');
    const resizeHandle = document.getElementById('properties-panel-resize-handle');
    let isResizing = false;
    let startX = 0;
    let startWidth = 0;
    
    resizeHandle.addEventListener('mousedown', function(e) {
      isResizing = true;
      startX = e.clientX;
      startWidth = parseInt(window.getComputedStyle(propertiesPanel).width, 10);
      resizeHandle.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
      e.preventDefault();
    });
    
    document.addEventListener('mousemove', function(e) {
      if (!isResizing) return;
      
      const diff = startX - e.clientX; // Reverse because we're resizing from left
      const newWidth = startWidth + diff;
      const minWidth = 200;
      const maxWidth = 600;
      
      if (newWidth >= minWidth && newWidth <= maxWidth) {
        propertiesPanel.style.width = newWidth + 'px';
      }
    });
    
    document.addEventListener('mouseup', function() {
      if (isResizing) {
        isResizing = false;
        resizeHandle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        // Save width preference
        localStorage.setItem('properties-panel-width', propertiesPanel.style.width);
      }
    });
    
    // Restore saved width
    const savedWidth = localStorage.getItem('properties-panel-width');
    if (savedWidth) {
      propertiesPanel.style.width = savedWidth;
    }
  });
</script>
