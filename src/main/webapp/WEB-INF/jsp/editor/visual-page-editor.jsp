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
  #visual-page-editor-container {
    display: flex;
    height: 600px; /* Will be calculated dynamically by JavaScript */
    min-height: 300px;
  }
  
  #editor-toolbar {
    background: #f8f9fa;
    border-bottom: 1px solid #dee2e6;
    padding: 10px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  #widget-palette {
    width: 250px;
    background: #ffffff;
    border-right: 1px solid #dee2e6;
    overflow-y: auto;
    padding: 15px;
  }
  
  #editor-canvas {
    flex: 1;
    background: #ffffff;
    overflow-y: auto;
    padding: 20px;
    position: relative;
  }
  
  #properties-panel {
    width: 300px;
    background: #f8f9fa;
    border-left: 1px solid #dee2e6;
    overflow-y: auto;
    padding: 15px;
  }
  
  .widget-palette-item {
    background: #fff;
    border: 1px solid #dee2e6;
    border-radius: 4px;
    padding: 10px;
    margin-bottom: 10px;
    cursor: move;
    transition: all 0.2s;
  }
  
  .widget-palette-item:hover {
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    border-color: #007bff;
  }
  
  .widget-palette-category {
    font-weight: bold;
    font-size: 14px;
    color: #495057;
    margin: 15px 0 10px 0;
    padding-bottom: 5px;
    border-bottom: 2px solid #dee2e6;
  }
  
  .canvas-row {
    border: 2px dashed #dee2e6;
    padding: 15px;
    margin-bottom: 15px;
    min-height: 80px;
    position: relative;
    transition: all 0.2s;
  }
  
  .canvas-row:hover {
    border-color: #007bff;
    background: #f8f9fa;
  }
  
  .canvas-row.drag-over {
    background: #e7f3ff;
    border-color: #007bff;
  }
  
  .canvas-column {
    border: 1px solid #dee2e6;
    padding: 10px;
    min-height: 60px;
    position: relative;
  }
  
  .canvas-column.drag-over {
    background: #e7f3ff;
    border-color: #007bff;
  }
  
  .canvas-widget {
    background: #ffffff;
    border: 1px solid #dee2e6;
    border-radius: 4px;
    padding: 15px;
    margin-bottom: 10px;
    position: relative;
    cursor: pointer;
  }
  
  .canvas-widget:hover {
    border-color: #007bff;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }
  
  .canvas-widget.selected {
    border-color: #007bff;
    box-shadow: 0 0 0 3px rgba(0,123,255,0.25);
  }
  
  .widget-controls {
    position: absolute;
    top: 5px;
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
    top: 5px;
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
    color: #6c757d;
  }
  
  .property-group {
    margin-bottom: 10px;
  }
  
  .property-label {
    font-weight: 600;
    font-size: 13px;
    margin-bottom: 5px;
    color: #495057;
  }
  
  .property-input {
    width: 100%;
    padding: 8px;
    border: 1px solid #ced4da;
    border-radius: 4px;
    font-size: 14px;
  }
  
  .property-input:focus {
    outline: none;
    border-color: #007bff;
    box-shadow: 0 0 0 2px rgba(0,123,255,0.25);
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
    background: white;
    padding: 30px;
    border-radius: 8px;
    box-shadow: 0 5px 15px rgba(0,0,0,0.3);
    width: 90%;
    max-width: 600px;
  }

  .layout-picker {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 20px;
  }

  .layout-option {
    border: 2px solid #dee2e6;
    border-radius: 4px;
    padding: 15px;
    cursor: pointer;
    transition: all 0.2s;
    text-align: center;
  }

  .layout-option:hover {
    border-color: #007bff;
    background: #f8f9fa;
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
    background: #fff;
    border: 1px solid #dee2e6;
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
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    border-color: #007bff;
  }

  .layout-label {
    font-size: 11px;
    color: #6c757d;
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
    border: 2px solid #dee2e6;
    border-radius: 6px;
    text-decoration: none;
    color: #495057;
    background: white;
    cursor: pointer;
    transition: all 0.2s;
    height: 100%;
    min-height: 180px;
  }

  #pre-designed-page-list a:hover {
    border-color: #007bff;
    background: #f8f9fa;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }

  .template-preview {
    width: 100%;
    height: 80px;
    background: white;
    border: 1px solid #dee2e6;
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
    color: #495057;
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
    border-bottom: 1px solid #dee2e6;
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
    color: #495057;
    border-bottom: 3px solid transparent;
    transition: all 0.2s;
    font-weight: 600;
    font-size: 12px;
    white-space: nowrap;
  }

  .tabs-nav a:hover {
    background-color: #f8f9fa;
  }

  .tabs-nav a.active {
    color: #007bff;
    border-bottom-color: #007bff;
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
    border: 1px solid #dee2e6;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    background: white;
  }

  .web-page-item:hover {
    border-color: #007bff;
    background: #f8f9fa;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }

  .web-page-item.selected {
    border-color: #007bff;
    background: #e7f3ff;
  }

  .web-page-info {
    flex-grow: 1;
  }

  .web-page-title {
    font-weight: 600;
    color: #333;
    margin-bottom: 3px;
  }

  .web-page-link {
    font-size: 12px;
    color: #6c757d;
  }

  #pages-loading {
    text-align: center;
    padding: 20px;
    color: #6c757d;
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
    color: #6c757d;
  }

  /* Preview Modal */
  #preview-modal {
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

  #preview-modal.active {
    display: flex;
  }

  #preview-modal-content {
    background: white;
    border-radius: 8px;
    box-shadow: 0 5px 25px rgba(0,0,0,0.3);
    width: 90%;
    height: 90%;
    max-width: 1200px;
    display: flex;
    flex-direction: column;
  }

  #preview-modal-header {
    padding: 15px 20px;
    border-bottom: 1px solid #dee2e6;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-shrink: 0;
  }

  #preview-modal-header h4 {
    margin: 0;
    font-size: 16px;
  }

  #preview-modal-body {
    flex: 1;
    overflow: auto;
    background: white;
    padding-top: 10px;
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
</style>
<link href="${ctx}/css/platform.css" rel="stylesheet">

<div id="visual-page-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>
  
  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div>
      <button id="add-row-btn" class="button tiny primary no-gap"><i class="${font:far()} fa-plus"></i> Add Row</button>
      <button id="undo-btn" class="button tiny secondary no-gap" disabled><i class="${font:far()} fa-undo"></i> Undo</button>
      <button id="redo-btn" class="button tiny secondary no-gap" disabled><i class="${font:far()} fa-redo"></i> Redo</button>
      <button id="pre-designed-page-btn" class="button tiny secondary no-gap"><i class="${font:far()} fa-magic"></i> Pre-Designed Page</button>
    </div>
    <div>
      <button id="toggle-preview-btn" class="button tiny secondary no-gap"><i class="${font:far()} fa-eye"></i> Preview</button>
      <button id="preview-btn" class="button tiny secondary no-gap"><i class="${font:far()} fa-database"></i> Data</button>
      <button id="save-btn" class="button tiny success no-gap"><i class="${font:far()} fa-save"></i> Save</button>
      <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="button tiny secondary no-gap">Cancel</a>
        </c:when>
        <c:when test="${!empty webPage.link}">
          <a href="${ctx}${webPage.link}" class="button tiny secondary no-gap">Cancel</a>
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
      <h5>Properties</h5>
      <div id="properties-content">
        <p style="color: #6c757d; font-size: 14px;">Select an element to edit its properties</p>
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
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <input type="hidden" name="webPage" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="webPageLink" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="returnPage" value="${returnPage}" />
  <input type="hidden" id="designer-data" name="designerData" value=""/>
</form>

<!-- Preview Modal -->
<div id="preview-modal">
  <div id="preview-modal-content">
    <div id="preview-modal-header">
      <h4>Page Preview</h4>
      <button id="close-preview-modal-btn" class="button tiny secondary no-gap"><i class="${font:far()} fa-times"></i> Close</button>
    </div>
    <div id="preview-modal-body">
      <div id="preview-loading" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i> Loading preview...
      </div>
      <div id="preview-error" style="display: none;"></div>
      <iframe id="preview-iframe" style="width: 100%; height: 100%; border: none; display: none;"></iframe>
    </div>
  </div>
</div>

<!-- Store existing XML safely for JS -->
<script id="existing-xml-data" type="text/plain"><c:out value="${webPage.pageXml}" escapeXml="true"/></script>

<!-- Load JavaScript modules -->
<g:compress>
  <script src="${ctx}/javascript/widgets/editor/pre-designed-templates.js"></script>
  <script src="${ctx}/javascript/widgets/editor/editor-main.js"></script>
  <script src="${ctx}/javascript/widgets/editor/drag-drop-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/layout-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/widget-registry.js"></script>
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
    window.pageEditor.init();

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

    // Preview Mode Toggle
    const togglePreviewBtn = document.getElementById('toggle-preview-btn');
    const closePreviewModalBtn = document.getElementById('close-preview-modal-btn');
    const previewModal = document.getElementById('preview-modal');
    const previewIframe = document.getElementById('preview-iframe');
    const previewLoading = document.getElementById('preview-loading');
    const previewError = document.getElementById('preview-error');

    togglePreviewBtn.addEventListener('click', function() {
      previewModal.classList.add('active');
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
    });

    closePreviewModalBtn.addEventListener('click', function() {
      previewModal.classList.remove('active');
    });

    // Close preview on ESC key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape' && previewModal.classList.contains('active')) {
        previewModal.classList.remove('active');
      }
    });
  });
</script>
