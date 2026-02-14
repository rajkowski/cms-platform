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
<web:script package="fullcalendar" file="index.global.min.js" />
<web:script package="tinymce" file="tinymce.min.js" />
<web:script package="jquery" file="jquery.min.js" />
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-content-editor.css" />
</g:compress>
<div id="visual-content-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="Content Editor" />
      <c:set var="activeApp" value="content" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="new-page-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-file-circle-plus"></i> New Page</button>
      <button id="new-content-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-plus"></i> New Content</button>
      <button id="new-blog-post-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-blog"></i> New Blog Post</button>
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
  
  <!-- Main Editor Container -->
  <div id="visual-content-editor-container">
    
    <!-- Left Panel: Tabs -->
    <div id="left-panel">
      <div class="left-panel-tabs-container">
        <ul class="tabs-nav">
          <li><a href="#pages-tab" class="active">Pages</a></li>
          <li><a href="#content-tab">Content</a></li>
          <li><a href="#hub-tab">Hub</a></li>
        </ul>

        <div id="pages-tab" class="tab-content active">
          <div style="margin-bottom: 15px;">
            <div class="search-input-wrapper">
              <input type="text" id="pages-search" class="property-input no-gap" placeholder="Search pages..." style="width: 100%; padding: 10px; font-size: 14px;" />
              <button type="button" class="search-reset-btn" title="Clear search" style="display:none;"><i class="${font:far()} fa-times-circle"></i></button>
            </div>
            <div id="pages-search-indicator" style="margin-top: 6px; font-size: 12px; color: var(--editor-text-muted); display: none;"></div>
          </div>
          <div id="pages-error" style="display: none;"></div>
          <div id="pages-empty" style="display: none;">
            <p>No pages available</p>
          </div>
          <ul id="page-tree">
            <!-- Page tree will be dynamically inserted here -->
          </ul>
        </div>

        <div id="content-tab" class="tab-content">
          <div style="margin-bottom: 15px;">
            <div class="search-input-wrapper">
              <input type="text" id="content-search" class="property-input" placeholder="Search content..." style="width: 100%; padding: 10px; font-size: 14px;" />
              <button type="button" class="search-reset-btn" title="Clear search" style="display:none;"><i class="${font:far()} fa-times-circle"></i></button>
            </div>
          </div>
          <div style="margin-bottom: 15px; display: flex; gap: 8px; align-items: center;">
            <label style="font-size: 12px; color: var(--editor-text-muted); margin: 0; white-space: nowrap;">Sort by:</label>
            <div class="button-group" style="flex: 1;">
              <button id="content-sort-recent" class="button tiny secondary no-gap active" data-sort="recent" style="flex: 1;">Most Recent</button>
              <button id="content-sort-alphabetical" class="button tiny secondary no-gap" data-sort="alphabetical" style="flex: 1;">A-Z</button>
            </div>
          </div>
          <div id="content-error" style="display: none;"></div>
          <div id="content-empty" style="display: none;">
            <p>No content available</p>
          </div>
          <div id="content-list">
            <!-- Content list will be dynamically inserted here -->
          </div>
        </div>

        <div id="hub-tab" class="tab-content">
          <div id="hub-list-container">
            <div class="hub-item" data-hub-type="blogs">
              <div class="hub-item-icon"><i class="${font:far()} fa-blog"></i></div>
              <div class="hub-item-info">
                <div class="hub-item-title">Blogs</div>
                <div class="hub-item-desc">View blog posts on calendar</div>
              </div>
            </div>
            <div class="hub-item" data-hub-type="calendars">
              <div class="hub-item-icon"><i class="${font:far()} fa-calendar"></i></div>
              <div class="hub-item-info">
                <div class="hub-item-title">Calendars</div>
                <div class="hub-item-desc">View calendar events</div>
              </div>
            </div>
            <div class="hub-item" data-hub-type="pages">
              <div class="hub-item-icon"><i class="${font:far()} fa-file-lines"></i></div>
              <div class="hub-item-info">
                <div class="hub-item-title">Web Pages</div>
                <div class="hub-item-desc">View page publish dates</div>
              </div>
            </div>
            <div class="hub-item" data-hub-type="content">
              <div class="hub-item-icon"><i class="${font:far()} fa-edit"></i></div>
              <div class="hub-item-info">
                <div class="hub-item-title">Content</div>
                <div class="hub-item-desc">View content updates</div>
              </div>
            </div>
            <div class="hub-item" data-hub-type="datasets">
              <div class="hub-item-icon"><i class="${font:far()} fa-table"></i></div>
              <div class="hub-item-info">
                <div class="hub-item-title">Datasets</div>
                <div class="hub-item-desc">View dataset updates</div>
              </div>
            </div>
            <div class="hub-item" data-hub-type="wikis">
              <div class="hub-item-icon"><i class="${font:far()} fa-book"></i></div>
              <div class="hub-item-info">
                <div class="hub-item-title">Wikis</div>
                <div class="hub-item-desc">View wiki updates</div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
    
    <!-- Middle Panel: Context-sensitive editor -->
    <div id="middle-panel">

      <!-- Zoom Controls - only visible when Site Navigation is selected -->
      <div id="sitemap-zoom-controls" class="sitemap-zoom-controls" style="display: none;">
        <button id="zoom-in-btn" class="zoom-btn" title="Zoom In"><i class="${font:far()} fa-search-plus"></i></button>
        <button id="zoom-out-btn" class="zoom-btn" title="Zoom Out"><i class="${font:far()} fa-search-minus"></i></button>
        <button id="zoom-fit-btn" class="zoom-btn" title="Zoom to Fit"><i class="${font:far()} fa-expand"></i></button>
        <button id="zoom-actual-btn" class="zoom-btn" title="Actual Size">1:1</button>
      </div>
      
      <!-- Middle Panel Header -->
      <div id="middle-panel-header" class="panel-header">
        <h3 id="middle-panel-title"><i id="middle-panel-icon" class="far"></i> <span id="middle-panel-title-text">Visual Content Editor</span></h3>
        <div id="middle-panel-tools">
          <!-- Pages Tab Tools -->
          <div id="pages-tools" class="panel-tools" style="display:none;">
            <button id="add-menu-tab-btn" class="button tiny primary no-gap radius" style="display:none;"><i class="${font:far()} fa-plus"></i> Add Menu Tab</button>
            <button id="refresh-sitemap-btn" class="button tiny secondary no-gap radius" style="display:none;"><i class="${font:far()} fa-refresh"></i> Refresh</button>
            <button id="add-root-page-btn" class="button tiny primary no-gap radius" style="display:none;"><i class="${font:far()} fa-plus"></i> Add Page</button>
            <div class="button-group">
              <button id="view-page-library-btn" class="button tiny secondary no-gap active" data-view="library">Page Library</button>
              <button id="view-site-navigation-btn" class="button tiny secondary no-gap" data-view="navigation">Site Navigation</button>
            </div>
          </div>
          
          <!-- Content Tab Tools -->
          <div id="content-tools" class="panel-tools" style="display:none;">
            <button id="save-draft-btn" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-save"></i> Save Draft</button>
            <button id="publish-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-check"></i> Publish</button>
          </div>
          
          <!-- Hub Tab Tools -->
          <div id="hub-tools" class="panel-tools" style="display:none;">
            <label style="display: flex; align-items: center; gap: 8px; margin: 0; font-size: 14px; white-space: nowrap;">
              <input type="checkbox" id="toggle-us-holidays" style="margin: 0" />
              <span>US Holidays</span>
            </label>
          </div>
        </div>
      </div>
      
      <!-- Pages Tab Views -->
      <div id="sitemap-explorer" style="display:none;">
        <!-- Site Navigation will be rendered here -->
      </div>
      
      <div id="page-library-explorer" style="display:none; flex-direction: column;">
        <div style="padding: 15px 20px; background: var(--editor-panel-bg); border-bottom: 1px solid var(--editor-border); flex: 0 0 auto;">
          <div class="search-input-wrapper">
            <input type="text" id="page-library-search" class="property-input no-gap" placeholder="Search pages to add to navigation..." style="width: 100%; padding: 10px; font-size: 14px;" />
            <button type="button" class="search-reset-btn" title="Clear search" style="display:none;"><i class="${font:far()} fa-times-circle"></i></button>
          </div>
        </div>
        <div id="page-library-content" style="padding: 20px; flex: 1 1 auto; overflow-y: auto;">
          <!-- Page Library will be rendered here -->
        </div>
      </div>
      
      <div id="content-editor" style="display:none;">
        <textarea id="content-html-editor"></textarea>
      </div>
      
      <div id="calendar-view" style="display:none;">
        <div id="calendar-selector">
          <!-- Blog/Calendar checkboxes will be rendered here -->
        </div>
        <div id="calendar-container">
          <!-- FullCalendar will be rendered here -->
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
          <div id="page-content-blocks">
            <h6>Content Blocks</h6>
            <div id="page-content-blocks-list">
              <div class="content-blocks-status">Select a page to view its content blocks</div>
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

<!-- New Page Modal -->
<div id="new-page-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 500px;">
    <h4>New Page</h4>
    <form id="new-page-form">
      <div class="property-group">
        <label class="property-label" for="page-title">Title of Page</label>
        <input type="text" id="page-title" class="property-input" placeholder="Enter page title" required />
      </div>
      <div class="property-group">
        <label class="property-label" for="page-link">Link</label>
        <input type="text" id="page-link" class="property-input" placeholder="/page-url" required />
        <div id="page-link-error" style="color: #dc3545; font-size: 12px; margin-top: 5px; display: none;">
          Link must start with a forward slash (/)
        </div>
        <div style="font-size: 12px; color: #6c757d; margin-top: 5px;">
          The URL path for this page (e.g., /about, /contact, /products/new)
        </div>
      </div>
      <div style="text-align: right; margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end;">
        <button type="button" id="cancel-new-page" class="button tiny secondary radius">Cancel</button>
        <button type="submit" id="create-page-btn" class="button tiny success radius">Create Page</button>
      </div>
    </form>
  </div>
</div>

<!-- New Content Modal -->
<div id="new-content-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 500px;">
    <h4>New Content Block</h4>
    <form id="new-content-form">
      <div class="property-group">
        <label class="property-label" for="content-unique-id">Unique ID</label>
        <input type="text" id="content-unique-id" class="property-input" placeholder="example-content-block" required />
        <div style="font-size: 12px; color: #6c757d; margin-top: 5px;">
          A unique identifier for this content block (e.g., header-banner, footer-text)
        </div>
      </div>
      <div class="property-group">
        <label class="property-label" for="content-initial-text">Initial Content (Optional)</label>
        <textarea id="content-initial-text" class="property-input" placeholder="Enter initial content..." rows="4"></textarea>
      </div>
      <div style="text-align: right; margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end;">
        <button type="button" id="cancel-new-content" class="button tiny secondary radius">Cancel</button>
        <button type="submit" id="create-content-btn" class="button tiny success radius">Create Content</button>
      </div>
    </form>
  </div>
</div>

<!-- New Blog Post Modal -->
<div id="new-blog-post-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 600px;">
    <h4>New Blog Post</h4>
    <form id="new-blog-post-form">
      <div class="property-group">
        <label class="property-label" for="blog-selection">Blog</label>
        <select id="blog-selection" class="property-input" required>
          <option value="">Select a blog...</option>
          <!-- Blog options will be dynamically inserted here -->
        </select>
      </div>
      <div class="property-group">
        <label class="property-label" for="blog-post-title">Title</label>
        <input type="text" id="blog-post-title" class="property-input" placeholder="Enter post title" required />
      </div>
      <div class="property-group">
        <label class="property-label" for="blog-post-content">Content</label>
        <textarea id="blog-post-content" class="property-input" placeholder="Enter post content..." rows="6"></textarea>
      </div>
      <div style="text-align: right; margin-top: 20px; display: flex; gap: 10px; justify-content: flex-end;">
        <button type="button" id="cancel-new-blog-post" class="button tiny secondary radius">Cancel</button>
        <button type="submit" id="create-blog-post-btn" class="button tiny success radius">Create Post</button>
      </div>
    </form>
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

<!-- Content Block Editor Modal -->
<div id="content-block-editor-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content content-block-editor-modal-content">
    <div class="content-block-editor-header">
      <h4 id="content-block-editor-title">Edit Content Block</h4>
      <div class="content-block-editor-actions">
        <button type="button" id="content-block-save-draft" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-save"></i> Save Draft</button>
        <button type="button" id="content-block-publish" class="button tiny success no-gap radius"><i class="${font:far()} fa-check"></i> Publish</button>
        <button type="button" id="content-block-editor-close" class="button tiny secondary no-gap radius"><i class="${font:far()} fa-times"></i> Close</button>
      </div>
    </div>
    <div class="content-block-editor-body">
      <textarea id="content-block-html-editor"></textarea>
    </div>
  </div>
</div>

<!-- JavaScript Modules -->
<g:compress>
  <script src="/javascript/widgets/editor/content-list-manager.js"></script>
  <script src="/javascript/widgets/editor/page-tree-manager.js"></script>
  <script src="/javascript/widgets/editor/page-library.js"></script>
  <script src="/javascript/widgets/editor/page-content-blocks.js"></script>
  <script src="/javascript/widgets/editor/sitemap-explorer.js"></script>
  <script src="/javascript/widgets/editor/hub-content-manager.js"></script>
  <script src="/javascript/widgets/editor/content-editor-bridge.js"></script>
  <script src="/javascript/widgets/editor/preview-manager.js"></script>
  <script src="/javascript/widgets/editor/visual-content-editor-init.js"></script>
</g:compress>
