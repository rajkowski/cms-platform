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
      <c:choose>
        <c:when test="${!empty returnPage}">
          <a href="${returnPage}" class="button tiny no-gap radius confirm-exit">Exit</a>
        </c:when>
        <c:otherwise>
          <a href="${ctx}/admin" class="button tiny no-gap radius confirm-exit">Exit</a>
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
          <button id="crop-btn" class="tool-btn" title="Crop" disabled><i class="${font:far()} fa-crop"></i></button>
          <button id="rotate-left-btn" class="tool-btn" title="Rotate Left" disabled><i class="${font:far()} fa-undo"></i></button>
          <button id="rotate-right-btn" class="tool-btn" title="Rotate Right" disabled><i class="${font:far()} fa-redo"></i></button>
          <button id="flip-horizontal-btn" class="tool-btn" title="Flip Horizontal" disabled><i class="${font:far()} fa-arrows-h"></i></button>
          <button id="flip-vertical-btn" class="tool-btn" title="Flip Vertical" disabled><i class="${font:far()} fa-arrows-v"></i></button>
          <button id="adjustments-btn" class="tool-btn" title="Adjustments" disabled><i class="${font:far()} fa-sliders-h"></i></button>
          <div class="tool-divider"></div>
          <button id="compare-btn" class="tool-btn" title="Compare Changes" disabled><i class="${font:far()} fa-eye"></i></button>
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
      
      <!-- Crop Tool Overlay -->
      <div id="crop-overlay" style="display: none;">
        <div class="crop-controls">
          <button id="apply-crop-btn" class="button tiny success no-gap">Apply</button>
          <button id="cancel-crop-btn" class="button tiny secondary no-gap">Cancel</button>
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
    selectedImageId: <c:out value="${selectedImageId}" default="-1" />,
    apiBaseUrl: '${ctx}/json',
    contextPath: '${ctx}',
    returnPage: '<c:out value="${returnPage}" />',
    userId: ${userSession.userId}
  };
  
  // Initialize the editor when DOM is ready
  document.addEventListener('DOMContentLoaded', function() {
    console.log('Initializing Visual Image Editor...');
    
    // Initialize dark mode from localStorage
    const savedTheme = localStorage.getItem('image-editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
      document.getElementById('dark-mode-toggle').querySelector('i').classList.replace('fa-moon', 'fa-sun');
    }
    
    // Load the image library
    loadImageLibrary();
    
    // Setup event listeners
    setupEventListeners();
    
    // If a specific image was requested, load it
    if (imageEditorConfig.selectedImageId > 0) {
      loadImage(imageEditorConfig.selectedImageId);
    }
  });
  
  // Load image library with pagination
  function loadImageLibrary(page = 1, searchTerm = '') {
    const container = document.querySelector('#image-list-container .image-grid');
    container.innerHTML = '<div class="loading-message"><i class="${font:far()} fa-spinner fa-spin"></i><p>Loading images...</p></div>';
    
    // TODO: Implement Ajax call to load images
    // For now, show a placeholder
    setTimeout(() => {
      container.innerHTML = '<div class="no-images-message"><i class="${font:far()} fa-image fa-3x"></i><p>No images found</p><button id="upload-first-image-btn" class="button tiny primary">Upload Your First Image</button></div>';
    }, 500);
  }
  
  // Load a specific image
  function loadImage(imageId) {
    console.log('Loading image:', imageId);
    // TODO: Implement image loading logic
  }
  
  // Setup all event listeners
  function setupEventListeners() {
    // Dark mode toggle
    document.getElementById('dark-mode-toggle').addEventListener('click', toggleDarkMode);
    
    // Search
    document.getElementById('image-search').addEventListener('input', debounce(function(e) {
      loadImageLibrary(1, e.target.value);
    }, 300));
    
    // Toolbar buttons
    document.getElementById('new-from-stock-btn').addEventListener('click', showStockPhotoDialog);
    document.getElementById('new-from-clipboard-btn').addEventListener('click', pasteFromClipboard);
    document.getElementById('import-btn').addEventListener('click', () => document.getElementById('image-file-input').click());
    document.getElementById('reload-btn').addEventListener('click', reloadCurrentImage);
    document.getElementById('save-btn').addEventListener('click', saveImageChanges);
    
    // File input
    document.getElementById('image-file-input').addEventListener('change', handleFileImport);
    
    // Image tools
    document.getElementById('crop-btn').addEventListener('click', startCrop);
    document.getElementById('rotate-left-btn').addEventListener('click', () => rotateImage(-90));
    document.getElementById('rotate-right-btn').addEventListener('click', () => rotateImage(90));
    document.getElementById('flip-horizontal-btn').addEventListener('click', () => flipImage('horizontal'));
    document.getElementById('flip-vertical-btn').addEventListener('click', () => flipImage('vertical'));
    document.getElementById('adjustments-btn').addEventListener('click', showAdjustments);
    document.getElementById('compare-btn').addEventListener('click', compareChanges);
    document.getElementById('reset-btn').addEventListener('click', resetChanges);
    
    // Warn on exit if unsaved changes
    window.addEventListener('beforeunload', function(e) {
      if (hasUnsavedChanges()) {
        e.preventDefault();
        e.returnValue = '';
      }
    });
    
    // Confirm exit links
    document.querySelectorAll('.confirm-exit').forEach(link => {
      link.addEventListener('click', function(e) {
        if (hasUnsavedChanges()) {
          e.preventDefault();
          showUnsavedChangesModal(() => {
            window.location.href = this.href;
          });
        }
      });
    });
  }
  
  // Toggle dark mode
  function toggleDarkMode() {
    const html = document.documentElement;
    const icon = this.querySelector('i');
    const isDark = html.getAttribute('data-theme') === 'dark';
    
    if (isDark) {
      html.removeAttribute('data-theme');
      localStorage.setItem('image-editor-theme', 'light');
      icon.classList.replace('fa-sun', 'fa-moon');
    } else {
      html.setAttribute('data-theme', 'dark');
      localStorage.setItem('image-editor-theme', 'dark');
      icon.classList.replace('fa-moon', 'fa-sun');
    }
  }
  
  // Utility function for debouncing
  function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }
  
  // Check if there are unsaved changes
  function hasUnsavedChanges() {
    // TODO: Implement change tracking
    return false;
  }
  
  // Show unsaved changes modal
  function showUnsavedChangesModal(onContinue) {
    // TODO: Implement modal display
    if (onContinue) onContinue();
  }
  
  // Placeholder functions for toolbar actions
  function showStockPhotoDialog() { console.log('Show stock photo dialog'); }
  function pasteFromClipboard() { console.log('Paste from clipboard'); }
  function handleFileImport(e) { console.log('Handle file import', e); }
  function reloadCurrentImage() { console.log('Reload current image'); }
  function saveImageChanges() { console.log('Save image changes'); }
  function startCrop() { console.log('Start crop'); }
  function rotateImage(degrees) { console.log('Rotate image', degrees); }
  function flipImage(direction) { console.log('Flip image', direction); }
  function showAdjustments() { console.log('Show adjustments'); }
  function compareChanges() { console.log('Compare changes'); }
  function resetChanges() { console.log('Reset changes'); }
</script>
