/**
 * Web Page Link Picker Modal
 * Universal modal for selecting web page links from the sitemap
 * 
 * @author AI Assistant
 * @created 1/10/26
 */

class PageLinkPickerModal {
  constructor() {
    this.modal = null;
    this.callback = null;
    this.pages = [];
  }

  /**
   * Show the page link picker modal
   * @param {Function} callback - Function to call when page is selected, receives page link
   * @param {string} currentLink - Currently selected link (optional)
   */
  show(callback, currentLink = '') {
    this.callback = callback;
    
    if (!this.modal) {
      this.createModal();
    }
    
    // Reset search
    const searchInput = this.modal.querySelector('#page-search');
    if (searchInput) {
      searchInput.value = '';
    }
    
    // Load pages from server
    this.loadPages(currentLink);
    
    this.modal.style.display = 'flex';
  }

  /**
   * Hide the modal
   */
  hide() {
    if (this.modal) {
      this.modal.style.display = 'none';
    }
  }

  /**
   * Create the modal DOM structure
   */
  createModal() {
    this.modal = document.createElement('div');
    this.modal.id = 'page-link-picker-modal';
    this.modal.className = 'modal-overlay';
    this.modal.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      display: none;
      align-items: center;
      justify-content: center;
      z-index: 99999;
    `;
    
    this.modal.innerHTML = `
      <div class="modal-content" style="width: 90%; max-width: 700px; max-height: 80vh; display: flex; flex-direction: column; overflow: hidden; background: #fff; border-radius: 8px; padding: 30px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);">
        <h4 style="margin: 0 0 15px 0; flex-shrink: 0;">Choose a Page</h4>
        <div style="margin-bottom: 15px; flex-shrink: 0;">
          <input type="text" id="page-search" class="property-input" placeholder="Search pages..." style="width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px;" />
        </div>
        <div id="page-list-container" style="flex: 1; overflow-y: auto; border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #f9f9f9; min-height: 200px;">
          <div id="page-loading" style="text-align: center; padding: 40px; color: #999;">
            <i class="fa fa-spinner fa-spin"></i> Loading pages...
          </div>
          <div id="page-error" style="display: none; padding: 20px; background: #f8d7da; border: 1px solid #f5c6cb; border-radius: 4px; color: #721c24;"></div>
          <div id="page-list"></div>
        </div>
        <div style="text-align: right; margin-top: 15px; display: flex; gap: 10px; justify-content: flex-end; flex-shrink: 0;">
          <button type="button" id="cancel-page-picker" class="button tiny secondary radius">Cancel</button>
        </div>
      </div>
    `;
    
    document.body.appendChild(this.modal);
    
    // Set up event listeners
    this.setupEventListeners();
  }

  /**
   * Set up event listeners
   */
  setupEventListeners() {
    // Cancel button
    const cancelBtn = this.modal.querySelector('#cancel-page-picker');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => this.hide());
    }
    
    // Search input
    const searchInput = this.modal.querySelector('#page-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();
        const filtered = query ? this.pages.filter(page => 
          page.title.toLowerCase().includes(query) || page.link.toLowerCase().includes(query)
        ) : this.pages;
        this.renderPages(filtered);
      });
    }
    
    // Close on overlay click
    this.modal.addEventListener('click', (e) => {
      if (e.target === this.modal) {
        this.hide();
      }
    });
  }

  /**
   * Load pages from the server
   */
  loadPages(currentLink = '') {
    const loadingDiv = this.modal.querySelector('#page-loading');
    const errorDiv = this.modal.querySelector('#page-error');
    const listDiv = this.modal.querySelector('#page-list');
    
    loadingDiv.style.display = 'block';
    errorDiv.style.display = 'none';
    listDiv.innerHTML = '';
    
    // Fetch pages from the sitemap endpoint
    fetch('/json/webPageList')
      .then(response => {
        if (!response.ok) {
          throw new Error('Failed to load pages');
        }
        return response.json();
      })
      .then(data => {
        loadingDiv.style.display = 'none';
        
        // Parse web page data
        this.pages = [];
        if (data && Array.isArray(data)) {
          data.forEach(page => {
            if (page.link) {
              this.pages.push({
                title: page.title,
                link: page.link,
              });
            }
          });
        }
        
        if (this.pages.length === 0) {
          listDiv.innerHTML = '<div style="text-align: center; padding: 40px; color: #999;">No pages found</div>';
        } else {
          this.renderPages(this.pages, currentLink);
        }
      })
      .catch(error => {
        loadingDiv.style.display = 'none';
        errorDiv.style.display = 'block';
        errorDiv.textContent = 'Error loading pages: ' + error.message;
      });
  }

  /**
   * Render pages in the list
   */
  renderPages(pages, currentLink = '') {
    const listDiv = this.modal.querySelector('#page-list');
    if (!listDiv) return;
    
    if (pages.length === 0) {
      listDiv.innerHTML = '<div style="text-align: center; padding: 40px; color: #999;">No pages found</div>';
      return;
    }
    
    listDiv.innerHTML = '';
    
    pages.forEach(page => {
      const isSelected = currentLink === page.link;
      const pageItem = document.createElement('div');
      pageItem.className = 'page-picker-item';
      pageItem.style.cssText = `
        display: flex;
        flex-direction: column;
        padding: 12px 15px;
        border: 2px solid ${isSelected ? '#007bff' : '#ddd'};
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;
        background: ${isSelected ? '#e7f3ff' : '#fff'};
        margin-bottom: 10px;
      `;
      
      const titleDiv = document.createElement('div');
      titleDiv.style.cssText = 'font-weight: 600; color: #333; margin-bottom: 4px;';
      titleDiv.textContent = page.title;
      
      const linkDiv = document.createElement('div');
      linkDiv.style.cssText = 'font-size: 12px; color: #666;';
      linkDiv.textContent = page.link;
      
      pageItem.appendChild(titleDiv);
      pageItem.appendChild(linkDiv);
      
      // Hover effect
      pageItem.addEventListener('mouseenter', () => {
        if (!isSelected) {
          pageItem.style.borderColor = '#007bff';
          pageItem.style.background = '#f0f7ff';
        }
      });
      
      pageItem.addEventListener('mouseleave', () => {
        if (!isSelected) {
          pageItem.style.borderColor = '#ddd';
          pageItem.style.background = '#fff';
        }
      });
      
      // Click handler
      pageItem.addEventListener('click', () => {
        if (this.callback) {
          this.callback(page.link);
        }
        this.hide();
      });
      
      listDiv.appendChild(pageItem);
    });
  }
}

// Create global instance
window.pageLinkPickerModal = new PageLinkPickerModal();
