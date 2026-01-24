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
  <script src="/javascript/widgets/document-editor/document-library-manager.js"></script>
  <script src="/javascript/widgets/document-editor/document-file-manager.js"></script>
  <script src="/javascript/widgets/document-editor/document-properties-manager.js"></script>
  <script src="/javascript/widgets/document-editor/document-editor-main.js"></script>
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
      <button id="new-url-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-link"></i> New URL</button>
      <button id="import-doc-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-upload"></i> Import</button>
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
        <h3>Folders</h3>
        <input type="text" id="document-search" placeholder="Search folders..." />
      </div>
      <div id="folder-list-container">
        <div class="empty-state">Loading folders...</div>
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
        <h3>File Details</h3>
      </div>
      <div id="document-properties-content">
        <div class="empty-state">Select a file to view details</div>
      </div>
    </div>
  </div>
</div>

<input type="file" id="file-upload-input" style="display: none;" multiple />

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
  });
</script>
