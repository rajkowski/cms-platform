/**
 * Info Tab Manager
 * Manages the Info tab content and page metadata editing in the visual editor
 * 
 * @author matt rajkowski
 * @created 1/10/26 12:00 PM
 */

class InfoTabManager {
  constructor(editor, rightPanelTabs) {
    this.editor = editor;
    this.rightPanelTabs = rightPanelTabs;
    this.originalData = null;
    this.currentData = null;
    this.sitemapChangeFrequencyOptions = {};
    this.container = null;
    this.isLoading = false;
    this.currentPageLink = null;
  }
  
  /**
   * Initialize the Info tab manager
   */
  init() {
    this.container = document.getElementById('info-tab-content');
    if (!this.container) {
      console.warn('InfoTabManager: Container element not found');
      return;
    }
    
    // Listen for page changes (Requirements 2.6, 7.1)
    document.addEventListener('pageChanged', (e) => {
      const pageLink = e.detail?.pageLink || e.detail?.link;
      if (pageLink) {
        // Clear previous data before loading new page
        this.clearForPageChange();
        this.loadPageInfo(pageLink);
      }
    });
    
    console.log('InfoTabManager initialized');
  }
  
  /**
   * Clear data when switching pages (preserves tab selection)
   * Different from clear() which is for complete reset
   */
  clearForPageChange() {
    this.originalData = null;
    this.currentData = null;
    // Don't clear currentPageLink yet - it will be set by loadPageInfo
    // Don't clear dirty state here - RightPanelTabs handles that
  }
  
  /**
   * Load page info from the API
   * @param {string} webPageLink - The page link to load info for
   */
  async loadPageInfo(webPageLink) {
    if (!webPageLink || this.isLoading) {
      return;
    }
    
    this.isLoading = true;
    this.currentPageLink = webPageLink;
    
    // Check if this is a new page (not saved yet)
    const isNewPage = this.editor.pagesTabManager?.selectedPageId === 'new';
    
    if (isNewPage) {
      // For new pages, create default data structure
      console.log('Loading info for new page:', webPageLink);
      
      // Get the title from the editor if available
      const pageTitle = this.editor.newPageTitle || '';
      
      this.originalData = {
        title: pageTitle,
        link: webPageLink,
        keywords: '',
        description: '',
        draft: true,
        searchable: true,
        showInSitemap: true,
        sitemapPriority: 0.5,
        sitemapChangeFrequency: '',
        imageUrl: ''
      };
      this.currentData = { ...this.originalData };
      this.sitemapChangeFrequencyOptions = {
        'always': 'Always',
        'hourly': 'Hourly',
        'daily': 'Daily',
        'weekly': 'Weekly',
        'monthly': 'Monthly',
        'yearly': 'Yearly',
        'never': 'Never'
      };
      
      this.isLoading = false;
      this.render();
      return;
    }
    
    // Show loading state for existing pages
    this.container.innerHTML = `
      <div style="text-align: center; padding: 20px; color: var(--editor-text-muted);">
        <i class="fa fa-spinner fa-spin"></i> Loading page info...
      </div>
    `;
    
    try {
      const response = await fetch(`/json/webPageInfo?link=${encodeURIComponent(webPageLink)}`);
      const data = await response.json();
      
      if (data.error) {
        this.showError(data.error);
        return;
      }
      
      // Store original data for change detection
      this.originalData = { ...data };
      this.currentData = { ...data };
      this.sitemapChangeFrequencyOptions = data.sitemapChangeFrequencyOptions || {};
      
      // Render the form
      this.render();
      
    } catch (error) {
      console.error('Error loading page info:', error);
      this.showError('Failed to load page info');
    } finally {
      this.isLoading = false;
    }
  }
  
  /**
   * Show an error message
   * @param {string} message - The error message to display
   */
  showError(message) {
    this.container.innerHTML = `
      <div style="padding: 15px; background: rgba(220, 53, 69, 0.1); border: 1px solid rgba(220, 53, 69, 0.3); border-radius: 4px; color: #dc3545;">
        <i class="fa fa-exclamation-triangle"></i> ${message}
      </div>
    `;
  }
  
  /**
   * Render the info form
   */
  render() {
    if (!this.currentData) {
      this.container.innerHTML = `
        <p style="color: var(--editor-text-muted); font-size: 14px;">Select a page to view its information</p>
      `;
      return;
    }
    
    const data = this.currentData;
    
    // Check if this is the home page (link = "/")
    const isHomePage = data.link === '/';
    
    // Build sitemap change frequency options
    let frequencyOptions = '<option value=""></option>';
    for (const [key, label] of Object.entries(this.sitemapChangeFrequencyOptions)) {
      const selected = data.sitemapChangeFrequency === key ? ' selected' : '';
      frequencyOptions += `<option value="${key}"${selected}>${label}</option>`;
    }
    
    this.container.innerHTML = `
      <div class="info-tab-form">
        <!-- Title -->
        <div class="property-group">
          <label class="property-label" for="info-title">Title</label>
          <input type="text" id="info-title" class="property-input" 
                 value="${this.escapeHtml(data.title || '')}" 
                 placeholder="Page title" 
                 ${isHomePage ? 'readonly style="background: var(--editor-hover-bg); cursor: not-allowed;"' : ''} />
          ${isHomePage ? '<div style="font-size: 12px; color: var(--editor-text-muted); margin-top: 5px;">Home page title cannot be edited here</div>' : ''}
        </div>
        
        <!-- Link (read-only) -->
        <div class="property-group">
          <label class="property-label" for="info-link">Link</label>
          <input type="text" id="info-link" class="property-input" 
                 value="${this.escapeHtml(data.link || '')}" 
                 readonly 
                 style="background: var(--editor-hover-bg); cursor: not-allowed;" />
        </div>
        
        <!-- Keywords -->
        <div class="property-group">
          <label class="property-label" for="info-keywords">Keywords</label>
          <input type="text" id="info-keywords" class="property-input" 
                 value="${this.escapeHtml(data.keywords || '')}" 
                 placeholder="Comma-separated keywords" />
        </div>
        
        <!-- Description -->
        <div class="property-group">
          <label class="property-label" for="info-description">Description</label>
          <textarea id="info-description" class="property-input" 
                    rows="3" 
                    placeholder="Page description">${this.escapeHtml(data.description || '')}</textarea>
        </div>
        
        <!-- Toggle Fields -->
        <div class="property-group">
          <label class="property-label">
            <input type="checkbox" id="info-publish" ${!data.draft ? 'checked' : ''} />
            Publish
          </label>
        </div>
        
        <div class="property-group">
          <label class="property-label">
            <input type="checkbox" id="info-searchable" ${data.searchable ? 'checked' : ''} />
            Searchable
          </label>
        </div>
        
        <div class="property-group">
          <label class="property-label">
            <input type="checkbox" id="info-show-in-sitemap" ${data.showInSitemap ? 'checked' : ''} />
            Show in Sitemap
          </label>
        </div>
        
        <!-- Sitemap Priority -->
        <div class="property-group">
          <label class="property-label" for="info-sitemap-priority">Sitemap Priority (0.0-1.0)</label>
          <input type="number" id="info-sitemap-priority" class="property-input" 
                 value="${data.sitemapPriority || '0.5'}" 
                 min="0" max="1" step="0.1" />
        </div>
        
        <!-- Sitemap Change Frequency -->
        <div class="property-group">
          <label class="property-label" for="info-sitemap-frequency">Change Frequency</label>
          <select id="info-sitemap-frequency" class="property-input">
            ${frequencyOptions}
          </select>
        </div>
        
        <!-- Open Graph Image -->
        <div class="property-group">
          <label class="property-label" for="info-image-url">Open Graph Image</label>
          <div style="display: flex; gap: 8px; align-items: flex-start;">
            <input type="text" id="info-image-url" class="property-input" 
                   value="${this.escapeHtml(data.imageUrl || '')}" 
                   placeholder="Image URL" 
                   style="flex: 1;" />
          </div>
          ${data.imageUrl ? `
            <div style="margin-top: 8px;">
              <img id="info-image-preview" src="${this.escapeHtml(data.imageUrl)}" 
                   style="max-width: 100%; max-height: 100px; border-radius: 4px; border: 1px solid var(--editor-border);" 
                   onerror="this.style.display='none'" />
            </div>
          ` : ''}
        </div>
      </div>
    `;
    
    // Set up event listeners for change detection
    this.setupEventListeners();
  }
  
  /**
   * Set up event listeners for form fields
   */
  setupEventListeners() {
    const fields = [
      'info-title',
      'info-keywords',
      'info-description',
      'info-publish',
      'info-searchable',
      'info-show-in-sitemap',
      'info-sitemap-priority',
      'info-sitemap-frequency',
      'info-image-url'
    ];
    
    fields.forEach(fieldId => {
      const element = document.getElementById(fieldId);
      if (element) {
        const eventType = element.type === 'checkbox' ? 'change' : 'input';
        element.addEventListener(eventType, () => {
          this.updateCurrentData();
          if (this.hasChanges()) {
            this.rightPanelTabs.markDirty('info');
          }
        });
      }
    });
    
    // Special handling for title field on new pages - update left panel in real-time
    const titleInput = document.getElementById('info-title');
    if (titleInput) {
      titleInput.addEventListener('input', () => {
        // Check if this is a new page
        const isNewPage = this.editor.pagesTabManager?.selectedPageId === 'new';
        if (isNewPage) {
          const newTitle = titleInput.value.trim() || 'New Page';
          this.updatePageTitleInLeftPanel(this.currentPageLink, newTitle);
        }
      });
    }
    
    // Update image preview when URL changes
    const imageUrlInput = document.getElementById('info-image-url');
    if (imageUrlInput) {
      imageUrlInput.addEventListener('input', () => {
        const preview = document.getElementById('info-image-preview');
        if (preview) {
          preview.src = imageUrlInput.value;
          preview.style.display = imageUrlInput.value ? 'block' : 'none';
        }
      });
    }
  }
  
  /**
   * Update current data from form fields
   */
  updateCurrentData() {
    if (!this.currentData) return;
    
    this.currentData.title = document.getElementById('info-title')?.value || '';
    this.currentData.keywords = document.getElementById('info-keywords')?.value || '';
    this.currentData.description = document.getElementById('info-description')?.value || '';
    this.currentData.draft = !document.getElementById('info-publish')?.checked;
    this.currentData.searchable = document.getElementById('info-searchable')?.checked || false;
    this.currentData.showInSitemap = document.getElementById('info-show-in-sitemap')?.checked || false;
    this.currentData.sitemapPriority = parseFloat(document.getElementById('info-sitemap-priority')?.value) || 0.5;
    this.currentData.sitemapChangeFrequency = document.getElementById('info-sitemap-frequency')?.value || '';
    this.currentData.imageUrl = document.getElementById('info-image-url')?.value || '';
  }
  
  /**
   * Check if there are unsaved changes
   * @returns {boolean} True if there are changes
   */
  hasChanges() {
    if (!this.originalData || !this.currentData) {
      return false;
    }
    
    const fieldsToCompare = [
      'title', 'keywords', 'description', 'draft', 
      'searchable', 'showInSitemap', 'sitemapPriority', 
      'sitemapChangeFrequency', 'imageUrl'
    ];
    
    for (const field of fieldsToCompare) {
      const original = this.originalData[field] ?? '';
      const current = this.currentData[field] ?? '';
      
      // Handle numeric comparison
      if (field === 'sitemapPriority') {
        if (parseFloat(original) !== parseFloat(current)) {
          return true;
        }
      } else if (original !== current) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Get current form data
   * @returns {Object} The current form data
   */
  getData() {
    this.updateCurrentData();
    return { ...this.currentData };
  }
  
  /**
   * Save page info to the API
   * @returns {Promise<Object>} The save result
   */
  async save() {
    if (!this.currentData || !this.currentPageLink) {
      return { success: false, message: 'No data to save' };
    }
    
    this.updateCurrentData();
    
    try {
      const formData = new FormData();
      formData.append('token', document.querySelector('input[name="token"]')?.value || '');
      formData.append('link', this.currentPageLink);
      formData.append('title', this.currentData.title || '');
      formData.append('keywords', this.currentData.keywords || '');
      formData.append('description', this.currentData.description || '');
      formData.append('publish', !this.currentData.draft ? 'true' : 'false');
      formData.append('searchable', this.currentData.searchable ? 'true' : 'false');
      formData.append('showInSitemap', this.currentData.showInSitemap ? 'true' : 'false');
      formData.append('sitemapPriority', this.currentData.sitemapPriority?.toString() || '0.5');
      formData.append('sitemapChangeFrequency', this.currentData.sitemapChangeFrequency || '');
      formData.append('imageUrl', this.currentData.imageUrl || '');
      
      console.log('Saving page info with data:', {
        link: this.currentPageLink,
        title: this.currentData.title,
        publish: !this.currentData.draft,
        searchable: this.currentData.searchable,
        showInSitemap: this.currentData.showInSitemap,
        sitemapPriority: this.currentData.sitemapPriority,
        sitemapChangeFrequency: this.currentData.sitemapChangeFrequency
      });
      
      const response = await fetch('/json/saveWebPageInfo', {
        method: 'POST',
        body: formData
      });
      
      // Check if response is OK
      if (!response.ok) {
        const errorText = await response.text();
        console.error('HTTP error response:', response.status, errorText);
        return { success: false, message: `HTTP ${response.status}: ${errorText}` };
      }
      
      const result = await response.json();
      console.log('Save page info result:', result);
      
      if (result.success) {
        // Update original data to match current (changes saved)
        this.originalData = { ...this.currentData };
        this.rightPanelTabs.clearDirtyForTab('info');
        
        // If the title changed, update the page name in the left panel
        if (this.currentData.title !== this.originalData.title) {
          this.updatePageTitleInLeftPanel(this.currentPageLink, this.currentData.title);
        }
      }
      
      return result;
      
    } catch (error) {
      console.error('Error saving page info:', error);
      return { success: false, message: 'Failed to save page info: ' + error.message };
    }
  }
  
  /**
   * Escape HTML special characters
   * @param {string} str - The string to escape
   * @returns {string} The escaped string
   */
  escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
  
  /**
   * Reset the form to original data
   */
  reset() {
    if (this.originalData) {
      this.currentData = { ...this.originalData };
      this.render();
      this.rightPanelTabs.clearDirtyForTab('info');
    }
  }
  
  /**
   * Clear the form
   */
  clear() {
    this.originalData = null;
    this.currentData = null;
    this.currentPageLink = null;
    this.container.innerHTML = `
      <p style="color: var(--editor-text-muted); font-size: 14px;">Select a page to view its information</p>
    `;
    this.rightPanelTabs.clearDirtyForTab('info');
  }

  /**
   * Update the page title in the left panel Pages tab
   * @param {string} pageLink - The page link
   * @param {string} newTitle - The new title
   */
  updatePageTitleInLeftPanel(pageLink, newTitle) {
    // Find the page item in the left panel
    const pageItem = document.querySelector(`.web-page-item[data-page-link="${pageLink}"]`);
    if (pageItem) {
      const titleElement = pageItem.querySelector('.web-page-title');
      if (titleElement) {
        // Preserve the icon if it exists
        const icon = titleElement.querySelector('i');
        if (icon) {
          titleElement.innerHTML = '';
          titleElement.appendChild(icon);
          titleElement.appendChild(document.createTextNode(' ' + newTitle));
        } else {
          titleElement.textContent = newTitle;
        }
        console.log('Updated page title in left panel:', newTitle);
      }
    }
  }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = InfoTabManager;
}
