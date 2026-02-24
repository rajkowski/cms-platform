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
    this.activeTab = 'preview';
    this.analyticsDays = 30;
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
      this.activeTab = 'preview';
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

    // Title is set by showFileProperties() to avoid flickering
    // Don't update it here since it's already correct

    if (!file || file.error) {
      this.contentEl.innerHTML = '<div class="empty-state">Select a file to see details</div>';
      return;
    }

    const editModeClass = this.isEditing ? 'editable' : '';
    const isPreviewActive = this.activeTab === 'preview';
    const isInfoActive = this.activeTab === 'info';
    const isVersionsActive = this.activeTab === 'versions';
    const isAnalyticsActive = this.activeTab === 'analytics';
    const titleInput = this.isEditing
      ? `<input type="text" class="property-input" id="prop-title" value="${file.title || file.filename || ''}" />`
      : `<div class="property-value">${file.title || file.filename || 'Untitled'}</div>`;
    const filenameInput = this.isEditing
      ? `<input type="text" class="property-input" id="prop-filename" value="${file.filename || ''}" />`
      : `<div class="property-value">${file.filename || ''}</div>`;
    const versionInput = this.isEditing
      ? `<input type="text" class="property-input" id="prop-version" value="${file.version || ''}" />`
      : `<div class="property-value">${file.version || ''}</div>`;
    const summaryInput = this.isEditing
      ? `<textarea class="property-input" id="prop-summary" rows="3">${file.summary || ''}</textarea>`
      : `<div class="property-value">${file.summary || ''}</div>`;

    this.contentEl.innerHTML = `
      <div class="property-tabs">
        <button class="property-tab ${isPreviewActive ? 'active' : ''}" data-tab="preview">Preview</button>
        <button class="property-tab ${isInfoActive ? 'active' : ''}" data-tab="info">Properties</button>
        <button class="property-tab ${isVersionsActive ? 'active' : ''}" data-tab="versions">Versions</button>
        <button class="property-tab ${isAnalyticsActive ? 'active' : ''}" data-tab="analytics">Analytics</button>
      </div>
      <div class="property-section no-gap ${isPreviewActive ? 'active' : ''}" data-tab="preview">
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
      <div class="property-section ${editModeClass} ${isInfoActive ? 'active' : ''}" data-tab="info">
        <div class="property-group ${editModeClass}">
          <label>Title</label>
          ${titleInput}
        </div>
        <div class="property-group ${editModeClass}">
          <label>Filename</label>
          ${filenameInput}
        </div>
        <div class="property-group ${editModeClass}">
          <label>Version</label>
          ${versionInput}
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
      <div class="property-section ${isVersionsActive ? 'active' : ''}" data-tab="versions">
        <div id="versions-loading" class="empty-state">Loading versions...</div>
      </div>
      <div class="property-section ${isAnalyticsActive ? 'active' : ''}" data-tab="analytics">
        <div id="analytics-content" class="empty-state">Loading analytics...</div>
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
        this.activeTab = 'info';
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
    const filenameInput = document.getElementById('prop-filename');
    const summaryInput = document.getElementById('prop-summary');
    const versionInput = document.getElementById('prop-version');

    if (!titleInput || !this.currentFile) {
      return;
    }

    const updates = {
      token: this.token,
      id: this.currentFile.id,
      title: titleInput.value.trim(),
      filename: filenameInput ? filenameInput.value.trim() : this.currentFile.filename,
      version: versionInput ? versionInput.value.trim() : this.currentFile.version,
      summary: summaryInput ? summaryInput.value.trim() : ''
    };

    try {
      this.editor.showLoading();
      const formData = new FormData();
      formData.append('token', updates.token);
      formData.append('id', updates.id);
      formData.append('title', updates.title);
      formData.append('filename', updates.filename);
      formData.append('version', updates.version);
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
      this.currentFile.filename = updates.filename;
      this.currentFile.version = updates.version;
      this.currentFile.summary = updates.summary;
      this.isEditing = false;
      this.render(this.currentFile);
      this.editor.setUnsavedChanges(false);

      // Reload file list to show updated title
      this.editor.files.reload();
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
        this.activeTab = tabName;
        tabs.forEach((t) => t.classList.toggle('active', t.dataset.tab === tabName));
        sections.forEach((section) => section.classList.toggle('active', section.dataset.tab === tabName));
        // Lazy load content for versions/downloads tabs
        if (tabName === 'versions') {
          this.loadVersions();
        } else if (tabName === 'analytics') {
          this.loadFileAnalytics();
        }
      });
    });
    // If initial tab is versions or downloads, load content
    if (this.activeTab === 'versions') {
      this.loadVersions();
    } else if (this.activeTab === 'analytics') {
      this.loadFileAnalytics();
    }
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
    // DrawIO preview
    else if (mimeType === 'application/vnd.jgraph.mxfile') {
      return `
        <iframe src="/assets/drawio/${fileUrl}" style="width: 100%; height: 450px; border: none;"></iframe>
      `;
    }
    // Text preview (will be loaded asynchronously)
    else if (mimeType.startsWith('text/') || mimeType === 'application/json') {
      // Return placeholder that will be replaced
      setTimeout(() => this.loadTextPreview('/assets/view/' + fileUrl), 100);
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

  async loadVersions() {
    const container = document.getElementById('versions-loading');
    if (!container || !this.currentFile) {
      return;
    }
    try {
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentFileVersions`, globalThis.location.origin);
      url.searchParams.set('fileId', this.currentFile.id);
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data = await response.json();
      const versions = data.versions || [];
      if (versions.length === 0) {
        container.outerHTML = '<div class="empty-state">No version history available.</div>';
        return;
      }

      let html = '<div class="versions-header" style="display:flex; justify-content:space-between; align-items:center; padding: 0.5rem;">';
      html += '<strong>Version History</strong>';
      html += '<button id="upload-new-version-btn" class="button tiny primary no-gap radius"><i class="fas fa-upload"></i> Upload New Version</button>';
      html += '</div>';
      html += '<table class="file-table" style="font-size:0.8rem;">';
      html += '<thead><tr><th>Version</th><th>Filename</th><th>Size</th><th>Downloads</th><th>Date</th></tr></thead>';
      html += '<tbody>';
      for (const v of versions) {
        const date = v.created ? new Date(v.created).toLocaleDateString() : '-';
        const size = this.formatSize(v.fileLength);
        html += `<tr>`;
        html += `<td>${this.escapeHtml(v.version || '1.0')}</td>`;
        html += `<td title="${this.escapeHtml(v.filename)}">${this.escapeHtml(v.filename || '-')}</td>`;
        html += `<td>${size}</td>`;
        html += `<td>${v.downloadCount || 0}</td>`;
        html += `<td>${date}</td>`;
        html += `</tr>`;
      }
      html += '</tbody></table>';

      const section = container.closest('.property-section[data-tab="versions"]');
      if (section) {
        section.innerHTML = html;
        const uploadBtn = section.querySelector('#upload-new-version-btn');
        if (uploadBtn) {
          uploadBtn.addEventListener('click', () => this.editor.fileManager.addVersion());
        }
      }
    } catch (err) {
      console.error('Error loading versions', err);
      if (container) {
        container.textContent = 'Failed to load versions.';
      }
    }
  }

  async loadFileAnalytics() {
    const section = this.contentEl ? this.contentEl.querySelector('.property-section[data-tab="analytics"]') : null;
    if (!section || !this.currentFile) return;

    section.innerHTML = '<div class="empty-state" style="padding:0.5rem;">Loading analytics...</div>';

    try {
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentFileAnalytics`, globalThis.location.origin);
      url.searchParams.set('fileId', this.currentFile.id);
      url.searchParams.set('days', this.analyticsDays);
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();

      const totalHits = data.totalHits || 0;
      const recentHits = data.recentHits || [];
      const monthlyData = data.monthlyData || [];

      let html = '<div class="file-analytics-content">';

      // Day selector
      html += '<div style="display:flex; justify-content:space-between; align-items:center; padding:0.5rem;">';
      html += `<strong>Downloads: ${totalHits} total</strong>`;
      html += '<div class="analytics-days-selector" style="display:flex; gap:0.25rem;">';
      html += `<button class="button tiny ${this.analyticsDays == 1 ? 'primary' : 'secondary'}" data-days="1">1d</button>`;
      html += `<button class="button tiny ${this.analyticsDays == 7 ? 'primary' : 'secondary'}" data-days="7">7d</button>`;
      html += `<button class="button tiny ${this.analyticsDays == 30 ? 'primary' : 'secondary'}" data-days="30">30d</button>`;
      html += `<button class="button tiny ${this.analyticsDays == 90 ? 'primary' : 'secondary'}" data-days="90">90d</button>`;
      html += '</div>';
      html += '</div>';

      // Monthly chart (last year)
      if (monthlyData.length > 0) {
        html += this.renderMonthlyChart(monthlyData);
      }

      // Recent hits table
      if (recentHits.length > 0) {
        html += '<table class="file-table" style="font-size:0.8rem; margin-top:0.5rem;">';
        html += '<thead><tr><th>Date</th><th>Hits</th></tr></thead>';
        html += '<tbody>';
        for (const hit of recentHits) {
          html += `<tr><td>${this.escapeHtml(hit.date)}</td><td>${hit.count}</td></tr>`;
        }
        html += '</tbody></table>';
      } else {
        html += '<div class="empty-state" style="padding:0.5rem;">No download data available for this period.</div>';
      }

      html += '</div>';
      section.innerHTML = html;

      // Wire day selector
      section.querySelectorAll('.analytics-days-selector .button').forEach((btn) => {
        btn.addEventListener('click', () => {
          this.analyticsDays = parseInt(btn.dataset.days);
          this.loadFileAnalytics();
        });
      });

    } catch (err) {
      console.error('Error loading file analytics', err);
      section.innerHTML = '<div class="empty-state">Failed to load analytics.</div>';
    }
  }

  renderMonthlyChart(monthlyData) {
    const maxVal = Math.max(...monthlyData.map(d => d.count), 1);
    const barWidth = 16;
    const barGap = 4;
    const chartH = 60;
    const paddingLeft = 30;
    const totalW = monthlyData.length * (barWidth + barGap) + paddingLeft;

    let svg = `<svg width="${totalW}" height="${chartH + 24}" style="max-width:100%; overflow:visible; margin:0.25rem 0.5rem;">`;

    // Y axis label
    svg += `<text x="0" y="${chartH / 2}" font-size="9" fill="var(--text-muted)" text-anchor="middle" transform="rotate(-90, 10, ${chartH / 2})">#</text>`;

    monthlyData.forEach((d, i) => {
      const x = paddingLeft + i * (barWidth + barGap);
      const barH = maxVal > 0 ? Math.max(2, Math.round((d.count / maxVal) * chartH)) : 2;
      const y = chartH - barH;
      const color = d.count > 0 ? 'var(--primary-color, #2563eb)' : 'var(--editor-border, #ccc)';

      svg += `<rect x="${x}" y="${y}" width="${barWidth}" height="${barH}" fill="${color}" rx="2">`;
      svg += `<title>${d.label}: ${d.count}</title></rect>`;

      // Month label (short)
      const shortLabel = d.label ? d.label.substring(5) : '';
      svg += `<text x="${x + barWidth / 2}" y="${chartH + 14}" font-size="8" fill="var(--text-muted)" text-anchor="middle">${shortLabel}</text>`;
    });

    svg += '</svg>';
    return `<div style="overflow-x:auto; padding:0 0.5rem;">${svg}</div>`;
  }

  async loadDownloads() {
    // Alias kept for backward compatibility - delegates to loadFileAnalytics
    this.loadFileAnalytics();
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
