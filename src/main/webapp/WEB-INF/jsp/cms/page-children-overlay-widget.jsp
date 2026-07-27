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
  ~
  ~ Page Children Overlay - body include (HTML + JS)
  ~ Included by main.jsp before </body> when site.pageChildren.overlay.enabled is true
  --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="g" uri="http://granule.com/tags" %>
<%@ taglib prefix="js" uri="/WEB-INF/tlds/javascript-escape.tld" %>
<%-- Floating trigger button --%>
<button id="global-children-pages-trigger" class="children-pages-trigger hide-for-print" type="button" aria-expanded="false" aria-controls="global-child-pages-panel" aria-label="Open child pages">
  <span class="children-pages-trigger-icon" aria-hidden="true"><i class="fa fa-sitemap"></i></span>
</button>

<%-- Slide-out panel --%>
<aside id="global-child-pages-panel" class="children-pages-panel hide-for-print" aria-label="Child Pages" aria-hidden="true" hidden>
  <div class="children-pages-panel-header">
    <h3 class="children-pages-title">Child Pages</h3>
    <button class="children-pages-close" type="button" aria-label="Close child pages">&times;</button>
  </div>
  <ul id="global-child-pages-list" class="overlay-page-tree" role="tree"></ul>
</aside>

<%-- Backdrop --%>
<div id="global-children-pages-backdrop" class="children-pages-backdrop hide-for-print" hidden></div>

<%-- Overlay JavaScript --%>
<script>
  $(document).ready(function() {
    (function() {
      var trigger = document.getElementById('global-children-pages-trigger');
      var panel = document.getElementById('global-child-pages-panel');
      var closeButton = panel ? panel.querySelector('.children-pages-close') : null;
      var backdrop = document.getElementById('global-children-pages-backdrop');
      var listElement = document.getElementById('global-child-pages-list');
      var pageId = Number('${masterWebPage.id}');
      var isLoaded = false;

      if (!trigger || !panel || !closeButton || !backdrop || !listElement || !pageId || pageId < 1) {
        if (trigger) {
          trigger.style.display = 'none';
        }
        return;
      }

      function setOpenState(isOpen) {
        trigger.setAttribute('aria-expanded', String(isOpen));
        panel.setAttribute('aria-hidden', String(!isOpen));
        panel.hidden = !isOpen;
        panel.classList.toggle('is-open', isOpen);
        backdrop.hidden = !isOpen;
        document.body.classList.toggle('child-pages-panel-open', isOpen);
      }

      function createTreeItem(node) {
        var listItem = document.createElement('li');
        var level = Number(node.level || 1);
        if (level < 1) {
          level = 1;
        }
        listItem.className = 'overlay-tree-item level-' + level;
        if (node.isParent) {
          listItem.classList.add('overlay-tree-item-parent');
        }
        if (node.isCurrent) {
          listItem.classList.add('overlay-tree-item-current');
        }
        listItem.setAttribute('role', 'treeitem');
        listItem.setAttribute('aria-level', String(level));
        // Keep top-level items flush-left and only indent deeper levels.
        var levelOffset = Math.max(level - 1, 0);
        listItem.style.setProperty('--indent', (levelOffset * 18) + 'px');

        var content = document.createElement('div');
        content.className = 'overlay-tree-content';

        var link = document.createElement('a');
        link.className = 'overlay-tree-link';
        link.textContent = node.title || 'Untitled';
        link.href = node.link || '#';
        if (node.isCurrent) {
          link.setAttribute('aria-current', 'page');
        }

        content.appendChild(link);
        listItem.appendChild(content);
        return listItem;
      }

      function renderFallbackMessage() {
        listElement.innerHTML = '<li class="overlay-tree-item level-1" role="treeitem" aria-level="1">' +
                '<div class="overlay-tree-content"><span class="overlay-tree-link">No child pages available</span></div></li>';
      }

      function renderLoadErrorMessage() {
        listElement.innerHTML = '<li class="overlay-tree-item level-1" role="treeitem" aria-level="1">' +
                '<div class="overlay-tree-content"><span class="overlay-tree-link">Unable to load child pages. Please try again.</span></div></li>';
      }

      function renderChildren(payload) {
        listElement.innerHTML = '';

        var parents = [];
        var currentPage = null;
        var children = [];

        if (Array.isArray(payload)) {
          children = payload;
        } else if (payload && typeof payload === 'object') {
          parents = Array.isArray(payload.parents) ? payload.parents : [];
          currentPage = payload.currentPage || null;
          children = Array.isArray(payload.children) ? payload.children : [];
        }

        var hasHierarchy = parents.length > 0 || !!currentPage;
        if (!hasHierarchy && children.length === 0) {
          renderFallbackMessage();
          isLoaded = true;
          return;
        }

        for (var p = 0; p < parents.length; p++) {
          var parentNode = Object.assign({}, parents[p], { level: p + 1, isParent: true });
          listElement.appendChild(createTreeItem(parentNode));
        }

        var hierarchyDepth = parents.length;
        if (currentPage && currentPage.id) {
          hierarchyDepth += 1;
          listElement.appendChild(createTreeItem(Object.assign({}, currentPage, {
            level: hierarchyDepth,
            isParent: true,
            isCurrent: true
          })));
        }

        if (children.length === 0) {
          isLoaded = true;
          return;
        }

        var baseLevel = Number(children[0].level || 1);
        if (baseLevel < 1) {
          baseLevel = 1;
        }

        for (var i = 0; i < children.length; i++) {
          var child = children[i];
          var normalizedChild = Object.assign({}, child, {
            level: Math.max(Number(child.level || 1) - baseLevel + 1, 1) + hierarchyDepth
          });
          listElement.appendChild(createTreeItem(normalizedChild));
        }
        isLoaded = true;
      }

      function loadChildren() {
        if (isLoaded) {
          return Promise.resolve();
        }
        listElement.innerHTML = '<li class="overlay-tree-item level-1"><div class="overlay-tree-content"><span class="overlay-tree-link">Loading...</span></div></li>';
        return fetch('${ctx}/json/pages/children?parentId=' + encodeURIComponent(String(pageId)) + '&includeParents=true', {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        })
          .then(function(response) { return response.json(); })
          .then(function(data) {
            if (!data || data.status !== 'ok' || data.data == null) {
              renderLoadErrorMessage();
              return;
            }
            renderChildren(data.data);
          })
          .catch(function() { renderLoadErrorMessage(); });
      }

      setOpenState(false);

      trigger.addEventListener('click', function() {
        var isOpen = trigger.getAttribute('aria-expanded') === 'true';
        if (isOpen) {
          setOpenState(false);
          return;
        }
        loadChildren().then(function() {
          setOpenState(true);
        });
      });

      closeButton.addEventListener('click', function() { setOpenState(false); });
      backdrop.addEventListener('click', function() { setOpenState(false); });
      document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') { setOpenState(false); }
      });
    })();
  });
</script>
