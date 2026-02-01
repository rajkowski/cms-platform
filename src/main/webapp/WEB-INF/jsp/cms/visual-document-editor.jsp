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
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-document-editor.css" />
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/widgets/document-editor/document-library-manager.js"></script>
  <script src="${ctx}/javascript/widgets/document-editor/document-file-manager.js"></script>
  <script src="${ctx}/javascript/widgets/document-editor/folder-details-manager.js"></script>
  <script src="${ctx}/javascript/widgets/document-editor/folder-permissions-manager.js"></script>
  <script src="${ctx}/javascript/widgets/document-editor/document-properties-manager.js"></script>
  <script src="${ctx}/javascript/widgets/document-editor/document-editor-main.js"></script>
</g:compress>
<div id="visual-document-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <h2>Document Editor</h2>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="new-folder-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-folder-plus"></i> New Repository</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <span id="unsaved-indicator"><i class="${font:far()} fa-exclamation-triangle"></i> Unsaved changes</span>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
    </div>

    <!-- App Grid -->
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
              <a href="${ctx}/admin/visual-image-editor" class="apps-item confirm-exit" title="Manage Images">
                <i class="${font:far()} fa-image"></i>
                <span class="apps-item-label">Images</span>
              </a>
              <a href="${ctx}/admin/visual-document-editor" class="apps-item active confirm-exit" title="Manage Documents">
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

  <div id="visual-document-editor-container">
    <!-- Left Panel -->
    <div id="document-library-panel">
      <div class="panel-header">
        <h3>Repositories</h3>
        <div class="search-container">
          <input type="text" id="document-search" placeholder="Search repositories..." />
        </div>
      </div>
      <div id="folder-list-container">
        <div class="empty-state">Loading...</div>
      </div>
    </div>

    <!-- Middle Panel -->
    <div id="document-browser-panel">
      <div class="panel-header">
        <h3 id="document-browser-title">Files</h3>
        <div id="breadcrumb-navigation" style="display: none">
          <nav class="breadcrumbs">
            <button id="breadcrumb-root" class="breadcrumb-link">Repository</button>
            <span id="breadcrumb-path"></span>
          </nav>
        </div>
        <div id="file-toolbar">
          <button id="new-subfolder-btn" class="button tiny success no-gap radius" disabled><i class="${font:far()} fa-folder-plus"></i> New Folder</button>
          <button id="import-doc-btn" class="button tiny primary no-gap radius" disabled><i class="${font:far()} fa-upload"></i> Upload Files</button>
          <button id="new-url-btn" class="button tiny primary no-gap radius" disabled><i class="${font:far()} fa-link"></i> Add URL</button>
          <button id="reload-files-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-sync"></i> Reload</button>
          <input type="text" id="file-search" class="property-input" placeholder="Search files..." />
        </div>
      </div>
      <div id="file-list-container">
        <table id="file-table" class="file-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Version</th>
              <th>Type</th>
              <th>Size</th>
              <th>Downloads</th>
              <th>Modified</th>
            </tr>
          </thead>
          <tbody>
            <tr><td colspan="6" class="empty-state">Select a folder to view files</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Right Panel -->
    <div id="document-properties-panel">
      <div id="properties-panel-resize-handle"></div>
      <div class="panel-header">
        <h3 id="properties-panel-title">File Details</h3>
      </div>
      <div id="properties-tabs" class="tab-navigation" style="display: none;">
        <button class="tab-btn active" data-tab="details" title="Details">Details</button>
        <button class="tab-btn" data-tab="permissions" title="Permissions">Permissions</button>
        <button class="tab-btn" data-tab="versions" title="Versions">Versions</button>
      </div>

      <!-- Repository (Parent Folder) Properties -->
      <div id="repository-properties-section" style="display: none;">
        <div class="properties-section">
          <h4>Repository Properties</h4>
          <div class="form-group">
            <label for="repo-name-display">Name</label>
            <input type="text" id="repo-name-display" class="property-input" readonly />
          </div>
          <div class="form-group">
            <label for="repo-summary-display">Summary</label>
            <textarea id="repo-summary-display" class="property-input" readonly rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>
              <input type="checkbox" id="repo-enabled" /> Enabled
            </label>
          </div>
          <button class="button tiny primary" id="edit-repo-properties-btn">Edit Properties</button>
        </div>

        <div class="properties-section">
          <h4>Categories</h4>
          <div id="repo-categories-list">
            <p class="empty-state">No categories</p>
          </div>
          <button class="button tiny secondary" id="add-repo-category-btn">Add Category</button>
        </div>

        <div class="properties-section">
          <h4>Permissions</h4>
          <div id="repo-permissions-list">
            <p class="empty-state">No group permissions</p>
          </div>
          <button class="button tiny secondary" id="add-repo-permission-btn">Add Group Access</button>
        </div>
      </div>

      <div id="folder-details-tab" style="display: none;"></div>
      <div id="folder-permissions-tab" style="display: none;"></div>

      <!-- Subfolder Properties -->
      <div id="subfolder-properties-section" style="display: none;">
        <div class="properties-section">
          <h4>Folder Properties</h4>
          <div class="form-group">
            <label for="subfolder-name-display">Name</label>
            <input type="text" id="subfolder-name-display" class="property-input" readonly />
          </div>
          <div class="form-group">
            <label for="subfolder-summary-display">Summary</label>
            <textarea id="subfolder-summary-display" class="property-input" readonly rows="3"></textarea>
          </div>
          <div class="form-group">
            <label for="subfolder-start-date-display">Start Date</label>
            <input type="date" id="subfolder-start-date-display" class="property-input" readonly />
          </div>
          <button class="button tiny primary" id="edit-subfolder-properties-btn">Edit Properties</button>
        </div>
      </div>

      <!-- File Properties -->
      <div id="file-properties-section" style="display: none;">
        <div id="document-properties-content">
          <div class="empty-state">Select a file to view details</div>
        </div>
      </div>

      <div id="file-versions-tab" style="display: none;">
        <div class="versions-toolbar">
          <button id="save-version-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-save"></i> Save Version</button>
        </div>
        <div id="versions-list-container">
          <div class="empty-state">No versions available</div>
        </div>
      </div>
    </div>
  </div>
</div>

<input type="file" id="file-upload-input" style="display: none;" multiple />

<!-- New Folder (Repository) Modal -->
<div id="new-folder-modal" class="reveal" data-reveal>
  <h3>Create Repository</h3>
  <form id="new-folder-form">
    <div class="form-group">
      <label for="folder-name">Repository Name <span class="required">*</span></label>
      <input type="text" id="folder-name" required placeholder="Enter repository name" />
    </div>

    <div class="form-group">
      <label for="folder-summary">Summary</label>
      <textarea id="folder-summary" placeholder="Enter optional summary" rows="3"></textarea>
    </div>

    <fieldset>
      <legend>Guest Access</legend>
      <label>
        <input type="checkbox" id="folder-guest-public" /> Allow Guests to View (Public)
      </label>
    </fieldset>

    <div class="form-group">
      <label for="folder-user-privacy">Logged-In Users Access <span class="required">*</span></label>
      <select id="folder-user-privacy" required>
        <option value="">-- Select Access Level --</option>
        <option value="public">All Files</option>
        <option value="private">Own Files Only</option>
        <option value="protected">No Files - Drop Box Only</option>
      </select>
    </div>

    <div class="button-group">
      <button type="button" id="save-folder-btn" class="button primary">Create Repository</button>
      <button type="button" class="button secondary" data-close>Cancel</button>
    </div>
  </form>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<!-- New Subfolder Modal -->
<div id="new-subfolder-modal" class="reveal" data-reveal>
  <h3>Create Folder</h3>
  <form id="new-subfolder-form">
    <div class="form-group">
      <label for="subfolder-name">Folder Name <span class="required">*</span></label>
      <input type="text" id="subfolder-name" required placeholder="Enter folder name" />
    </div>

    <div class="form-group">
      <label for="subfolder-summary">Summary <span class="required">*</span></label>
      <textarea id="subfolder-summary" required placeholder="Enter folder summary" rows="3"></textarea>
    </div>

    <div class="form-group">
      <label for="subfolder-start-date">Start Date (Optional)</label>
      <input type="date" id="subfolder-start-date" />
    </div>

    <div class="button-group">
      <button type="button" id="save-subfolder-btn" class="button primary">Create Folder</button>
      <button type="button" class="button secondary" data-close>Cancel</button>
    </div>
  </form>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<!-- Folder Group Modal -->
<div id="folder-group-modal" class="reveal" data-reveal>
  <h3 id="folder-group-modal-title">Add Group Access</h3>
  <form id="folder-group-form">
    <div class="form-group">
      <label for="folder-group-select">Group</label>
      <select id="folder-group-select" required>
        <option value="">-- Select Group --</option>
      </select>
    </div>
    
    <div class="form-group">
      <label for="folder-group-privacy-type">Privacy Type</label>
      <select id="folder-group-privacy-type">
        <option value="0">Undefined</option>
        <option value="1">Public</option>
        <option value="2">Public Read Only</option>
        <option value="3">Protected</option>
        <option value="4">Private</option>
      </select>
    </div>

    <fieldset>
      <legend>Permissions</legend>
      <label>
        <input type="checkbox" id="folder-group-add-permission" /> Can Add Files
      </label>
      <label>
        <input type="checkbox" id="folder-group-edit-permission" /> Can Edit Files
      </label>
      <label>
        <input type="checkbox" id="folder-group-delete-permission" /> Can Delete Files
      </label>
    </fieldset>

    <div class="button-group">
      <button type="button" id="save-folder-group-btn" class="button primary">Save</button>
      <button type="button" class="button secondary" data-close>Cancel</button>
    </div>
  </form>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<div id="upload-modal" class="reveal" data-reveal>
  <h3>Upload Files</h3>
  <div id="upload-progress" style="display: none;">
    <progress id="upload-progress-bar" max="100" value="0"></progress>
    <p id="upload-status">Uploading...</p>
  </div>
  <div id="upload-success" style="display: none;">
    <p><i class="fas fa-check-circle" style="color: green;"></i> Files uploaded successfully!</p>
  </div>
  <div id="upload-error" style="display: none;">
    <p style="color: red;"><i class="fas fa-exclamation-circle"></i> <span id="upload-error-message"></span></p>
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
    <button id="discard-changes-btn" class="button alert">Discard</button>
    <button class="button secondary" data-close>Cancel</button>
  </div>
  <button class="close-button" data-close aria-label="Close modal" type="button">
    <span aria-hidden="true">&times;</span>
  </button>
</div>

<script>
  const documentEditorConfig = {
    token: '<c:out value="${userSession.formToken}" />',
    apiBaseUrl: '${ctx}/json',
    contextPath: '${ctx}',
    returnPage: '<c:out value="${returnPage}" />',
    selectedFolderId: <c:out value="${selectedFolderId}" default="-1" />,
    selectedFileId: <c:out value="${selectedFileId}" default="-1" />,
    userId: <c:out value="${userSession.userId}" default="-1" />
  };

  document.addEventListener('DOMContentLoaded', function() {
    setupAppsMenu();

    const savedTheme = localStorage.getItem('editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.dataset.theme = 'dark';
      const darkModeIcon = document.querySelector('#dark-mode-toggle-menu i');
      if (darkModeIcon) {
        darkModeIcon.classList.replace('fa-moon', 'fa-sun');
      }
    }
    const documentEditor = new DocumentEditor(documentEditorConfig);
    documentEditor.init();
    
    // Properties Panel Resizing
    const propertiesPanelElement = document.getElementById('document-properties-panel');
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
        localStorage.setItem('document-properties-panel-width', propertiesPanelElement.style.width);
      }
    });
    
    // Restore saved width
    const savedWidth = localStorage.getItem('document-properties-panel-width');
    if (savedWidth) {
      propertiesPanelElement.style.width = savedWidth;
    }
  });
</script>
