/**
 * Sitemap Explorer for Visual Content Editor
 * Handles rendering and interaction with the site menu structure and page hierarchy
 * 
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

class SitemapExplorer {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.sitemapData = null;
    this.draggedElement = null;
    this.svgCanvas = null;
    // Horizontal layout configuration
    this.tabSpacing = 180;     // Horizontal spacing between tabs
    this.itemVerticalSpacing = 80;    // Vertical spacing between stacked items
    this.itemVerticalOffset = 135; // Vertical distance from tab to first item
    this.layoutTop = 70; // Top offset for the diagram layout
    // Zoom configuration
    this.zoomLevel = 1.0;
    this.minZoom = 0.5;
    this.maxZoom = 2.0;
    this.zoomStep = 0.1;
    // View state tracking for preserving scroll position during sync
    this.isInitialized = false;
    this.savedScrollLeft = 0;
    this.savedScrollTop = 0;
    this.savedTransformOrigin = 'top left';
    // Sync state tracking
    this.isSyncing = false;
    this.isDirty = false;
    this.syncTimer = null;
    this.syncIntervalMs = 15000;
    this.lastChangeTime = null;
    this.maxInactivityMs = 60000; // Stop syncing after 1 minute of no activity
    this.tabIsVisible = false;
  }

  /**
   * Initialize the sitemap explorer
   */
  init() {
    this.setupEventListeners();
    this.loadSitemapData();
    this.setupContextMenus();
    this.setupTabVisibilityObserver();
    this.startAutoSync();
  }

  /**
   * Setup observer to detect when the Pages/Site Navigation tab becomes visible or hidden
   */
  setupTabVisibilityObserver() {
    let previouslyVisible = false;

    // Check if there's an element that indicates this tab is active
    // This looks for a tab button or panel with aria-selected or active class
    const checkTabVisibility = () => {
      const sitemapContainer = document.getElementById('sitemap-explorer');
      if (!sitemapContainer) {
        this.tabIsVisible = false;
        return;
      }

      // Check if container is visible in the DOM and displayed
      const currentlyVisible = sitemapContainer.offsetParent !== null &&
        getComputedStyle(sitemapContainer).display !== 'none';

      // Trigger callbacks when visibility changes
      if (currentlyVisible !== previouslyVisible) {
        previouslyVisible = currentlyVisible;
        if (currentlyVisible) {
          this.onTabVisible();
        } else {
          this.onTabHidden();
        }
      }

      this.tabIsVisible = currentlyVisible;
    };

    // Initial check
    checkTabVisibility();

    // Set up a MutationObserver to detect changes to the container's visibility
    const observer = new MutationObserver(() => {
      checkTabVisibility();
    });

    const sitemapContainer = document.getElementById('sitemap-explorer');
    if (sitemapContainer) {
      observer.observe(sitemapContainer, {
        attributes: true,
        attributeFilter: ['style', 'class'],
        subtree: true
      });
    }

    // Also check periodically in case visibility changes via CSS or parent visibility
    setInterval(() => {
      checkTabVisibility();
    }, 1000);
  }

  /**
   * Check if the tab has been inactive for too long
   */
  isInactivityTimeout() {
    if (!this.lastChangeTime) {
      return false;
    }
    const timeSinceChange = Date.now() - this.lastChangeTime;
    return timeSinceChange > this.maxInactivityMs;
  }

  /**
   * Mark that a change has been made to the sitemap
   */
  markDirty() {
    this.isDirty = true;
    this.lastChangeTime = Date.now();
  }

  /**
   * Clear the dirty flag and reset for next sync cycle
   */
  clearDirty() {
    this.isDirty = false;
  }

  /**
   * Start auto-sync timer to keep sitemap in sync
   */
  startAutoSync() {
    // Sync every 15 seconds, but only if:
    // 1. The Pages/Site Navigation tab is visible
    // 2. Changes have been made to the sitemap
    // 3. Not already syncing
    // 4. Haven't hit inactivity timeout
    this.syncTimer = setInterval(() => {
      // Only sync if tab is visible, there are changes, and we're not already syncing
      if (this.tabIsVisible && this.isDirty && !this.isSyncing) {
        // Check inactivity timeout - if inactive too long, stop syncing
        if (this.isInactivityTimeout()) {
          this.clearDirty();
          return;
        }

        this.loadSitemapData();
      }
    }, this.syncIntervalMs);
  }

  /**
   * Stop auto-sync timer
   */
  stopAutoSync() {
    if (this.syncTimer) {
      clearInterval(this.syncTimer);
      this.syncTimer = null;
    }
  }

  /**
   * Mark sync in progress
   */
  setSyncing(isSyncing) {
    this.isSyncing = isSyncing;
  }

  /**
   * Set up event listeners
   */
  setupEventListeners() {
    // Close context menu on click outside
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.context-menu')) {
        this.hideContextMenu();
      }
    });
  }

  /**
   * Setup context menu handlers
   */
  setupContextMenus() {
    document.addEventListener('contextmenu', (e) => {
      const menuTab = e.target.closest('.menu-tab');
      const menuItem = e.target.closest('.menu-item');

      if (menuTab || menuItem) {
        e.preventDefault();

        if (menuTab) {
          this.showTabContextMenu(e, menuTab);
        } else if (menuItem) {
          this.showItemContextMenu(e, menuItem);
        }
      }
    });
  }

  /**
   * Load sitemap data from server
   */
  loadSitemapData() {
    this.setSyncing(true);
    fetch('/json/sitemap/structure', {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        this.setSyncing(false);
        if (data.status === 'ok' && data.data) {
          this.sitemapData = data.data;
          // Only sync if tab is currently visible
          if (this.tabIsVisible) {
            // Use sync for subsequent refreshes to preserve scroll position
            if (this.isInitialized) {
              this.syncSitemap();
            } else {
              this.renderSitemap();
              this.isInitialized = true;
            }
            // Clear dirty flag only after successful sync when tab is visible
            this.clearDirty();
          } else {
            // Tab is not visible, mark data as received but don't render yet
            if (!this.isInitialized) {
              this.isInitialized = true;
            }
          }
        } else {
          this.showError(data.error || 'Failed to load sitemap');
        }
      })
      .catch(error => {
        this.setSyncing(false);
        console.error('Error loading sitemap:', error);
        this.showError('Error loading sitemap: ' + error.message);
      });
  }

  /**
   * Save current view state (scroll position, zoom, etc.)
   */
  saveViewState() {
    const container = document.getElementById('sitemap-explorer');
    if (container) {
      this.savedScrollLeft = container.scrollLeft;
      this.savedScrollTop = container.scrollTop;
    }
  }

  /**
   * Restore view state to preserved scroll position and zoom
   */
  restoreViewState() {
    const container = document.getElementById('sitemap-explorer');
    if (container) {
      // Restore scroll position immediately after DOM updates
      // Use requestAnimationFrame to ensure restoration after browser repaint
      requestAnimationFrame(() => {
        container.scrollLeft = this.savedScrollLeft;
        container.scrollTop = this.savedScrollTop;
      });
    }
  }

  /**
   * Sync sitemap data without redrawing - updates DOM in place to preserve view state
   */
  syncSitemap() {
    // Save current view state before update
    this.saveViewState();

    const tabsDiv = document.querySelector('.menu-tabs-arc');
    if (!tabsDiv) {
      // If no tabs container found, fall back to full render
      this.renderSitemap();
      return;
    }

    // Update menu tabs and items in place
    this.syncMenuTabs(tabsDiv);

    // Redraw connections to reflect any structural changes
    setTimeout(() => this.drawConnections(), 50);

    // Restore view state after updates
    this.restoreViewState();
  }

  /**
   * Sync menu tabs - update tabs and items in place without clearing container
   */
  syncMenuTabs(tabsDiv) {
    if (!this.sitemapData || !this.sitemapData.menuTabs) {
      return;
    }

    const tabs = Array.isArray(this.sitemapData.menuTabs) ? [...this.sitemapData.menuTabs] : [];
    const homeIndex = tabs.findIndex((tab) => this.isHomeTab(tab));
    if (homeIndex > 0) {
      const [homeTab] = tabs.splice(homeIndex, 1);
      tabs.unshift(homeTab);
    }

    // Update existing tabs and add new ones
    const existingTabs = Array.from(tabsDiv.querySelectorAll('.menu-tab:not(.empty-tab-target)'));

    tabs.forEach((tab, index) => {
      const isHome = this.isHomeTab(tab);
      const x = 20 + (index * this.tabSpacing);
      const y = 0;

      let tabEl = existingTabs[index];

      if (!tabEl || tabEl.dataset.tabId !== tab.id) {
        // Tab was added or order changed - recreate from this point
        this.renderSitemap();
        return;
      }

      // Update tab content
      const tabMeta = tab.link ? this.escapeHtml(tab.link) : (tab.items ? tab.items.length : 0);
      tabEl.innerHTML = `
        <div class="tab-content">
          <span class="tab-title">${this.escapeHtml(tab.title)}</span>
          <span class="tab-count">${tabMeta}</span>
        </div>
      `;

      // Update position in case zoom changed
      tabEl.style.left = `${x}px`;
      tabEl.style.top = `${this.layoutTop + y}px`;
      tabEl.dataset.x = x;
      tabEl.dataset.y = y;

      // Update menu items for this tab
      if (!isHome) {
        this.syncMenuItems(tab);
      }
    });

    // If tab count changed, rebuild from scratch
    if (existingTabs.length !== tabs.length) {
      this.renderSitemap();
      return;
    }

    // Update empty tab position if needed
    const emptyTabEl = tabsDiv.querySelector('.empty-tab-target');
    if (emptyTabEl) {
      const emptyTabIndex = tabs.length;
      const emptyTabX = 20 + (emptyTabIndex * this.tabSpacing);
      emptyTabEl.style.left = `${emptyTabX}px`;
      emptyTabEl.dataset.x = emptyTabX;
    }
  }

  /**
   * Sync menu items for a tab - update items in place
   */
  syncMenuItems(tab) {
    if (!tab.items || tab.items.length === 0) {
      return;
    }

    const itemsContainer = document.querySelector(`.menu-items-container[data-tab-id="${tab.id}"]`);
    if (!itemsContainer) {
      return;
    }

    const existingItems = Array.from(itemsContainer.querySelectorAll('.menu-item:not(.empty-item-target)'));
    const tabEl = document.querySelector(`.menu-tab[data-tab-id="${tab.id}"]`);
    const tabCenterX = parseFloat(tabEl.dataset.x || 0) + (tabEl.offsetWidth / 2);

    // Update existing items and add new ones
    tab.items.forEach((item, index) => {
      const itemY = parseFloat(tabEl.dataset.y || 0) + this.itemVerticalOffset + (index * this.itemVerticalSpacing);

      let itemEl = existingItems[index];

      if (!itemEl || itemEl.dataset.itemId !== item.id) {
        // Item was added or removed - rebuild from scratch
        this.renderSitemap();
        return;
      }

      // Update item content
      itemEl.innerHTML = `
        <div class="item-content">
          <span class="item-title">${this.escapeHtml(item.title)}</span>
          <span class="item-link">${this.escapeHtml(item.link)}</span>
        </div>
      `;

      // Update position
      itemEl.style.left = `${tabCenterX}px`;
      itemEl.style.top = `${this.layoutTop + itemY}px`;
      itemEl.dataset.centerX = tabCenterX;
      itemEl.dataset.y = itemY;
    });

    // If item count changed, rebuild from scratch
    if (existingItems.length !== tab.items.length) {
      this.renderSitemap();
      return;
    }

    // Update empty item position if needed
    if (tab.items.length > 0) {
      const emptyItemEl = itemsContainer.querySelector('.empty-item-target');
      if (emptyItemEl) {
        const emptyItemIndex = tab.items.length;
        const emptyItemY = parseFloat(tabEl.dataset.y || 0) + this.itemVerticalOffset + (emptyItemIndex * this.itemVerticalSpacing);
        emptyItemEl.style.top = `${this.layoutTop + emptyItemY}px`;
        emptyItemEl.dataset.y = emptyItemY;
      }
    }
  }

  /**
   * Render the sitemap visualization
   */
  renderSitemap() {
    const container = document.getElementById('sitemap-explorer');
    if (!container) return;

    container.innerHTML = '';
    container.style.display = 'block';
    container.style.position = 'relative';

    // Create SVG canvas for drawing connections
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.setAttribute('class', 'sitemap-canvas');
    svg.style.position = 'absolute';
    svg.style.top = '0';
    svg.style.left = '0';
    svg.style.pointerEvents = 'none';
    container.appendChild(svg);
    this.svgCanvas = svg;

    // Create zoom controls (before main canvas so they stay fixed)
    this.createZoomControls(container);

    // Create main container with graph paper background
    const mainDiv = document.createElement('div');
    mainDiv.className = 'sitemap-main';
    mainDiv.id = 'sitemap-main-canvas';
    mainDiv.style.transformOrigin = 'top left';
    mainDiv.style.minWidth = '100%';
    mainDiv.style.minHeight = '100%';
    container.appendChild(mainDiv);

    // Add drop support to main canvas for creating new tabs from pages
    mainDiv.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'copy';
    });

    mainDiv.addEventListener('drop', (e) => {
      e.preventDefault();
      const pageData = e.dataTransfer.getData('application/x-page-data');
      if (pageData) {
        try {
          const page = JSON.parse(pageData);
          if (page && page.title) {
            // Create a new tab from the dropped page
            this.createMenuTab({ title: page.title, link: this.resolvePageLink(page) });
          }
        } catch (error) {
          console.error('Error parsing page data:', error);
        }
      }
    });

    // Render menu tabs on arc
    this.renderMenuTabs(mainDiv);

    // Update background after tabs and items are rendered
    this.updateBackgroundSize();

    // Draw connections after elements are rendered
    setTimeout(() => this.drawConnections(), 50);

    // Redraw connections on scroll and resize
    container.addEventListener('scroll', () => this.drawConnections());
    window.addEventListener('resize', () => this.drawConnections());

    // Add event listeners for toolbar buttons
    this.setupToolbarButtons();

    // If this render is part of a sync operation, restore scroll position after DOM is complete
    if (this.isInitialized && (this.savedScrollLeft > 0 || this.savedScrollTop > 0)) {
      requestAnimationFrame(() => {
        container.scrollLeft = this.savedScrollLeft;
        container.scrollTop = this.savedScrollTop;
      });
    }
  }

  /**
   * Add new menu tab
   */
  addMenuTab() {
    this.showAddMenuTabModal();
  }

  /**
   * Show modal for adding a new menu tab
   */
  showAddMenuTabModal() {
    // Prevent multiple modals from being created
    if (document.querySelector('.add-menu-tab-modal')) {
      return;
    }

    // Create modal overlay
    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
    `;

    // Create modal content
    const modal = document.createElement('div');
    modal.className = 'add-menu-tab-modal';
    modal.style.cssText = `
      background: var(--editor-panel-bg, #fff);
      border-radius: 8px;
      padding: 24px;
      min-width: 400px;
      max-width: 500px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
      color: var(--editor-text, #333);
    `;

    modal.innerHTML = `
      <h3 style="margin: 0 0 20px 0; font-size: 18px; color: var(--editor-text, #333);">
        <i class="far fa-plus-circle"></i> Add Menu Tab
      </h3>
      <form id="add-menu-tab-form">
        <div style="margin-bottom: 16px;">
          <label style="display: block; margin-bottom: 6px; font-weight: 600; font-size: 14px;">Title *</label>
          <input type="text" id="menu-tab-title" required
            style="width: 100%; padding: 8px 12px; border: 1px solid var(--editor-border, #ddd); border-radius: 4px; font-size: 14px; background: var(--editor-input-bg, #fff); color: var(--editor-text, #333); box-sizing: border-box;"
            placeholder="Enter menu tab title">
        </div>
        <div style="margin-bottom: 20px;">
          <label style="display: block; margin-bottom: 6px; font-weight: 600; font-size: 14px;">Link</label>
          <input type="text" id="menu-tab-link"
            style="width: 100%; padding: 8px 12px; border: 1px solid var(--editor-border, #ddd); border-radius: 4px; font-size: 14px; background: var(--editor-input-bg, #fff); color: var(--editor-text, #333); box-sizing: border-box;"
            placeholder="/page-link">
          <small style="display: block; margin-top: 4px; color: var(--editor-muted-text, #666); font-size: 12px;">
            Required. Starts with /
          </small>
          <div id="link-error" style="display: none; margin-top: 4px; color: #d32f2f; font-size: 12px;"></div>
        </div>
        <div style="display: flex; gap: 10px; justify-content: flex-end;">
          <button type="button" id="cancel-btn"
            style="padding: 8px 16px; border: 1px solid var(--editor-border, #ddd); border-radius: 4px; background: var(--editor-panel-bg, #fff); color: var(--editor-text, #333); cursor: pointer; font-size: 14px;">
            Cancel
          </button>
          <button type="submit" id="submit-btn"
            style="padding: 8px 16px; border: none; border-radius: 4px; background: #2196F3; color: #fff; cursor: pointer; font-size: 14px; font-weight: 600;">
            Add Menu Tab
          </button>
        </div>
      </form>
    `;

    overlay.appendChild(modal);
    document.body.appendChild(overlay);

    // Focus on title input
    const titleInput = document.getElementById('menu-tab-title');
    const linkInput = document.getElementById('menu-tab-link');
    const linkError = document.getElementById('link-error');
    const form = document.getElementById('add-menu-tab-form');

    setTimeout(() => titleInput?.focus(), 100);

    // Validate link input on change
    linkInput?.addEventListener('input', () => {
      const link = linkInput.value.trim();
      if (!link || !link.startsWith('/')) {
        linkError.textContent = 'Link must start with /';
        linkError.style.display = 'block';
      } else {
        linkError.style.display = 'none';
      }
    });

    // Handle form submission
    const handleSubmit = (e) => {
      e.preventDefault();

      const title = titleInput.value.trim();
      const link = linkInput.value.trim();

      if (!title) {
        alert('Title is required');
        return;
      }

      if (!link || !link.startsWith('/')) {
        linkError.textContent = 'Link must start with /';
        linkError.style.display = 'block';
        return;
      }

      // Create the menu tab
      const data = { title };
      if (link) {
        data.link = link;
      }

      this.createMenuTab(data);
      document.body.removeChild(overlay);
    };

    form?.addEventListener('submit', handleSubmit);

    // Handle cancel
    const cancelBtn = document.getElementById('cancel-btn');
    cancelBtn?.addEventListener('click', () => {
      document.body.removeChild(overlay);
    });

    // Close on overlay click
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) {
        document.body.removeChild(overlay);
      }
    });

    // Close on Escape key
    const handleEscape = (e) => {
      if (e.key === 'Escape') {
        document.body.removeChild(overlay);
        document.removeEventListener('keydown', handleEscape);
      }
    };
    document.addEventListener('keydown', handleEscape);
  }

  /**
   * Setup toolbar buttons in title bar
   */
  setupToolbarButtons() {
    const addMenuTabBtn = document.getElementById('add-menu-tab-btn');
    const refreshSitemapBtn = document.getElementById('refresh-sitemap-btn');

    if (addMenuTabBtn) {
      // Remove old listener if it exists to prevent duplicates
      if (this._addMenuTabHandler) {
        addMenuTabBtn.removeEventListener('click', this._addMenuTabHandler);
      }
      this._addMenuTabHandler = () => this.addMenuTab();
      addMenuTabBtn.addEventListener('click', this._addMenuTabHandler);
    }

    if (refreshSitemapBtn) {
      // Remove old listener if it exists to prevent duplicates
      if (this._refreshSitemapHandler) {
        refreshSitemapBtn.removeEventListener('click', this._refreshSitemapHandler);
      }
      this._refreshSitemapHandler = () => this.refresh();
      refreshSitemapBtn.addEventListener('click', this._refreshSitemapHandler);
    }
  }

  /**
   * Create menu tab on server
   */
  createMenuTab(data) {
    const params = new FormData();
    params.append('title', data.title);
    params.append('link', data.link);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/create-tab', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to create menu tab');
        }
      })
      .catch(error => {
        console.error('Error creating menu tab:', error);
        this.showError('Error creating menu tab: ' + error.message);
      });
  }

  /**
   * Render menu tabs in horizontal layout
   */
  renderMenuTabs(container) {
    if (!this.sitemapData || !this.sitemapData.menuTabs) {
      return;
    }

    const tabsDiv = document.createElement('div');
    tabsDiv.className = 'menu-tabs-arc';
    container.appendChild(tabsDiv);

    const tabs = Array.isArray(this.sitemapData.menuTabs) ? [...this.sitemapData.menuTabs] : [];

    tabs.forEach((tab, index) => {
      const isHome = this.isHomeTab(tab);
      // Horizontal layout: position left to right starting from left edge
      const x = 20 + (index * this.tabSpacing);
      const y = 0; // Top row

      const tabEl = document.createElement('div');
      tabEl.className = isHome ? 'menu-tab home-tab' : 'menu-tab draggable-tab';
      tabEl.draggable = !isHome;
      tabEl.dataset.tabId = tab.id;
      tabEl.dataset.x = x;
      tabEl.dataset.y = y;
      // Position from left edge
      tabEl.style.left = `${x}px`;
      tabEl.style.top = `${this.layoutTop + y}px`;

      const tabMeta = tab.link ? this.escapeHtml(tab.link) : (tab.items ? tab.items.length : 0);
      tabEl.innerHTML = `
        <div class="tab-content">
          <span class="tab-title">${this.escapeHtml(tab.title)}</span>
          <span class="tab-count">${tabMeta}</span>
        </div>
      `;

      if (!isHome) {
        tabEl.addEventListener('dragstart', (e) => this.handleTabDragStart(e, tab));
        tabEl.addEventListener('dragover', (e) => {
          e.preventDefault();
          tabEl.classList.add('drag-over');
        });
        tabEl.addEventListener('dragleave', () => {
          tabEl.classList.remove('drag-over');
        });
        tabEl.addEventListener('drop', (e) => {
          tabEl.classList.remove('drag-over');
          this.handleTabDrop(e, tab);
        });
        tabEl.addEventListener('dragend', () => {
          tabEl.classList.remove('dragging');
        });
      }

      tabEl.addEventListener('click', () => this.selectTab(tab));

      tabsDiv.appendChild(tabEl);

      const tabWidth = tabEl.offsetWidth || 0;
      const tabCenterX = x + (tabWidth / 2);

      // Render menu items under this tab
      if (!isHome) {
        this.renderMenuItems(container, tab, tabCenterX, y);
      }
    });

    // Always render an empty tab as a drop target for new tabs
    const emptyTabIndex = tabs.length;
    const emptyTabX = 20 + (emptyTabIndex * this.tabSpacing);
    const emptyTabY = 0;

    const emptyTabEl = document.createElement('div');
    emptyTabEl.className = 'menu-tab empty-tab-target';
    emptyTabEl.dataset.isEmptyTab = 'true';
    emptyTabEl.dataset.x = emptyTabX;
    emptyTabEl.dataset.y = emptyTabY;
    // Important: do NOT set draggable to true
    emptyTabEl.style.left = `${emptyTabX}px`;
    emptyTabEl.style.top = `${this.layoutTop + emptyTabY}px`;
    emptyTabEl.style.opacity = '0.5';
    emptyTabEl.innerHTML = `
      <div class="tab-content">
        <span class="tab-title" style="color: var(--editor-muted-text, #999);">+ New Tab</span>
        <span class="tab-count">0</span>
      </div>
    `;

    // Empty tabs accept drops but are not draggable
    emptyTabEl.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'copy';
      emptyTabEl.classList.add('drag-over');
    });
    emptyTabEl.addEventListener('dragleave', () => {
      emptyTabEl.classList.remove('drag-over');
    });
    emptyTabEl.addEventListener('drop', (e) => {
      emptyTabEl.classList.remove('drag-over');
      this.handleEmptyTabDrop(e);
    });
    emptyTabEl.addEventListener('click', (e) => {
      e.preventDefault();
      this.addMenuTab();
    });

    tabsDiv.appendChild(emptyTabEl);
  }

  /**
   * Render menu items stacked vertically under a tab
   */
  renderMenuItems(container, tab, tabCenterX, tabY) {
    if (!tab.items || tab.items.length === 0) {
      return;
    }

    const itemsDiv = document.createElement('div');
    itemsDiv.className = 'menu-items-container';
    itemsDiv.dataset.tabId = tab.id;
    container.appendChild(itemsDiv);

    // Stack items vertically under the tab
    // Store the center X position for proper centering
    const itemCenterX = tabCenterX;

    tab.items.forEach((item, index) => {
      // Stack items vertically with spacing
      const itemY = tabY + this.itemVerticalOffset + (index * this.itemVerticalSpacing);

      const itemEl = document.createElement('div');
      itemEl.className = 'menu-item draggable-item';
      itemEl.draggable = true;
      itemEl.dataset.itemId = item.id;
      itemEl.dataset.tabId = tab.id;
      itemEl.dataset.itemIndex = index;
      // Store center X so connections can use it directly
      itemEl.dataset.centerX = itemCenterX;
      itemEl.dataset.y = itemY;
      // Position from center using left + translateX(-50%) via CSS
      itemEl.style.left = `${itemCenterX}px`;
      itemEl.style.top = `${this.layoutTop + itemY}px`;
      itemEl.innerHTML = `
        <div class="item-content">
          <span class="item-title">${this.escapeHtml(item.title)}</span>
          <span class="item-link">${this.escapeHtml(item.link)}</span>
        </div>
      `;

      itemEl.addEventListener('dragstart', (e) => this.handleItemDragStart(e, item));
      itemEl.addEventListener('dragover', (e) => {
        e.preventDefault();
        itemEl.classList.add('drag-over');
      });
      itemEl.addEventListener('dragleave', () => {
        itemEl.classList.remove('drag-over');
      });
      itemEl.addEventListener('drop', (e) => {
        itemEl.classList.remove('drag-over');
        this.handleItemDrop(e, item);
      });
      itemEl.addEventListener('dragend', () => {
        itemEl.classList.remove('dragging');
      });
      itemEl.addEventListener('click', () => this.selectItem(item));

      itemsDiv.appendChild(itemEl);
    });

    // Add empty item as a drop target after all real items (only if there is at least 1 item)
    if (tab.items.length > 0) {
      const emptyItemIndex = tab.items.length;
      const emptyItemY = tabY + this.itemVerticalOffset + (emptyItemIndex * this.itemVerticalSpacing);

      const emptyItemEl = document.createElement('div');
      emptyItemEl.className = 'menu-item empty-item-target';
      emptyItemEl.dataset.isEmptyItem = 'true';
      emptyItemEl.dataset.tabId = tab.id;
      // Important: do NOT set draggable to true
      emptyItemEl.dataset.centerX = itemCenterX;
      emptyItemEl.dataset.y = emptyItemY;
      emptyItemEl.style.left = `${itemCenterX}px`;
      emptyItemEl.style.top = `${this.layoutTop + emptyItemY}px`;
      emptyItemEl.style.opacity = '0.5';
      emptyItemEl.innerHTML = `
        <div class="item-content">
          <span class="item-title" style="color: var(--editor-muted-text, #999);">+ Drop here</span>
        </div>
      `;

      // Empty items accept drops but are not draggable
      emptyItemEl.addEventListener('dragover', (e) => {
        e.preventDefault();
        e.dataTransfer.dropEffect = 'copy';
        emptyItemEl.classList.add('drag-over');
      });
      emptyItemEl.addEventListener('dragleave', () => {
        emptyItemEl.classList.remove('drag-over');
      });
      emptyItemEl.addEventListener('drop', (e) => {
        emptyItemEl.classList.remove('drag-over');
        this.handleEmptyItemDrop(e, tab.id);
      });

      itemsDiv.appendChild(emptyItemEl);
    }
  }

  isHomeTab(tab) {
    if (!tab) return false;
    return tab.home === true;
  }

  resolvePageLink(page) {
    if (!page) return '';

    return page.link || page.pageLink || page.url || page.path || '';
  }

  /**
   * Render page hierarchy
   */
  renderPageHierarchy(container) {
    if (!this.sitemapData || !this.sitemapData.pageLibrary) {
      return;
    }

    const hierarchySection = document.createElement('div');
    hierarchySection.className = 'page-hierarchy-section';
    hierarchySection.style.marginTop = '20px';
    hierarchySection.style.padding = '20px';
    hierarchySection.style.background = 'var(--editor-panel-bg)';
    hierarchySection.style.borderRadius = '8px';
    hierarchySection.style.border = '1px solid var(--editor-border)';

    const header = document.createElement('div');
    header.className = 'hierarchy-header';
    header.style.display = 'flex';
    header.style.justifyContent = 'space-between';
    header.style.alignItems = 'center';
    header.style.marginBottom = '15px';
    header.innerHTML = `
      <h3 style="margin: 0; color: var(--editor-text); font-size: 16px;">
        <i class="far fa-sitemap"></i> Page Library
      </h3>
      <button class="button tiny primary no-gap radius" id="add-root-page-btn">
        <i class="far fa-plus"></i> Add Page
      </button>
    `;
    hierarchySection.appendChild(header);

    const hierarchyDiv = document.createElement('div');
    hierarchyDiv.className = 'page-hierarchy-container';
    hierarchyDiv.style.display = 'flex';
    hierarchyDiv.style.flexWrap = 'wrap';
    hierarchyDiv.style.gap = '15px';

    const pages = this.sitemapData.pageLibrary;

    // Group pages by parent
    const pagesByParent = {};
    pages.forEach(page => {
      const parentId = page.parentId || 0;
      if (!pagesByParent[parentId]) {
        pagesByParent[parentId] = [];
      }
      pagesByParent[parentId].push(page);
    });

    // Render root pages (no parent)
    const rootPages = pagesByParent[0] || [];
    rootPages.forEach(page => {
      this.renderPageHierarchyNode(hierarchyDiv, page, pagesByParent, 0);
    });

    hierarchySection.appendChild(hierarchyDiv);
    container.appendChild(hierarchySection);

    // Add event listener for add page button
    document.getElementById('add-root-page-btn')?.addEventListener('click', () => {
      this.addChildPage({ id: null, title: 'Root' });
    });
  }

  /**
   * Render a page hierarchy node with children
   */
  renderPageHierarchyNode(container, page, pagesByParent, level) {
    const hasChildren = pagesByParent[page.id] && pagesByParent[page.id].length > 0;

    const pageWrapper = document.createElement('div');
    pageWrapper.className = 'page-hierarchy-node';
    pageWrapper.style.marginLeft = (level * 20) + 'px';
    pageWrapper.style.width = 'calc(100% - ' + (level * 20) + 'px)';

    const pageEl = document.createElement('div');
    pageEl.className = 'page-hierarchy-box';
    pageEl.dataset.pageId = page.id;
    pageEl.draggable = true;
    pageEl.style.position = 'relative';
    pageEl.style.padding = '12px';
    pageEl.style.marginBottom = '8px';
    pageEl.style.display = 'flex';
    pageEl.style.alignItems = 'center';
    pageEl.style.gap = '10px';
    pageEl.style.cursor = 'move';

    pageEl.innerHTML = `
      ${hasChildren ? '<i class="far fa-chevron-down page-hierarchy-toggle" style="cursor: pointer; font-size: 12px;"></i>' : '<i style="width: 12px;"></i>'}
      <div style="flex: 1;">
        <div class="page-box-header">
          <i class="far fa-file-lines"></i>
          <span>${this.escapeHtml(page.title)}</span>
        </div>
        <div class="page-box-link">${this.escapeHtml(page.link)}</div>
      </div>
    `;

    // Add drag support
    pageEl.addEventListener('dragstart', (e) => {
      e.dataTransfer.effectAllowed = 'copy';
      e.dataTransfer.setData('application/x-page-data', JSON.stringify({
        id: page.id,
        title: page.title,
        link: page.link
      }));
      pageEl.style.opacity = '0.5';
    });

    pageEl.addEventListener('dragend', (e) => {
      pageEl.style.opacity = '1';
    });

    pageEl.addEventListener('click', (e) => {
      if (!e.target.closest('.page-hierarchy-toggle')) {
        this.selectPage(page);
      }
    });

    pageWrapper.appendChild(pageEl);

    // Add children container
    if (hasChildren) {
      const childrenContainer = document.createElement('div');
      childrenContainer.className = 'page-hierarchy-children';
      childrenContainer.style.marginTop = '5px';

      pagesByParent[page.id].forEach(childPage => {
        this.renderPageHierarchyNode(childrenContainer, childPage, pagesByParent, level + 1);
      });

      pageWrapper.appendChild(childrenContainer);

      // Toggle expand/collapse
      const toggle = pageEl.querySelector('.page-hierarchy-toggle');
      if (toggle) {
        toggle.addEventListener('click', (e) => {
          e.stopPropagation();
          childrenContainer.classList.toggle('collapsed');
          toggle.classList.toggle('fa-chevron-down');
          toggle.classList.toggle('fa-chevron-right');
        });
      }
    }

    container.appendChild(pageWrapper);
  }

  /**
   * Draw SVG connections between menu tabs and items in vertical stacks
   */
  drawConnections() {
    if (!this.svgCanvas) return;

    // Clear existing lines
    const existingLines = this.svgCanvas.querySelectorAll('line, polyline, path');
    existingLines.forEach(line => line.remove());

    const explorerContainer = document.getElementById('sitemap-explorer');
    if (!explorerContainer) return;

    // Draw horizontal connections between tabs using stored positions and zoom
    const tabs = Array.from(document.querySelectorAll('.menu-tab'));
    for (let i = 0; i < tabs.length - 1; i++) {
      const tab1 = tabs[i];
      const tab2 = tabs[i + 1];

      // Use stored positions and apply zoom
      const tab1X = parseFloat(tab1.dataset.x || 0) * this.zoomLevel;
      const tab1Y = (this.layoutTop + parseFloat(tab1.dataset.y || 0)) * this.zoomLevel;
      const tab1Width = tab1.offsetWidth * this.zoomLevel;
      const tab1Height = tab1.offsetHeight * this.zoomLevel;

      const tab2X = parseFloat(tab2.dataset.x || 0) * this.zoomLevel;
      const tab2Y = (this.layoutTop + parseFloat(tab2.dataset.y || 0)) * this.zoomLevel;
      const tab2Height = tab2.offsetHeight * this.zoomLevel;

      // Connect from right edge of tab1 to left edge of tab2
      const x1 = tab1X + tab1Width;
      const y1 = tab1Y + (tab1Height / 2);
      const x2 = tab2X;
      const y2 = tab2Y + (tab2Height / 2);

      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', x1);
      line.setAttribute('y1', y1);
      line.setAttribute('x2', x2);
      line.setAttribute('y2', y2);
      line.setAttribute('class', 'tab-connection-line');

      this.svgCanvas.appendChild(line);
    }

    // Draw connections for vertical stacks: tab to first item, then item to item
    const items = document.querySelectorAll('.menu-item');

    tabs.forEach(tab => {
      const tabId = tab.dataset.tabId;

      // Use stored positions and apply zoom for tab
      const tabX = parseFloat(tab.dataset.x || 0) * this.zoomLevel;
      const tabY = (this.layoutTop + parseFloat(tab.dataset.y || 0)) * this.zoomLevel;
      const tabWidth = tab.offsetWidth * this.zoomLevel;
      const tabHeight = tab.offsetHeight * this.zoomLevel;

      // Connect from bottom center of tab
      const tabCenterX = tabX + (tabWidth / 2);
      const tabBottomY = tabY + tabHeight;

      // Find all items belonging to this tab, sorted by index
      const tabItems = Array.from(items)
        .filter(item => item.dataset.tabId === tabId)
        .sort((a, b) => Number.parseInt(a.dataset.itemIndex) - Number.parseInt(b.dataset.itemIndex));

      // Connect tab to first item only
      if (tabItems.length > 0) {
        const firstItem = tabItems[0];
        // Use the stored centerX for proper alignment
        const firstItemCenterX = parseFloat(firstItem.dataset.centerX || 0) * this.zoomLevel;
        const firstItemY = (this.layoutTop + parseFloat(firstItem.dataset.y || 0)) * this.zoomLevel;

        // Straight vertical line from tab center to item center
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', tabCenterX);
        line.setAttribute('y1', tabBottomY);
        line.setAttribute('x2', firstItemCenterX);
        line.setAttribute('y2', firstItemY);
        line.setAttribute('class', 'connection-line');
        line.setAttribute('stroke', 'var(--editor-border)');
        line.setAttribute('stroke-width', '2');

        this.svgCanvas.appendChild(line);
      }

      // Connect each item to the next in the stack
      for (let i = 0; i < tabItems.length - 1; i++) {
        const currentItem = tabItems[i];
        const nextItem = tabItems[i + 1];

        // Use stored center positions and apply zoom
        const currentCenterX = parseFloat(currentItem.dataset.centerX || 0) * this.zoomLevel;
        const currentY = (this.layoutTop + parseFloat(currentItem.dataset.y || 0)) * this.zoomLevel;
        const currentHeight = currentItem.offsetHeight * this.zoomLevel;

        const nextCenterX = parseFloat(nextItem.dataset.centerX || 0) * this.zoomLevel;
        const nextY = (this.layoutTop + parseFloat(nextItem.dataset.y || 0)) * this.zoomLevel;

        const currentBottomY = currentY + currentHeight;

        // Straight vertical line from item center to item center
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', currentCenterX);
        line.setAttribute('y1', currentBottomY);
        line.setAttribute('x2', nextCenterX);
        line.setAttribute('y2', nextY);
        line.setAttribute('class', 'connection-line');
        line.setAttribute('stroke', 'var(--editor-border)');
        line.setAttribute('stroke-width', '2');

        this.svgCanvas.appendChild(line);
      }
    });
  }

  /**
   * Handle tab drag start
   */
  handleTabDragStart(e, tab) {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('tab', JSON.stringify(tab));
    // Do NOT set page data for menu tabs - only page tree pages should create new tabs

    this.draggedElement = e.target.closest('.menu-tab');
    this.draggedElement.classList.add('dragging');
  }

  /**
   * Handle tab drop - adds pages to the tab's menu item stack
   */
  handleTabDrop(e, tab) {
    e.preventDefault();
    e.stopPropagation();

    if (this.isHomeTab(tab)) {
      return;
    }

    if (this.draggedElement) {
      this.draggedElement.classList.remove('dragging');
    }

    // First check for tab-to-tab drag (reordering)
    const draggedTabData = e.dataTransfer.getData('tab');
    if (draggedTabData) {
      try {
        const draggedTab = JSON.parse(draggedTabData);
        if (draggedTab.id !== tab.id) {
          this.reorderTab(draggedTab.id, tab.id);
        }
      } catch (error) {
        console.error('Error parsing tab data:', error);
      }
      return;
    }

    // Then check for item-to-tab drag (should not happen)
    const draggedItemData = e.dataTransfer.getData('item');
    if (draggedItemData) {
      // Items dropped on tabs should be ignored
      return;
    }

    // Finally check for page data (from page tree only)
    const pageData = e.dataTransfer.getData('application/x-page-data');
    if (pageData) {
      // Only create menu items if page came from page tree, not from sitemap
      const draggedElement = this.draggedElement;
      if (!draggedElement || !draggedElement.classList.contains('menu-tab') && !draggedElement.classList.contains('menu-item')) {
        try {
          const page = JSON.parse(pageData);
          const pageLink = this.resolvePageLink(page);
          if (page && pageLink) {
            // Add page to this tab's menu item stack
            this.createMenuItem(tab.id, { title: page.title || pageLink, link: pageLink, pageId: page.id });
            return;
          }
          this.showError('Page link is required to create a menu item.');
        } catch (error) {
          console.error('Error parsing page data:', error);
        }
      }
    }
  }

  /**
   * Handle drop on empty tab target - creates a new tab from a page
   */
  handleEmptyTabDrop(e) {
    e.preventDefault();
    e.stopPropagation();

    if (this.draggedElement) {
      this.draggedElement.classList.remove('dragging');
    }

    // Only create tabs from page tree pages, not from sitemap elements
    const draggedElement = this.draggedElement;
    if (draggedElement && (draggedElement.classList.contains('menu-tab') || draggedElement.classList.contains('menu-item'))) {
      // Ignore drops from menu tabs and items
      return;
    }

    const pageData = e.dataTransfer.getData('application/x-page-data');
    if (pageData) {
      try {
        const page = JSON.parse(pageData);
        const pageLink = this.resolvePageLink(page);
        const title = page.title || pageLink || 'New Tab';
        if (page && title) {
          // Create a new tab from the dropped page, including the link
          this.createMenuTab({ title, link: pageLink });
          return;
        }
      } catch (error) {
        console.error('Error parsing page data:', error);
      }
    }
  }

  /**
   * Handle item drop - reorders items within or between stacks, or adds pages
   */
  handleItemDrop(e, item) {
    e.preventDefault();
    e.stopPropagation();

    if (this.draggedElement) {
      this.draggedElement.classList.remove('dragging');
    }

    // First check for item-to-item drag (reordering)
    const draggedItemData = e.dataTransfer.getData('item');
    if (draggedItemData) {
      try {
        const draggedItem = JSON.parse(draggedItemData);
        if (draggedItem.id !== item.id) {
          // Reorder item in the same or different stack
          this.reorderItem(draggedItem.id, item.id);
        }
      } catch (error) {
        console.error('Error parsing item data:', error);
      }
      return;
    }

    // Then check for page data (from page tree only)
    const draggedElement = this.draggedElement;
    if (draggedElement && (draggedElement.classList.contains('menu-tab') || draggedElement.classList.contains('menu-item'))) {
      // Ignore drops from menu tabs and items
      return;
    }

    const pageData = e.dataTransfer.getData('application/x-page-data');
    if (pageData) {
      try {
        const page = JSON.parse(pageData);
        const pageLink = this.resolvePageLink(page);
        if (page && pageLink) {
          // Add page as a menu item in the same tab as the target item
          this.createMenuItem(item.tabId || item.menuTabId, {
            title: page.title || pageLink,
            link: pageLink,
            pageId: page.id
          });
          return;
        }

        this.showError('Page link is required to create a menu item.');
      } catch (error) {
        console.error('Error parsing page data:', error);
      }
    }
  }

  /**
   * Handle drop on empty item target - adds a page as a new menu item
   */
  handleEmptyItemDrop(e, tabId) {
    e.preventDefault();
    e.stopPropagation();

    // Prevent adding items to the home tab
    const targetTab = this.sitemapData?.menuTabs?.find(t => t.id === tabId);
    if (targetTab && this.isHomeTab(targetTab)) {
      return;
    }

    if (this.draggedElement) {
      this.draggedElement.classList.remove('dragging');
    }

    // First check for item-to-item drag (reordering)
    const draggedItemData = e.dataTransfer.getData('item');
    if (draggedItemData) {
      try {
        const draggedItem = JSON.parse(draggedItemData);
        // Moving an item to the end of the stack - use the empty item as target
        this.reorderItem(draggedItem.id, null, tabId);
      } catch (error) {
        console.error('Error parsing item data:', error);
      }
      return;
    }

    // Then check for page data (from page tree only)
    const draggedElement = this.draggedElement;
    if (draggedElement && (draggedElement.classList.contains('menu-tab') || draggedElement.classList.contains('menu-item'))) {
      // Ignore drops from menu tabs and items
      return;
    }

    const pageData = e.dataTransfer.getData('application/x-page-data');
    if (pageData) {
      try {
        const page = JSON.parse(pageData);
        const pageLink = this.resolvePageLink(page);
        if (page && pageLink) {
          // Add page to this tab's menu item stack
          this.createMenuItem(tabId, { title: page.title || pageLink, link: pageLink, pageId: page.id });
          return;
        }

        this.showError('Page link is required to create a menu item.');
      } catch (error) {
        console.error('Error parsing page data:', error);
      }
    }
  }

  /**
   * Handle item drag start
   */
  handleItemDragStart(e, item) {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('item', JSON.stringify(item));
    // Do NOT set page data for menu items - only page tree pages should create new items

    this.draggedElement = e.target.closest('.menu-item');
    this.draggedElement.classList.add('dragging');

    // Create a drag image from the element without the transform
    const dragImage = document.createElement('div');
    dragImage.style.position = 'absolute';
    dragImage.style.left = '-9999px';
    dragImage.style.top = '-9999px';
    dragImage.style.width = this.draggedElement.offsetWidth + 'px';
    dragImage.style.height = this.draggedElement.offsetHeight + 'px';
    dragImage.style.background = getComputedStyle(this.draggedElement).backgroundColor;
    dragImage.style.border = getComputedStyle(this.draggedElement).border;
    dragImage.style.borderRadius = getComputedStyle(this.draggedElement).borderRadius;
    dragImage.style.padding = getComputedStyle(this.draggedElement).padding;
    dragImage.style.boxShadow = getComputedStyle(this.draggedElement).boxShadow;
    dragImage.style.color = getComputedStyle(this.draggedElement).color;
    dragImage.style.fontSize = '12px';
    dragImage.style.textAlign = 'center';
    dragImage.style.display = 'flex';
    dragImage.style.flexDirection = 'column';
    dragImage.style.justifyContent = 'center';
    dragImage.style.userSelect = 'none';
    dragImage.style.pointerEvents = 'none';

    // Copy the content
    const titleEl = this.draggedElement.querySelector('.item-title');
    const linkEl = this.draggedElement.querySelector('.item-link');
    if (titleEl) {
      const title = document.createElement('div');
      title.textContent = titleEl.textContent;
      title.style.fontWeight = 'bold';
      title.style.fontSize = '12px';
      dragImage.appendChild(title);
    }
    if (linkEl) {
      const link = document.createElement('div');
      link.textContent = linkEl.textContent;
      link.style.fontSize = '10px';
      link.style.color = '#999';
      dragImage.appendChild(link);
    }

    document.body.appendChild(dragImage);
    const rect = this.draggedElement.getBoundingClientRect();
    e.dataTransfer.setDragImage(dragImage, rect.width / 2, rect.height / 2);

    // Clean up after drag
    setTimeout(() => dragImage.remove(), 0);
  }

  /**
   * Reorder tab on server
   */
  reorderTab(tabId, targetTabId) {
    const params = new FormData();
    params.append('tabId', tabId);
    params.append('targetTabId', targetTabId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/reorder-tab', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to reorder tab');
        }
      })
      .catch(error => {
        console.error('Error reordering tab:', error);
        this.showError('Error reordering tab: ' + error.message);
      });
  }

  /**
   * Reorder item on server
   */
  reorderItem(itemId, targetItemId, newMenuTabId) {
    const params = new FormData();
    params.append('itemId', itemId);
    if (targetItemId) {
      params.append('targetItemId', targetItemId);
    }
    if (newMenuTabId) {
      params.append('newMenuTabId', newMenuTabId);
    }
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/reorder-item', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to reorder item');
        }
      })
      .catch(error => {
        console.error('Error reordering item:', error);
        this.showError('Error reordering item: ' + error.message);
      });
  }

  /**
   * Select tab
   */
  selectTab(tab) {
    document.querySelectorAll('.menu-tab').forEach(el => el.classList.remove('selected'));
    document.querySelectorAll('.menu-item').forEach(el => el.classList.remove('selected'));
    const tabEl = document.querySelector(`[data-tab-id="${tab.id}"]`);
    tabEl?.classList.add('selected');

    // Show properties in right panel
    this.showTabProperties(tab);

    if (this.editorBridge && typeof this.editorBridge.showPagePreview === 'function') {
      this.editorBridge.showPagePreview(tab.link);
    }
  }

  /**
   * Select item
   */
  selectItem(item) {
    document.querySelectorAll('.menu-tab').forEach(el => el.classList.remove('selected'));
    document.querySelectorAll('.menu-item').forEach(el => el.classList.remove('selected'));
    const itemEl = document.querySelector(`[data-item-id="${item.id}"]`);
    itemEl?.classList.add('selected');

    // Show properties in right panel
    this.showItemProperties(item);

    if (this.editorBridge && typeof this.editorBridge.showPagePreview === 'function') {
      this.editorBridge.showPagePreview(item.link);
    }
  }

  /**
   * Select page
   */
  selectPage(page) {
    document.querySelectorAll('.page-hierarchy-box').forEach(el => el.classList.remove('selected'));
    document.querySelector(`[data-page-id="${page.id}"]`)?.classList.add('selected');
  }

  /**
   * Show error message
   */
  showError(message) {
    console.error(message);
  }

  /**
   * Show tab context menu
   */
  showTabContextMenu(e, tabElement) {
    const tabId = tabElement.dataset.tabId;
    const tab = this.sitemapData.menuTabs.find(t => t.id == tabId);

    // Don't show context menu for home tab
    if (this.isHomeTab(tab)) {
      return;
    }

    this.showContextMenu(e.clientX, e.clientY, [
      {
        label: 'Edit Tab',
        icon: 'fa-edit',
        action: () => this.editTab(tab)
      },
      {
        label: 'Add Menu Item',
        icon: 'fa-plus',
        action: () => this.addMenuItem(tab)
      },
      { divider: true },
      {
        label: 'Delete Tab',
        icon: 'fa-trash',
        action: () => this.deleteTab(tab)
      }
    ]);
  }

  /**
   * Show item context menu
   */
  showItemContextMenu(e, itemElement) {
    const itemId = itemElement.dataset.itemId;
    const tabId = itemElement.dataset.tabId;
    const tab = this.sitemapData.menuTabs.find(t => t.id == tabId);
    const item = tab?.items.find(i => i.id == itemId);

    this.showContextMenu(e.clientX, e.clientY, [
      {
        label: 'Edit Item',
        icon: 'fa-edit',
        action: () => this.editItem(item, tab)
      },
      {
        label: 'Move to Another Tab',
        icon: 'fa-arrows',
        action: () => this.moveItem(item)
      },
      { divider: true },
      {
        label: 'Delete Item',
        icon: 'fa-trash',
        action: () => this.deleteItem(item, tab)
      }
    ]);
  }

  /**
   * Show page context menu
   */
  showPageContextMenu(e, pageElement) {
    const pageId = pageElement.dataset.pageId;
    const page = this.sitemapData.pageLibrary.find(p => p.id == pageId);

    this.showContextMenu(e.clientX, e.clientY, [
      {
        label: 'Edit Page',
        icon: 'fa-edit',
        action: () => this.editPage(page)
      },
      {
        label: 'View Content Blocks',
        icon: 'fa-cubes',
        action: () => this.viewPageContent(page)
      },
      { divider: true },
      {
        label: 'Add Child Page',
        icon: 'fa-plus',
        action: () => this.addChildPage(page)
      }
    ]);
  }

  /**
   * Show context menu
   */
  showContextMenu(x, y, items) {
    this.hideContextMenu();

    const menu = document.createElement('div');
    menu.className = 'context-menu';
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';

    items.forEach(item => {
      if (item.divider) {
        const divider = document.createElement('div');
        divider.className = 'context-menu-divider';
        menu.appendChild(divider);
      } else {
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
      }
    });

    document.body.appendChild(menu);
  }

  /**
   * Hide context menu
   */
  hideContextMenu() {
    const existingMenu = document.querySelector('.context-menu');
    if (existingMenu) {
      existingMenu.remove();
    }
  }

  /**
   * Edit tab
   */
  editTab(tab) {
    if (this.isHomeTab(tab)) {
      this.showError('Cannot edit the home tab');
      return;
    }
    const newTitle = prompt('Enter new tab title:', tab.title);
    if (newTitle && newTitle !== tab.title) {
      this.updateTab(tab.id, { title: newTitle });
    }
  }

  /**
   * Add menu item
   */
  addMenuItem(tab) {
    if (this.isHomeTab(tab)) {
      this.showError('Cannot add menu items to the home tab');
      return;
    }
    const title = prompt('Enter menu item title:');
    if (title) {
      const link = prompt('Enter menu item link:');
      if (link) {
        this.createMenuItem(tab.id, { title, link });
      }
    }
  }

  /**
   * Delete tab
   */
  deleteTab(tab) {
    if (this.isHomeTab(tab)) {
      this.showError('Cannot delete the home tab');
      return;
    }
    if (confirm(`Are you sure you want to delete the tab "${tab.title}"?`)) {
      this.removeTab(tab.id);
    }
  }

  /**
   * Edit item
   */
  editItem(item, tab) {
    const newTitle = prompt('Enter new item title:', item.title);
    if (newTitle && newTitle !== item.title) {
      const newLink = prompt('Enter new item link:', item.link);
      if (newLink) {
        this.updateItem(item.id, { title: newTitle, link: newLink });
      }
    }
  }

  /**
   * Move item to another tab
   */
  moveItem(item) {
    // Show list of tabs to move to
    const tabsList = this.sitemapData.menuTabs
      .map((t, i) => `${i + 1}. ${t.title}`)
      .join('\n');

    const selection = prompt(`Move to which tab?\n\n${tabsList}\n\nEnter number:`);
    if (selection) {
      const index = Number.parseInt(selection, 10) - 1;
      if (index >= 0 && index < this.sitemapData.menuTabs.length) {
        const targetTab = this.sitemapData.menuTabs[index];
        this.moveItemToTab(item.id, targetTab.id);
      }
    }
  }

  /**
   * Delete item
   */
  deleteItem(item, tab) {
    if (confirm(`Are you sure you want to delete "${item.title}"?`)) {
      this.removeItem(item.id);
    }
  }

  /**
   * Edit page
   */
  editPage(page) {
    // Navigate to page editor
    if (this.editorBridge) {
      this.editorBridge.loadPage(page.id);
    }
  }

  /**
   * View page content blocks
   */
  viewPageContent(page) {
    alert(`Content blocks for page "${page.title}" will be displayed here.`);
  }

  /**
   * Add child page
   */
  addChildPage(parentPage) {
    const title = prompt('Enter new page title:');
    if (title) {
      const link = prompt('Enter page link:');
      if (link) {
        this.createPage({ title, link, parentId: parentPage.id });
      }
    }
  }

  /**
   * Update tab on server
   */
  updateTab(tabId, data) {
    const params = new FormData();
    params.append('tabId', tabId);
    params.append('title', data.title);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/update-tab', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to update tab');
        }
      })
      .catch(error => {
        console.error('Error updating tab:', error);
        this.showError('Error updating tab: ' + error.message);
      });
  }

  /**
   * Create menu item on server
   */
  createMenuItem(tabId, data) {
    const params = new FormData();
    params.append('tabId', tabId);
    params.append('title', data.title);
    params.append('link', data.link);
    if (data.pageId) {
      params.append('pageId', data.pageId);
    }
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/create-item', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to create menu item');
        }
      })
      .catch(error => {
        console.error('Error creating menu item:', error);
        this.showError('Error creating menu item: ' + error.message);
      });
  }

  /**
   * Remove tab on server
   */
  removeTab(tabId) {
    const params = new FormData();
    params.append('tabId', tabId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/delete-tab', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to delete tab');
        }
      })
      .catch(error => {
        console.error('Error deleting tab:', error);
        this.showError('Error deleting tab: ' + error.message);
      });
  }

  /**
   * Update item on server
   */
  updateItem(itemId, data) {
    const params = new FormData();
    params.append('itemId', itemId);
    params.append('title', data.title);
    params.append('link', data.link);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/update-item', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to update item');
        }
      })
      .catch(error => {
        console.error('Error updating item:', error);
        this.showError('Error updating item: ' + error.message);
      });
  }

  /**
   * Move item to another tab
   */
  moveItemToTab(itemId, targetTabId) {
    const params = new FormData();
    params.append('itemId', itemId);
    params.append('targetTabId', targetTabId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/move-item', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to move item');
        }
      })
      .catch(error => {
        console.error('Error moving item:', error);
        this.showError('Error moving item: ' + error.message);
      });
  }

  /**
   * Remove item on server
   */
  removeItem(itemId) {
    const params = new FormData();
    params.append('itemId', itemId);
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/delete-item', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to delete item');
        }
      })
      .catch(error => {
        console.error('Error deleting item:', error);
        this.showError('Error deleting item: ' + error.message);
      });
  }

  /**
   * Create page on server
   */
  createPage(data) {
    const params = new FormData();
    params.append('title', data.title);
    params.append('link', data.link);
    if (data.parentId) {
      params.append('parentId', data.parentId);
    }
    params.append('token', globalThis.getFormToken());

    fetch('/json/sitemap/create-page', {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      },
      body: params
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok') {
          this.markDirty();
          this.loadSitemapData();
        } else {
          this.showError(data.error || 'Failed to create page');
        }
      })
      .catch(error => {
        console.error('Error creating page:', error);
        this.showError('Error creating page: ' + error.message);
      });
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
   * Refresh sitemap - manually triggered refresh (always renders if data is loaded)
   */
  refresh() {
    // Force render if we have data, even if tab is hidden
    if (this.sitemapData) {
      if (this.isInitialized) {
        this.syncSitemap();
      } else {
        this.renderSitemap();
        this.isInitialized = true;
      }
    } else {
      // If no data yet, load it
      this.loadSitemapData();
    }
    this.clearDirty();
  }

  /**
   * Called when the tab becomes visible - renders pending data if available
   */
  onTabVisible() {
    this.tabIsVisible = true;
    // If we have data that wasn't rendered yet (because tab was hidden), render it now
    if (this.sitemapData && !this.isInitialized) {
      this.renderSitemap();
      this.isInitialized = true;
    } else if (this.sitemapData && this.isInitialized && this.isDirty) {
      // If tab just became visible and we have pending changes, sync them
      this.syncSitemap();
    }
  }

  /**
   * Called when the tab becomes hidden
   */
  onTabHidden() {
    this.tabIsVisible = false;
  }

  /**
   * Setup zoom controls - uses existing element in DOM and wires up event listeners
   */
  createZoomControls(container) {
    // Use existing controls from JSP DOM
    const controlsDiv = document.getElementById('sitemap-zoom-controls');
    if (!controlsDiv) return;

    // Wire up existing buttons to event listeners
    const zoomInBtn = document.getElementById('zoom-in-btn');
    const zoomOutBtn = document.getElementById('zoom-out-btn');
    const zoomFitBtn = document.getElementById('zoom-fit-btn');
    const zoomActualBtn = document.getElementById('zoom-actual-btn');

    if (zoomInBtn) {
      zoomInBtn.addEventListener('click', () => this.zoomIn());
    }
    if (zoomOutBtn) {
      zoomOutBtn.addEventListener('click', () => this.zoomOut());
    }
    if (zoomFitBtn) {
      zoomFitBtn.addEventListener('click', () => this.zoomToFit());
    }
    if (zoomActualBtn) {
      zoomActualBtn.addEventListener('click', () => this.zoomActual());
    }

    // Show the controls (they're hidden by default unless Site Navigation is active)
    if (controlsDiv) controlsDiv.style.display = 'flex';
  }

  /**
   * Zoom in
   */
  zoomIn() {
    this.zoomLevel = Math.min(this.maxZoom, this.zoomLevel + this.zoomStep);
    this.applyZoom();
  }

  /**
   * Zoom out
   */
  zoomOut() {
    this.zoomLevel = Math.max(this.minZoom, this.zoomLevel - this.zoomStep);
    this.applyZoom();
  }

  /**
   * Zoom to fit - fits only the menu tabs/items content to the visible container
   */
  zoomToFit() {
    const container = document.getElementById('sitemap-explorer');
    const mainDiv = document.getElementById('sitemap-main-canvas');
    if (!container || !mainDiv) return;

    // Reset zoom temporarily to get true content dimensions
    const currentZoom = this.zoomLevel;
    mainDiv.style.transform = 'scale(1)';

    // Get content bounds from actual elements
    const tabs = document.querySelectorAll('.menu-tab');
    const items = document.querySelectorAll('.menu-item');

    let maxX = 0, maxY = 0;
    tabs.forEach(tab => {
      const x = parseFloat(tab.dataset.x || 0) + tab.offsetWidth;
      const y = parseFloat(tab.dataset.y || 0) + tab.offsetHeight;
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
    });
    items.forEach(item => {
      const x = parseFloat(item.dataset.x || 0) + item.offsetWidth;
      const y = parseFloat(item.dataset.y || 0) + item.offsetHeight;
      maxX = Math.max(maxX, x);
      maxY = Math.max(maxY, y);
    });

    // Add padding and layout offset
    const contentWidth = maxX + 100;
    const contentHeight = maxY + this.layoutTop + 100;

    // Account for zoom controls space (they're positioned absolutely)
    const containerWidth = container.clientWidth - 40;
    const containerHeight = container.clientHeight - 40;

    const scaleX = containerWidth / contentWidth;
    const scaleY = containerHeight / contentHeight;

    this.zoomLevel = Math.min(scaleX, scaleY, this.maxZoom);
    this.zoomLevel = Math.max(this.zoomLevel, this.minZoom);
    this.applyZoom();
  }

  /**
   * Zoom to actual size (1:1)
   */
  zoomActual() {
    this.zoomLevel = 1.0;
    this.applyZoom();
  }

  /**
   * Apply zoom transformation to content only
   */
  applyZoom() {
    const mainDiv = document.getElementById('sitemap-main-canvas');
    if (!mainDiv) return;

    mainDiv.style.transform = `scale(${this.zoomLevel})`;

    // Update background size to match scaled content
    this.updateBackgroundSize();

    // Redraw connections after zoom
    setTimeout(() => this.drawConnections(), 50);
  }

  /**
   * Update graph paper background size and scale to match zoom level
   */
  updateBackgroundSize() {
    const container = document.getElementById('sitemap-explorer');
    const mainDiv = document.getElementById('sitemap-main-canvas');
    if (!container || !mainDiv) return;

    // Scale the background pattern with zoom level for consistent appearance
    const gridSize = 20 * this.zoomLevel;
    mainDiv.style.backgroundSize = `${gridSize}px ${gridSize}px`;
    mainDiv.style.backgroundPosition = '-1px -1px';

    // Calculate content bounds to ensure background covers everything
    const tabs = document.querySelectorAll('.menu-tab');
    const items = document.querySelectorAll('.menu-item');

    // Start with container dimensions, accounting for zoom scaling
    // When zoomed, we need larger unscaled dimensions to cover the scaled viewport
    let maxX = container.clientWidth / this.zoomLevel;
    let maxY = container.clientHeight / this.zoomLevel;

    tabs.forEach(tab => {
      const x = parseFloat(tab.dataset.x || 0) + tab.offsetWidth;
      const y = this.layoutTop + parseFloat(tab.dataset.y || 0) + tab.offsetHeight;
      maxX = Math.max(maxX, x + 100);
      maxY = Math.max(maxY, y + 100);
    });

    items.forEach(item => {
      const x = parseFloat(item.dataset.centerX || 0) + item.offsetWidth / 2;
      const y = this.layoutTop + parseFloat(item.dataset.y || 0) + item.offsetHeight;
      maxX = Math.max(maxX, x + 100);
      maxY = Math.max(maxY, y + 100);
    });

    // Set explicit dimensions to ensure background repeats across full area
    mainDiv.style.width = `${maxX}px`;
    mainDiv.style.height = `${maxY}px`;
    mainDiv.style.minWidth = '100%';
    mainDiv.style.minHeight = '100%';

    // Update SVG canvas dimensions to match mainDiv for consistent background coverage
    if (this.svgCanvas) {
      this.svgCanvas.setAttribute('width', maxX);
      this.svgCanvas.setAttribute('height', maxY);
    }
  }

  /**
   * Show tab properties in right panel
   */
  showTabProperties(tab) {
    if (!this.editorBridge || !this.editorBridge.showProperties) return;

    const properties = {
      type: 'Menu Tab',
      title: tab.title,
      id: tab.id,
      link: tab.link,
      itemCount: tab.items ? tab.items.length : 0,
      fields: [
        { label: 'Title', value: tab.title, editable: true },
        { label: 'Link', value: tab.link || '' },
        { label: 'Items', value: tab.items ? tab.items.length : 0 }
      ]
    };

    this.editorBridge.showProperties(properties);
  }

  /**
   * Show item properties in right panel
   */
  showItemProperties(item) {
    if (!this.editorBridge || !this.editorBridge.showProperties) return;

    const properties = {
      type: 'Menu Item',
      title: item.title,
      id: item.id,
      link: item.link,
      fields: [
        { label: 'Title', value: item.title, editable: true },
        { label: 'Link', value: item.link, editable: true }
      ]
    };

    this.editorBridge.showProperties(properties);
  }
}
