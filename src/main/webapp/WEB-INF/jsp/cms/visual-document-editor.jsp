<%--
  ~ Visual Document Editor
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
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-document-editor.css" />
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

  <!-- Title Bar -->
  <div id="editor-titlebar">
    <div class="titlebar-left">
      <img src="${ctx}/images/favicon.png" alt="Logo" />
      <h2>Document Editor</h2>
    </div>
    <div class="titlebar-right">
      <div class="button-group round">
        <a href="${ctx}/admin/visual-page-editor" class="button tiny confirm-exit">Pages</a>
        <a href="${ctx}/admin/visual-image-editor" class="button tiny confirm-exit">Images</a>
        <a href="${ctx}/admin/visual-document-editor" class="button tiny confirm-exit active">Documents</a>
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
    <div class="toolbar-section left">
      <button id="new-folder-btn" class="button tiny info no-gap radius"><i class="${font:far()} fa-folder-plus"></i> New Repository</button>
      <button id="import-doc-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-upload"></i> Upload Files</button>
      <button id="new-url-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-link"></i> Add URL</button>
    </div>
    <div class="toolbar-section center">
      <button id="reload-files-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-sync"></i> Reload</button>
      <button id="save-version-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-save"></i> Save Version</button>
      <span id="unsaved-indicator"><i class="${font:far()} fa-exclamation-triangle"></i> Unsaved changes</span>
    </div>
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
      <button id="dark-mode-toggle" class="button tiny secondary no-gap radius" title="Toggle Dark Mode"><i class="${font:far()} fa-moon"></i></button>
    </div>
  </div>

  <div id="visual-document-editor-container">
    <!-- Left Panel -->
    <div id="document-library-panel">
      <div class="panel-header">
        <h3>Repositories</h3>
        <input type="text" id="document-search" placeholder="Search repositories..." />
      </div>
      <div id="folder-list-container">
        <div class="empty-state">Loading...</div>
      </div>
    </div>

    <!-- Middle Panel -->
    <div id="document-browser-panel">
      <div class="panel-header">
        <h3 id="document-browser-title">Files</h3>
        <div id="file-toolbar">
          <button id="view-toggle-btn" class="button tiny secondary no-gap radius" title="Toggle view"><i class="fas fa-th-list"></i></button>
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
      <div id="file-preview-container" style="display: none;">
        <div class="file-preview-header">
          <button id="close-preview-btn" class="button tiny secondary no-gap radius"><i class="fas fa-times"></i> Close Preview</button>
        </div>
        <div id="file-preview-content">
          <div class="empty-state">Select a file to preview</div>
        </div>
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
      </div>
      <div id="document-properties-content">
        <div class="empty-state">Select a folder or file to view details</div>
      </div>
      <div id="folder-details-tab" style="display: none;"></div>
      <div id="folder-permissions-tab" style="display: none;"></div>
    </div>
  </div>
</div>

<input type="file" id="file-upload-input" style="display: none;" multiple />

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
    const savedTheme = localStorage.getItem('editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.dataset.theme = 'dark';
      const darkModeIcon = document.querySelector('#dark-mode-toggle i');
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
