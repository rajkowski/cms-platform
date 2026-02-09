/**
 * Content List Manager for Visual Content Editor
 * Handles loading, searching, and selecting content blocks with pagination support
 * 
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

class ContentListManager {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.contentItems = [];
    this.selectedContentId = null;
    this.offset = 0;
    this.limit = 50;
    this.isLoading = false;
    this.hasMore = true;
    this.searchQuery = '';
    this.pageCache = new Map();
    this.itemHeight = 88;
    this.bufferRows = 6;
    this.virtualContainer = null;
  }

  /**
   * Initialize the content list manager
   */
  init() {
    this.setupEventListeners();
    this.setupSearchListener();
    this.setupScrollListener();
    this.loadContent();
  }

  /**
   * Set up event listeners for content items
   */
  setupEventListeners() {
    const contentList = document.getElementById('content-list');
    if (contentList) {
      contentList.addEventListener('click', (e) => this.handleContentClick(e));
    }
  }

  /**
   * Set up search/filter listener
   */
  setupSearchListener() {
    const searchInput = document.getElementById('content-search');
    if (searchInput) {
      let debounceTimer;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
          this.searchQuery = e.target.value.trim();
          this.offset = 0;
          this.contentItems = [];
          this.hasMore = true;
          this.pageCache.clear();
          this.virtualContainer = null;
          this.loadContent();
        }, 300);
      });
    }
  }

  /**
   * Set up scroll listener for infinite scroll pagination
   */
  setupScrollListener() {
    const contentList = document.getElementById('content-list');
    if (contentList) {
      contentList.addEventListener('scroll', (e) => {
        const element = e.target;
        if (element.scrollHeight - element.scrollTop <= element.clientHeight + 100) {
          if (!this.isLoading && this.hasMore) {
            this.loadContent();
          }
        }
        this.updateVirtualList();
      });
    }
  }

  /**
   * Load content blocks from the server with pagination
   */
  loadContent() {
    if (this.isLoading) return;

    this.isLoading = true;
    this.showLoading();

    const params = new URLSearchParams();
    params.append('offset', this.offset);
    params.append('limit', this.limit);
    if (this.searchQuery) {
      params.append('search', this.searchQuery);
    }

    const currentOffset = this.offset;
    if (currentOffset < this.contentItems.length) {
      this.hideLoading();
      return;
    }

    if (this.pageCache.has(currentOffset)) {
      const cachedItems = this.pageCache.get(currentOffset);
      this.applyLoadedItems(cachedItems, currentOffset);
      return;
    }

    fetch(`/json/content/list?${params}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        this.isLoading = false;

        if (data.status === 'ok' && data.data) {
          const items = Array.isArray(data.data) ? data.data : [];
          this.pageCache.set(currentOffset, items);
          this.applyLoadedItems(items, currentOffset);
        } else {
          this.showError(data.error || 'Failed to load content');
        }
      })
      .catch(error => {
        this.isLoading = false;
        console.error('Error loading content:', error);
        this.showError('Error loading content: ' + error.message);
      });
  }

  /**
   * Render the content list
   */
  renderContentList() {
    const contentList = document.getElementById('content-list');
    if (!contentList) return;

    if (this.contentItems.length === 0) {
      if (this.offset === 0) {
        contentList.innerHTML = '<div class="content-list-empty">No content blocks available</div>';
      }
      this.hideLoading();
      return;
    }

    if (!this.virtualContainer) {
      this.virtualContainer = document.createElement('div');
      this.virtualContainer.className = 'content-virtual-list';
      contentList.innerHTML = '';
      contentList.appendChild(this.virtualContainer);
    }

    this.updateVirtualList();
    this.hideLoading();
  }

  updateVirtualList() {
    const contentList = document.getElementById('content-list');
    if (!contentList || !this.virtualContainer) return;

    const viewportHeight = contentList.clientHeight || 1;
    const scrollTop = contentList.scrollTop || 0;
    const totalItems = this.contentItems.length;

    const startIndex = Math.max(0, Math.floor(scrollTop / this.itemHeight) - this.bufferRows);
    const endIndex = Math.min(totalItems, Math.ceil((scrollTop + viewportHeight) / this.itemHeight) + this.bufferRows);

    const topSpacerHeight = startIndex * this.itemHeight;
    const bottomSpacerHeight = (totalItems - endIndex) * this.itemHeight;

    let html = `<div class="content-list-spacer" style="height:${topSpacerHeight}px"></div>`;
    for (let i = startIndex; i < endIndex; i += 1) {
      html += this.renderContentItem(this.contentItems[i]);
    }
    html += `<div class="content-list-spacer" style="height:${bottomSpacerHeight}px"></div>`;

    this.virtualContainer.innerHTML = html;
  }

  /**
   * Render individual content item
   */
  renderContentItem(item) {
    const selected = this.selectedContentId === item.id ? 'selected' : '';
    const draftClass = item.draft_content ? 'has-draft' : '';
    const preview = item.content ? item.content.substring(0, 120).replace(/<[^>]*>/g, '') : '';

    return `
      <div class="content-list-item ${selected} ${draftClass}" data-content-id="${item.id}" data-unique-id="${item.unique_id}">
        <div class="content-item-header">
          <div class="content-item-title">${this.escapeHtml(item.unique_id)}</div>
          ${item.draft_content ? '<span class="draft-badge">Draft</span>' : ''}
        </div>
        <div class="content-item-preview">${this.escapeHtml(preview)}${preview.length > 100 ? '...' : ''}</div>
        <div class="content-item-meta">
          <span class="meta-modified">Modified: ${this.formatDate(item.modified)}</span>
        </div>
      </div>
    `;
  }

  /**
   * Handle content item click
   */
  handleContentClick(e) {
    const item = e.target.closest('.content-list-item');
    if (!item) return;

    const contentId = item.dataset.contentId;
    const uniqueId = item.dataset.uniqueId;

    // Update selected state
    document.querySelectorAll('.content-list-item').forEach(el => el.classList.remove('selected'));
    item.classList.add('selected');

    this.selectedContentId = contentId;

    // Load the content into the editor
    this.editorBridge.loadContent(contentId, uniqueId);
  }

  /**
   * Select content programmatically (from editor)
   */
  selectContent(contentId) {
    this.selectedContentId = contentId;

    // Highlight the item
    const targetIndex = this.contentItems.findIndex(item => String(item.id) === String(contentId));
    if (targetIndex >= 0) {
      const contentList = document.getElementById('content-list');
      if (contentList) {
        contentList.scrollTop = targetIndex * this.itemHeight;
        this.updateVirtualList();
      }
    }
  }

  /**
   * Show loading indicator
   */
  showLoading() {
    const loadingIndicator = document.getElementById('loading-indicator');
    if (loadingIndicator) {
      loadingIndicator.style.display = 'inline-block';
    }
    const contentList = document.getElementById('content-list');
    if (contentList) {
      contentList.classList.add('loading');
    }
  }

  /**
   * Hide loading indicator
   */
  hideLoading() {
    const loadingIndicator = document.getElementById('loading-indicator');
    if (loadingIndicator) {
      loadingIndicator.style.display = 'none';
    }
    const contentList = document.getElementById('content-list');
    if (contentList) {
      contentList.classList.remove('loading');
    }
  }

  /**
   * Show error message
   */
  showError(message) {
    const errorDiv = document.getElementById('content-error');
    if (errorDiv) {
      errorDiv.textContent = message;
      errorDiv.style.display = 'block';
    }
  }

  applyLoadedItems(items, currentOffset) {
    this.isLoading = false;

    if (currentOffset === 0) {
      this.contentItems = [];
    }

    if (items.length < this.limit) {
      this.hasMore = false;
    } else {
      this.offset = currentOffset + items.length;
    }

    items.forEach(item => this.contentItems.push(item));
    this.renderContentList();
  }

  /**
   * Format date for display
   */
  formatDate(dateString) {
    if (!dateString) return 'Unknown';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    } catch (e) {
      return dateString;
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
   * Refresh content list
   */
  refresh() {
    this.offset = 0;
    this.contentItems = [];
    this.hasMore = true;
    this.pageCache.clear();
    this.virtualContainer = null;
    this.loadContent();
  }
}
