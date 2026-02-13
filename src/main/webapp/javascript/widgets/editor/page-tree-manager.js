/**
 * Page Tree Manager for Visual Content Editor
 * Handles loading and displaying the hierarchical page structure with drag-and-drop
 * 
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

class PageTreeManager {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.pages = [];
    this.expandedNodes = new Set();
    this.selectedPageId = null;
    this.loadedParents = new Set();
    this.isLoading = false;
    this.loadingNodes = new Set();
    this.childrenCache = new Map();
    this.searchResults = [];
    this.searchIndex = 0;
    this.searchQuery = '';
    this.pendingMutations = new Set();
    this.isSyncing = false;
    this.syncTimer = null;
    this.syncIntervalMs = 15000;
    this.isDirty = false;
    this.lastChangeTime = null;
    this.maxInactivityMs = 60000;
  }

  /**
   * Initialize the page tree manager
   */
  init() {
    this.setupEventListeners();
    this.setupSearchListener();
    this.setupLibraryDropZone();
    this.loadPages();
    this.startAutoSync();
  }

  normalizeParentId(parentId) {
    if (parentId === null || parentId === undefined) return null;
    const parentIdStr = String(parentId);
    if (parentIdStr === 'root' || parentIdStr === '-1' || parentIdStr === 'null') {
      return null;
    }
    return parentIdStr;
  }

  getChildrenKey(parentId) {
    return this.normalizeParentId(parentId);
  }

  getChildrenList(parentId) {
    const key = this.getChildrenKey(parentId);
    if (key === null) {
      return this.pages;
    }
    return this.childrenCache.get(key);
  }

  setChildrenList(parentId, children) {
    const key = this.getChildrenKey(parentId);
    if (key === null) {
      this.pages = children;
      this.childrenCache.set(null, children);
      return;
    }
    this.childrenCache.set(key, children);
  }

  findPageById(pageId) {
    const pageIdStr = String(pageId);
    const rootMatch = this.pages.find(page => String(page.id) === pageIdStr);
    if (rootMatch) return rootMatch;

    for (const children of this.childrenCache.values()) {
      if (!Array.isArray(children)) continue;
      const match = children.find(page => String(page.id) === pageIdStr);
      if (match) return match;
    }

    return null;
  }

  findParentId(pageId) {
    const pageIdStr = String(pageId);
    if (this.pages.some(page => String(page.id) === pageIdStr)) {
      return null;
    }

    for (const [parentId, children] of this.childrenCache.entries()) {
      if (!Array.isArray(children)) continue;
      if (children.some(page => String(page.id) === pageIdStr)) {
        return parentId;
      }
    }

    return null;
  }

  removePageFromCache(pageId) {
    const pageIdStr = String(pageId);
    let parentId = null;
    let removedPage = null;

    const rootIndex = this.pages.findIndex(page => String(page.id) === pageIdStr);
    if (rootIndex >= 0) {
      removedPage = this.pages.splice(rootIndex, 1)[0];
      parentId = null;
    } else {
      for (const [parentKey, children] of this.childrenCache.entries()) {
        if (!Array.isArray(children)) continue;
        const index = children.findIndex(page => String(page.id) === pageIdStr);
        if (index >= 0) {
          removedPage = children.splice(index, 1)[0];
          parentId = parentKey;
          break;
        }
      }
    }

    if (removedPage) {
      const removedKey = String(removedPage.id);
      this.childrenCache.delete(removedKey);
      this.loadedParents.delete(removedKey);
      this.expandedNodes.delete(removedKey);
    }

    return { parentId, removedPage };
  }

  insertPageIntoCache(page, parentId, position, referenceId) {
    const targetParentId = this.normalizeParentId(parentId);
    let children = this.getChildrenList(targetParentId);
    if (!Array.isArray(children)) {
      children = [];
      this.setChildrenList(targetParentId, children);
    }

    const referenceIdStr = referenceId ? String(referenceId) : null;
    let insertIndex = children.length;

    if (referenceIdStr) {
      const referenceIndex = children.findIndex(child => String(child.id) === referenceIdStr);
      if (referenceIndex >= 0) {
        insertIndex = position === 'before' ? referenceIndex : referenceIndex + 1;
      }
    }

    children.splice(insertIndex, 0, page);
    this.setChildrenList(targetParentId, children);
  }

  updateParentHasChildren(parentId) {
    const parentKey = this.normalizeParentId(parentId);
    if (parentKey === null) return;

    const parentPage = this.findPageById(parentKey);
    if (!parentPage) return;

    const children = this.childrenCache.get(parentKey) || [];
    parentPage.hasChildren = children.length > 0;
  }

  startAutoSync() {
    if (this.syncTimer) return;

    this.syncTimer = setInterval(() => this.syncFromServer(), this.syncIntervalMs);
    window.addEventListener('focus', () => this.syncFromServer());
  }

  isInactivityTimeout() {
    if (!this.lastChangeTime) return false;
    return Date.now() - this.lastChangeTime > this.maxInactivityMs;
  }

  markDirty() {
    this.isDirty = true;
    this.lastChangeTime = Date.now();
  }

  clearDirty() {
    this.isDirty = false;
  }

  touchActivity() {
    if (!this.isDirty) return;
    this.lastChangeTime = Date.now();
  }

  syncFromServer() {
    if (!this.isDirty) return;
    if (this.isInactivityTimeout()) {
      this.clearDirty();
      return;
    }
    if (this.isLoading || this.isSyncing || this.pendingMutations.size > 0) return;

    const requests = [];
    const shouldSyncRoot = this.expandedNodes.has('root') || this.pages.length === 0;

    if (shouldSyncRoot) {
      requests.push(this.refreshChildren(null, { render: false }));
    }

    this.expandedNodes.forEach(pageId => {
      if (pageId === 'root') return;
      if (this.pendingMutations.has(String(pageId))) return;
      requests.push(this.refreshChildren(pageId, { render: false }));
    });

    if (requests.length === 0) return;

    this.isSyncing = true;
    Promise.all(requests)
      .then(() => {
        this.renderTree();
      })
      .finally(() => {
        this.isSyncing = false;
      });
  }

  refreshChildren(parentId, options = {}) {
    const parentKey = this.normalizeParentId(parentId);
    const parentIdParam = parentKey === null ? 'null' : parentKey;

    return fetch(`/json/pages/children?parentId=${parentIdParam}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status !== 'ok' || !data.data) return;
        const children = Array.isArray(data.data) ? data.data : [];
        this.setChildrenList(parentKey, children);
        if (parentKey !== null) {
          this.loadedParents.add(parentKey);
        }
        this.updateParentHasChildren(parentKey);
        if (options.render !== false) {
          this.renderTree();
        }
      })
      .catch(error => {
        console.error('[PageTreeManager] Error refreshing children:', error);
      });
  }

  /**
   * Set up event listeners for page tree
   */
  setupEventListeners() {
    const pageTree = document.getElementById('page-tree');
    if (pageTree) {
      pageTree.addEventListener('click', (e) => this.handleTreeClick(e));
      pageTree.addEventListener('contextmenu', (e) => this.handleTreeContextMenu(e));
      pageTree.addEventListener('dragstart', (e) => this.handleDragStart(e));
      pageTree.addEventListener('dragover', (e) => this.handleDragOver(e));
      pageTree.addEventListener('drop', (e) => this.handleDrop(e));
      pageTree.addEventListener('dragend', (e) => this.handleDragEnd(e));
    }

    document.addEventListener('click', (e) => {
      if (!e.target.closest('.context-menu')) {
        this.hideContextMenu();
      }
    });
  }

  /**
   * Set up search/filter listener
   */
  setupSearchListener() {
    const searchInput = document.getElementById('pages-search');
    if (searchInput) {
      let debounceTimer;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          const query = e.target.value.toLowerCase().trim();
          this.updateSearch(query);
        }, 300);
      });

      searchInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
          e.preventDefault();
          this.focusNextSearchResult();
        }
      });
    }
  }

  /**
   * Load pages starting from root (parentId=null)
   */
  loadPages() {
    this.isLoading = true;
    this.showLoading();

    console.log('[PageTreeManager] Loading root pages...');

    fetch('/json/pages/children?parentId=null', {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        this.isLoading = false;
        this.hideLoading();

        console.log('[PageTreeManager] Root pages loaded:', data);

        if (data.status === 'ok' && data.data) {
          this.pages = Array.isArray(data.data) ? data.data : [];
          this.childrenCache.set(null, this.pages);
          // Expand root by default when initial pages load
          this.expandedNodes.add('root');
          console.log('[PageTreeManager] Pages set:', this.pages.length, 'Root expanded:', this.expandedNodes.has('root'));
          this.renderTree();
        } else {
          this.showError(data.error || 'Failed to load pages');
        }
      })
      .catch(error => {
        this.isLoading = false;
        this.hideLoading();
        console.error('Error loading pages:', error);
        this.showError('Error loading pages: ' + error.message);
      });
  }

  /**
   * Load children for a specific page node
   */
  loadPageChildren(parentId) {
    // Guard against undefined or invalid parentId
    if (!parentId || parentId === 'undefined') {
      console.error('[PageTreeManager] Invalid parentId for loading children:', parentId);
      return Promise.reject(new Error('Invalid parent ID'));
    }

    const parentIdStr = String(parentId);

    if (this.childrenCache.has(parentIdStr)) {
      console.log('[PageTreeManager] Children already cached for:', parentIdStr);
      this.loadedParents.add(parentIdStr);
      return Promise.resolve();
    }

    if (this.loadedParents.has(parentIdStr)) {
      console.log('[PageTreeManager] Children already loaded for:', parentIdStr);
      return Promise.resolve();
    }

    console.log('[PageTreeManager] Fetching children for parentId:', parentIdStr);
    this.loadingNodes.add(parentIdStr);
    this.renderTree();

    return fetch(`/json/pages/children?parentId=${parentId}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        this.loadedParents.add(parentIdStr);
        this.loadingNodes.delete(parentIdStr);

        console.log('[PageTreeManager] Children loaded for', parentIdStr, ':', data);

        if (data.status === 'ok' && data.data) {
          const children = Array.isArray(data.data) ? data.data : [];
          this.childrenCache.set(parentIdStr, children);
          console.log('[PageTreeManager] Cached', children.length, 'children for', parentIdStr);
          // Re-render tree to properly display children, toggle state, and container visibility
          this.renderTree();
        }
      })
      .catch(error => {
        this.loadingNodes.delete(parentIdStr);
        console.error('[PageTreeManager] Error loading page children:', error);
      });
  }

  /**
   * Render the entire tree
   */
  renderTree() {
    const pageTree = document.getElementById('page-tree');
    if (!pageTree) return;

    if (this.pages.length === 0) {
      pageTree.innerHTML = `
        <li class="page-tree-node root-node" data-page-id="root" draggable="false" data-droppable="true">
          <div class="page-tree-item root-item">
            <span class="tree-toggle empty"><i class="fa fa-folder"></i></span>
            <span class="page-icon"><i class="far fa-folder"></i></span>
            <span class="page-title" style="font-weight: 700;">Root</span>
          </div>
          <ul class="page-children" data-parent-id="root" style="min-height: 20px; list-style: none;">
            <li class="page-tree-empty">Drag pages here from the Page Library to organize the sitemap</li>
          </ul>
        </li>
      `;
      return;
    }

    // Wrap pages under Root element
    const isRootExpanded = this.expandedNodes.has('root');
    const expandedClass = isRootExpanded ? 'expanded' : '';
    const chevronIcon = isRootExpanded ? 'fa-chevron-down' : 'fa-chevron-right';
    const childrenStyle = isRootExpanded ? '' : 'style="display: none;"';

    console.log('[PageTreeManager] Rendering tree - Root expanded:', isRootExpanded, 'Pages count:', this.pages.length);

    const html = `
      <li class="page-tree-node root-node ${expandedClass}" data-page-id="root" draggable="false" data-droppable="true">
        <div class="page-tree-item root-item">
          <span class="tree-toggle ${expandedClass}" data-page-id="root"><i class="fa ${chevronIcon}"></i></span>
          <span class="page-icon"><i class="far fa-folder"></i></span>
          <span class="page-title" style="font-weight: 700;">Root</span>
        </div>
        <ul class="page-children" data-parent-id="root" style="list-style: none;${isRootExpanded ? '' : ' display: none;'}">
          ${this.pages.map(page => this.renderTreeNode(page, null)).join('')}
        </ul>
      </li>
    `;
    pageTree.innerHTML = html;
  }

  /**
   * Render individual tree node
   */
  renderTreeNode(page, parentId) {
    const selected = this.selectedPageId === page.id ? 'selected' : '';
    const draggable = 'draggable="true"';
    // Mark nodes as droppable to enable reordering
    const droppable = 'data-droppable="true"';

    let html = `
      <li class="page-tree-node ${selected}" data-page-id="${page.id}" data-parent-id="${parentId}" ${draggable} ${droppable}>
        <div class="page-tree-item">
    `;

    html += this.renderTreeToggle(page);

    html += `
          <span class="page-icon"><i class="far fa-file-lines"></i></span>
          <span class="page-title">${this.escapeHtml(page.title)}</span>
          <span class="page-link">${this.escapeHtml(page.link)}</span>
    `;

    if (page.draft_content) {
      html += `<span class="draft-badge">Draft</span>`;
    }

    html += `
        </div>
    `;

    html += this.renderTreeChildren(page);

    html += `</li>`;

    return html;
  }

  renderTreeToggle(page) {
    if (page.hasChildren) {
      const pageIdStr = String(page.id);
      const expanded = this.expandedNodes.has(pageIdStr) ? 'expanded' : '';
      const loading = this.loadingNodes.has(pageIdStr) ? 'loading' : '';
      let icon = 'fa-chevron-right';
      if (this.loadingNodes.has(pageIdStr)) {
        icon = 'fa-spinner fa-spin';
      } else if (expanded) {
        icon = 'fa-chevron-down';
      }
      return `<span class="tree-toggle ${expanded} ${loading}" data-page-id="${page.id}">
        <i class="fa ${icon}"></i>
      </span>`;
    }

    return `<span class="tree-toggle empty" data-page-id=""><i class="fa fa-circle" style="font-size: 6px;"></i></span>`;
  }

  renderTreeChildren(page) {
    if (!page.hasChildren) {
      return '';
    }

    const pageIdStr = String(page.id);
    const cachedChildren = this.childrenCache.get(pageIdStr);
    const isExpanded = this.expandedNodes.has(pageIdStr);

    console.log('[PageTreeManager] Rendering children for page', page.id, '- expanded:', isExpanded, 'cached:', cachedChildren?.length || 0);

    if (!isExpanded) {
      return `<ul class="page-children" data-parent-id="${page.id}" style="display: none; list-style: none;"></ul>`;
    }

    let html = `<ul class="page-children" data-parent-id="${page.id}" style="list-style: none;">`;
    if (cachedChildren && cachedChildren.length > 0) {
      html += cachedChildren.map(child => this.renderTreeNode(child, page.id)).join('');
    }
    html += `</ul>`;

    return html;
  }

  /**
   * Handle tree item click
   */
  handleTreeClick(e) {
    // Handle toggle click
    if (e.target.closest('.tree-toggle')) {
      const toggle = e.target.closest('.tree-toggle');
      const pageId = toggle.dataset.pageId;

      console.log('[PageTreeManager] Toggle clicked - pageId:', pageId, 'Currently expanded:', this.expandedNodes.has(pageId));

      // Skip if pageId is undefined (empty toggle) or invalid
      if (!pageId || pageId === 'undefined') {
        console.log('[PageTreeManager] Skipping empty toggle');
        return;
      }

      if (this.expandedNodes.has(pageId)) {
        console.log('[PageTreeManager] Collapsing node:', pageId);
        this.expandedNodes.delete(pageId);
        this.touchActivity();
      } else {
        console.log('[PageTreeManager] Expanding node:', pageId);
        this.expandedNodes.add(pageId);
        this.touchActivity();
        // Only load children for non-root nodes that have children
        if (pageId !== 'root') {
          this.loadPageChildren(pageId).then(() => {
            this.renderTree();
          });
          return;
        }
      }

      this.renderTree();
      return;
    }

    // Handle page item click
    const item = e.target.closest('.page-tree-item');
    if (!item) return;

    const node = item.closest('li');
    const pageId = node.dataset.pageId;

    // Update selected state
    document.querySelectorAll('.page-tree-node').forEach(el => el.classList.remove('selected'));
    node.classList.add('selected');

    this.selectedPageId = pageId;
    this.showPageDetailsFromNode(node);
  }

  showPageDetailsFromNode(node) {
    if (!node || node.dataset.pageId === 'root') return;

    const title = node.querySelector('.page-title')?.textContent || '';
    const link = node.querySelector('.page-link')?.textContent || '';
    const pageId = node.dataset.pageId;

    if (this.editorBridge && typeof this.editorBridge.showProperties === 'function') {
      this.editorBridge.showProperties({
        type: 'Page',
        title,
        id: pageId,
        link,
        fields: [
          { label: 'Title', value: title },
          { label: 'Link', value: link },
          { label: 'ID', value: pageId }
        ]
      });
    }

    if (this.editorBridge && typeof this.editorBridge.showPagePreview === 'function') {
      this.editorBridge.showPagePreview(link);
    }
  }

  /**
   * Handle drag start
   */
  handleDragStart(e) {
    const node = e.target.closest('.page-tree-node');
    if (!node) return;

    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/html', node.innerHTML);
    const title = node.querySelector('.page-title')?.textContent || '';
    const link = node.querySelector('.page-link')?.textContent || '';
    e.dataTransfer.setData('application/x-page-data', JSON.stringify({
      id: node.dataset.pageId,
      title,
      link
    }));
    node.classList.add('dragging');
    this.draggedElement = node;
  }

  /**
   * Handle drag over
   */
  handleDragOver(e) {
    e.preventDefault();
    const draggedNode = this.draggedElement || document.querySelector('[class*="dragging"]');
    const isLibraryDrag = draggedNode && draggedNode.classList.contains('page-hierarchy-box');
    const isTreeDrag = draggedNode && draggedNode.closest('#page-tree');
    const isSitemapDrag = draggedNode && (draggedNode.classList.contains('menu-item') || draggedNode.classList.contains('menu-tab'));

    // Allow drop from library (copy), tree (move/reorder), or sitemap (copy)
    if (isLibraryDrag || isSitemapDrag) {
      e.dataTransfer.dropEffect = 'copy';
    } else if (isTreeDrag) {
      e.dataTransfer.dropEffect = 'move';
    } else {
      e.dataTransfer.dropEffect = 'none';
    }

    // Clear previous drop zone indicators
    document.querySelectorAll('.drop-before, .drop-after, .drop-inside').forEach(el => {
      el.classList.remove('drop-before', 'drop-after', 'drop-inside');
    });

    const node = e.target.closest('.page-tree-node');
    if (!node) return;

    // Don't show drag-over on the dragged element itself
    if (this.draggedElement === node) return;

    // Calculate drop zone based on mouse Y position
    const dropZone = this.calculateDropZone(e, node);
    this.currentDropZone = dropZone;
    
    if (dropZone.position === 'before') {
      node.classList.add('drop-before');
    } else if (dropZone.position === 'after') {
      node.classList.add('drop-after');
    } else if (dropZone.position === 'inside') {
      node.classList.add('drop-inside');
    }
  }

  /**
   * Calculate drop zone based on mouse position relative to target element
   * Returns { position: 'before'|'after'|'inside', node }
   */
  calculateDropZone(e, node) {
    const item = node.querySelector('.page-tree-item');
    if (!item) return { position: 'after', node };

    const rect = item.getBoundingClientRect();
    const mouseY = e.clientY;
    const itemTop = rect.top;
    const itemBottom = rect.bottom;
    const itemHeight = rect.height;

    // Define zones: top 25% = before, bottom 25% = after, middle 50% = inside
    const topZone = itemTop + (itemHeight * 0.25);
    const bottomZone = itemBottom - (itemHeight * 0.25);

    // For root node, only allow 'inside' (as child)
    if (node.dataset.pageId === 'root') {
      return { position: 'inside', node };
    }

    if (mouseY < topZone) {
      return { position: 'before', node };
    } else if (mouseY > bottomZone) {
      return { position: 'after', node };
    } else {
      return { position: 'inside', node };
    }
  }

  /**
   * Handle drop
   */
  handleDrop(e) {
    e.preventDefault();

    let targetNode = e.target.closest('.page-tree-node');
    const draggedNode = this.draggedElement || document.querySelector('[class*="dragging"]');

    // If no node found, check if dropping on empty UL area
    if (!targetNode) {
      // Check if dropping on root UL area (empty drop zone) (#page-tree)
      if (e.target.id === 'page-tree') {
        targetNode = document.querySelector('#page-tree .root-node');
      }
    }

    if (!targetNode || !draggedNode) return;

    const isLibraryDrag = draggedNode.classList.contains('page-hierarchy-box');
    const isTreeDrag = draggedNode.closest('#page-tree');
    const isSitemapDrag = draggedNode.classList.contains('menu-item') || draggedNode.classList.contains('menu-tab');

    if (!isLibraryDrag && !isTreeDrag && !isSitemapDrag) {
      this.clearDropZones();
      return;
    }

    const draggedPageId = draggedNode.dataset.pageId;
    const targetPageId = targetNode.dataset.pageId;

    // Don't allow dropping on self
    if (draggedPageId === targetPageId) {
      this.clearDropZones();
      return;
    }

    // Get drop zone information
    const dropZone = this.currentDropZone || this.calculateDropZone(e, targetNode);

    if (isLibraryDrag || isSitemapDrag) {
      // Adding a page from library or sitemap to hierarchy
      // For sitemap items, try to get pageId from the page data
      let pageIdToAdd = draggedPageId;
      
      if (isSitemapDrag) {
        // Try to get page data from drag transfer
        try {
          const pageData = e.dataTransfer.getData('application/x-page-data');
          if (pageData) {
            const page = JSON.parse(pageData);
            pageIdToAdd = page.id;
          }
        } catch (err) {
          console.error('Error parsing page data from sitemap drag:', err);
        }
      }
      
      if (pageIdToAdd) {
        const parentId = targetPageId === 'root' ? -1 : targetPageId;
        this.addPageToHierarchy(pageIdToAdd, parentId);
      }
    } else if (isTreeDrag) {
      // Reordering pages within the tree with position
      this.reorderPageWithPosition(draggedPageId, targetPageId, dropZone.position);
    }

    this.clearDropZones();
  }

  /**
   * Clear all drop zone visual indicators
   */
  clearDropZones() {
    document.querySelectorAll('.drop-before, .drop-after, .drop-inside').forEach(el => {
      el.classList.remove('drop-before', 'drop-after', 'drop-inside');
    });
    this.currentDropZone = null;
  }

  /**
   * Handle drag end
   */
  handleDragEnd(e) {
    document.querySelectorAll('.page-tree-node, .page-hierarchy-box').forEach(node => {
      node.classList.remove('dragging', 'drag-over');
    });
    this.clearDropZones();
    this.draggedElement = null;
    const libraryExplorer = document.getElementById('page-library-explorer');
    if (libraryExplorer) {
      libraryExplorer.classList.remove('delete-drop');
    }
    document.body.style.cursor = '';
  }

  handleTreeContextMenu(e) {
    const node = e.target.closest('.page-tree-node');
    if (!node || node.dataset.pageId === 'root') return;

    e.preventDefault();

    const pageId = node.dataset.pageId;
    const title = node.querySelector('.page-title')?.textContent || 'Page';

    this.showContextMenu(e.clientX, e.clientY, [
      {
        label: 'Move to...',
        icon: 'fa-arrows',
        action: () => this.promptMovePage(pageId, title)
      },
      {
        label: 'Remove from Tree',
        icon: 'fa-trash',
        action: () => {
          if (confirm(`Remove "${title}" from the page tree?`)) {
            this.removePageFromHierarchy(pageId);
          }
        }
      }
    ]);
  }

  promptMovePage(pageId, title) {
    const targets = this.getMoveTargets(pageId);
    if (!targets.length) {
      this.showError('No available targets for move.');
      return;
    }

    const choices = targets.map((target, index) => `${index + 1}. ${target.label}`).join('\n');
    const selection = prompt(`Move "${title}" to:\n\n${choices}\n\nEnter number:`);
    if (!selection) return;

    const index = Number.parseInt(selection, 10) - 1;
    if (Number.isNaN(index) || index < 0 || index >= targets.length) {
      this.showError('Invalid selection.');
      return;
    }

    const target = targets[index];
    this.reorderPage(pageId, target.id);
  }

  getMoveTargets(excludedPageId) {
    const targets = [];

    targets.push({ id: -1, label: 'Root' });

    const nodes = document.querySelectorAll('#page-tree .page-tree-node');
    nodes.forEach(node => {
      const pageId = node.dataset.pageId;
      if (!pageId || pageId === 'root' || pageId === excludedPageId) return;

      const title = node.querySelector('.page-title')?.textContent || `Page ${pageId}`;
      targets.push({ id: pageId, label: title });
    });

    return targets;
  }

  showContextMenu(x, y, items) {
    this.hideContextMenu();

    const menu = document.createElement('div');
    menu.className = 'context-menu';
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';

    items.forEach(item => {
      const menuItem = document.createElement('div');
      menuItem.className = 'context-menu-item';
      menuItem.innerHTML = `
        <i class="far ${item.icon}"></i>
        <span>${this.escapeHtml(item.label)}</span>
      `;
      menuItem.addEventListener('click', () => {
        item.action();
        this.hideContextMenu();
      });
      menu.appendChild(menuItem);
    });

    document.body.appendChild(menu);
  }

  hideContextMenu() {
    const existingMenu = document.querySelector('.context-menu');
    if (existingMenu) {
      existingMenu.remove();
    }
  }

  setupLibraryDropZone() {
    const libraryExplorer = document.getElementById('page-library-explorer');
    if (!libraryExplorer) return;

    libraryExplorer.addEventListener('dragover', (e) => {
      e.preventDefault();
      libraryExplorer.classList.add('delete-drop');
      e.dataTransfer.dropEffect = 'move';
      document.body.style.cursor = 'not-allowed';
    });

    libraryExplorer.addEventListener('dragleave', () => {
      libraryExplorer.classList.remove('delete-drop');
      document.body.style.cursor = '';
    });

    libraryExplorer.addEventListener('drop', (e) => {
      e.preventDefault();
      libraryExplorer.classList.remove('delete-drop');
      document.body.style.cursor = '';

      const draggedNode = document.querySelector('.page-tree-node.dragging');
      if (!draggedNode || !draggedNode.closest('#page-tree')) return;

      const pageId = draggedNode.dataset.pageId;
      if (pageId && pageId !== 'root') {
        this.removePageFromHierarchy(pageId);
      }
    });
  }

  /**
   * Reorder page (drag and drop)
   */
  reorderPage(pageId, targetPageId) {
    this.markDirty();
    const params = new FormData();
    params.append('pageId', pageId);
    params.append('targetPageId', targetPageId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/pages/reorder', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.refreshChildren(null);
        } else {
          this.showError(data.error || 'Failed to reorder pages');
        }
      })
      .catch(error => {
        console.error('Error reordering pages:', error);
        this.showError('Error reordering pages: ' + error.message);
      });
  }

  /**
   * Reorder page with specific position (before/after/inside)
   */
  reorderPageWithPosition(pageId, targetPageId, position) {
    this.markDirty();
    const targetNode = document.querySelector(`.page-tree-node[data-page-id="${targetPageId}"]`);
    const targetParentId = targetNode ? targetNode.dataset.parentId : null;
    const originalParentId = this.findParentId(pageId);
    const movedPage = this.removePageFromCache(pageId).removedPage;

    if (movedPage) {
      const newParentId = position === 'inside' ? targetPageId : targetParentId;
      const referenceId = position === 'inside' ? null : targetPageId;
      this.insertPageIntoCache(movedPage, newParentId, position, referenceId);
      this.updateParentHasChildren(originalParentId);
      this.updateParentHasChildren(newParentId);
      if (newParentId) {
        this.expandedNodes.add(String(newParentId));
      }
      this.renderTree();
    }

    const originalParentKey = this.normalizeParentId(originalParentId);
    const newParentKey = this.normalizeParentId(position === 'inside' ? targetPageId : targetParentId);
    if (originalParentKey !== null) {
      this.pendingMutations.add(String(originalParentKey));
    }
    if (newParentKey !== null) {
      this.pendingMutations.add(String(newParentKey));
    }

    const params = new FormData();
    params.append('pageId', pageId);
    params.append('targetPageId', targetPageId);
    params.append('position', position); // 'before', 'after', or 'inside'
    params.append('token', globalThis.getFormToken());

    fetch('/json/pages/reorder', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          const refreshes = [];
          refreshes.push(this.refreshChildren(originalParentId, { render: false }));
          refreshes.push(this.refreshChildren(position === 'inside' ? targetPageId : targetParentId, { render: false }));
          Promise.all(refreshes).finally(() => this.renderTree());
        } else {
          this.showError(data.error || 'Failed to reorder pages');
          this.refreshChildren(originalParentId);
        }
      })
      .catch(error => {
        console.error('Error reordering pages:', error);
        this.showError('Error reordering pages: ' + error.message);
        this.refreshChildren(originalParentId);
      })
      .finally(() => {
        if (originalParentKey !== null) {
          this.pendingMutations.delete(String(originalParentKey));
        }
        if (newParentKey !== null) {
          this.pendingMutations.delete(String(newParentKey));
        }
      });
  }

  addPageToHierarchy(pageId, parentPageId) {
    this.markDirty();
    const parentKey = this.normalizeParentId(parentPageId);
    if (parentKey !== null) {
      this.pendingMutations.add(String(parentKey));
    }

    const params = new FormData();
    params.append('pageId', pageId);
    params.append('parentPageId', parentPageId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/pages/add-to-hierarchy', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          const message = data?.data?.message ? String(data.data.message) : '';
          if (message && message.toLowerCase().includes('already in hierarchy')) {
            this.showError(message);
            return;
          }
          this.refreshChildren(parentPageId);
        } else {
          this.showError(data.error || 'Failed to add page to hierarchy');
        }
      })
      .catch(error => {
        console.error('Error adding page to hierarchy:', error);
        this.showError('Error adding page to hierarchy: ' + error.message);
      })
      .finally(() => {
        if (parentKey !== null) {
          this.pendingMutations.delete(String(parentKey));
        }
      });
  }

  removePageFromHierarchy(pageId) {
    this.markDirty();
    const { parentId } = this.removePageFromCache(pageId);
    if (parentId !== null && parentId !== undefined) {
      this.pendingMutations.add(String(parentId));
    }
    this.updateParentHasChildren(parentId);
    this.renderTree();

    const params = new FormData();
    params.append('pageId', pageId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/pages/remove-from-hierarchy', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.refreshChildren(parentId);
        } else {
          this.showError(data.error || 'Failed to remove page from hierarchy');
          this.refreshChildren(parentId);
        }
      })
      .catch(error => {
        console.error('Error removing page from hierarchy:', error);
        this.showError('Error removing page from hierarchy: ' + error.message);
        this.refreshChildren(parentId);
      })
      .finally(() => {
        if (parentId !== null && parentId !== undefined) {
          this.pendingMutations.delete(String(parentId));
        }
      });
  }

  /**
   * Update search results and tree visibility
   */
  updateSearch(query) {
    this.searchQuery = query;
    this.searchIndex = 0;
    this.searchResults = this.collectSearchResults(query);

    this.applySearchFilter(this.searchResults, query);

    if (this.searchResults.length > 0) {
      this.focusSearchResult(0);
      return;
    }

    this.updateSearchIndicator();
  }

  collectSearchResults(query) {
    const results = [];
    const items = document.querySelectorAll('.page-tree-item');

    items.forEach(item => {
      const node = item.closest('.page-tree-node');
      if (!node || node.dataset.pageId === 'root') return;

      const title = item.querySelector('.page-title')?.textContent.toLowerCase() || '';
      const link = item.querySelector('.page-link')?.textContent.toLowerCase() || '';

      if (!query || title.includes(query) || link.includes(query)) {
        results.push(node);
      }
    });

    return results;
  }

  applySearchFilter(results, query) {
    const nodes = document.querySelectorAll('.page-tree-node');

    if (!query) {
      nodes.forEach(node => {
        node.style.display = '';
      });
      return;
    }

    const visibleNodes = new Set();
    const rootNode = document.querySelector('#page-tree .root-node');
    if (rootNode) {
      visibleNodes.add(rootNode);
    }

    results.forEach(node => {
      visibleNodes.add(node);
      this.expandParentsForNode(node, visibleNodes);
    });

    nodes.forEach(node => {
      node.style.display = visibleNodes.has(node) ? '' : 'none';
    });
  }

  expandParentsForNode(node, visibleNodes) {
    let current = node.parentElement;
    while (current) {
      const parentNode = current.closest('.page-tree-node');
      if (!parentNode) break;

      visibleNodes.add(parentNode);
      const parentId = parentNode.dataset.pageId;
      if (parentId && parentId !== 'root') {
        this.expandedNodes.add(String(parentId));
      } else if (parentId === 'root') {
        this.expandedNodes.add('root');
      }

      const toggle = parentNode.querySelector(':scope > .page-tree-item .tree-toggle');
      if (toggle && !toggle.classList.contains('loading')) {
        toggle.classList.add('expanded');
        const icon = toggle.querySelector('i');
        if (icon) {
          icon.className = 'fa fa-chevron-down';
        }
      }

      const childrenContainer = parentNode.querySelector(':scope > .page-children');
      if (childrenContainer) {
        childrenContainer.style.display = '';
      }

      current = parentNode.parentElement;
    }
  }

  focusNextSearchResult() {
    if (!this.searchResults.length) {
      this.updateSearchIndicator();
      return;
    }

    this.searchIndex = (this.searchIndex + 1) % this.searchResults.length;
    this.focusSearchResult(this.searchIndex);
  }

  focusSearchResult(index) {
    const node = this.searchResults[index];
    if (!node) return;

    this.searchResults.forEach(result => result.classList.remove('selected'));
    node.classList.add('selected');

    node.scrollIntoView({ block: 'center', behavior: 'smooth' });
    this.showPageDetailsFromNode(node);
    this.touchActivity();
    this.updateSearchIndicator();
  }

  updateSearchIndicator() {
    const indicator = document.getElementById('pages-search-indicator');
    if (!indicator) return;

    if (!this.searchQuery) {
      indicator.style.display = 'none';
      indicator.textContent = '';
      return;
    }

    const total = this.searchResults.length;
    const current = total ? (this.searchIndex + 1) : 0;
    indicator.textContent = `${current} of ${total}`;
    indicator.style.display = 'block';
  }

  /**
   * Show error message
   */
  showError(message) {
    const errorDiv = document.getElementById('pages-error');
    if (errorDiv) {
      errorDiv.textContent = message;
      errorDiv.style.display = 'block';
    }
  }

  /**
   * Escape HTML special characters
   */
  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Refresh page tree
   */
  refresh() {
    this.expandedNodes.clear();
    this.loadedParents.clear();
    this.loadingNodes.clear();
    this.childrenCache.clear();
    this.loadPages();
  }

  showLoading() {
    const loadingIndicator = document.getElementById('loading-indicator');
    if (loadingIndicator) {
      loadingIndicator.style.display = 'inline-block';
    }
  }

  hideLoading() {
    const loadingIndicator = document.getElementById('loading-indicator');
    if (loadingIndicator) {
      loadingIndicator.style.display = 'none';
    }
  }

}
