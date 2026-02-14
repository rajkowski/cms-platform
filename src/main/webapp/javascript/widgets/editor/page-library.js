/**
 * Page Library Manager for Visual Content Editor
 * Handles loading and rendering draggable web pages for the Pages view library
 *
 * @author matt rajkowski
 * @created 02/08/26 04:00 PM
 */

class PageLibraryManager {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.pages = [];
    this.isLoading = false;
  }

  /**
   * Initialize the page library
   */
  init() {
    this.setupEventListeners();
    this.loadPages();
  }

  /**
   * Refresh the page library
   */
  refresh() {
    this.loadPages();
  }

  /**
   * Set up event listeners
   */
  setupEventListeners() {
    const container = document.getElementById('page-library-content');
    if (!container) return;

    container.addEventListener('click', (e) => {
      const pageBox = e.target.closest('.page-hierarchy-box');
      if (!pageBox) return;

      document.querySelectorAll('.page-hierarchy-box').forEach(el => el.classList.remove('selected'));
      pageBox.classList.add('selected');

      // Show selected page in right pane preview/properties
      this.showSelectedPagePreview(pageBox);
    });
  }

  /**
   * Show selected page in right pane preview/properties
   */
  showSelectedPagePreview(pageBox) {
    const pageId = pageBox.dataset.pageId;
    const title = pageBox.querySelector('.page-box-header span')?.textContent || 'Page';
    const link = pageBox.querySelector('.page-box-link')?.textContent || '';

    // Hide any error messages when page is selected
    this.hideError();

    // Show properties and preview
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
   * Load web pages from the server
   */
  loadPages() {
    if (this.isLoading) return;

    const container = document.getElementById('page-library-content');
    if (!container) return;

    this.isLoading = true;
    this.showLoading(container);

    fetch('/json/webPageList', {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
      })
      .then(data => {
        this.isLoading = false;
        const pages = Array.isArray(data) ? data : [];
        this.pages = pages.map(page => this.normalizePage(page)).filter(page => page.link);
        this.renderLibrary(container);
        this.applyActiveSearch();
      })
      .catch(error => {
        this.isLoading = false;
        this.showError(container, 'Error loading pages: ' + error.message);
      });
  }

  normalizePage(page) {
    if (!page) {
      return { id: null, title: '', link: '' };
    }

    const id = page.id ?? page.pageId ?? page.webPageId ?? page.web_page_id ?? null;
    const title = page.title || page.name || page.link || 'Untitled Page';
    const link = this.resolvePageLink(page);

    return { id, title, link };
  }

  resolvePageLink(page) {
    if (!page) return '';

    return page.link || page.pageLink || page.url || page.path || '';
  }

  showLoading(container) {
    container.innerHTML = `
      <div class="page-library-status" style="text-align: center; padding: 40px; color: var(--editor-text-muted);">
        Loading pages...
      </div>
    `;
  }

  showError(container, message) {
    container.innerHTML = `
      <div class="page-library-status" style="text-align: center; padding: 40px; color: #c33;">
        ${this.escapeHtml(message)}
      </div>
    `;
  }

  /**
   * Hide error message
   */
  hideError() {
    const errorDiv = document.getElementById('pages-error');
    if (errorDiv) {
      errorDiv.style.display = 'none';
      errorDiv.innerHTML = '';
    }
  }

  renderLibrary(container) {
    container.innerHTML = '';

    if (this.pages.length === 0) {
      container.innerHTML = `
        <div class="page-library-status" style="text-align: center; padding: 40px; color: var(--editor-text-muted);">
          No pages found
        </div>
      `;
      return;
    }

    const section = document.createElement('div');
    section.className = 'page-hierarchy-section';
    section.style.padding = '10px 0';

    const list = document.createElement('div');
    list.className = 'page-hierarchy-container';

    const sortedPages = [...this.pages].sort((a, b) => {
      return String(a.title).localeCompare(String(b.title), undefined, { sensitivity: 'base' });
    });

    sortedPages.forEach(page => {
      const wrapper = document.createElement('div');
      wrapper.className = 'page-hierarchy-node';

      const pageEl = document.createElement('div');
      pageEl.className = 'page-hierarchy-box';
      pageEl.dataset.pageId = page.id ?? '';
      pageEl.draggable = true;
      pageEl.style.display = 'flex';
      pageEl.style.alignItems = 'center';
      pageEl.style.gap = '10px';
      pageEl.style.padding = '12px';
      pageEl.style.cursor = 'grab';

      pageEl.innerHTML = `
        <i class="far fa-file-lines"></i>
        <div style="flex: 1;">
          <div class="page-box-header">
            <span>${this.escapeHtml(page.title)}</span>
          </div>
          <div class="page-box-link">${this.escapeHtml(page.link)}</div>
        </div>
      `;

      pageEl.addEventListener('dragstart', (e) => {
        e.dataTransfer.effectAllowed = 'copy';
        e.dataTransfer.setData('application/x-page-data', JSON.stringify({
          id: page.id,
          title: page.title,
          link: page.link
        }));
        pageEl.classList.add('dragging');
        pageEl.style.opacity = '0.6';
      });

      pageEl.addEventListener('dragend', () => {
        pageEl.classList.remove('dragging');
        pageEl.style.opacity = '';
      });

      wrapper.appendChild(pageEl);
      list.appendChild(wrapper);
    });

    section.appendChild(list);
    container.appendChild(section);
  }

  applyActiveSearch() {
    const searchInput = document.getElementById('page-library-search');
    if (!searchInput) return;

    const query = searchInput.value.toLowerCase().trim();
    if (!query) return;

    const nodes = document.querySelectorAll('.page-hierarchy-container .page-hierarchy-box');
    nodes.forEach(node => {
      const title = node.querySelector('.page-box-header span')?.textContent.toLowerCase() || '';
      const link = node.querySelector('.page-box-link')?.textContent.toLowerCase() || '';
      const wrapper = node.closest('.page-hierarchy-node');

      if (wrapper) {
        if (title.includes(query) || link.includes(query)) {
          wrapper.style.display = '';
        } else {
          wrapper.style.display = 'none';
        }
      }
    });
  }

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}
