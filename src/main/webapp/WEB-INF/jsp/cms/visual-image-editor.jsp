<%--
  ~ Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
<web:stylesheet package="spectrum" file="spectrum.css" />
<web:script package="spectrum" file="spectrum.js" />
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-image-editor.css" />
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/widgets/image-editor/image-library-manager.js"></script>
  <script src="${ctx}/javascript/widgets/image-editor/image-viewer-manager.js"></script>
  <script src="${ctx}/javascript/widgets/image-editor/image-properties-manager.js"></script>
  <script src="${ctx}/javascript/widgets/image-editor/image-editor-main.js"></script>
</g:compress>
<div id="visual-image-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Title Bar -->
  <div id="editor-titlebar">
    <div class="titlebar-left">
      <img src="${ctx}/images/favicon.png" alt="Logo" />
      <h2>Image Editor</h2>
    </div>
    <div class="titlebar-right">
      <div class="button-group round">
        <a href="${ctx}/admin/visual-page-editor" class="button confirm-exit">Pages</a>
        <a href="${ctx}/admin/visual-image-editor" class="button confirm-exit active">Images</a>
        <a href="${ctx}/admin/visual-document-editor" class="button confirm-exit">Documents</a>
      </div>
      <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="button radius confirm-exit">Exit</a>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/" class="button radius confirm-exit">Exit</a>
        </c:otherwise>
      </c:choose>
    </div>
  </div>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="new-from-stock-btn" class="button tiny success no-gap radius" title="New from Stock Photo"><i class="${font:far()} fa-images"></i> Stock Photo</button>
      <button id="new-from-clipboard-btn" class="button tiny primary no-gap radius" title="New from Clipboard"><i class="${font:far()} fa-clipboard"></i> From Clipboard</button>
      <button id="import-btn" class="button tiny primary no-gap radius" title="Import Image"><i class="${font:far()} fa-upload"></i> Import</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <button id="reload-btn" class="button tiny secondary no-gap radius" title="Reload"><i class="${font:far()} fa-sync"></i> Reload</button>
      <button id="save-btn" class="button tiny no-gap radius" disabled title="Save Changes"><i class="${font:far()} fa-save"></i> Save</button>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
      <button id="dark-mode-toggle" class="button tiny secondary no-gap radius" title="Toggle Dark Mode"><i class="${font:far()} fa-moon"></i></button>
    </div>
  </div>
  
  <!-- Main Editor Container -->
  <div id="visual-image-editor-container">
    
    <!-- Left Panel: Image Library -->
    <div id="image-library-panel">
      <div class="panel-header">
        <h3>Image Library</h3>
        <div class="search-container">
          <input type="text" id="image-search" placeholder="Search images..." />
        </div>
      </div>
      <div id="image-list-container">
        <!-- Images will be dynamically loaded here -->
        <div class="image-grid">
          <div class="loading-message">
            <i class="${font:far()} fa-spinner fa-spin"></i>
            <p>Loading images...</p>
          </div>
        </div>
      </div>
      <div id="image-pagination">
        <!-- Pagination controls will be added here -->
      </div>
    </div>
    
    <!-- Middle Panel: Image Viewer/Editor -->
    <div id="image-viewer-panel">
      <div class="panel-header">
        <h3 id="image-viewer-title">Select an Image</h3>
        <div id="image-tools">
          <button id="crop-selection-btn" class="tool-btn" title="Crop image to selection" disabled><i class="${font:far()} fa-crop"></i> Crop</button>
          <button id="copy-selection-btn" class="tool-btn" title="Copy selection to clipboard" disabled><i class="${font:far()} fa-copy"></i> Copy</button>
          <button id="clear-selection-btn" class="tool-btn" title="Clear selection" disabled><i class="${font:far()} fa-times"></i> Clear</button>
          <div class="tool-divider"></div>
          <button id="rotate-left-btn" class="tool-btn" title="Rotate Left" disabled><i class="${font:far()} fa-undo"></i></button>
          <button id="rotate-right-btn" class="tool-btn" title="Rotate Right" disabled><i class="${font:far()} fa-redo"></i></button>
          <button id="flip-horizontal-btn" class="tool-btn" title="Flip Horizontal" disabled><i class="${font:far()} fa-arrows-h"></i></button>
          <button id="flip-vertical-btn" class="tool-btn" title="Flip Vertical" disabled><i class="${font:far()} fa-arrows-v"></i></button>
          <button id="adjustments-btn" class="tool-btn" title="Adjustments" disabled><i class="${font:far()} fa-sliders-h"></i></button>
          <div class="tool-divider"></div>
          <button id="reset-btn" class="tool-btn" title="Reset Changes" disabled><i class="${font:far()} fa-times-circle"></i></button>
        </div>
      </div>
      <div id="image-viewer-content">
        <div class="no-image-selected">
          <i class="${font:far()} fa-image fa-5x"></i>
          <p>Select an image from the library to begin editing</p>
        </div>
        <div id="image-canvas-container" style="display: none;">
          <canvas id="image-canvas"></canvas>
        </div>
      </div>
      
      <!-- Adjustments Panel -->
      <div id="adjustments-panel" style="display: none;">
        <h4>Image Adjustments</h4>
        <div class="adjustment-control">
          <label>Brightness</label>
          <input type="range" id="brightness-slider" min="-100" max="100" value="0" />
          <span id="brightness-value">0</span>
        </div>
        <div class="adjustment-control">
          <label>Contrast</label>
          <input type="range" id="contrast-slider" min="-100" max="100" value="0" />
          <span id="contrast-value">0</span>
        </div>
        <div class="adjustment-control">
          <label>Saturation</label>
          <input type="range" id="saturation-slider" min="-100" max="100" value="0" />
          <span id="saturation-value">0</span>
        </div>
        <div class="adjustment-controls">
          <button id="apply-adjustments-btn" class="button tiny success no-gap">Apply</button>
          <button id="cancel-adjustments-btn" class="button tiny secondary no-gap">Cancel</button>
        </div>
      </div>
    </div>
    
    <!-- Right Panel: Image Properties -->
    <div id="image-properties-panel">
      <div id="properties-panel-resize-handle"></div>
      <div class="panel-header">
        <h3>Image Properties</h3>
      </div>
      <div id="properties-content">
        <div class="no-image-selected">
          <p>Select an image to view properties</p>
        </div>
        <form id="image-metadata-form" style="display: none;">
          <div class="property-group">
            <label for="image-title">Title</label>
            <input type="text" id="image-title" class="property-input" />
          </div>
          
          <div class="property-group">
            <label for="image-filename">Filename</label>
            <input type="text" id="image-filename" class="property-input" readonly />
          </div>
          
          <div class="property-group">
            <label for="image-alt-text">Alt Text</label>
            <input type="text" id="image-alt-text" class="property-input" />
          </div>
          
          <div class="property-group">
            <label for="image-description">Description</label>
            <textarea id="image-description" class="property-input" rows="3"></textarea>
          </div>
          
          <div class="property-group">
            <label>Dimensions</label>
            <div id="image-dimensions" class="property-value">-</div>
          </div>
          
          <div class="property-group">
            <label>File Size</label>
            <div id="image-file-size" class="property-value">-</div>
          </div>
          
          <div class="property-group">
            <label>File Type</label>
            <div id="image-file-type" class="property-value">-</div>
          </div>
          
          <div class="property-group">
            <label>Created</label>
            <div id="image-created" class="property-value">-</div>
          </div>
          
          <div class="property-group">
            <label>Modified</label>
            <div id="image-modified" class="property-value">-</div>
          </div>
          
          <div class="property-group">
            <label>Version</label>
            <div id="image-version-info" class="property-value">-</div>
            <button type="button" id="view-versions-btn" class="button tiny expanded secondary">View Versions</button>
          </div>
          
          <div class="property-group">
            <button type="button" id="upload-new-version-btn" class="button tiny expanded primary">
              <i class="${font:far()} fa-upload"></i> Upload New Version
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<!-- Hidden file input for image import -->
<input type="file" id="image-file-input" accept="image/*" style="display: none;" />

<!-- Modals and Dialogs -->
<div id="versions-modal" class="reveal" data-reveal>
  <h3>Image Versions</h3>
  <div id="versions-list">
    <!-- Versions will be loaded here -->
  </div>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<div id="unsaved-changes-modal" class="reveal" data-reveal>
  <h3>Unsaved Changes</h3>
  <p>You have unsaved changes. Do you want to save them before continuing?</p>
  <div class="button-group">
    <button id="save-and-continue-btn" class="button primary">Save & Continue</button>
    <button id="discard-changes-btn" class="button alert">Discard Changes</button>
    <button class="button secondary" data-close>Cancel</button>
  </div>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<script>
  // Configuration and Initialization
  const imageEditorConfig = {
    token: '<c:out value="${userSession.formToken}" />',
    selectedImageId: <c:out value="${selectedImageId}" default="-1" />,
    apiBaseUrl: '${ctx}/json',
    contextPath: '${ctx}',
    returnPage: '<c:out value="${returnPage}" />',
    userId: <c:out value="${userSession.userId}" default="-1" />
  };
  
  // Global editor instance
  let imageEditor;
  
  // Initialize the editor when DOM is ready
  document.addEventListener('DOMContentLoaded', function() {
    console.log('Initializing Visual Image Editor...');
    
    // Initialize dark mode from localStorage
    const savedTheme = localStorage.getItem('editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      const darkModeIcon = document.querySelector('#dark-mode-toggle i');
      if (darkModeIcon) {
        darkModeIcon.classList.replace('fa-moon', 'fa-sun');
      }
    }
    
    // Create and initialize the editor
    imageEditor = new ImageEditor(imageEditorConfig);
    imageEditor.init();
    
    // Properties Panel Resizing
    const propertiesPanelElement = document.getElementById('image-properties-panel');
    const resizeHandle = document.getElementById('properties-panel-resize-handle');
    let isResizing = false;
    let startX = 0;
    let startWidth = 0;
    
    resizeHandle.addEventListener('mousedown', function(e) {
      isResizing = true;
      startX = e.clientX;
      startWidth = parseInt(window.getComputedStyle(propertiesPanelElement).width, 10);
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
        propertiesPanelElement.style.width = newWidth + 'px';
      }
    });
    
    document.addEventListener('mouseup', function() {
      if (isResizing) {
        isResizing = false;
        resizeHandle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        // Save width preference
        localStorage.setItem('image-properties-panel-width', propertiesPanelElement.style.width);
      }
    });
    
    // Restore saved width
    const savedWidth = localStorage.getItem('image-properties-panel-width');
    if (savedWidth) {
      propertiesPanelElement.style.width = savedWidth;
    }
  });
</script>
