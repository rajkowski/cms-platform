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
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-image-editor.css" />
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
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

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <h2>Image Editor</h2>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="import-btn" class="button tiny primary no-gap radius" title="Import Image"><i class="${font:far()} fa-upload"></i> Import</button>
      <button id="new-from-clipboard-btn" class="button tiny primary no-gap radius" title="New from Clipboard"><i class="${font:far()} fa-clipboard"></i> From Clipboard</button>
      <button id="new-from-stock-btn" class="button tiny success no-gap radius" title="New from Stock Photo"><i class="${font:far()} fa-images"></i> Stock Photo...</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <button id="reload-btn" class="button tiny secondary no-gap radius" title="Reload"><i class="${font:far()} fa-sync"></i> Reload</button>
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
          <!-- Quick Access Apps -->
          <div class="apps-menu-section">
            <div class="apps-grid">
              <a href="${ctx}/admin/visual-page-editor" class="apps-item confirm-exit" title="Edit Pages">
                <i class="${font:far()} fa-file-lines"></i>
                <span class="apps-item-label">Pages</span>
              </a>
              <a href="${ctx}/admin/visual-image-editor" class="apps-item active confirm-exit" title="Manage Images">
                <i class="${font:far()} fa-image"></i>
                <span class="apps-item-label">Images</span>
              </a>
              <a href="${ctx}/admin/visual-document-editor" class="apps-item confirm-exit" title="Manage Documents">
                <i class="${font:far()} fa-file"></i>
                <span class="apps-item-label">Documents</span>
              </a>
              <a href="${ctx}/admin/visual-data-editor" class="apps-item confirm-exit" title="Manage Data">
                <i class="${font:far()} fa-table"></i>
                <span class="apps-item-label">Data</span>
              </a>
            </div>
          </div>

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
  
  <!-- Main Editor Container -->
  <div id="visual-image-editor-container">
    
    <!-- Left Panel: Image Library -->
    <div id="image-library-panel">
      <div class="panel-header">
        <h3>Image Library <span id="image-count-badge" class="badge secondary">0</span></h3>
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
          <button id="save-image-btn" class="tool-btn primary" title="Save Copy" disabled><i class="${font:far()} fa-save"></i> Save Copy</button>
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
        <h3>Image Details</h3>
      </div>
      
      <!-- Tabs -->
      <div class="properties-tabs">
        <button class="tab-button active" data-tab="properties">Properties</button>
        <button class="tab-button" data-tab="versions">Versions</button>
      </div>
      
      <!-- Properties Tab Content -->
      <div id="properties-tab-content" class="tab-content active">
        <div class="no-image-selected">
          <p>Select an image to view properties</p>
        </div>
        <form id="image-metadata-form" class="hidden">
          <div class="property-group">
            <label for="image-title">Title</label>
            <input type="text" id="image-title" class="property-input" />
          </div>
          
          <div class="property-group">
            <label for="image-url">URL</label>
            <div class="url-display-container">
              <a id="image-url" href="#" target="_blank" class="property-url" title="Click to open in new tab">-</a>
              <button type="button" id="copy-url-btn" class="button tiny secondary" title="Copy URL to clipboard">
                <i class="${font:far()} fa-copy"></i>
              </button>
            </div>
          </div>
          
          <div class="property-group">
            <label for="image-filename">Filename</label>
            <input type="text" id="image-filename" class="property-input" />
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
          
          <div class="property-actions">
            <button type="button" id="save-metadata-btn" class="button tiny expanded primary" disabled>
              <i class="${font:far()} fa-save"></i> Save Properties
            </button>
            <button type="button" id="delete-image-btn" class="button tiny expanded alert">
              <i class="${font:far()} fa-trash"></i> Delete Image
            </button>
          </div>
        </form>
      </div>
      
      <!-- Versions Tab Content -->
      <div id="versions-tab-content" class="tab-content">
        <div class="no-image-selected">
          <p>Select an image to view versions</p>
        </div>
        <div id="versions-list-container" class="hidden">
          <div class="property-group">
            <button type="button" id="upload-new-version-btn" class="button tiny expanded primary">
              <i class="${font:far()} fa-upload"></i> Upload New Version
            </button>
          </div>
          
          <div class="versions-info">
            <p><strong>Current Version:</strong> <span id="current-version-number">1</span></p>
            <p><strong>Total Versions:</strong> <span id="total-versions-count">1</span></p>
          </div>
          
          <div id="versions-list">
            <!-- Version history will be loaded here -->
            <div class="loading-message">
              <i class="${font:far()} fa-spinner fa-spin"></i>
              <p>Loading versions...</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Hidden file input for image import -->
<input type="file" id="image-file-input" accept="image/*" style="display: none;" />

<!-- Modals and Dialogs -->
<div id="versions-modal" class="reveal" data-reveal>
  <h3>Image Versions</h3>
  <div id="versions-modal-list">
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

    // Setup tab switching function
    function setupTabs() {
      const tabButtons = document.querySelectorAll('.tab-button');
      const tabContents = document.querySelectorAll('.tab-content');
      
      console.log('Tab buttons found:', tabButtons.length);
      console.log('Tab contents found:', tabContents.length);
      
      tabButtons.forEach(button => {
        button.addEventListener('click', () => {
          const targetTab = button.getAttribute('data-tab');
          console.log('Clicked tab:', targetTab);
          
          if (!targetTab) {
            console.error('No data-tab attribute found on button:', button);
            return;
          }
          
          // Remove active class from all buttons and contents
          tabButtons.forEach(btn => btn.classList.remove('active'));
          tabContents.forEach(content => {
            content.classList.remove('active');
            content.classList.add('hidden');
          });
          
          // Add active class to clicked button and corresponding content
          button.classList.add('active');
          const targetContentId = targetTab + '-tab-content';
          const targetContent = document.getElementById(targetContentId);
          
          if (targetContent) {
            targetContent.classList.add('active');
            targetContent.classList.remove('hidden');
          } else {
            console.error('Tab content element not found. ID:', targetContentId);
          }
          
          // If switching to versions tab, load versions
          if (targetTab === 'versions' && imageEditor) {
            imageEditor.imageProperties.loadVersions();
          }
        });
      });
    }

    setupAppsMenu();
    
    // Setup tab switching
    setupTabs();
    
    // Initialize dark mode from localStorage
    const savedTheme = localStorage.getItem('editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      const darkModeIcon = document.querySelector('#dark-mode-toggle-menu i');
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
