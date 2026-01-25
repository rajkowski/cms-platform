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
        <button class="property-tab active" data-tab="info">Properties</button>
        <button class="property-tab" data-tab="versions">Versions</button>
        <button class="property-tab" data-tab="downloads">Downloads</button>
      </div>
      <div class="property-section active ${editModeClass}" data-tab="info">
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
}
