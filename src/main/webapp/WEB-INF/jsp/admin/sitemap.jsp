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
<%@ page import="static com.simisinc.platform.ApplicationInfo.VERSION" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="web" uri="/WEB-INF/tlds/web.tld" %>
<jsp:useBean id="userSession" class="com.simisinc.platform.presentation.controller.UserSession" scope="session"/>
<jsp:useBean id="widgetContext" class="com.simisinc.platform.presentation.controller.WidgetContext" scope="request"/>
<jsp:useBean id="menuTabList" class="java.util.ArrayList" scope="request"/>
<jsp:useBean id="menuTab" class="com.simisinc.platform.domain.model.cms.MenuTab" scope="request"/>
<web:stylesheet package="dragula" file="dragula.min.css" />
<g:compress>
  <link rel="stylesheet" href="${ctx}/css/platform-sitemap-editor.css" />
</g:compress>
<div class="sitemap-editor">
  <div id="sticky-container" data-sticky-container>
    <div id="sticky-item" data-sticky style="width:100%" data-margin-top="1" data-sticky-on="small">
      <c:if test="${!empty title}">
        <h4><c:if test="${!empty icon}"><i class="fa ${icon}"></i> </c:if><c:out value="${title}"/></h4>
      </c:if>
      <%@include file="../page_messages.jspf" %>
      <%-- Live preview bar --%>
      <div id="menu-preview" class="menu-preview">
        <div class="preview-label">
          <i class="fa fa-eye"></i> Live Preview
        </div>
        <div id="preview-tabs" class="preview-tabs">
          <c:forEach items="${menuTabList}" var="menuTab" varStatus="status">
            <div class="preview-tab" data-tab-id="${menuTab.id}">
              <c:if test="${!empty menuTab.icon}">
                <i class="fa ${menuTab.icon}"></i>
              </c:if>
              <span class="preview-tab-name"><c:out value="${menuTab.name}" /></span>
              <c:if test="${!empty menuTab.menuItemList}">
                <div class="preview-dropdown">
                  <c:forEach items="${menuTab.menuItemList}" var="menuItem">
                    <div class="preview-dropdown-item">
                      <c:out value="${menuItem.name}" />
                    </div>
                  </c:forEach>
                </div>
              </c:if>
            </div>
          </c:forEach>
        </div>
      </div>
      <div class="sitemap-actions">
        <button type="button" id="add-menu-btn" class="button radius success">
          <i class="fa fa-plus"></i> Add Menu Tab
        </button>
        <button type="button" id="save-sitemap-btn" class="button radius primary">
          <i class="fa fa-save"></i> Save Changes
        </button>
        <a href="${ctx}/admin/sitemap" class="button radius secondary">Reload</a>
      </div>
    </div>
  </div>
  <div id="sitemap-container" class="sitemap-container">
    <c:choose>
      <c:when test="${empty menuTabList}">
        <div class="empty-state">
          <i class="fa fa-sitemap fa-3x"></i>
          <h5>No menu tabs found</h5>
          <p>Click "Add Menu Tab" to create your first menu item.</p>
        </div>
      </c:when>
      <c:otherwise>
        <c:forEach items="${menuTabList}" var="menuTab" varStatus="status">
          <div class="menu-tab" data-tab-id="${menuTab.id}" data-is-home="${status.first}">
            <div class="menu-tab-header">
              <div class="menu-tab-controls">
                <c:choose>
                  <c:when test="${status.first}">
                    <i class="fa fa-home" title="Home tab"></i>
                  </c:when>
                  <c:otherwise>
                    <i class="fa fa-grip-vertical drag-handle" title="Drag to reorder"></i>
                  </c:otherwise>
                </c:choose>
              </div>
              <div class="menu-tab-content">
                <div class="menu-tab-name" contenteditable="${!status.first}" data-original="<c:out value='${menuTab.name}' />">
                  <c:out value="${menuTab.name}" />
                </div>
                <div class="menu-tab-link">
                  <input type="text" class="link-input no-gap" value="<c:out value='${menuTab.link}' />" placeholder="/page-url" data-original="<c:out value='${menuTab.link}' />">
                </div>
                <div class="menu-tab-icon">
                  <input type="text" class="icon-input no-gap" value="<c:out value='${menuTab.icon}' />" placeholder="fa-icon" data-original="<c:out value='${menuTab.icon}' />">
                </div>
              </div>
              <div class="menu-tab-actions">
                <c:if test="${!status.first}">
                  <button type="button" class="btn-icon add-item-btn" title="Add menu item">
                    <i class="fa fa-plus"></i>
                  </button>
                  <button type="button" class="btn-icon delete-tab-btn" title="Delete tab">
                    <i class="fa fa-trash"></i>
                  </button>
                </c:if>
              </div>
            </div>
            <c:if test="${!status.first}">
              <div class="menu-items-container" data-tab-id="${menuTab.id}">
                <c:forEach items="${menuTab.menuItemList}" var="menuItem">
                  <div class="menu-item" data-item-id="${menuItem.id}">
                    <div class="menu-item-drag">
                      <i class="fa fa-grip-vertical drag-handle"></i>
                    </div>
                    <div class="menu-item-content">
                      <div class="menu-item-name" contenteditable="true" data-original="<c:out value='${menuItem.name}' />">
                        <c:out value="${menuItem.name}" />
                      </div>
                      <input type="text" class="menu-item-link no-gap" value="<c:out value='${menuItem.link}' />" placeholder="/page-url" data-original="<c:out value='${menuItem.link}' />">
                    </div>
                    <div class="menu-item-actions">
                      <button type="button" class="btn-icon delete-item-btn" title="Delete item">
                        <i class="fa fa-trash"></i>
                      </button>
                    </div>
                  </div>
                </c:forEach>
              </div>
            </c:if>
          </div>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </div>
</div>
<form id="sitemap-form" method="post" style="display: none;">
  <input type="hidden" name="widget" value="${widgetContext.uniqueId}"/>
  <input type="hidden" name="token" value="${userSession.formToken}"/>
  <input type="hidden" id="sitemap-data" name="sitemapData" value=""/>
</form>
<div id="add-menu-modal" class="modal" style="display: none;">
  <div class="modal-content">
    <h5>Add New Menu Tab</h5>
    <form id="add-menu-form">
      <div class="form-group">
        <label>Menu Name *</label>
        <input type="text" id="new-menu-name" placeholder="Menu Name" required>
      </div>
      <div class="form-group">
        <label>Page Link</label>
        <input type="text" id="new-menu-link" placeholder="/page-url">
      </div>
      <div class="form-group">
        <label>Icon</label>
        <input type="text" id="new-menu-icon" placeholder="fa-icon">
      </div>
      <div class="modal-actions">
        <button type="submit" class="button success">Add Menu</button>
        <button type="button" class="button secondary cancel-btn">Cancel</button>
      </div>
    </form>
  </div>
</div>
<web:script package="dragula" file="dragula.min.js" />
<script>
document.addEventListener('DOMContentLoaded', function() {
  var widgetId = '${widgetContext.uniqueId}';
  var token = '${userSession.formToken}';
  var baseUri = '${widgetContext.uri}';
  var contextPath = '${ctx}';
  var nextTempId = -1;
  var hasChanges = false;
  var isUpdatingFromPreview = false; // Flag to prevent circular updates
  var updatePreviewTimeout = null; // For debouncing preview updates
  var deletedTabs = []; // Track deleted menu tabs
  var deletedItems = []; // Track deleted menu items

  // Initialize drag and drop
  var tabDragula = dragula([document.getElementById('sitemap-container')], {
    moves: function(el, container, handle) {
      // Only allow dragging menu tabs by their specific drag handle
      return el.classList.contains('menu-tab') && 
             handle.classList.contains('drag-handle') && 
             el.dataset.isHome !== 'true' &&
             handle.closest('.menu-tab-controls'); // Must be in tab controls, not item controls
    },
    accepts: function(el, target, source, sibling) {
      if (sibling && sibling.dataset.isHome === 'true') return false;
      return el.classList.contains('menu-tab');
    }
  });

  // Preview drag and drop
  var previewDragula = dragula([document.getElementById('preview-tabs')], {
    moves: function(el, container, handle) {
      // Allow dragging preview tabs, but not the home tab
      return el.classList.contains('preview-tab') && el.dataset.isHome !== 'true';
    },
    accepts: function(el, target, source, sibling) {
      if (sibling && sibling.dataset.isHome === 'true') return false;
      return el.classList.contains('preview-tab');
    }
  });

  var menuContainers = Array.from(document.querySelectorAll('.menu-items-container'));
  var itemDragula = null;
  if (menuContainers.length > 0) {
    itemDragula = dragula(menuContainers, {
      moves: function(el, container, handle) {
        // Only allow dragging menu items by their specific drag handle
        return el.classList.contains('menu-item') && 
               handle.classList.contains('drag-handle') &&
               handle.closest('.menu-item-drag'); // Must be in item drag area
      },
      accepts: function(el, target, source, sibling) {
        return el.classList.contains('menu-item') && target.classList.contains('menu-items-container');
      }
    });
    itemDragula.on('drop', function() {
      markChanged();
      if (!isUpdatingFromPreview) {
        updatePreview();
      }
    });
  }

  tabDragula.on('drop', function() {
    markChanged();
    if (!isUpdatingFromPreview) {
      updatePreview();
    }
  });

  previewDragula.on('drop', function(el, target, source, sibling) {
    // Sync the editor order with the preview order
    isUpdatingFromPreview = true;
    syncEditorFromPreview();
    isUpdatingFromPreview = false;
    markChanged();
  });

  // Initialize preview
  rebuildPreview();

  // Event handlers
  document.getElementById('add-menu-btn').addEventListener('click', showAddMenuModal);
  document.getElementById('save-sitemap-btn').addEventListener('click', saveSitemap);
  document.getElementById('add-menu-form').addEventListener('submit', function(e) {
    e.preventDefault();
    addMenuTab();
  });
  document.querySelector('.cancel-btn').addEventListener('click', hideAddMenuModal);

  // Content editing
  document.addEventListener('input', function(e) {
    if (e.target.matches('[contenteditable], .link-input, .icon-input, .menu-item-link')) {
      markChanged();
      if (!isUpdatingFromPreview) {
        updatePreview();
      }
    }
  });

  // Prevent newlines in contenteditable fields
  document.addEventListener('keydown', function(e) {
    if (e.target.matches('[contenteditable]')) {
      if (e.key === 'Enter') {
        e.preventDefault();
        e.target.blur(); // Exit editing mode
      }
    }
  });

  // Clean up any pasted content in contenteditable fields
  document.addEventListener('paste', function(e) {
    if (e.target.matches('[contenteditable]')) {
      e.preventDefault();
      var text = (e.clipboardData || window.clipboardData).getData('text/plain');
      // Remove newlines and extra whitespace
      text = text.replace(/\r?\n|\r/g, ' ').replace(/\s+/g, ' ').trim();
      document.execCommand('insertText', false, text);
    }
  });

  // Button clicks
  document.addEventListener('click', function(e) {
    if (e.target.closest('.delete-tab-btn')) {
      deleteTab(e.target.closest('.menu-tab'));
    } else if (e.target.closest('.delete-item-btn')) {
      deleteMenuItem(e.target.closest('.menu-item'));
    } else if (e.target.closest('.add-item-btn')) {
      addMenuItem(e.target.closest('.menu-tab'));
    }
  });

  function showAddMenuModal() {
    document.getElementById('add-menu-modal').style.display = 'block';
    document.getElementById('new-menu-name').focus();
  }

  function hideAddMenuModal() {
    document.getElementById('add-menu-modal').style.display = 'none';
    document.getElementById('add-menu-form').reset();
  }

  function addMenuTab() {
    var name = document.getElementById('new-menu-name').value.trim();
    var link = document.getElementById('new-menu-link').value.trim();
    var icon = document.getElementById('new-menu-icon').value.trim();

    if (!name) return;

    var tempId = nextTempId--;
    var menuTab = createMenuTabElement(tempId, name, link, icon, false);
    
    document.getElementById('sitemap-container').appendChild(menuTab);
    updateDragula();
    markChanged();
    updatePreview();
    hideAddMenuModal();
  }

  function addMenuItem(tabElement) {
    var tabId = tabElement.dataset.tabId;
    var container = tabElement.querySelector('.menu-items-container');
    
    if (!container) {
      container = document.createElement('div');
      container.className = 'menu-items-container';
      container.dataset.tabId = tabId;
      tabElement.appendChild(container);
    }

    var tempId = nextTempId--;
    var menuItem = createMenuItemElement(tempId, '', '');
    container.appendChild(menuItem);
    
    var nameField = menuItem.querySelector('.menu-item-name');
    nameField.focus();
    
    updateDragula();
    markChanged();
    updatePreview();
  }

  function deleteTab(tabElement) {
    if (!confirm('Are you sure you want to delete this menu tab and all of its items?')) {
      return;
    }

    var tabId = tabElement.dataset.tabId;
    if (tabId > 0) {
      // Track deletion for existing tab
      deletedTabs.push(parseInt(tabId));
      // Also track any existing menu items for deletion
      var itemElements = tabElement.querySelectorAll('.menu-item');
      itemElements.forEach(function(itemEl) {
        var itemId = itemEl.dataset.itemId;
        if (itemId > 0) {
          deletedItems.push(parseInt(itemId));
        }
      });
    }
    
    // Remove from DOM
    tabElement.remove();
    updateDragula();
    markChanged();
    updatePreview();
  }

  function deleteMenuItem(itemElement) {
    if (!confirm('Are you sure you want to delete this menu item?')) {
      return;
    }

    var itemId = itemElement.dataset.itemId;
    if (itemId > 0) {
      // Track deletion for existing item
      deletedItems.push(parseInt(itemId));
    }
    
    // Remove from DOM
    itemElement.remove();
    markChanged();
    updatePreview();
  }

  function createMenuTabElement(id, name, link, icon, isHome) {
    var div = document.createElement('div');
    div.className = 'menu-tab';
    div.dataset.tabId = id;
    div.dataset.isHome = isHome;
    
    var controlsHtml = !isHome ? 
      '<i class="fa fa-grip-vertical drag-handle" title="Drag to reorder"></i>' : 
      '<i class="fa fa-home" title="Home tab"></i>';
    
    var actionsHtml = !isHome ? 
      '<button type="button" class="btn-icon add-item-btn" title="Add menu item">' +
      '<i class="fa fa-plus"></i>' +
      '</button>' +
      '<button type="button" class="btn-icon delete-tab-btn" title="Delete tab">' +
      '<i class="fa fa-trash"></i>' +
      '</button>' : '';
    
    var containerHtml = !isHome ? 
      '<div class="menu-items-container" data-tab-id="' + id + '"></div>' : '';
    
    div.innerHTML = 
      '<div class="menu-tab-header">' +
        '<div class="menu-tab-controls">' + controlsHtml + '</div>' +
        '<div class="menu-tab-content">' +
          '<div class="menu-tab-name" contenteditable="' + (!isHome) + '" data-original="' + name + '">' + name + '</div>' +
          '<div class="menu-tab-link">' +
            '<input type="text" class="link-input no-gap" value="' + link + '" placeholder="/page-url" data-original="' + link + '">' +
          '</div>' +
          '<div class="menu-tab-icon">' +
            '<input type="text" class="icon-input no-gap" value="' + icon + '" placeholder="fa-icon" data-original="' + icon + '">' +
          '</div>' +
        '</div>' +
        '<div class="menu-tab-actions">' + actionsHtml + '</div>' +
      '</div>' + containerHtml;
    
    return div;
  }

  function createMenuItemElement(id, name, link) {
    var div = document.createElement('div');
    div.className = 'menu-item';
    div.dataset.itemId = id;
    
    div.innerHTML = 
      '<div class="menu-item-drag">' +
        '<i class="fa fa-grip-vertical drag-handle"></i>' +
      '</div>' +
      '<div class="menu-item-content">' +
        '<div class="menu-item-name" contenteditable="true" data-original="' + name + '">' + name + '</div>' +
        '<input type="text" class="menu-item-link no-gap" value="' + link + '" placeholder="/page-url" data-original="' + link + '">' +
      '</div>' +
      '<div class="menu-item-actions">' +
        '<button type="button" class="btn-icon delete-item-btn" title="Delete item">' +
          '<i class="fa fa-trash"></i>' +
        '</button>' +
      '</div>';
    
    return div;
  }

  function updateDragula() {
    if (itemDragula) {
      itemDragula.destroy();
    }
    
    var menuContainers = Array.from(document.querySelectorAll('.menu-items-container'));
    if (menuContainers.length > 0) {
      itemDragula = dragula(menuContainers, {
        moves: function(el, container, handle) {
          // Only allow dragging menu items by their specific drag handle
          return el.classList.contains('menu-item') && 
                 handle.classList.contains('drag-handle') &&
                 handle.closest('.menu-item-drag'); // Must be in item drag area
        },
        accepts: function(el, target, source, sibling) {
          return el.classList.contains('menu-item') && target.classList.contains('menu-items-container');
        }
      });
      
      itemDragula.on('drop', function() {
        markChanged();
        if (!isUpdatingFromPreview) {
          updatePreview();
        }
      });
    }
  }

  function markChanged() {
    hasChanges = true;
    var saveBtn = document.getElementById('save-sitemap-btn');
    saveBtn.classList.add('alert');
    
    // Update button text to show pending changes
    var pendingCount = deletedTabs.length + deletedItems.length;
    if (pendingCount > 0) {
      saveBtn.innerHTML = '<i class="fa fa-save"></i> Save Changes (' + pendingCount + ' deletion' + (pendingCount > 1 ? 's' : '') + ' pending)';
    } else {
      saveBtn.innerHTML = '<i class="fa fa-save"></i> Save Changes';
    }
  }

  function updatePreview() {
    // Don't update preview if we're currently dragging in the preview
    if (previewDragula && previewDragula.dragging) {
      return;
    }
    
    // Clear any pending update
    if (updatePreviewTimeout) {
      clearTimeout(updatePreviewTimeout);
    }
    
    // Debounce the update to prevent rapid rebuilds
    updatePreviewTimeout = setTimeout(function() {
      rebuildPreview();
    }, 50);
  }

  function rebuildPreview() {
    var previewContainer = document.getElementById('preview-tabs');
    
    // Store current dragula state and destroy it
    if (previewDragula) {
      previewDragula.destroy();
    }
    
    // Clear and rebuild preview
    previewContainer.innerHTML = '';
    
    var tabElements = document.querySelectorAll('.menu-tab');
    tabElements.forEach(function(tabEl) {
      var tabId = tabEl.dataset.tabId;
      var isHome = tabEl.dataset.isHome === 'true';
      
      var nameEl = tabEl.querySelector('.menu-tab-name');
      var iconEl = tabEl.querySelector('.icon-input');
      
      var name = nameEl.textContent.trim();
      var icon = iconEl.value.trim();
      
      // Create preview tab
      var previewTab = document.createElement('div');
      previewTab.className = 'preview-tab';
      previewTab.dataset.tabId = tabId;
      previewTab.dataset.isHome = isHome;
      
      var tabContent = '';
      if (icon) {
        tabContent += '<i class="fa ' + icon + '"></i> ';
      }
      tabContent += '<span class="preview-tab-name">' + name + '</span>';
      
      // Add dropdown for menu items
      var itemElements = tabEl.querySelectorAll('.menu-item');
      if (itemElements.length > 0 && !isHome) {
        var dropdown = document.createElement('div');
        dropdown.className = 'preview-dropdown';
        
        itemElements.forEach(function(itemEl) {
          var itemNameEl = itemEl.querySelector('.menu-item-name');
          var itemName = itemNameEl.textContent.trim();
          
          var dropdownItem = document.createElement('div');
          dropdownItem.className = 'preview-dropdown-item';
          dropdownItem.textContent = itemName;
          dropdown.appendChild(dropdownItem);
        });
        
        tabContent += dropdown.outerHTML;
      }
      
      previewTab.innerHTML = tabContent;
      previewContainer.appendChild(previewTab);
    });
    
    // Reinitialize preview dragula
    previewDragula = dragula([previewContainer], {
      moves: function(el, container, handle) {
        // Allow dragging preview tabs, but not the home tab
        return el.classList.contains('preview-tab') && el.dataset.isHome !== 'true';
      },
      accepts: function(el, target, source, sibling) {
        if (sibling && sibling.dataset.isHome === 'true') return false;
        return el.classList.contains('preview-tab');
      }
    });

    previewDragula.on('drop', function(el, target, source, sibling) {
      // Sync the editor order with the preview order
      isUpdatingFromPreview = true;
      syncEditorFromPreview();
      isUpdatingFromPreview = false;
      markChanged();
    });
  }

  function syncEditorFromPreview() {
    var previewTabs = document.querySelectorAll('.preview-tab');
    var editorContainer = document.getElementById('sitemap-container');
    
    // Create a document fragment to hold the reordered elements
    var fragment = document.createDocumentFragment();
    
    // Add elements to fragment in the order they appear in preview
    previewTabs.forEach(function(previewTab) {
      var tabId = previewTab.dataset.tabId;
      var editorTab = editorContainer.querySelector('.menu-tab[data-tab-id="' + tabId + '"]');
      if (editorTab) {
        fragment.appendChild(editorTab);
      }
    });
    
    // Clear the container and append the reordered elements
    editorContainer.innerHTML = '';
    editorContainer.appendChild(fragment);
  }

  function saveSitemap() {
    var sitemapData = collectSitemapData();
    document.getElementById('sitemap-data').value = JSON.stringify(sitemapData);
    document.getElementById('sitemap-form').submit();
  }

  function collectSitemapData() {
    var tabs = [];
    var tabElements = document.querySelectorAll('.menu-tab');
    
    tabElements.forEach(function(tabEl, index) {
      var tabId = tabEl.dataset.tabId;
      var isHome = tabEl.dataset.isHome === 'true';
      
      var nameEl = tabEl.querySelector('.menu-tab-name');
      var linkEl = tabEl.querySelector('.link-input');
      var iconEl = tabEl.querySelector('.icon-input');
      
      var tab = {
        id: parseInt(tabId),
        name: nameEl.textContent.trim(),
        link: linkEl.value.trim(),
        icon: iconEl.value.trim(),
        order: index,
        isHome: isHome,
        items: []
      };

      var itemElements = tabEl.querySelectorAll('.menu-item');
      itemElements.forEach(function(itemEl, itemIndex) {
        var itemId = itemEl.dataset.itemId;
        var nameEl = itemEl.querySelector('.menu-item-name');
        var linkEl = itemEl.querySelector('.menu-item-link');
        
        tab.items.push({
          id: parseInt(itemId),
          name: nameEl.textContent.trim(),
          link: linkEl.value.trim(),
          order: itemIndex,
          tabId: parseInt(tabId)
        });
      });

      tabs.push(tab);
    });

    return { 
      tabs: tabs,
      deletedTabs: deletedTabs,
      deletedItems: deletedItems
    };
  }
});
</script>
