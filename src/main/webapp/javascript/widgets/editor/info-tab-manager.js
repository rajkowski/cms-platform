/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 * 
 * Info Tab Manager
 * Manages the Info tab content and page metadata editing in the visual editor
 * 
 * @author matt rajkowski
 * @created 1/10/26 12:00 PM
 */

class InfoTabManager {
  constructor(editor, rightPanelTabs) {
    this.editor = editor;
    this.token = editor.config.token;
    this.rightPanelTabs = rightPanelTabs;
    this.originalData = null;
    this.currentData = null;
    this.sitemapChangeFrequencyOptions = {};
    this.container = null;
    this.isLoading = false;
    this.currentPageLink = null;
    this.pageVersions = [];
    this.pageVersionsLoading = false;
    this.pageVersionsError = null;
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
    this.pageVersions = [];
    this.pageVersionsLoading = false;
    this.pageVersionsError = null;
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
      this.renderVersionHistorySection();
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
      this.loadVersionHistory();
      
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

        <div class="property-group version-history-group">
          <div class="version-history-header">
            <label class="property-label">Layout Versions</label>
            <button type="button" id="refresh-page-versions-btn" class="button tiny secondary radius no-gap" title="Refresh version list">
              <i class="far fa-rotate"></i>
            </button>
          </div>
          <div id="page-version-history" class="version-history-container">
            <div class="version-history-empty">Loading version history...</div>
          </div>
        </div>
      </div>
    `;
    
    // Set up event listeners for change detection
    this.setupEventListeners();
    this.renderVersionHistorySection();
  }

  getCurrentPageId() {
    const selectedPageId = parseInt(this.editor?.pagesTabManager?.selectedPageId, 10);
    if (!Number.isNaN(selectedPageId) && selectedPageId > 0) {
      return selectedPageId;
    }

    const currentDataId = parseInt(this.currentData?.id, 10);
    if (!Number.isNaN(currentDataId) && currentDataId > 0) {
      return currentDataId;
    }

    const configId = parseInt(this.editor?.config?.webPageId, 10);
    if (!Number.isNaN(configId) && configId > 0) {
      return configId;
    }

    return -1;
  }

  normalizeJsonResponse(payload) {
    if (payload && payload.status === 'ok' && Object.prototype.hasOwnProperty.call(payload, 'data')) {
      return payload.data;
    }
    return payload;
  }

  async loadVersionHistory() {
    const pageId = this.getCurrentPageId();
    if (pageId <= 0) {
      this.pageVersions = [];
      this.pageVersionsError = null;
      this.pageVersionsLoading = false;
      this.renderVersionHistorySection();
      return;
    }

    this.pageVersionsLoading = true;
    this.pageVersionsError = null;
    this.renderVersionHistorySection();

    try {
      const response = await fetch(`/json/webPageVersions?webPageId=${pageId}`);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const raw = await response.json();
      const data = this.normalizeJsonResponse(raw);
      this.pageVersions = Array.isArray(data) ? data : [];
      this.pageVersionsError = null;
    } catch (error) {
      console.error('Error loading page versions:', error);
      this.pageVersions = [];
      this.pageVersionsError = error.message || 'Failed to load versions';
    } finally {
      this.pageVersionsLoading = false;
      this.renderVersionHistorySection();
    }
  }

  renderVersionHistorySection() {
    const historyContainer = document.getElementById('page-version-history');
    const refreshButton = document.getElementById('refresh-page-versions-btn');
    if (!historyContainer) {
      return;
    }

    const pageId = this.getCurrentPageId();
    if (refreshButton) {
      refreshButton.disabled = this.pageVersionsLoading || pageId <= 0;
      refreshButton.onclick = () => this.loadVersionHistory();
    }

    if (pageId <= 0) {
      historyContainer.innerHTML = '<div class="version-history-empty">Save this page first to start tracking layout versions.</div>';
      return;
    }

    if (this.pageVersionsLoading) {
      historyContainer.innerHTML = '<div class="version-history-loading"><i class="far fa-spinner fa-spin"></i> Loading versions...</div>';
      return;
    }

    if (this.pageVersionsError) {
      historyContainer.innerHTML = `<div class="version-history-error">${this.escapeHtml(this.pageVersionsError)}</div>`;
      return;
    }

    if (!this.pageVersions || this.pageVersions.length === 0) {
      historyContainer.innerHTML = '<div class="version-history-empty">No saved versions yet.</div>';
      return;
    }

    const itemsHtml = this.pageVersions.slice(0, 30).map((version) => {
      const created = this.formatVersionTimestamp(version.created);
      const user = this.escapeHtml(version.createdByName || `User ${version.createdBy || ''}`);
      const notes = this.escapeHtml(version.notes || 'Version saved before update');
      return `
        <li class="version-history-item">
          <div class="version-history-meta">
            <div class="version-history-date">${created}</div>
            <div class="version-history-user">${user}</div>
            <div class="version-history-notes">${notes}</div>
          </div>
          <div class="version-history-actions">
            <button type="button" class="button tiny secondary radius no-gap preview-page-version-btn" data-version-id="${version.versionId}">
              Preview
            </button>
            <button type="button" class="button tiny primary radius no-gap restore-page-version-btn" data-version-id="${version.versionId}">
              Restore
            </button>
          </div>
        </li>
      `;
    }).join('');

    historyContainer.innerHTML = `<ul class="version-history-list">${itemsHtml}</ul>`;

    historyContainer.querySelectorAll('.preview-page-version-btn').forEach((button) => {
      button.addEventListener('click', () => {
        const versionId = parseInt(button.dataset.versionId, 10);
        if (!Number.isNaN(versionId) && versionId > 0) {
          const version = this.pageVersions.find((item) => parseInt(item.versionId, 10) === versionId);
          if (version) {
            this.showVersionPreviewModal(version);
          }
        }
      });
    });

    historyContainer.querySelectorAll('.restore-page-version-btn').forEach((button) => {
      button.addEventListener('click', () => {
        const versionId = parseInt(button.dataset.versionId, 10);
        if (!Number.isNaN(versionId) && versionId > 0) {
          const version = this.pageVersions.find((item) => parseInt(item.versionId, 10) === versionId);
          if (version) {
            this.restoreVersion(version);
          }
        }
      });
    });
  }

  formatVersionTimestamp(rawValue) {
    if (!rawValue) {
      return 'Unknown date';
    }

    const date = new Date(rawValue);
    if (Number.isNaN(date.getTime())) {
      return this.escapeHtml(rawValue);
    }

    return date.toLocaleString();
  }

  async showVersionPreviewModal(version) {
    this.closeVersionPreviewModal();

    const created = this.formatVersionTimestamp(version.created);
    const createdBy = this.escapeHtml(version.createdByName || `User ${version.createdBy || ''}`);
    const notes = this.escapeHtml(version.notes || 'Version saved before update');

    const overlay = document.createElement('div');
    overlay.id = 'page-version-preview-overlay';
    overlay.className = 'version-modal-overlay';
    overlay.innerHTML = `
      <div class="version-modal-dialog" role="dialog" aria-modal="true" aria-label="Version Layout Preview">
        <div class="version-modal-header">
          <div>
            <div class="version-modal-title">Layout Preview</div>
            <div class="version-modal-subtitle">${created} by ${createdBy}</div>
          </div>
          <button type="button" class="version-modal-close" aria-label="Close">&times;</button>
        </div>
        <div class="version-modal-notes">${notes}</div>
        <div class="version-modal-body version-preview-body">
          <div class="version-preview-loading"><i class="far fa-spinner fa-spin"></i> Loading preview...</div>
          <iframe id="version-preview-iframe" class="version-preview-iframe" title="Page layout preview" style="display: none;"></iframe>
        </div>
        <div class="version-modal-footer">
          <button type="button" class="button tiny primary radius no-gap restore-preview-version-btn">Restore This Layout</button>
          <button type="button" class="button tiny secondary radius no-gap close-preview-version-btn">Close</button>
        </div>
      </div>
    `;

    const closeHandler = () => this.closeVersionPreviewModal();
    overlay.querySelector('.version-modal-close').addEventListener('click', closeHandler);
    overlay.querySelector('.close-preview-version-btn').addEventListener('click', closeHandler);
    overlay.querySelector('.restore-preview-version-btn').addEventListener('click', async () => {
      const versionId = parseInt(version.versionId, 10);
      if (!Number.isNaN(versionId) && versionId > 0) {
        await this.restoreVersion(version, { closePreviewOnSuccess: true });
      }
    });
    overlay.addEventListener('click', (event) => {
      if (event.target === overlay) {
        closeHandler();
      }
    });

    this.versionModalEscapeHandler = (event) => {
      if (event.key === 'Escape') {
        closeHandler();
      }
    };
    document.addEventListener('keydown', this.versionModalEscapeHandler);

    document.body.appendChild(overlay);

    // Load preview content
    try {
      let pageXmlTrimmed = version.pageXml ? version.pageXml.trim() : '';
      if (!pageXmlTrimmed) {
        throw new Error('Page XML is empty or invalid');
      }

      // Remove BOM (Byte Order Mark) if present
      if (pageXmlTrimmed.charCodeAt(0) === 0xFEFF) {
        pageXmlTrimmed = pageXmlTrimmed.slice(1);
      }

      // Validate that XML starts with < or <?xml
      if (!pageXmlTrimmed.match(/^<\?xml|^</)) {
        console.warn('Invalid XML format detected');
        throw new Error('XML content appears to be malformed or incomplete');
      }

      const formData = new FormData();
      formData.append('designerData', pageXmlTrimmed);
      formData.append('containerPreview', 'true');

      const response = await fetch(this.currentPageLink, {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Preview response error:', response.status, errorText);
        throw new Error(`HTTP ${response.status}: ${errorText.substring(0, 200)}`);
      }

      const html = await response.text();
      const iframe = overlay.querySelector('.version-preview-iframe');
      const loading = overlay.querySelector('.version-preview-loading');

      if (!overlay.isConnected) {
        return;
      }

      if (iframe && loading) {
        const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
        iframeDoc.open();
        iframeDoc.write(html);
        iframeDoc.close();
        loading.style.display = 'none';
        iframe.style.display = 'block';
      }
    } catch (error) {
      console.error('Error loading version preview:', error);
      const loading = overlay.querySelector('.version-preview-loading');
      if (!overlay.isConnected) {
        return;
      }
      if (loading) {
        const errorMsg = error.message || 'Unknown error';
        loading.innerHTML = `<div style="color: #dc3545; padding: 20px;"><i class="far fa-exclamation-triangle"></i> Failed to load preview: ${this.escapeHtml(errorMsg)}</div>`;
      }
    }
  }

  closeVersionPreviewModal() {
    const existing = document.getElementById('page-version-preview-overlay');
    if (existing && existing.parentNode) {
      existing.parentNode.removeChild(existing);
    }
    if (this.versionModalEscapeHandler) {
      document.removeEventListener('keydown', this.versionModalEscapeHandler);
      this.versionModalEscapeHandler = null;
    }
  }

  async restoreVersion(version, options = {}) {
    if (!this.currentPageLink) {
      return;
    }

    const versionId = parseInt(version?.versionId, 10);
    if (Number.isNaN(versionId) || versionId <= 0) {
      return;
    }

    if (this.editor && this.editor.isDirty && this.editor.isDirty()) {
      const proceedDirty = this.editor.showConfirmDialog
        ? await this.editor.showConfirmDialog('You have unsaved changes. Loading this version will replace the current layout in the editor and discard unsaved edits. Continue?')
        : window.confirm('You have unsaved changes. Loading this version will replace the current layout in the editor and discard unsaved edits. Continue?');
      if (!proceedDirty) {
        return;
      }
    }

    const proceed = this.editor && this.editor.showConfirmDialog
      ? await this.editor.showConfirmDialog('Load this layout version into the editor? This will replace the current page layout in the editor. Click Publish to save it permanently.')
      : window.confirm('Load this layout version into the editor? This will replace the current page layout in the editor. Click Publish to save it permanently.');
    if (!proceed) {
      return;
    }

    try {
      if (this.editor && this.editor.showLoadingIndicator) {
        this.editor.showLoadingIndicator('Loading version...');
      }

      let pageXmlTrimmed = version.pageXml ? version.pageXml.trim() : '';
      if (!pageXmlTrimmed) {
        throw new Error('Selected version has no layout XML');
      }

      if (pageXmlTrimmed.charCodeAt(0) === 0xFEFF) {
        pageXmlTrimmed = pageXmlTrimmed.slice(1);
      }

      if (!pageXmlTrimmed.match(/^<\?xml|^</)) {
        throw new Error('Selected version contains invalid layout XML');
      }

      if (this.editor.canvasController) {
        this.editor.canvasController.selectedElement = null;
        this.editor.canvasController.selectedContext = null;
      }
      if (window.previewHoverManager && typeof window.previewHoverManager.clearLockedSelection === 'function') {
        window.previewHoverManager.clearLockedSelection();
      }
      if (this.editor.propertiesPanel && typeof this.editor.propertiesPanel.clear === 'function') {
        this.editor.propertiesPanel.clear();
      }

      this.editor.config.existingXml = pageXmlTrimmed;
      this.editor.config.hasExistingLayout = true;
      this.editor.layoutManager.structure = { rows: [] };
      if (this.editor.layoutManager && this.editor.layoutManager.resetIds) {
        this.editor.layoutManager.resetIds();
      }
      this.editor.loadExistingLayout(pageXmlTrimmed);
      this.editor.saveToHistory();
      if (this.editor.updateSaveIndicator) {
        this.editor.updateSaveIndicator();
      }

      if (options.closePreviewOnSuccess) {
        this.closeVersionPreviewModal();
      }

      if (this.editor && this.editor.showSaveToast) {
        this.editor.showSaveToast('Layout version loaded into the editor. Click Publish to save it.', 'success');
      }
    } catch (error) {
      console.error('Error restoring page version:', error);
      if (this.editor && this.editor.showSaveToast) {
        this.editor.showSaveToast('Failed to load version: ' + error.message, 'error');
      }
    } finally {
      if (this.editor && this.editor.hideLoadingIndicator) {
        this.editor.hideLoadingIndicator();
      }
    }
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
            // Trigger save indicator update in main editor
            if (this.editor && typeof this.editor.updateSaveIndicator === 'function') {
              this.editor.updateSaveIndicator();
            }
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
      formData.append('token', this.token);
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
