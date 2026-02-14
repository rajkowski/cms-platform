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
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="webPage" class="com.simisinc.platform.domain.model.cms.WebPage" scope="request"/>
<web:stylesheet package="spectrum" file="spectrum.css" />
<web:stylesheet package="dragula" file="dragula.min.css" />
<web:script package="spectrum" file="spectrum.js" />
<web:script package="tinymce" file="tinymce.min.js" />
<web:script package="dragula" file="dragula.min.js" />
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/visual-page-editor.css" />
</g:compress>
<g:compress>
  <link rel="stylesheet" type="text/css" href="${ctx}/css/platform/preview-hover.css" />
</g:compress>
<g:compress>
  <script src="/javascript/widgets/editor/element-detector.js"></script>
  <script src="/javascript/widgets/editor/hover-overlay.js"></script>
  <script src="/javascript/widgets/editor/property-editor-bridge.js"></script>
  <script src="/javascript/widgets/editor/preview-hover-manager.js"></script>
</g:compress>
<div id="visual-page-editor-wrapper">
  <c:if test="${!empty title}">
    <h4><c:if test="${!empty icon}"><i class="${font:far()} ${icon}"></i> </c:if><c:out value="${title}"/></h4>
  </c:if>
  <%@include file="../page_messages.jspf" %>

  <!-- Toolbar -->
  <div id="editor-toolbar">
    <div class="titlebar-left">
      <a href="${ctx}/"><img src="${ctx}/images/favicon.png" alt="Logo" /></a>
      <c:set var="editorName" value="Web Page Editor" />
      <c:set var="activeApp" value="pages" />
      <%@include file="editor-app-switcher.jspf" %>
    </div>

    <!-- Left Section -->
    <div class="toolbar-section left">
      <button id="add-page-btn" class="button tiny success no-gap radius"><i class="${font:far()} fa-file-circle-plus"></i> Add a Page</button>
      <button id="add-row-btn" class="button tiny primary no-gap radius"><i class="${font:far()} fa-plus"></i> Add Row</button>
      <button id="undo-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-undo"></i> Undo</button>
      <button id="redo-btn" class="button tiny secondary no-gap radius" disabled><i class="${font:far()} fa-redo"></i> Redo</button>
    </div>

    <!-- Center Section -->
    <div class="toolbar-section center">
      <div id="preview-state-group" class="button-group">
        <button type="button" class="button tiny secondary no-gap left" data-preview-state="preview" title="Preview" aria-label="Preview">
          <i class="${font:far()} fa-eye"></i>
        </button>
        <button type="button" class="button tiny secondary no-gap" data-preview-state="layout" title="Layout" aria-label="Layout">
          <i class="${font:far()} fa-table-columns"></i>
        </button>
        <button type="button" class="button tiny secondary no-gap right" data-preview-state="preview-layout" title="Preview + Layout" aria-label="Preview + Layout">
          <i class="${font:far()} fa-eye"></i>
          <i class="${font:far()} fa-table-columns"></i>
        </button>
      </div>
    </div>

    <div class="toolbar-section next">
      <button id="save-btn" class="button tiny no-gap radius"><i class="${font:far()} fa-save"></i> Publish</button>
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
          <!-- Actions Section -->
          <div class="apps-menu-section">
            <a href="#" id="dark-mode-toggle-menu" class="apps-menu-item">
              <i class="${font:far()} fa-moon"></i>
              <span>Dark Mode</span>
            </a>
            <c:if test="${userSession.hasRole('admin')}">
              <a href="#" id="static-site-generator-btn" class="apps-menu-item">
                <i class="${font:far()} fa-globe"></i>
                <span>Static Site Generator...</span>
              </a>
            </c:if>
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
              <c:when test="${!empty webPage.link}">
                <a href="${ctx}${webPage.link}" class="apps-menu-item confirm-exit">
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
  <div id="visual-page-editor-container" class="preview-only">
    
    <!-- Widget Palette -->
    <div id="widget-palette">
      <div class="palette-tabs-container">
        <ul class="tabs-nav">
          <li><a href="#pages-tab" class="active">Pages</a></li>
          <li><a href="#layouts-tab">Layouts</a></li>
          <li><a href="#widgets-tab">Widgets</a></li>
        </ul>

        <div id="widgets-tab" class="tab-content">
          <div style="margin-bottom: 15px;">
            <input type="text" id="widget-search" class="property-input" placeholder="Search widgets..." style="width: 100%; padding: 10px; font-size: 14px;" />
          </div>
          <div id="widget-list-container">
            <!-- Widgets will be dynamically inserted here -->
          </div>
        </div>

        <div id="layouts-tab" class="tab-content">
          <!-- Templates Button -->
          <div style="padding: 10px; border-bottom: 1px solid var(--editor-border);">
            <button id="pre-designed-page-btn-layouts" class="button expanded secondary radius" style="width: 100%; margin: 0;">
              <i class="${font:far()} fa-magic"></i> Choose a Template
            </button>
          </div>
          
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
          <div style="margin-bottom: 15px;">
            <input type="text" id="pages-search" class="property-input" placeholder="Search pages..." style="width: 100%; padding: 10px; font-size: 14px;" />
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
      <div id="preview-error" style="display: none;"></div>
      <div style="height: 100%; width: 100%;<c:if test="${!empty themePropertyMap['theme.body.backgroundColor']}">background-color:<c:out value="${themePropertyMap['theme.body.backgroundColor']}" /></c:if>">
        <iframe id="preview-iframe" title="Page preview" style="<c:if test="${!empty themePropertyMap['theme.body.backgroundColor']}">background-color:<c:out value="${themePropertyMap['theme.body.backgroundColor']}" /></c:if>"></iframe>
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
    
    <!-- Right Panel with Tabs -->
    <div id="properties-panel">
      <div id="properties-panel-resize-handle"></div>
      
      <!-- Right Panel Tabs -->
      <div class="right-panel-tabs-container">
        <ul class="tabs-nav right-panel-tabs-nav">
          <li><a href="#info-tab" class="active" data-tab="info">Page Info</a></li>
          <li><a href="#properties-tab" data-tab="properties">Properties</a></li>
          <li><a href="#css-tab" data-tab="css">CSS</a></li>
          <li><a href="#xml-tab" data-tab="xml">XML</a></li>
        </ul>
        
        <!-- Info Tab Content -->
        <div id="info-tab" class="tab-content right-panel-tab-content active" data-tab="info">
          <div id="info-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select a page to view its information</p>
          </div>
        </div>
        
        <!-- Properties Tab Content -->
        <div id="properties-tab" class="tab-content right-panel-tab-content" data-tab="properties">
          <div id="properties-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">Select an element to edit its properties</p>
          </div>
        </div>
        
        <!-- CSS Tab Content -->
        <div id="css-tab" class="tab-content right-panel-tab-content" data-tab="css">
          <div id="css-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">CSS editor will be loaded here</p>
          </div>
        </div>
        
        <!-- XML Tab Content -->
        <div id="xml-tab" class="tab-content right-panel-tab-content" data-tab="xml">
          <div id="xml-tab-content">
            <p style="color: var(--editor-text-muted); font-size: 14px;">XML editor will be loaded here</p>
          </div>
        </div>
      </div>
    </div>
    
  </div>
</div>

<!-- Add Page Modal -->
<div id="add-page-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 500px;">
    <h4>New Page</h4>
    <form id="add-page-form">
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
        <button type="button" id="cancel-add-page" class="button tiny secondary radius">Cancel</button>
        <button type="submit" id="create-page-btn" class="button tiny success radius">Create Page</button>
      </div>
    </form>
  </div>
</div>

<%@include file="static-site-modal.jsp" %>

<!-- Pre-Designed Page Modal -->
<div id="pre-designed-page-modal" class="modal-overlay" style="display:none;">
  <div class="modal-content" style="max-width: 800px;">
    <h4 style="color:var(--editor-text);margin: 0 0 15px 0; flex-shrink: 0;">Choose a Template</h4>
    <div id="icon-grid" style="flex: 1; overflow-y: auto; border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #f9f9f9; min-height: 200px;">
      <ul id="pre-designed-page-list">
        <!-- Templates items will be rendered here -->
      </ul>
    </div>
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
    <div style="text-align: right; margin-top: 15px; display: flex; gap: 10px; justify-content: flex-end; flex-shrink: 0;">
      <button id="close-pre-designed-page-modal" class="button tiny secondary radius">Cancel</button>

    <!-- Initialize Preview Hover integration -->
    <script>
      (function () {
        function initPreviewHover() {
          try {
            // Prefer preview iframe's document/body as the hover context
            var iframe = document.getElementById('preview-iframe');
            var initWithIframe = function(ifr) {
              if (!ifr || !ifr.contentDocument) { return null; }
              var iframeDoc = ifr.contentDocument;
              var container = iframeDoc.body || iframeDoc.documentElement;
              return { doc: iframeDoc, win: ifr.contentWindow, container: container };
            };

            var ctx = initWithIframe(iframe);
            var previewContainer = (ctx && ctx.container) ? ctx.container : (document.getElementById('editor-canvas') || document.querySelector('#visual-page-editor-wrapper'));
            if (!previewContainer) { return; }

            var propertyApi = (window.PropertyEditorAPI || window.EditorPropertyAPI || null);
            
            // Only create manager if PreviewHoverManager class is available
            var manager = null;
            if (typeof window.PreviewHoverManager === 'function') {
              manager = new window.PreviewHoverManager(previewContainer, propertyApi);
            }

            // Expose for debugging
            window.__previewHoverManager = manager;

            // Set up iframe postMessage proxy for reliable event capture
            function setupIframeEventProxy() {
              if (!iframe || !iframe.contentWindow || !ctx || !ctx.doc) {
                console.warn('Preview hover: iframe proxy setup skipped - missing context');
                return;
              }
              
              // Inject event proxy script into iframe
              var proxyScript = ctx.doc.createElement('script');
              proxyScript.textContent = '(function() {' +
                'var throttle = 16;' +
                'var lastTime = 0;' +
                'var eventCount = 0;' +
                'function forwardEvent(e) {' +
                '  var now = Date.now();' +
                '  if (now - lastTime < throttle) return;' +
                '  lastTime = now;' +
                '  eventCount++;' +
                '  if (eventCount <= 3) {' +
                '    console.log("Iframe proxy: forwarding event", e.type, e.clientX, e.clientY);' +
                '  }' +
                '  if (window.parent && e.clientX != null && e.clientY != null) {' +
                '    try {' +
                '      window.parent.postMessage({' +
                '      type: "preview-hover-event",' +
                '      eventType: e.type,' +
                '      clientX: e.clientX,' +
                '      clientY: e.clientY,' +
                '      target: e.target ? (e.target.tagName || "unknown") : "unknown"' +
                '      }, "*");' +
                '    } catch (err) {' +
                '      console.error("Iframe proxy: postMessage failed", err);' +
                '    }' +
                '  }' +
                '}' +
                'document.addEventListener("mousemove", forwardEvent, true);' +
                'document.addEventListener("pointermove", forwardEvent, true);' +
                'console.log("Preview hover: iframe event proxy active");' +
              '})();';
              
              try {
                (ctx.doc.body || ctx.doc.documentElement).appendChild(proxyScript);
                console.log('Preview hover: iframe event proxy injected');
              } catch (e) {
                console.warn('Preview hover: failed to inject iframe proxy', e);
              }
              
              // Parent listener for postMessage events
              var messageCount = 0;
              window.addEventListener('message', function(e) {
                if (!e.data || e.data.type !== 'preview-hover-event') { return; }
                
                messageCount++;
                if (messageCount <= 3) {
                  console.log('Parent received hover event:', e.data.clientX, e.data.clientY);
                }
                
                // Forward to element detector if manager is active
                if (manager && manager.elementDetector && manager.isEnabled) {
                  var detector = manager.elementDetector;
                  if (detector && typeof detector.detectElementAtPoint === 'function') {
                    var elementInfo = detector.detectElementAtPoint(e.data.clientX, e.data.clientY);
                    if (elementInfo && detector.onElementDetected) {
                      detector.onElementDetected(elementInfo);
                    } else if (!elementInfo && detector.onElementLost) {
                      detector.onElementLost();
                    }
                  }
                } else {
                  if (messageCount === 1) {
                    console.warn('Preview hover: manager not active', {
                      hasManager: !!manager,
                      hasDetector: !!(manager && manager.elementDetector),
                      isEnabled: !!(manager && manager.isEnabled)
                    });
                  }
                }
              });
            }

            // Helper: determine current preview mode from wrapper or toggle state
            function isPreviewMode() {
              var toggle = document.getElementById('preview-state-group');
              if (toggle && toggle.dataset && toggle.dataset.previewState) {
                return toggle.dataset.previewState !== 'layout';
              }
              var wrapper = document.getElementById('visual-page-editor-wrapper');
              if (!wrapper) return false;
              var modeAttr = wrapper.getAttribute('data-mode');
              if (modeAttr) return modeAttr === 'preview' || modeAttr === 'preview-layout';
              return wrapper.classList.contains('preview') || wrapper.classList.contains('preview-mode');
            }

            // Initial state (ensure iframe-ready)
            if (iframe && iframe.contentDocument && iframe.contentDocument.readyState !== 'complete') {
              iframe.addEventListener('load', function(){
                setupIframeEventProxy();
                if (manager && typeof manager.handlePreviewModeChange === 'function') {
                  manager.handlePreviewModeChange(isPreviewMode());
                }
              });
            } else {
              setupIframeEventProxy();
              if (manager && typeof manager.handlePreviewModeChange === 'function') {
                manager.handlePreviewModeChange(isPreviewMode());
              }
            }

            // Listen for existing editor mode-change events if available
            document.addEventListener('cms:editor:mode-change', function (e) {
              var mode = e && e.detail && e.detail.mode ? e.detail.mode : null;
              if (manager && typeof manager.handlePreviewModeChange === 'function') {
                manager.handlePreviewModeChange(mode === 'preview');
              }
            });

            // Fallback: observe class/attribute changes on wrapper
            var wrapper = document.getElementById('visual-page-editor-wrapper');
            if (wrapper && window.MutationObserver) {
              var observer = new MutationObserver(function () {
                if (manager && typeof manager.handlePreviewModeChange === 'function') {
                  manager.handlePreviewModeChange(isPreviewMode());
                }
              });
              observer.observe(wrapper, { attributes: true, attributeFilter: ['class', 'data-mode'] });
            }

            // Clean up on unload
            window.addEventListener('beforeunload', function () {
              try { manager.disable(); } catch (e) {}
            });
          } catch (err) {
            if (window.console && console.error) {
              console.error('Preview Hover init failed:', err);
            }
          }
        }

        if (document.readyState === 'loading') {
          document.addEventListener('DOMContentLoaded', initPreviewHover);
        } else {
          initPreviewHover();
        }
      })();
    </script>
    </div>
  </div>
</div>

<!-- Hidden form for submission -->
<form id="editor-form" method="post" style="display: none;">
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <input type="hidden" name="webPage" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="webPageLink" value="<c:out value="${webPage.link}" />"/>
  <input type="hidden" name="returnPage" value="${returnPage}" />
  <input type="hidden" id="designer-data" name="designerData" value=""/>
</form>

<!-- Store existing XML safely for JS -->
<script id="existing-xml-data" type="text/plain"><c:out value="${webPage.pageXml}" escapeXml="true"/></script>

<!-- Load JavaScript modules -->
<web:script package="ace" file="ace.js" charset="utf-8" />
<web:script package="ace" file="mode-css.js" charset="utf-8" />
<web:script package="ace" file="mode-xml.js" charset="utf-8" />
<web:script package="ace" file="theme-chrome.js" charset="utf-8" />
<web:script package="ace" file="theme-monokai.js" charset="utf-8" />
<g:compress>
  <script src="${ctx}/javascript/apps-menu-controller.js"></script>
</g:compress>
<g:compress>
  <script src="${ctx}/javascript/icon-picker-modal.js"></script>
  <script src="${ctx}/javascript/page-link-picker-modal.js"></script>
  <script src="${ctx}/javascript/widgets/editor/widget-registry.js"></script>
  <script src="${ctx}/javascript/widgets/editor/pre-designed-templates.js"></script>
  <script src="${ctx}/javascript/widgets/editor/viewport-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/editor-main.js"></script>
  <script src="${ctx}/javascript/widgets/editor/drag-drop-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/layout-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/canvas-controller.js"></script>
  <script src="${ctx}/javascript/widgets/editor/properties-panel.js"></script>
  <script src="${ctx}/javascript/widgets/editor/pages-tab-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/right-panel-tabs.js"></script>
  <script src="${ctx}/javascript/widgets/editor/info-tab-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/css-tab-manager.js"></script>
  <script src="${ctx}/javascript/widgets/editor/xml-tab-manager.js"></script>
  <script src="${ctx}/javascript/static-site-manager.js"></script>
</g:compress>

<script>
  // Initialize the editor
  // Declare propertiesPanel at module scope so it's accessible throughout
  let propertiesPanel;
  
  document.addEventListener('DOMContentLoaded', function() {
    setupAppsMenu();
    setupEditorAppSwitcher();
    
    const editorConfig = {
      token: '<c:out value="${userSession.formToken}" />',
      webPageLink: '<c:out value="${webPage.link}" />',
      webPageId: <c:out value="${webPage.id}" default="-1"/>,
      existingXml: document.getElementById('existing-xml-data').textContent
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&#034;/g, '"')
        .replace(/&amp;/g, '&'),
      hasExistingLayout: <c:out value="${hasExistingLayout ? 'true' : 'false'}" default="false"/>
    };
    
    window.pageEditor = new PageEditor(editorConfig);
    
    // Initialize PreviewHoverManager for hover functionality in preview mode
    // const previewContainer = document.getElementById('preview-container');
    // const previewIframe = document.getElementById('preview-iframe');
    
    // Set up preview update listener BEFORE initializing the page editor
    // Default to 'layout' for new pages, 'preview' for existing pages
    const isNewPage = editorConfig.webPageId === -1 || editorConfig.webPageId === 0;
    let previewState = isNewPage ? 'layout' : 'preview';
    const previewStates = ['preview', 'preview-layout', 'layout'];
    const previewStateGroup = document.getElementById('preview-state-group');
    const previewStateButtons = previewStateGroup ? previewStateGroup.querySelectorAll('[data-preview-state]') : [];
    const previewWrapper = document.getElementById('visual-page-editor-wrapper');
    const editorContainer = document.getElementById('visual-page-editor-container');
    const previewContainer = document.getElementById('preview-container');
    const editorCanvas = document.getElementById('editor-canvas');
    const previewIframe = document.getElementById('preview-iframe');
    const previewLoading = document.getElementById('preview-loading');
    const previewError = document.getElementById('preview-error');

    // Guard: cleanup any lingering Dragula artifacts that may affect scrolling
    function cleanupDragulaArtifacts() {
      try {
        // Ensure iframe allows pointer interactions
        if (previewIframe) {
          previewIframe.style.pointerEvents = 'auto';
        }
        // Restore scrolling behavior in preview container
        if (previewContainer) {
          previewContainer.style.touchAction = 'auto';
          previewContainer.style.overscrollBehavior = 'auto';
        }
        // Remove global dragula class if it lingers
        if (document && document.body && document.body.classList) {
          document.body.classList.remove('gu-unselectable');
        }
        // Remove any orphaned mirror nodes
        Array.prototype.slice.call(document.querySelectorAll('.gu-mirror')).forEach(function(node){
          if (node && node.parentNode && typeof node.remove === 'function') { node.remove(); }
          else if (node && node.parentNode) { node.parentNode.removeChild(node); }
        });
      } catch (e) {
        console.debug('Preview cleanupDragulaArtifacts error:', e);
      }
    }

    function isPreviewEnabled() {
      return previewState !== 'layout';
    }

    function applyPreviewState(state, { refresh = false } = {}) {
      previewState = state;

      if (previewStateGroup) {
        previewStateGroup.dataset.previewState = state;
      }

      if (previewStateButtons && previewStateButtons.length) {
        previewStateButtons.forEach(button => {
          const buttonState = button.dataset.previewState;
          const isActive = buttonState === state;
          button.classList.toggle('active', isActive);
          button.setAttribute('aria-pressed', String(isActive));
        });
      }

      if (previewWrapper) {
        previewWrapper.setAttribute('data-mode', state);
      }

      if (editorContainer) {
        editorContainer.classList.remove('preview-only', 'preview-layout', 'layout-only');
        if (state === 'preview') {
          editorContainer.classList.add('preview-only');
        } else if (state === 'preview-layout') {
          editorContainer.classList.add('preview-layout');
        } else {
          editorContainer.classList.add('layout-only');
        }
      }

      if (isPreviewEnabled()) {
        previewContainer.classList.add('active');
        previewIframe.classList.add('active');
        if (window.previewHoverManager && typeof window.previewHoverManager.handlePreviewModeChange === 'function') {
          window.previewHoverManager.handlePreviewModeChange(true);
        }
        if (refresh) {
          refreshPreview();
        }
      } else {
        previewContainer.classList.remove('active');
        previewIframe.classList.remove('active');
        // Clear any locked selections in the preview hover system when disabling preview
        if (window.previewHoverManager && typeof window.previewHoverManager.clearLockedSelection === 'function') {
          window.previewHoverManager.clearLockedSelection();
        }
        if (window.previewHoverManager && typeof window.previewHoverManager.handlePreviewModeChange === 'function') {
          window.previewHoverManager.handlePreviewModeChange(false);
        }
      }
    }

    // Get the actual PropertiesPanel instance for use by PreviewHoverManager
    propertiesPanel = window.pageEditor.getPropertiesPanel();
    
    // Initialize PreviewHoverManager with the preview iframe as the container
    // The iframe content will be the actual preview container where hover detection occurs
    if (typeof PreviewHoverManager === 'function') {
      window.previewHoverManager = new PreviewHoverManager(previewIframe, propertiesPanel);
    }
    
    // General function to open a page in the preview iframe
    window.openPageInIframe = function(url, loadingMessage = 'Loading...') {
      // Show loading indicator
      if (window.pageEditor) {
        window.pageEditor.showLoadingIndicator(loadingMessage);
      }
      
      // Reset the properties panel
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      
      // Clear any locked selections and hide selection boxes
      if (window.previewHoverManager) {
        if (typeof window.previewHoverManager.clearLockedSelection === 'function') {
          window.previewHoverManager.clearLockedSelection();
        }
        // Disable hover manager to hide all selection boxes
        if (typeof window.previewHoverManager.disable === 'function') {
          window.previewHoverManager.disable();
        }
      }
      
      // Set iframe source
      previewIframe.src = url;
      
      // Handle iframe load
      previewIframe.onload = function() {
        // Hide loading indicator when iframe loads
        if (window.pageEditor) {
          window.pageEditor.hideLoadingIndicator();
        }
      };
      
      // Switch to preview mode
      applyPreviewState('preview');
      // Ensure scrolling remains functional after entering preview
      cleanupDragulaArtifacts();
    };
    
    // Initialize preview mode based on page type
    function initializePreviewMode() {
      applyPreviewState(previewState, { refresh: isPreviewEnabled() });
    }

    // Function to refresh the preview
    function refreshPreview() {
      if (!isPreviewEnabled()) {
        return;
      }
      previewIframe.style.display = 'none';
      previewError.style.display = 'none';
      
      // Show loading indicator in toolbar
      if (window.pageEditor) {
        window.pageEditor.showLoadingIndicator('Loading preview...');
      }

      // Get the current editor data as XML
      const layoutManager = window.pageEditor.getLayoutManager();
      const designerData = layoutManager.toXML(forPreview=true);

      // Get the selected page link from the pages tab manager
      const webPageLink = window.pageEditor.pagesTabManager.getSelectedPageLink();
      
      // Get current viewport for preview sizing
      const currentViewport = window.pageEditor.getViewportManager().getCurrentViewport();
      
      // Send to server for rendering
      const formData = new FormData();
      // formData.append('widget', '<c:out value="${widgetContext.uniqueId}" />');
      formData.append('token', '<c:out value="${userSession.formToken}" />');
      formData.append('webPageLink', webPageLink);
      formData.append('designerData', designerData);
      formData.append('containerPreview', 'true');
      formData.append('viewport', currentViewport); // Add viewport info
      
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
        // Hide loading indicator
        if (window.pageEditor) {
          window.pageEditor.hideLoadingIndicator();
        }
        // Write the complete HTML response to the iframe
        const iframeDoc = previewIframe.contentDocument || previewIframe.contentWindow.document;
        iframeDoc.open();
        iframeDoc.write(html);
        iframeDoc.close();

        // Apply viewport styles to iframe after content loads
        previewIframe.onload = function() {
          if (window.pageEditor && window.pageEditor.getViewportManager()) {
            window.pageEditor.getViewportManager().applyPreviewViewportStyles();
          }

          // Re-apply layout selection after preview refresh (keeps selection persistent)
          if (window.pageEditor && window.pageEditor.getCanvasController && typeof window.pageEditor.getCanvasController === 'function') {
            const canvasController = window.pageEditor.getCanvasController();
            if (canvasController && typeof canvasController.restoreSelection === 'function') {
              canvasController.restoreSelection({ updateProperties: false, syncPreview: true });
            }
          }
          
          // Re-initialize hover manager for the new iframe content
          if (window.previewHoverManager && typeof window.previewHoverManager.refreshIframeReferences === 'function' && isPreviewEnabled()) {
            // Refresh iframe references after preview reload
            window.previewHoverManager.refreshIframeReferences();
            
            // Small delay to ensure iframe content is fully loaded
            setTimeout(() => {
              if (window.previewHoverManager && typeof window.previewHoverManager.handlePreviewModeChange === 'function') {
                window.previewHoverManager.handlePreviewModeChange(true);
              }
            }, 100);
          }
        };
        
        previewIframe.style.display = 'block';
        // Clean up any lingering drag state that could affect scrolling
        cleanupDragulaArtifacts();
      })
      .catch(error => {
        // Hide loading indicator
        if (window.pageEditor) {
          window.pageEditor.hideLoadingIndicator();
        }
        previewError.style.display = 'block';
        previewError.textContent = 'Error loading preview: ' + error.message;
      });
    }

    // Make refreshPreview globally available for property change events
    window.refreshPreview = refreshPreview;

    // Re-attach hover/click when iframe navigates back to the current page link
    // Track the currently selected page link (absolute URL) and compare on each iframe load
    function toAbsolute(href) {
      try { return new URL(href, window.location.origin).href; } catch (_) { return href; }
    }
    function normalizeAbs(href) {
      try {
        const u = new URL(href, window.location.origin);
        const origin = u.origin;
        const path = (u.pathname || '/').replace(/\/+$/, ''); // strip trailing slashes
        return origin + path;
      } catch (_) {
        return href;
      }
    }
    function getCurrentPageLinkAbs() {
      try {
        const link = window.pageEditor && window.pageEditor.pagesTabManager
          ? window.pageEditor.pagesTabManager.getSelectedPageLink()
          : null;
        return link ? toAbsolute(link) : null;
      } catch (_) { return null; }
    }
    let currentPageLinkAbs = getCurrentPageLinkAbs();

    // Update cached link whenever the page selection changes
    document.addEventListener('pageChanged', function(){
      currentPageLinkAbs = getCurrentPageLinkAbs();
    });

    // Listen for all iframe load events (navigation inside preview)
    if (previewIframe) {
      previewIframe.addEventListener('load', function() {
        // If iframe landed back on the exact current page link, re-attach hover
        try {
          const iframeHref = previewIframe.contentWindow && previewIframe.contentWindow.location
            ? previewIframe.contentWindow.location.href
            : null;
          const iframeAbs = iframeHref ? normalizeAbs(toAbsolute(iframeHref)) : null;
          // Refresh the cached link in case selection changed just before load
          const selectedAbsRaw = getCurrentPageLinkAbs() || currentPageLinkAbs;
          const selectedAbs = selectedAbsRaw ? normalizeAbs(selectedAbsRaw) : null;
          console.debug('Preview iframe load: comparing URLs', { iframeAbs, selectedAbs, match: iframeAbs === selectedAbs });
          if (iframeAbs && selectedAbs && iframeAbs === selectedAbs) {
            // Ensure the iframe hover bridge and CSS are present after navigation
            if (window.previewHoverManager && typeof window.previewHoverManager.refreshIframeReferences === 'function') {
              window.previewHoverManager.refreshIframeReferences();
              // Small delay ensures document is ready before enabling
              setTimeout(function(){
                console.debug('Preview iframe: reinitializing bridge for reattachment');
                if (isPreviewEnabled()) {
                  if (typeof window.previewHoverManager.reinitializeBridge === 'function') {
                    window.previewHoverManager.reinitializeBridge();
                  }
                } else {
                  if (typeof window.previewHoverManager.handlePreviewModeChange === 'function') {
                    window.previewHoverManager.handlePreviewModeChange(true);
                  }
                }
              }, 100);
            }
          }
        } catch (err) {
          console.debug('Preview iframe load reattach check failed:', err);
        }
      });
    }

    // Listen for navigation detection messages from iframe bridge
    window.addEventListener('message', function(e) {
      if (e.data && e.data.type === 'previewHover:navigationDetected') {
        console.log('Preview iframe navigation detected via bridge signal');
        // Same reattachment logic as load handler
        try {
          const iframeHref = previewIframe.contentWindow && previewIframe.contentWindow.location
            ? previewIframe.contentWindow.location.href
            : null;
          const iframeAbs = iframeHref ? normalizeAbs(toAbsolute(iframeHref)) : null;
          const selectedAbsRaw = getCurrentPageLinkAbs() || currentPageLinkAbs;
          const selectedAbs = selectedAbsRaw ? normalizeAbs(selectedAbsRaw) : null;
          console.debug('Preview navigation signal: comparing URLs', { iframeAbs, selectedAbs, match: iframeAbs === selectedAbs });
          if (iframeAbs && selectedAbs && iframeAbs === selectedAbs && window.previewHoverManager) {
            console.log('Preview navigation returned to current page, reattaching hover');
            window.previewHoverManager.refreshIframeReferences();
            setTimeout(function(){
              if (isPreviewEnabled()) {
                window.previewHoverManager.reinitializeBridge();
              } else {
                window.previewHoverManager.handlePreviewModeChange(true);
              }
            }, 100);
          }
        } catch (err) {
          console.debug('Preview navigation reattach failed:', err);
        }
      }
    });

    // Listen for page changes
    document.addEventListener('pageChanged', function(e) {
      console.log('pageChanged event fired, previewState:', previewState);
      // Reset the properties panel (unselect any previously selected widget)
      if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
        window.pageEditor.getPropertiesPanel().clear();
      }
      
      // If in preview mode, refresh the preview when page is switched
      if (isPreviewEnabled()) {
        console.log('Refreshing preview due to page change');
        // Add a small delay to ensure the layout is fully processed
        setTimeout(() => {
          refreshPreview();
        }, 100);
      }
    });

    // Listen for viewport changes
    document.addEventListener('viewportChanged', function(e) {
      console.log('viewportChanged event fired, previewState:', previewState);
      
      // If there's a locked selection in the preview hover, redraw it after viewport change
      if (window.previewHoverManager && typeof window.previewHoverManager.getLockedElement === 'function') {
        const lockedEl = window.previewHoverManager.getLockedElement();
        if (lockedEl && lockedEl.type) {
          console.log('Redrawing locked selection after viewport change');
          setTimeout(() => {
            if (window.previewHoverManager && typeof window.previewHoverManager.redrawLockedOutline === 'function') {
              window.previewHoverManager.redrawLockedOutline();
            }
          }, 150);
        }
      }
      
      // If in preview mode, refresh the preview when viewport is switched
      if (isPreviewEnabled()) {
        console.log('Refreshing preview due to viewport change');
        // Add a small delay to ensure the viewport styles are applied
        setTimeout(() => {
          refreshPreview();
        }, 100);
      }
    });

    // Listen for layout changes to keep preview in sync
    document.addEventListener('layoutChanged', function(e) {
      if (isPreviewEnabled()) {
        setTimeout(() => {
          refreshPreview();
        }, 50);
      }
    });

    if (previewStateButtons && previewStateButtons.length) {
      previewStateButtons.forEach(button => {
        button.addEventListener('click', function() {
          const nextState = button.dataset.previewState || 'preview';
          applyPreviewState(nextState, { refresh: nextState !== 'layout' });
        });
      });
    }

    // Add click handler to preview-container background to unselect elements
    // This allows users to click outside the iframe to clear all selections
    previewContainer.addEventListener('click', function(e) {
      // Only trigger if clicking directly on the preview-container background (not on child elements like iframe)
      if (isPreviewEnabled() && e.target === previewContainer && window.previewHoverManager) {
        console.log('Preview container background clicked, clearing selections');
        // Disable and re-enable to clear the current outline
        window.previewHoverManager.disable();
        window.previewHoverManager.enable();
        // Clear properties panel
        if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
          window.pageEditor.getPropertiesPanel().clear();
        }
      }
    });

    // Now initialize the page editor
    window.pageEditor.init();

    // Initialize preview mode by default
    setTimeout(() => {
      initializePreviewMode();
    }, 500);

    // Enhancement: Add canvas background click handler to unselect everything
    if (editorCanvas) {
      editorCanvas.addEventListener('click', function(e) {
        // Only unselect if clicking directly on the canvas background (not on child elements)
        if (e.target === editorCanvas) {
          // Clear the properties panel (unselect everything)
          if (window.pageEditor && window.pageEditor.getPropertiesPanel()) {
            document.querySelectorAll('.canvas-widget.selected').forEach(el => {
              el.classList.remove('selected');
            });
            window.pageEditor.getPropertiesPanel().clear();
            // Dispatch event for PreviewHoverManager to clear preview lock
            document.dispatchEvent(new CustomEvent('propertiesPanelCleared'));
          }
        }
      });
    }

    // Set up middle section button handlers
    const returnPage = '<c:out value="${returnPage}" />';
    
    // Exit button confirmation handler
    const exitButtons = document.querySelectorAll('a.confirm-exit');
    exitButtons.forEach(button => {
      button.addEventListener('click', async function(e) {
      const isDirty = window.pageEditor.isDirty && window.pageEditor.isDirty();
      if (isDirty) {
        e.preventDefault();
        const confirmed = await window.pageEditor.showConfirmDialog('You have unsaved changes. Are you sure you want to exit?');
        if (confirmed) {
          window.location.href = button.href;
        }
      }
      });
    });

    // Add Page button handler
    document.getElementById('add-page-btn').addEventListener('click', async function(e) {
      e.preventDefault();
      
      // Check if editor has unsaved changes
      const isDirty = window.pageEditor.isDirty && window.pageEditor.isDirty();
      if (isDirty) {
        const confirmed = await window.pageEditor.showConfirmDialog('You have unsaved changes. Are you sure you want to create a new page?');
        if (!confirmed) {
          return;
        }
      }
      
      // Show the add page modal
      showAddPageModal();
    });
    
    // Helper function to close modal
    function closePreDesignedPageModal() {
      document.getElementById('pre-designed-page-modal').classList.remove('active');
    }

    // Show modal - handle both toolbar button (if exists) and layouts tab button
    const toolbarBtn = document.getElementById('pre-designed-page-btn');
    const layoutsBtn = document.getElementById('pre-designed-page-btn-layouts');
    
    if (toolbarBtn) {
      toolbarBtn.addEventListener('click', function() {
        document.getElementById('pre-designed-page-modal').classList.add('active');
      });
    }
    
    if (layoutsBtn) {
      layoutsBtn.addEventListener('click', function() {
        document.getElementById('pre-designed-page-modal').classList.add('active');
      });
    }
    
    // Hide modal
    document.getElementById('close-pre-designed-page-modal').addEventListener('click', function() {
      closePreDesignedPageModal();
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

    // Set up palette tabs (left panel only - scoped to widget-palette)
    const widgetPalette = document.getElementById('widget-palette');
    const paletteTabs = widgetPalette.querySelectorAll('.tabs-nav a');
    const paletteTabContents = widgetPalette.querySelectorAll('.tab-content');

    paletteTabs.forEach(tab => {
      tab.addEventListener('click', e => {
        e.preventDefault();

        paletteTabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');

        const target = document.querySelector(tab.getAttribute('href'));
        paletteTabContents.forEach(tc => tc.classList.remove('active'));
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
      const titlebar = document.getElementById('editor-titlebar');
      const toolbar = document.getElementById('editor-toolbar');
      const container = document.getElementById('visual-page-editor-container');
      
      if (!wrapper || !titlebar || !toolbar || !container) return;
      
      // Get the viewport height
      const viewportHeight = window.innerHeight;
      
      // Calculate the height used by elements above the container
      const wrapperTop = wrapper.getBoundingClientRect().top;
      const titlebarHeight = titlebar.offsetHeight;
      const toolbarHeight = toolbar.offsetHeight;
      
      // Get any title element if it exists
      const titleElement = wrapper.querySelector('h4');
      const titleHeight = titleElement ? titleElement.offsetHeight : 0;
      
      // Get the messages container if it exists
      const messagesElement = wrapper.querySelector('[role="alert"], .messages-container');
      const messagesHeight = messagesElement ? messagesElement.offsetHeight : 0;
      
      // Calculate available height: viewport height - space from top - title - messages - titlebar - toolbar
      const availableHeight = viewportHeight - wrapperTop - titleHeight - messagesHeight - titlebarHeight - toolbarHeight;
      
      // Set the container height with a minimum
      const finalHeight = Math.max(availableHeight, 300);
      container.style.height = finalHeight + 'px';
    }

    // Add Page Modal Functions
    function showAddPageModal() {
      const modal = document.getElementById('add-page-modal');
      const titleInput = document.getElementById('page-title');
      const linkInput = document.getElementById('page-link');
      const errorDiv = document.getElementById('page-link-error');
      
      // Clear previous values and errors
      titleInput.value = '';
      linkInput.value = '';
      errorDiv.style.display = 'none';
      linkInput.classList.remove('error');
      
      // Show modal
      modal.classList.add('active');
      modal.style.display = 'flex';
      
      // Focus on title input
      setTimeout(() => {
        titleInput.focus();
      }, 100);
    }

    function hideAddPageModal() {
      const modal = document.getElementById('add-page-modal');
      modal.classList.remove('active');
      modal.style.display = 'none';
    }

    // Make functions globally accessible
    window.showAddPageModal = showAddPageModal;
    window.hideAddPageModal = hideAddPageModal;
    window.createNewPage = createNewPage;

    function validatePageLink(link) {
      // Must start with /
      if (!link.startsWith('/')) {
        return false;
      }
      
      // Basic validation - no spaces, valid URL characters
      const validLinkPattern = /^\/[a-zA-Z0-9\-_\/]*$/;
      return validLinkPattern.test(link);
    }

    function generateLinkFromTitle(title) {
      if (!title) return '';
      
      // Convert title to URL-friendly format
      return '/' + title
        .toLowerCase()
        .replace(/[^a-z0-9\s\-_]/g, '') // Remove special characters
        .replace(/\s+/g, '-') // Replace spaces with hyphens
        .replace(/\-+/g, '-') // Replace multiple hyphens with single
        .replace(/^\-|\-$/g, ''); // Remove leading/trailing hyphens
    }

    // Set up Add Page Modal event listeners
    document.getElementById('cancel-add-page').addEventListener('click', hideAddPageModal);
    
    // Auto-generate link from title
    document.getElementById('page-title').addEventListener('input', function(e) {
      const linkInput = document.getElementById('page-link');
      if (!linkInput.value || linkInput.dataset.autoGenerated !== 'false') {
        const generatedLink = generateLinkFromTitle(e.target.value);
        linkInput.value = generatedLink;
        linkInput.dataset.autoGenerated = 'true';
      }
    });
    
    // Mark link as manually edited when user types in it
    document.getElementById('page-link').addEventListener('input', function(e) {
      e.target.dataset.autoGenerated = 'false';
      
      // Clear error state when user starts typing
      const errorDiv = document.getElementById('page-link-error');
      errorDiv.style.display = 'none';
      e.target.classList.remove('error');
    });
    
    // Handle form submission
    document.getElementById('add-page-form').addEventListener('submit', function(e) {
      e.preventDefault();
      
      const titleInput = document.getElementById('page-title');
      const linkInput = document.getElementById('page-link');
      const errorDiv = document.getElementById('page-link-error');
      const createBtn = document.getElementById('create-page-btn');
      
      const title = titleInput.value.trim();
      const link = linkInput.value.trim();
      
      // Validate inputs
      if (!title) {
        titleInput.focus();
        return;
      }
      
      if (!link) {
        linkInput.focus();
        return;
      }
      
      if (!validatePageLink(link)) {
        errorDiv.style.display = 'block';
        linkInput.classList.add('error');
        linkInput.style.borderColor = '#dc3545';
        linkInput.focus();
        return;
      }
      
      // Check if page already exists
      const existingPage = window.pageEditor.pagesTabManager.pages.find(page => page.link === link);
      if (existingPage) {
        errorDiv.textContent = 'A page with this link already exists';
        errorDiv.style.display = 'block';
        linkInput.classList.add('error');
        linkInput.style.borderColor = '#dc3545';
        linkInput.focus();
        return;
      }
      
      // Check if we're already editing a new page with this link
      if (window.pageEditor.pagesTabManager.selectedPageId === 'new' && 
          window.pageEditor.pagesTabManager.selectedPageLink === link) {
        errorDiv.textContent = 'You are already editing a page with this link';
        errorDiv.style.display = 'block';
        linkInput.classList.add('error');
        linkInput.style.borderColor = '#dc3545';
        linkInput.focus();
        return;
      }
      
      // Disable create button to prevent double submission
      createBtn.disabled = true;
      createBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Creating...';
      
      // Create the new page
      createNewPage(title, link);
    });
    
    function createNewPage(title, link) {
      console.log('Creating new page:', title, link);
      
      // Hide the modal
      hideAddPageModal();
      
      // Switch to the Pages tab to show the new page
      const pagesTab = document.querySelector('a[href="#pages-tab"]');
      const pagesTabContent = document.getElementById('pages-tab');
      
      if (pagesTab && pagesTabContent) {
        // Remove active class from all palette tabs (left panel only)
        const widgetPalette = document.getElementById('widget-palette');
        widgetPalette.querySelectorAll('.tabs-nav a').forEach(tab => tab.classList.remove('active'));
        widgetPalette.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
        
        // Activate the Pages tab
        pagesTab.classList.add('active');
        pagesTabContent.classList.add('active');
        
        console.log('Switched to Pages tab');
      }
      
      // Use the PageEditor's createNewPage method
      if (window.pageEditor && typeof window.pageEditor.createNewPage === 'function') {
        window.pageEditor.createNewPage(title, link);
      } else {
        console.error('PageEditor createNewPage method not available');
      }
      
      // Re-enable create button for next time
      const createBtn = document.getElementById('create-page-btn');
      createBtn.disabled = false;
      createBtn.innerHTML = 'Create Page';
    }

    // Close modal on ESC key
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        const addPageModal = document.getElementById('add-page-modal');
        if (addPageModal && addPageModal.classList.contains('active')) {
          hideAddPageModal();
        }
        
        const modal = document.getElementById('pre-designed-page-modal');
        if (modal && modal.classList.contains('active')) {
          closePreDesignedPageModal();
        }
      }
    });

    // Close modal on overlay click
    document.getElementById('add-page-modal').addEventListener('click', function(e) {
      if (e.target === this) {
        hideAddPageModal();
      }
    });
    
    // Calculate height on load
    calculateContainerHeight();
    
    // Recalculate on window resize
    window.addEventListener('resize', calculateContainerHeight);
    
    // Dark Mode Toggle
    const darkModeToggleMenu = document.getElementById('dark-mode-toggle-menu');
    const editorWrapper = document.getElementById('visual-page-editor-wrapper');
    
    // Check for saved theme preference
    const savedTheme = localStorage.getItem('editor-theme') || 'light';
    if (savedTheme === 'dark') {
      editorWrapper.setAttribute('data-theme', 'dark');
      const iconElement = darkModeToggleMenu.querySelector('i');
      if (iconElement) {
        iconElement.className = '${font:far()} fa-sun';
      }
    }
    
    darkModeToggleMenu.addEventListener('click', function(e) {
      e.preventDefault();
      const currentTheme = editorWrapper.getAttribute('data-theme');
      const iconElement = darkModeToggleMenu.querySelector('i');
      if (currentTheme === 'dark') {
        editorWrapper.removeAttribute('data-theme');
        if (iconElement) {
          iconElement.className = '${font:far()} fa-moon';
        }
        localStorage.setItem('editor-theme', 'light');
      } else {
        editorWrapper.setAttribute('data-theme', 'dark');
        if (iconElement) {
          iconElement.className = '${font:far()} fa-sun';
        }
        localStorage.setItem('editor-theme', 'dark');
      }
    });
    
    // Properties Panel Resizing
    const propertiesPanelElement = document.getElementById('properties-panel');
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
        localStorage.setItem('properties-panel-width', propertiesPanelElement.style.width);
      }
    });
    
    // Restore saved width
    const savedWidth = localStorage.getItem('properties-panel-width');
    if (savedWidth) {
      propertiesPanelElement.style.width = savedWidth;
    }
    
    // Static Site Modal
    const staticSiteManager = new StaticSiteManager({
      token: '<c:out value="${userSession.formToken}" />',
      modalId: 'static-site-modal',
      openModalBtnId: 'static-site-generator-btn',
      closeModalBtnId: 'close-static-site-modal',
      generateBtnId: 'generate-static-site-btn',
      fileListId: 'static-site-list',
      pollingIndicatorId: 'static-site-polling-indicator',
      listUrl: '${ctx}/json/static-sites/list?action=list',
      generateUrl: '${ctx}/json/static-sites/generate',
      deleteUrl: '${ctx}/json/static-sites/delete',
      downloadUrl: '${ctx}/json/static-sites/download?action=download',
      gitSettingsUrl: '${ctx}/json/static-sites/git-settings?action=GET_GIT_SETTINGS',
      saveGitSettingsUrl: '${ctx}/json/static-sites/save-git-settings'
    });
    staticSiteManager.init();
  });
</script>
