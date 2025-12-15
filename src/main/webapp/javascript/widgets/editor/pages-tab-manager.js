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
  }

  /**
   * Initialize the pages tab
   */
  init() {
    this.setupEventListeners();
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

    fetch('/json/webPageList')
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
          if (emptyEl) {
            emptyEl.style.display = 'block';
          }
          if (listEl) {
            listEl.innerHTML = '';
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
          errorEl.style.display = 'block';
          errorEl.textContent = 'Error loading pages: ' + error.message;
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
      item.appendChild(info);

      li.appendChild(item);
      listEl.appendChild(li);
    });
  }

  /**
   * Handle page selection
   */
  handlePageClick(e) {
    const item = e.target.closest('.web-page-item');
    if (!item) {
      return;
    }

    const pageId = item.getAttribute('data-page-id');
    const pageLink = item.getAttribute('data-page-link');

    // Check if user has unsaved changes
    if (this.pageEditor.isDirty && this.pageEditor.isDirty()) {
      if (!confirm('You have unsaved changes. Are you sure you want to switch pages?')) {
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
    }

    this.selectedPageLink = pageLink;

    // Load the page content
    this.loadPageContent(pageLink);
  }

  /**
   * Load the content of a specific page
   */
  loadPageContent(pageLink) {
    // Show loading state
    const canvas = this.pageEditor.elements.canvas;
    const loadingHTML = `
      <div style="text-align: center; padding: 40px; color: #6c757d;">
        <i class="fa fa-spinner fa-spin"></i> Loading page...
      </div>
    `;
    canvas.innerHTML = loadingHTML;

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

          // Clear current layout and load the new one
          this.pageEditor.layoutManager.structure = { rows: [] };
          if (this.pageEditor.config.hasExistingLayout) {
            this.pageEditor.loadExistingLayout(data.pageXml);
          }
          this.pageEditor.saveToHistory();

          console.log('Page content loaded successfully');
        } else {
          throw new Error('No page content returned');
        }
      })
      .catch(error => {
        console.error('Error loading page content:', error);
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
}
