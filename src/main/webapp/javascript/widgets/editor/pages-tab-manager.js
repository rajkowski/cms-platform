/**
 * Pages Tab Manager for Visual Page Editor
 * Handles loading, displaying, and switching between web pages for editing
 * 
 * @author matt rajkowski
 * @created 12/14/25 10:00 AM
 */

class PagesTabManager {
  constructor(pageEditor) {
    this.pageEditor = pageEditor;
    this.pages = [];
    this.selectedPageId = null;
    this.selectedPageLink = null;
    this.currentSortBy = 'a-z'; // Default sorting
    this.hierarchyMode = false;
    this.currentParentId = null;
    this.hierarchyStack = []; // Stack for back navigation
  }

  /**
   * Initialize the pages tab
   */
  init() {
    this.setupEventListeners();
    this.setupSearchListener();
    this.setupSortListener();
    this.setupHierarchyNavigation();
    this.loadPages();
  }

  /**
   * Set up event listeners for the pages tab
   */
  setupEventListeners() {
    const pageList = document.getElementById('web-page-list');
    if (pageList) {
      pageList.addEventListener('click', (e) => this.handlePageClick(e));
    }
  }

  /**
   * Set up sort selector listener
   */
  setupSortListener() {
    const sortSelect = document.getElementById('pages-sort-select');
    if (sortSelect) {
      sortSelect.addEventListener('change', (e) => {
        this.currentSortBy = e.target.value;
        this.hierarchyMode = this.currentSortBy === 'hierarchy';
        
        // Show/hide hierarchy navigation
        const hierarchyNav = document.getElementById('pages-hierarchy-nav');
        if (hierarchyNav) {
          hierarchyNav.style.display = this.hierarchyMode ? 'block' : 'none';
        }
        
        // Reset hierarchy state when switching modes
        if (!this.hierarchyMode) {
          this.currentParentId = null;
          this.hierarchyStack = [];
        }
        
        this.loadPages();
      });
    }
  }

  /**
   * Set up hierarchy navigation buttons
   */
  setupHierarchyNavigation() {
    const backToRootBtn = document.getElementById('pages-back-to-root');
    if (backToRootBtn) {
      backToRootBtn.addEventListener('click', () => {
        this.currentParentId = null;
        this.hierarchyStack = [];
        this.loadPages();
      });
    }

    const backToParentBtn = document.getElementById('pages-back-to-parent');
    if (backToParentBtn) {
      backToParentBtn.addEventListener('click', () => {
        if (this.hierarchyStack.length > 0) {
          this.hierarchyStack.pop(); // Remove current level
          this.currentParentId = this.hierarchyStack.length > 0 
            ? this.hierarchyStack[this.hierarchyStack.length - 1].id 
            : null;
          this.loadPages();
        }
      });
    }
  }

  /**
   * Set up search/filter listener for the pages tab
   */
  setupSearchListener() {
    const searchInput = document.getElementById('pages-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();
        const filtered = query ? this.pages.filter(page => {
          const searchText = (page.title + ' ' + page.link).toLowerCase();
          const terms = query.split(/\s+/);
          
          // All search terms must exist in the text, in any order
          return terms.every(term => searchText.includes(term));
        }).sort((a, b) => {
          // Sort by relevance: prioritize title matches, then by earliest match position
          const aText = (a.title + ' ' + a.link).toLowerCase();
          const bText = (b.title + ' ' + b.link).toLowerCase();
          const aTitleText = a.title.toLowerCase();
          const bTitleText = b.title.toLowerCase();
          
          // Find position of first matching term in each result
          const queryTerms = query.split(/\s+/);
          
          // Calculate minimum position for a
          let aMinPos = Infinity;
          for (const t of queryTerms) {
            const titlePos = aTitleText.indexOf(t);
            const fullPos = aText.indexOf(t);
            const pos = titlePos >= 0 ? titlePos : fullPos;
            if (pos >= 0 && pos < aMinPos) {
              aMinPos = pos;
            }
          }
          
          // Calculate minimum position for b
          let bMinPos = Infinity;
          for (const t of queryTerms) {
            const titlePos = bTitleText.indexOf(t);
            const fullPos = bText.indexOf(t);
            const pos = titlePos >= 0 ? titlePos : fullPos;
            if (pos >= 0 && pos < bMinPos) {
              bMinPos = pos;
            }
          }
          
          return aMinPos - bMinPos;
        }) : this.pages;
        
        if (this.hierarchyMode) {
          this.renderHierarchyList(filtered);
        } else {
          this.renderPageListFiltered(filtered);
        }
      });
    }
  }

  /**
   * Load the list of web pages from the server
   */
  loadPages() {
    const loadingEl = document.getElementById('pages-loading');
    const errorEl = document.getElementById('pages-error');
    const emptyEl = document.getElementById('pages-empty');
    const listEl = document.getElementById('web-page-list');

    if (loadingEl) {
      loadingEl.style.display = 'block';
    }
    if (errorEl) {
      errorEl.style.display = 'none';
    }
    if (emptyEl) {
      emptyEl.style.display = 'none';
    }

    // If in hierarchy mode, use the hierarchy endpoint
    if (this.hierarchyMode) {
      this.loadHierarchyPages();
      return;
    }

    // Otherwise, use the regular page list endpoint with sorting
    const url = `/json/webPageList?sortBy=${encodeURIComponent(this.currentSortBy)}`;
    
    fetch(url)
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then(pages => {
        this.pages = pages;
        if (loadingEl) {
          loadingEl.style.display = 'none';
        }

        if (pages.length === 0) {
          // Check if we have a current page that doesn't exist yet
          const currentPageLink = this.pageEditor.config.webPageLink;
          if (currentPageLink) {
            // Show the new page even if no other pages exist
            this.renderPageList(pages);
          } else {
            if (emptyEl) {
              emptyEl.style.display = 'block';
            }
            if (listEl) {
              listEl.innerHTML = '';
            }
          }
        } else {
          if (emptyEl) {
            emptyEl.style.display = 'none';
          }
          this.renderPageList(pages);
        }
      })
      .catch(error => {
        console.error('Error loading pages:', error);
        if (loadingEl) {
          loadingEl.style.display = 'none';
        }
        if (errorEl) {
          this.showError(errorEl, 'Error loading pages: ' + error.message);
        }
        
        // Even if there's an error loading pages, show the current page if it exists
        const currentPageLink = this.pageEditor.config.webPageLink;
        if (currentPageLink && listEl) {
          const newPageItem = this.createNewPageItem(currentPageLink);
          listEl.innerHTML = '';
          listEl.appendChild(newPageItem);
        }
      });
  }

  /**
   * Render the list of pages in the UI
   */
  renderPageList(pages) {
    const listEl = document.getElementById('web-page-list');
    if (!listEl) {
      return;
    }

    listEl.innerHTML = '';

    // Check if the current page being edited exists in the pages list
    const currentPageLink = this.pageEditor.config.webPageLink;
    const currentPageExists = pages.some(page => page.link === currentPageLink);

    // If the current page doesn't exist, add it as a new page at the top
    if (!currentPageExists && currentPageLink) {
      // Use the stored title from the editor if available
      const pageTitle = this.pageEditor.newPageTitle || null;
      const newPageItem = this.createNewPageItem(currentPageLink, pageTitle);
      listEl.appendChild(newPageItem);
    }

    pages.forEach(page => {
      const li = document.createElement('li');
      const item = document.createElement('div');
      item.className = 'web-page-item';
      item.setAttribute('data-page-id', page.id);
      item.setAttribute('data-page-link', page.link);

      // Check if this is the currently editing page
      if (this.pageEditor.config.webPageLink === page.link) {
        item.classList.add('selected');
        this.selectedPageId = page.id;
        this.selectedPageLink = page.link;
      }

      const info = document.createElement('div');
      info.className = 'web-page-info';

      const title = document.createElement('div');
      title.className = 'web-page-title';
      title.textContent = page.title;

      const link = document.createElement('div');
      link.className = 'web-page-link';
      link.textContent = page.link;

      info.appendChild(title);
      info.appendChild(link);
      
      // Add modified date if available and in "modified" sort mode
      if (page.modified && this.currentSortBy === 'modified') {
        const modified = document.createElement('div');
        modified.className = 'web-page-modified';
        modified.textContent = this.formatDate(page.modified);
        info.appendChild(modified);
      }
      
      item.appendChild(info);

      li.appendChild(item);
      listEl.appendChild(li);
    });
  }

  /**
   * Render a filtered list of pages in the UI
   * @param {Array} pages - The filtered pages to render
   */
  renderPageListFiltered(pages) {
    const listEl = document.getElementById('web-page-list');
    if (!listEl) {
      return;
    }

    listEl.innerHTML = '';

    if (pages.length === 0) {
      listEl.innerHTML = '<li style="text-align: center; padding: 40px; color: #999;"><i class="far fa-search"></i> No pages found</li>';
      return;
    }

    // Check if the current page being edited exists in the filtered pages list
    const currentPageLink = this.pageEditor.config.webPageLink;
    const currentPageExists = pages.some(page => page.link === currentPageLink);

    // If the current page doesn't exist in filtered results, still show it at the top
    if (!currentPageExists && currentPageLink) {
      const pageTitle = this.pageEditor.newPageTitle || null;
      const newPageItem = this.createNewPageItem(currentPageLink, pageTitle);
      listEl.appendChild(newPageItem);
    }

    pages.forEach(page => {
      const li = document.createElement('li');
      const item = document.createElement('div');
      item.className = 'web-page-item';
      item.setAttribute('data-page-id', page.id);
      item.setAttribute('data-page-link', page.link);

      // Check if this is the currently editing page
      if (currentPageLink === page.link) {
        item.classList.add('selected');
      }

      const info = document.createElement('div');
      info.className = 'web-page-info';

      const title = document.createElement('div');
      title.className = 'web-page-title';
      title.textContent = page.title;

      const link = document.createElement('div');
      link.className = 'web-page-link';
      link.textContent = page.link;

      info.appendChild(title);
      info.appendChild(link);
      item.appendChild(info);

      li.appendChild(item);
      listEl.appendChild(li);
    });
  }

  /**
   * Create a new page item for pages that don't exist yet
   * @param {string} pageLink - The page link
   * @param {string} pageTitle - Optional title for the page (defaults to "New Page")
   */
  createNewPageItem(pageLink, pageTitle = null) {
    const li = document.createElement('li');
    const item = document.createElement('div');
    item.className = 'web-page-item new-page-item';
    item.setAttribute('data-page-id', 'new');
    item.setAttribute('data-page-link', pageLink);
    item.classList.add('selected'); // Mark as selected since it's the current page

    // Set the selected state
    this.selectedPageId = 'new';
    this.selectedPageLink = pageLink;

    const info = document.createElement('div');
    info.className = 'web-page-info';

    const title = document.createElement('div');
    title.className = 'web-page-title';
    // Use provided title or default to "New Page"
    const displayTitle = pageTitle || 'New Page';
    title.innerHTML = `<i class="far fa-plus-circle"></i> ${this.escapeHtml(displayTitle)}`;

    const link = document.createElement('div');
    link.className = 'web-page-link';
    link.textContent = pageLink;

    const status = document.createElement('div');
    status.className = 'web-page-status';
    status.style.cssText = 'font-size: 11px; color: #28a745; font-weight: 600; margin-top: 2px;';
    status.textContent = 'Not saved yet';

    info.appendChild(title);
    info.appendChild(link);
    info.appendChild(status);
    item.appendChild(info);

    li.appendChild(item);
    return li;
  }

  /**
   * Handle page selection
   */
  async handlePageClick(e) {
    const item = e.target.closest('.web-page-item');
    if (!item) {
      return;
    }

    const pageId = item.getAttribute('data-page-id');
    const pageLink = item.getAttribute('data-page-link');

    // Hide error message when page is selected
    this.hideError();

    // Check if user has unsaved changes
    const isDirty = this.pageEditor.isDirty && this.pageEditor.isDirty();
    console.log('Page click - isDirty:', isDirty, 'History length:', this.pageEditor.history.length);
    
    if (isDirty) {
      const confirmed = await this.pageEditor.showConfirmDialog('You have unsaved changes. Are you sure you want to switch pages?');
      if (!confirmed) {
        return;
      }
    }

    this.switchPage(pageLink);
  }

  /**
   * Switch to a different page for editing
   */
  switchPage(pageLink) {
    console.log('Switching to page:', pageLink);

    // Update the selected state
    document.querySelectorAll('.web-page-item').forEach(item => {
      item.classList.remove('selected');
    });
    const selectedItem = document.querySelector(`[data-page-link="${pageLink}"]`);
    if (selectedItem) {
      selectedItem.classList.add('selected');
      this.selectedPageId = selectedItem.getAttribute('data-page-id');
    }

    this.selectedPageLink = pageLink;

    // Load the page content
    this.loadPageContent(pageLink);
  }

  /**
   * Load the content of a specific page
   */
  loadPageContent(pageLink) {
    // Check if this is a new page (doesn't exist in the server yet)
    const isNewPage = this.selectedPageId === 'new';
    
    // Clear any selections in the canvas controller
    if (this.pageEditor.canvasController) {
      this.pageEditor.canvasController.selectedElement = null;
      this.pageEditor.canvasController.selectedContext = null;
    }
    
    // Clear any locked selections in the preview hover manager
    if (window.previewHoverManager && typeof window.previewHoverManager.clearLockedSelection === 'function') {
      window.previewHoverManager.clearLockedSelection();
    }
    
    // Clear the properties panel
    if (this.pageEditor.propertiesPanel && typeof this.pageEditor.propertiesPanel.clear === 'function') {
      this.pageEditor.propertiesPanel.clear();
    }
    
    if (isNewPage) {
      // For new pages, start with empty content
      console.log('Loading new page:', pageLink);
      
      // Show loading indicator briefly for consistency
      this.pageEditor.showLoadingIndicator('Creating new page...');
      
      // Update the editor config for the new page
      this.pageEditor.config.webPageLink = pageLink;
      this.pageEditor.config.existingXml = '';
      this.pageEditor.config.hasExistingLayout = false;

      // Clear current layout
      this.pageEditor.layoutManager.structure = { rows: [] };
      // Reset ID counters so row/column/widget IDs start fresh per page
      if (this.pageEditor.layoutManager && this.pageEditor.layoutManager.resetIds) {
        this.pageEditor.layoutManager.resetIds();
      }
      
      // Render empty canvas
      const canvas = this.pageEditor.elements.canvas;
      canvas.innerHTML = `
        <div class="empty-canvas" style="cursor: pointer;">
          <i class="far fa-plus-circle fa-3x margin-bottom-10"></i>
          <h5>Start Building Your New Page</h5>
          <p>Click "Add Row" to begin or drag widgets from the palette</p>
        </div>
      `;
      
      // Set up click handler for empty canvas
      const emptyCanvas = canvas.querySelector('.empty-canvas');
      if (emptyCanvas) {
        emptyCanvas.addEventListener('click', () => this.pageEditor.addRow());
      }
      
      // Reset history for new page
      this.pageEditor.history = [];
      this.pageEditor.historyIndex = -1;
      this.pageEditor.saveToHistory();
      
      // Set baseline for dirty detection
      this.pageEditor.setSavedState();
      
      // Update save indicator
      this.pageEditor.updateSaveIndicator();

      // Hide loading indicator
      setTimeout(() => {
        this.pageEditor.hideLoadingIndicator();
      }, 300);

      // Dispatch page changed event
      document.dispatchEvent(new CustomEvent('pageChanged', { detail: { pageLink } }));
      
      return;
    }
    
    // Show loading indicator in toolbar
    this.pageEditor.showLoadingIndicator('Loading page...');
    
    // Show loading state in canvas
    const canvas = this.pageEditor.elements.canvas;

    // Fetch the page content using the JSON service
    const encodedLink = encodeURIComponent(pageLink);
    fetch(`/json/webPageContent?link=${encodedLink}`)
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        if (data && data.pageXml !== undefined) {
          // Update the editor config and reload the layout
          this.pageEditor.config.webPageLink = pageLink;
          this.pageEditor.config.existingXml = data.pageXml;
          this.pageEditor.config.hasExistingLayout = data.pageXml && data.pageXml.length > 0;

          // Clear current layout and reset ID counters before loading the new one
          this.pageEditor.layoutManager.structure = { rows: [] };
          if (this.pageEditor.layoutManager && this.pageEditor.layoutManager.resetIds) {
            this.pageEditor.layoutManager.resetIds();
          }
          if (this.pageEditor.config.hasExistingLayout) {
            this.pageEditor.loadExistingLayout(data.pageXml);
          } else {
            // Hide loading indicator if no existing layout to load
            this.pageEditor.hideLoadingIndicator();
          }
          
          // Reset history and save the loaded state as the new baseline
          this.pageEditor.history = [];
          this.pageEditor.historyIndex = -1;
          this.pageEditor.saveToHistory();
          
          // Set baseline for dirty detection - must be called AFTER saveToHistory
          this.pageEditor.setSavedState();
          
          // Force update save indicator to reflect clean state
          if (this.pageEditor.updateSaveIndicator) {
            this.pageEditor.updateSaveIndicator();
          }

          // Dispatch a custom event to notify that the page has been switched
          document.dispatchEvent(new CustomEvent('pageChanged', { detail: { pageLink } }));

          console.log('Page content loaded successfully - state is clean');
        } else {
          throw new Error('No page content returned');
        }
      })
      .catch(error => {
        console.error('Error loading page content:', error);
        
        // Hide loading indicator
        this.pageEditor.hideLoadingIndicator();
        
        canvas.innerHTML = `
          <div style="padding: 40px; color: #721c24; background: #f8d7da; border-radius: 4px;">
            <i class="fa fa-exclamation-triangle"></i> Error loading page: ${error.message}
          </div>
        `;
      });
  }

  /**
   * Get the current selected page link
   */
  getSelectedPageLink() {
    return this.selectedPageLink;
  }

  /**
   * Update the new page item after successful save
   * This converts the "new page" item to a regular page item
   */
  updateNewPageAfterSave(pageData) {
    const newPageItem = document.querySelector('.new-page-item');
    if (newPageItem && pageData) {
      // Remove the new-page-item class and styling
      newPageItem.classList.remove('new-page-item');
      
      // Update the data attributes
      newPageItem.setAttribute('data-page-id', pageData.id || 'saved');
      
      // Update the title to remove the "New Page" indicator
      const titleEl = newPageItem.querySelector('.web-page-title');
      if (titleEl) {
        titleEl.innerHTML = pageData.title || this.extractTitleFromLink(pageData.link);
      }
      
      // Remove or update the status indicator
      const statusEl = newPageItem.querySelector('.web-page-status');
      if (statusEl) {
        statusEl.textContent = 'Saved';
        statusEl.style.color = '#28a745';
        // Remove the status after a delay
        setTimeout(() => {
          if (statusEl.parentNode) {
            statusEl.parentNode.removeChild(statusEl);
          }
        }, 2000);
      }
      
      // Update the selected page ID
      this.selectedPageId = pageData.id || 'saved';
      
      console.log('Updated new page item after save');
    }
  }

  /**
   * Extract a readable title from a page link
   */
  extractTitleFromLink(link) {
    if (!link) return 'Untitled Page';
    
    // Remove leading slash and convert to title case
    const cleanLink = link.replace(/^\/+/, '');
    return cleanLink
      .split(/[-_\/]/)
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ') || 'Home Page';
  }

  /**
   * Refresh the pages list from the server
   * This is useful after saving a new page to get the updated list
   */
  refreshPagesList() {
    console.log('Refreshing pages list...');
    this.loadPages();
  }

  /**
   * Escape HTML special characters
   * @param {string} str - The string to escape
   * @returns {string} The escaped string
   */
  escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  /**
   * Show error message with dismiss button
   */
  showError(errorEl, message) {
    if (errorEl) {
      errorEl.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px;">
          <span>${this.escapeHtml(message)}</span>
          <button type="button" class="close-error-btn" aria-label="Close error" style="background: none; border: none; padding: 0; cursor: pointer; color: #721c24; font-size: 20px; line-height: 1; flex-shrink: 0;">
            ×
          </button>
        </div>
      `;
      errorEl.style.display = 'block';
      
      // Add click handler for close button
      const closeBtn = errorEl.querySelector('.close-error-btn');
      if (closeBtn) {
        closeBtn.addEventListener('click', () => this.hideError());
      }
    }
  }

  /**
   * Hide error message
   */
  hideError() {
    const errorEl = document.getElementById('pages-error');
    if (errorEl) {
      errorEl.style.display = 'none';
      errorEl.innerHTML = '';
    }
  }

  /**
   * Load pages in hierarchy mode
   */
  loadHierarchyPages() {
    const loadingEl = document.getElementById('pages-loading');
    const errorEl = document.getElementById('pages-error');
    const listEl = document.getElementById('web-page-list');

    if (loadingEl) {
      loadingEl.style.display = 'block';
    }

    // Construct URL with parentId parameter
    const parentIdParam = this.currentParentId !== null ? this.currentParentId : 'null';
    const url = `/json/pages/children?parentId=${parentIdParam}`;

    fetch(url)
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then(result => {
        if (loadingEl) {
          loadingEl.style.display = 'none';
        }

        if (result.status === 'ok' && result.data) {
          this.pages = result.data;
          this.updateHierarchyNavigation();
          this.renderHierarchyList(result.data);
        } else {
          throw new Error(result.error || 'Failed to load hierarchy');
        }
      })
      .catch(error => {
        console.error('Error loading hierarchy:', error);
        if (loadingEl) {
          loadingEl.style.display = 'none';
        }
        if (errorEl) {
          this.showError(errorEl, 'Error loading page hierarchy: ' + error.message);
        }
      });
  }

  /**
   * Update hierarchy navigation UI
   */
  updateHierarchyNavigation() {
    const pathEl = document.getElementById('pages-current-path');
    const backToParentBtn = document.getElementById('pages-back-to-parent');

    if (pathEl) {
      if (this.hierarchyStack.length === 0) {
        pathEl.textContent = 'Root Level';
      } else {
        const pathParts = this.hierarchyStack.map(item => item.title);
        pathEl.textContent = pathParts.join(' > ');
      }
    }

    if (backToParentBtn) {
      backToParentBtn.style.display = this.hierarchyStack.length > 0 ? 'inline-block' : 'none';
    }
  }

  /**
   * Render hierarchy list with expand/collapse icons
   */
  renderHierarchyList(pages) {
    const listEl = document.getElementById('web-page-list');
    if (!listEl) {
      return;
    }

    listEl.innerHTML = '';

    if (!pages || pages.length === 0) {
      listEl.innerHTML = '<li style="text-align: center; padding: 40px; color: #999;">No pages at this level</li>';
      return;
    }

    pages.forEach(page => {
      const li = document.createElement('li');
      const item = document.createElement('div');
      item.className = 'web-page-item';
      if (page.hasChildren) {
        item.classList.add('has-children');
      }
      item.setAttribute('data-page-id', page.id);
      item.setAttribute('data-page-link', page.link);

      // Check if this is the currently editing page
      if (this.pageEditor.config.webPageLink === page.link) {
        item.classList.add('selected');
        this.selectedPageId = page.id;
        this.selectedPageLink = page.link;
      }

      // Add expand icon if page has children
      if (page.hasChildren) {
        const expandIcon = document.createElement('div');
        expandIcon.className = 'web-page-expand-icon';
        
        // Create icon element programmatically
        const icon = document.createElement('i');
        icon.className = 'far fa-chevron-right';
        expandIcon.appendChild(icon);
        
        expandIcon.setAttribute('data-page-id', page.id);
        expandIcon.setAttribute('data-page-title', page.title);
        expandIcon.addEventListener('click', (e) => {
          e.stopPropagation();
          this.expandHierarchyNode(page.id, page.title);
        });
        item.appendChild(expandIcon);
      }

      const info = document.createElement('div');
      info.className = 'web-page-info';

      const title = document.createElement('div');
      title.className = 'web-page-title';
      title.textContent = page.title;

      const link = document.createElement('div');
      link.className = 'web-page-link';
      link.textContent = page.link;

      info.appendChild(title);
      info.appendChild(link);
      item.appendChild(info);

      li.appendChild(item);
      listEl.appendChild(li);
    });
  }

  /**
   * Expand a hierarchy node to show its children
   */
  expandHierarchyNode(pageId, pageTitle) {
    this.hierarchyStack.push({ id: pageId, title: pageTitle });
    this.currentParentId = pageId;
    this.loadPages();
  }

  /**
   * Format a timestamp for display
   * Handles both number and string timestamps from JSON responses
   */
  formatDate(timestamp) {
    // Validate and convert timestamp
    if (!timestamp) {
      return 'Unknown';
    }
    
    // Handle string timestamps from JSON
    const timestampNum = typeof timestamp === 'string' ? parseInt(timestamp, 10) : timestamp;
    
    if (typeof timestampNum !== 'number' || isNaN(timestampNum)) {
      return 'Unknown';
    }
    
    const date = new Date(timestampNum);
    
    // Check for invalid date
    if (isNaN(date.getTime())) {
      return 'Unknown';
    }
    
    const now = new Date();
    
    // Handle future dates
    if (date > now) {
      const options = { year: 'numeric', month: 'short', day: 'numeric' };
      return date.toLocaleDateString(undefined, options);
    }
    
    // Compare calendar dates, not just time differences
    // This ensures accurate "Today"/"Yesterday" across midnight boundaries
    const dateStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const nowStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const diffMs = nowStart - dateStart;
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
      return 'Today';
    } else if (diffDays === 1) {
      return 'Yesterday';
    } else if (diffDays < 7) {
      return `${diffDays} days ago`;
    } else if (diffDays < 30) {
      // Use 30 days as approximation for readability
      // More precise than actual month length, simpler than date math
      const weeks = Math.floor(diffDays / 7);
      return `${weeks} ${weeks === 1 ? 'week' : 'weeks'} ago`;
    } else {
      // For dates older than ~1 month, show absolute date
      const options = { year: 'numeric', month: 'short', day: 'numeric' };
      return date.toLocaleDateString(undefined, options);
    }
  }
}


