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
<web:script package="jquery" file="jquery.min.js" />
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-hierarchy-editor.css" />
</g:compress>
<div id="visual-hierarchy-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="Hierarchy Editor" />
      <c:set var="activeApp" value="hierarchy" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Right Section -->
    <div class="toolbar-section right">
      <div id="loading-indicator" style="display: none;">
        <i class="${font:far()} fa-spinner fa-spin"></i>
      </div>
    </div>

    <div class="titlebar-right">
      <!-- Right Panel Toggle Button -->
      <button id="toggle-right-panel-btn" class="toggle-panel-btn active" title="Toggle Preview Panel">
        <i class="${font:far()} fa-square-caret-right"></i>
      </button>
      
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
  
  <!-- Main Editor Container -->
  <div id="visual-hierarchy-editor-container">

    <!-- Left Panel: Tabs -->
    <div id="left-panel">
      <div class="left-panel-tabs-container">
        <div id="middle-panel-header" class="panel-header">
          <h3 id="middle-panel-title"><i id="middle-panel-icon" class="fas fa-folder-tree"></i> <span id="middle-panel-title-text">Hierarchy</span></h3>
        </div>
        <div id="pages-tab" class="tab-content active">
          <div style="margin-bottom: 15px;">
            <div class="search-input-wrapper">
              <input type="text" id="pages-search" class="property-input no-gap" placeholder="Search pages..." style="width: 100%; padding: 10px; font-size: 14px;" />
              <button type="button" class="search-reset-btn" title="Clear search" style="display:none;"><i class="${font:far()} fa-times-circle"></i></button>
            </div>
            <div id="pages-search-indicator" style="margin-top: 6px; font-size: 12px; color: var(--editor-text-muted); display: none;"></div>
          </div>
          <div class="hierarchy-toolbar-row">
            <button type="button" id="page-tree-expand-toggle" class="button tiny secondary no-gap">
              Collapse All
            </button>
          </div>
          <div id="pages-error" style="display: none;"></div>
          <div id="pages-empty" style="display: none;">
            <p>No pages available</p>
          </div>
          <ul id="page-tree">
            <!-- Page tree will be dynamically inserted here -->
          </ul>
        </div>
      </div>
    </div>
    
    <!-- Middle Panel: Context-sensitive editor -->
    <div id="middle-panel">
      <div class="panel-header">
        <h3 id="middle-panel-title"><i id="middle-panel-icon" class="fas fa-folder-open"></i> <span id="middle-panel-title-text">Page Library</span></h3>
      </div>
      <div id="page-library-explorer" style="display:none; flex-direction: column;">
        <div style="padding: 15px 20px; background: var(--editor-panel-bg); border-bottom: 1px solid var(--editor-border); flex: 0 0 auto;">
          <div class="hierarchy-toolbar-row">
            <fieldset class="button-group hierarchy-filter-toggle" id="page-library-filter-toggle" aria-label="Page library filter">
              <button type="button" id="page-library-filter-available" class="button tiny secondary no-gap active" data-filter="available">Available Pages</button>
              <button type="button" id="page-library-filter-all" class="button tiny secondary no-gap" data-filter="all">All Pages</button>
            </fieldset>
          </div>
          <div class="search-input-wrapper">
            <input type="text" id="page-library-search" class="property-input no-gap" placeholder="Search pages to add to the hierarchy..." style="width: 100%; padding: 10px; font-size: 14px;" />
            <button type="button" class="search-reset-btn" title="Clear search" style="display:none;"><i class="${font:far()} fa-times-circle"></i></button>
          </div>
        </div>
        <div id="page-library-content" style="padding: 20px; flex: 1 1 auto; overflow-y: auto;">
          <!-- Page Library will be rendered here -->
        </div>
      </div>      
    </div>
    
    <!-- Right Panel: Preview and Properties -->
    <div id="right-panel" style="display: flex;">
      <div id="right-panel-resize-handle"></div>
      
      <div class="right-panel-tabs-container">
        <ul class="tabs-nav right-panel-tabs-nav">
          <li><a href="#preview-tab" class="active" data-tab="preview">Preview</a></li>
          <li><a href="#properties-tab" data-tab="properties">Properties</a></li>
        </ul>
        
        <!-- Preview Tab Content -->
        <div id="preview-tab" class="tab-content right-panel-tab-content active" data-tab="preview">
          <div id="preview-content">
            <div id="preview-iframe-container">
              <iframe id="preview-iframe" title="Content preview"></iframe>
            </div>
          </div>
        </div>
        
        <!-- Properties Tab Content -->
        <div id="properties-tab" class="tab-content right-panel-tab-content" data-tab="properties">
          <div id="properties-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select content to view its properties</p>
          </div>
        </div>
      </div>
    </div>
    
  </div>
</div>

<!-- Hidden form for POST submissions -->
<form id="editor-form" method="post" style="display: none;">
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <input type="hidden" id="content-id" name="contentId" value=""/>
  <input type="hidden" id="content-data" name="content" value=""/>
  <input type="hidden" id="is-draft" name="isDraft" value=""/>
  <input type="hidden" name="returnPage" value="${returnPage}" />
</form>

<!-- JavaScript Modules -->
<g:compress>
  <script src="/javascript/widgets/editor/page-tree-manager.js"></script>
  <script src="/javascript/widgets/editor/page-library.js"></script>
  <script src="/javascript/widgets/editor/visual-hierarchy-editor.js"></script>
  <script src="/javascript/widgets/editor/content-editor-bridge.js"></script>
  <script src="/javascript/widgets/editor/preview-manager.js"></script>
  <script src="/javascript/widgets/editor/visual-hierarchy-editor-init.js"></script>
</g:compress>
