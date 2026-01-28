/**
 * Handles right-panel metadata display and editing for the document editor
 */

class DocumentPropertiesManager {
  constructor(editor) {
    this.editor = editor;
    this.token = editor.config.token;
    this.contentEl = document.getElementById('document-properties-content');
    this.infoSection = null;
    this.currentFile = null;
    this.isEditing = false;
  }

  init() {
    if (this.contentEl) {
      this.infoSection = this.contentEl.querySelector('.property-section.info');
    }
    this.setupResizeListener();
  }

  setupResizeListener() {
    window.addEventListener('resize', () => this.resizePreviewContainer());
    // Initial resize to set the correct height
    setTimeout(() => this.resizePreviewContainer(), 100);
  }

  resizePreviewContainer() {
    const previewContent = document.getElementById('file-preview-content');
    if (!previewContent) {
      return;
    }

    // Get the preview content element's position
    const rect = previewContent.getBoundingClientRect();
    const topOffset = rect.top;
    
    // Calculate available height: window height - top position - padding
    const availableHeight = window.innerHeight - topOffset - 16; // 16px for bottom padding
    
    // Set the height
    previewContent.style.height = Math.max(availableHeight, 200) + 'px';
  }

  async loadFile(fileId) {
    if (!fileId) {
      return;
    }
    try {
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentContent`, globalThis.location.origin);
      url.searchParams.set('fileId', fileId);
      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      this.currentFile = await response.json();
      this.isEditing = false;
      this.render(this.currentFile);
      this.editor.setUnsavedChanges(false);
    } catch (err) {
      console.error('Unable to load file', err);
    } finally {
      this.editor.hideLoading();
    }
  }

  render(file) {
    if (!this.contentEl) {
      return;
    }

    // Hide folder tabs and folder detail/permission tabs, show file content
    const tabs = document.getElementById('properties-tabs');
    if (tabs) {
      tabs.style.display = 'none';
    }

    const detailsTab = document.getElementById('folder-details-tab');
    if (detailsTab) {
      detailsTab.style.display = 'none';
    }

    const permissionsTab = document.getElementById('folder-permissions-tab');
    if (permissionsTab) {
      permissionsTab.style.display = 'none';
    }

    this.contentEl.style.display = 'block';

    const titleEl = document.getElementById('properties-panel-title');
    if (titleEl) {
      titleEl.textContent = 'File Details';
    }

    if (!file || file.error) {
      this.contentEl.innerHTML = '<div class="empty-state">Select a file to see details</div>';
      return;
    }

    const editModeClass = this.isEditing ? 'editable' : '';
    const titleInput = this.isEditing
      ? `<input type="text" class="property-input" id="prop-title" value="${file.title || file.filename || ''}" />`
      : `<div class="property-value">${file.title || file.filename || 'Untitled'}</div>`;
    const filenameInput = this.isEditing
      ? `<input type="text" class="property-input" id="prop-filename" value="${file.filename || ''}" />`
      : `<div class="property-value">${file.filename || ''}</div>`;
    const summaryInput = this.isEditing
      ? `<textarea class="property-input" id="prop-summary" rows="3">${file.summary || ''}</textarea>`
      : `<div class="property-value">${file.summary || ''}</div>`;

    this.contentEl.innerHTML = `
      <div class="property-tabs">
        <button class="property-tab active" data-tab="preview">Preview</button>
        <button class="property-tab" data-tab="info">Properties</button>
        <button class="property-tab" data-tab="versions">Versions</button>
        <button class="property-tab" data-tab="downloads">Downloads</button>
      </div>
      <div class="property-section active" data-tab="preview">
        <div class="preview-toolbar" style="padding: 0.5rem; display: flex; gap: 0.5rem;">
          <button id="preview-download-btn" class="button tiny success no-gap radius" title="Download this file"><i class="fas fa-download"></i> Download</button>
          <button id="preview-add-version-btn" class="button tiny primary no-gap radius" title="Add a new version of this file"><i class="fas fa-plus"></i> Add Version</button>
          <button id="preview-move-file-btn" class="button tiny secondary no-gap radius" title="Move file to another folder"><i class="fas fa-folder"></i> Move...</button>
          <button id="preview-delete-file-btn" class="button tiny alert no-gap radius" title="Delete this file"><i class="fas fa-trash"></i> Delete File</button>
        </div>
        <div id="file-preview-content" style="padding: 1rem; overflow: auto; height: 300px;">
          ${this.renderPreviewContent(file)}
        </div>
      </div>
      <div class="property-section ${editModeClass}" data-tab="info">
        <div class="property-group ${editModeClass}">
          <label>Title</label>
          ${titleInput}
        </div>
        <div class="property-group ${editModeClass}">
          <label>Filename</label>
          ${filenameInput}
        </div>
        <div class="property-group">
          <label>Version</label>
          <div class="property-value">${file.version || ''}</div>
        </div>
        <div class="property-group">
          <label>MIME Type</label>
          <div class="property-value">${file.mimeType || ''}</div>
        </div>
        <div class="property-group">
          <label>Size</label>
          <div class="property-value">${this.formatSize(file.fileLength)}</div>
        </div>
        <div class="property-group">
          <label>Downloads</label>
          <div class="property-value">${file.downloadCount || 0}</div>
        </div>
        <div class="property-group ${editModeClass}">
          <label>Summary</label>
          ${summaryInput}
        </div>
        ${this.isEditing ? `
          <div class="property-actions">
            <button id="save-properties-btn" class="button primary tiny">Save</button>
            <button id="cancel-edit-btn" class="button secondary tiny">Cancel</button>
          </div>
        ` : `
          <div class="property-actions">
            <button id="edit-properties-btn" class="button primary tiny">Edit</button>
          </div>
        `}
      </div>
      <div class="property-section" data-tab="versions">
        <div class="empty-state">Version history coming soon.</div>
      </div>
      <div class="property-section" data-tab="downloads">
        <div class="empty-state">Download analytics coming soon.</div>
      </div>
    `;

    this.wireTabs();
    this.wireButtons();
    this.wirePreviewButtons();
    
    // Resize preview container after render
    setTimeout(() => this.resizePreviewContainer(), 50);
  }

  wireButtons() {
    const editBtn = document.getElementById('edit-properties-btn');
    const saveBtn = document.getElementById('save-properties-btn');
    const cancelBtn = document.getElementById('cancel-edit-btn');

    if (editBtn) {
      editBtn.addEventListener('click', () => {
        this.isEditing = true;
        this.render(this.currentFile);
      });
    }

    if (saveBtn) {
      saveBtn.addEventListener('click', () => this.saveProperties());
    }

    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => {
        this.isEditing = false;
        this.render(this.currentFile);
      });
    }
  }

  async saveProperties() {
    const titleInput = document.getElementById('prop-title');
    const summaryInput = document.getElementById('prop-summary');

    if (!titleInput || !this.currentFile) {
      return;
    }

    const updates = {
      token: this.token,
      id: this.currentFile.id,
      title: titleInput.value.trim(),
      summary: summaryInput ? summaryInput.value.trim() : ''
    };

    try {
      this.editor.showLoading();
      const formData = new FormData();
      formData.append('token', updates.token);
      formData.append('id', updates.id);
      formData.append('title', updates.title);
      formData.append('summary', updates.summary);

      const response = await fetch('/json/documentUpdate', {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      if (result.success === false) {
        throw new Error(result.message || 'Save failed');
      }

      // Update current file with saved values
      this.currentFile.title = updates.title;
      this.currentFile.summary = updates.summary;
      this.isEditing = false;
      this.render(this.currentFile);
      this.editor.setUnsavedChanges(false);

      // Reload file list to show updated title
      this.editor.files.reload();

      alert('Properties saved successfully');
    } catch (err) {
      console.error('Save failed', err);
      alert(`Failed to save properties: ${err.message}`);
    } finally {
      this.editor.hideLoading();
    }
  }

  wireTabs() {
    const tabs = this.contentEl.querySelectorAll('.property-tab');
    const sections = this.contentEl.querySelectorAll('.property-section');
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        const tabName = tab.dataset.tab;
        tabs.forEach((t) => t.classList.toggle('active', t.dataset.tab === tabName));
        sections.forEach((section) => section.classList.toggle('active', section.dataset.tab === tabName));
      });
    });
  }

  formatSize(bytes) {
    if (!bytes || bytes < 0) {
      return '';
    }
    const units = ['B', 'KB', 'MB', 'GB'];
    const power = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    const size = bytes / Math.pow(1024, power);
    return `${size.toFixed(1)} ${units[power]}`;
  }

  renderPreviewContent(file) {
    if (!file) {
      return '<div class="empty-state">No preview available</div>';
    }

    const fileType = file.fileType || '';
    const mimeType = file.mimeType || '';
    const fileUrl = file.url || '';

    // If we don't have a URL yet, show a loading state
    if (!fileUrl && !fileType && !mimeType) {
      return '<div class="empty-state">Loading preview...</div>';
    }

    // If no URL but we have type info, show type-based message
    if (!fileUrl) {
      return '<div class="empty-state">No preview available</div>';
    }

    // Image preview
    if (fileType === 'image' || mimeType.startsWith('image/')) {
      return `<img src="/assets/view/${fileUrl}" alt="${this.escapeHtml(file.title || file.filename)}" style="max-width: 100%; height: auto;" />`;
    }
    // PDF preview
    else if (fileType === 'pdf' || mimeType === 'application/pdf') {
      return `<iframe src="/assets/view/${fileUrl}" type="application/pdf" style="width: 100%; height: 500px; border: none;"></iframe>`;
    }
    // URL preview
    else if (fileType === 'url' || mimeType === 'text/uri-list') {
      return `<div class="url-preview" style="padding: 1rem;"><a href="${this.escapeHtml(file.filename)}" target="_blank">${this.escapeHtml(file.filename)}</a></div>`;
    }
    // Video preview
    else if (fileType === 'video' || mimeType.startsWith('video/')) {
      return `
        <video controls style="max-width: 100%; height: auto;">
          <source src="/assets/view/${fileUrl}" type="${mimeType}">
          Your browser does not support the video tag.
        </video>
      `;
    }
    // Text preview (will be loaded asynchronously)
    else if (mimeType.startsWith('text/') || mimeType === 'application/json') {
      // Return placeholder that will be replaced
      setTimeout(() => this.loadTextPreview(fileUrl), 100);
      return '<div id="text-preview-loading">Loading text preview...</div>';
    }
    // Default message
    else {
      const icon = this.getMimeIcon(mimeType, file.filename);
      return `
        <div class="empty-state">
          ${icon}
          <div style="margin-top: 0.5rem; font-size: 0.875rem; color: var(--text-muted);">${this.escapeHtml(mimeType || 'Unknown type')}</div>
          <div style="margin-top: 0.5rem;">Preview not available for this file type</div>
          <a href="/assets/file/${fileUrl}" target="_blank" style="margin-top: 0.5rem; display: inline-block;">Download File</a>
        </div>
      `;
    }
  }

  async loadTextPreview(fileUrl) {
    const container = document.getElementById('text-preview-loading');
    if (!container) {
      return;
    }
    try {
      const response = await fetch(fileUrl);
      const text = await response.text();
      container.outerHTML = `<pre style="white-space: pre-wrap; word-wrap: break-word;">${this.escapeHtml(text)}</pre>`;
    } catch (err) {
      console.error('Unable to load text preview', err);
      container.outerHTML = '<div class="empty-state">Unable to load text preview</div>';
    }
  }

  getMimeIcon(mimeType, filename) {
    const type = (mimeType || '').toLowerCase();
    if (type === 'text/uri-list') return '<i class="fas fa-link fa-3x"></i>';
    if (type.startsWith('image/')) return '<i class="fas fa-file-image fa-3x"></i>';
    if (type === 'application/pdf') return '<i class="fas fa-file-pdf fa-3x"></i>';
    if (type.startsWith('video/')) return '<i class="fas fa-file-video fa-3x"></i>';
    if (type.startsWith('audio/')) return '<i class="fas fa-file-audio fa-3x"></i>';
    if (type.startsWith('text/') || type === 'application/json') return '<i class="fas fa-file-alt fa-3x"></i>';
    if (type.includes('spreadsheet') || type.includes('excel') || type === 'application/vnd.ms-excel') return '<i class="fas fa-file-excel fa-3x"></i>';
    if (type.includes('presentation') || type.includes('powerpoint')) return '<i class="fas fa-file-powerpoint fa-3x"></i>';
    if (type.includes('word')) return '<i class="fas fa-file-word fa-3x"></i>';
    if (type.includes('zip') || type.includes('compressed') || type.includes('tar')) return '<i class="fas fa-file-archive fa-3x"></i>';
    if (type.includes('xml') || type.includes('html') || type.includes('javascript')) return '<i class="fas fa-file-code fa-3x"></i>';
    return '<i class="fas fa-file fa-3x"></i>';
  }

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  wirePreviewButtons() {
    const downloadBtn = document.getElementById('preview-download-btn');
    const addVersionBtn = document.getElementById('preview-add-version-btn');
    const moveFileBtn = document.getElementById('preview-move-file-btn');
    const deleteFileBtn = document.getElementById('preview-delete-file-btn');

    if (downloadBtn) {
      downloadBtn.addEventListener('click', () => this.editor.fileManager.downloadFile());
    }
    if (addVersionBtn) {
      addVersionBtn.addEventListener('click', () => this.editor.fileManager.addVersion());
    }
    if (moveFileBtn) {
      moveFileBtn.addEventListener('click', () => this.editor.fileManager.moveFile());
    }
    if (deleteFileBtn) {
      deleteFileBtn.addEventListener('click', () => this.editor.fileManager.deleteFile());
    }
  }
}
