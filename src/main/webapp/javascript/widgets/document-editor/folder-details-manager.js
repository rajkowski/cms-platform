/**
 * Manages folder details editing (name, summary, privacy settings)
 */

class FolderDetailsManager {
  constructor(editor) {
    this.editor = editor;
    this.token = editor.config.token;
    this.containerEl = document.getElementById('folder-details-tab');
    this.currentFolder = null;
    this.isEditing = false;
    this.permissionsManager = new FolderPermissionsManager(this);
    this.unsavedChanges = false;
  }

  init() {
    this.permissionsManager.init();

    // Setup edit/save buttons
    const editBtn = document.getElementById('edit-folder-details-btn');
    if (editBtn) {
      editBtn.addEventListener('click', () => this.toggleEditMode());
    }

    const saveBtn = document.getElementById('save-folder-details-btn');
    if (saveBtn) {
      saveBtn.addEventListener('click', () => this.saveFolderDetails());
    }

    const cancelBtn = document.getElementById('cancel-folder-details-btn');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => this.cancelEditMode());
    }
  }

  async loadFolder(folderId) {
    if (!folderId || folderId === -1) {
      this.clearDisplay();
      return;
    }

    try {
      const url = new URL(
        `${this.editor.config.apiBaseUrl}/folderDetails`,
        globalThis.location.origin
      );
      url.searchParams.set('folderId', folderId);

      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      this.currentFolder = await response.json();
      this.isEditing = false;
      this.unsavedChanges = false;

      // Show folder tabs and hide file content
      const tabs = document.getElementById('properties-tabs');
      if (tabs) {
        tabs.style.display = 'flex';
      }

      // @todo determine if subfolder then change title accordingly

      const titleEl = document.getElementById('properties-panel-title');
      if (titleEl) {
        titleEl.textContent = 'Repository: ' + (this.currentFolder.name || 'Untitled');
      }

      const contentArea = document.getElementById('document-properties-content');
      if (contentArea) {
        contentArea.style.display = 'none';
      }

      this.render();

      // Load permissions for this folder
      await this.permissionsManager.loadFolderGroups(folderId);

      // Render permissions after loading
      this.permissionsManager.render();

      // Switch to details tab to show the content
      this.editor.switchTab('details');

    } catch (err) {
      console.error('Unable to load folder details', err);
      this.clearDisplay();
    } finally {
      this.editor.hideLoading();
    }
  }

  render() {
    if (!this.containerEl) {
      return;
    }

    if (!this.currentFolder) {
      this.clearDisplay();
      return;
    }

    let html = '<div class="folder-details-content">';

    // Folder info header
    html += '<div class="property-section">';
    html += '<div class="folder-header-info">';
    html += `<h4>${this.currentFolder.name || 'Untitled Folder'}</h4>`;
    if (this.isEditing) {
      // Edit mode - no summary in header
    } else {
      html += '<p class="info-text">' + (this.currentFolder.summary || 'No description') + '</p>';
    }
    html += '</div>';
    html += '</div>';

    if (this.isEditing) {
      // Edit mode
      html += this.renderEditMode();
    } else {
      // View mode
      html += this.renderViewMode();
    }

    html += '</div>';
    this.containerEl.innerHTML = html;

    this.attachEventListeners();
    this.permissionsManager.render();
  }

  renderViewMode() {
    let html = '<div class="property-section">';
    html += '<div style="display:flex; gap:0.5rem; flex-wrap:wrap;">';
    html += '<button id="edit-folder-details-btn" class="button tiny primary"><i class="fas fa-edit"></i> Edit Details</button>';
    html += '<button id="delete-folder-btn" class="button tiny alert"><i class="fas fa-trash"></i> Delete Repository</button>';
    html += '</div>';
    html += '</div>';

    html += '<div class="property-section">';
    html += '<h5>Details</h5>';
    html += '<table class="property-table">';
    html += `<tr><th>Name</th><td>${this.currentFolder.name || '-'}</td></tr>`;
    html += `<tr><th>Summary</th><td>${this.currentFolder.summary || '-'}</td></tr>`;
    html += `<tr><th>File Count</th><td>${this.currentFolder.fileCount || 0}</td></tr>`;
    html += `<tr><th>Created By</th><td>User #${this.currentFolder.createdBy}</td></tr>`;
    html += `<tr><th>Modified</th><td>${this.formatDate(this.currentFolder.modified)}</td></tr>`;
    html += '</table>';
    html += '</div>';

    return html;
  }

  renderEditMode() {
    let html = '<div class="property-section">';
    html += '<form id="folder-details-form">';

    html += '<div class="form-group">';
    html += '<label>Folder Name</label>';
    html += `<input type="text" id="folder-name-input" class="property-input" value="${this.currentFolder.name || ''}" required />`;
    html += '</div>';

    html += '<div class="form-group">';
    html += '<label>Summary</label>';
    html += `<textarea id="folder-summary-input" class="property-input" rows="3">${this.currentFolder.summary || ''}</textarea>`;
    html += '</div>';

    html += '<div class="form-group">';
    html += '<label><input type="checkbox" id="folder-enabled-input" ' + (this.currentFolder.enabled ? 'checked' : '') + ' /> Enabled</label>';
    html += '</div>';

    html += '<div class="button-group">';
    html += '<button type="button" id="save-folder-details-btn" class="button small success"><i class="fas fa-save"></i> Save</button>';
    html += '<button type="button" id="cancel-folder-details-btn" class="button small secondary">Cancel</button>';
    html += '</div>';

    html += '</form>';
    html += '</div>';

    return html;
  }

  attachEventListeners() {
    const editBtn = document.getElementById('edit-folder-details-btn');
    if (editBtn) {
      editBtn.removeEventListener('click', () => this.toggleEditMode());
      editBtn.addEventListener('click', () => this.toggleEditMode());
    }

    const deleteBtn = document.getElementById('delete-folder-btn');
    if (deleteBtn) {
      deleteBtn.removeEventListener('click', () => this.deleteFolder());
      deleteBtn.addEventListener('click', () => this.deleteFolder());
    }

    const saveBtn = document.getElementById('save-folder-details-btn');
    if (saveBtn) {
      saveBtn.removeEventListener('click', () => this.saveFolderDetails());
      saveBtn.addEventListener('click', () => this.saveFolderDetails());
    }

    const cancelBtn = document.getElementById('cancel-folder-details-btn');
    if (cancelBtn) {
      cancelBtn.removeEventListener('click', () => this.cancelEditMode());
      cancelBtn.addEventListener('click', () => this.cancelEditMode());
    }
  }

  toggleEditMode() {
    this.isEditing = true;
    this.render();
  }

  cancelEditMode() {
    this.isEditing = false;
    this.unsavedChanges = false;
    this.render();
  }

  async saveFolderDetails() {
    const nameInput = document.getElementById('folder-name-input');
    const summaryInput = document.getElementById('folder-summary-input');
    const enabledInput = document.getElementById('folder-enabled-input');

    const name = nameInput ? nameInput.value.trim() : '';
    if (!name) {
      alert('Folder name is required');
      return;
    }

    try {
      const payload = {
        token: this.token,
        id: this.currentFolder.id,
        name: name,
        summary: summaryInput ? summaryInput.value : '',
        enabled: enabledInput ? enabledInput.checked : false
      };

      const formData = new FormData();
      formData.append('token', payload.token);
      formData.append('id', payload.id);
      formData.append('name', payload.name);
      formData.append('summary', payload.summary);
      formData.append('enabled', payload.enabled);

      const response = await fetch(`${this.editor.config.apiBaseUrl}/folderSave`, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      if (result.success === false || result.error) {
        alert(result.message || 'Failed to save folder');
        return;
      }

      // Update local folder data
      this.currentFolder.name = name;
      this.currentFolder.summary = summaryInput ? summaryInput.value : '';
      this.currentFolder.enabled = enabledInput ? enabledInput.checked : false;

      this.isEditing = false;
      this.unsavedChanges = false;
      this.render();
      this.setUnsavedChanges(false);

    } catch (err) {
      console.error('Error saving folder', err);
      alert('Error saving repository: ' + err.message);
    }
  }

  clearDisplay() {
    if (this.containerEl) {
      this.containerEl.innerHTML = '<div class="empty-state">Select a folder to view details</div>';
    }

    const tabs = document.getElementById('properties-tabs');
    if (tabs) {
      tabs.style.display = 'none';
    }

    const analyticsTab = document.getElementById('folder-analytics-tab');
    if (analyticsTab) {
      analyticsTab.innerHTML = '';
      analyticsTab.style.display = 'none';
    }

    const contentArea = document.getElementById('document-properties-content');
    if (contentArea) {
      contentArea.style.display = 'block';
      contentArea.innerHTML = '<div class="empty-state">Select a folder or file to view details</div>';
    }

    this.currentFolder = null;
    this.isEditing = false;
  }

  formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  setFolderAndLoad(folder) {
    this.loadFolder(folder.id);
  }

  async deleteFolder() {
    if (!this.currentFolder) {
      return;
    }
    const name = this.currentFolder.name || 'this repository';
    if (!confirm(`Delete "${name}"? This will permanently delete all files and cannot be undone.`)) {
      return;
    }
    try {
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('folderId', this.currentFolder.id);
      const response = await fetch(`${this.editor.config.apiBaseUrl}/documentDeleteFolder`, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });
      const result = await response.json();
      if (result.success) {
        this.clearDisplay();
        this.editor.library.reload();
      } else {
        alert(result.message || 'Failed to delete repository.');
      }
    } catch (err) {
      console.error('Error deleting folder', err);
      alert('Error deleting repository: ' + err.message);
    }
  }

  async loadAnalytics() {
    const analyticsTab = document.getElementById('folder-analytics-tab');
    if (!analyticsTab || !this.currentFolder) {
      return;
    }

    analyticsTab.innerHTML = '<div style="padding:0.5rem;"><em>Loading analytics...</em></div>';

    const days = analyticsTab.dataset.days || 30;

    try {
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentAnalytics`, globalThis.location.origin);
      url.searchParams.set('folderId', this.currentFolder.id);
      url.searchParams.set('days', days);
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data = await response.json();
      const assets = data.assets || [];

      let html = '<div class="folder-analytics-content">';
      html += '<div style="display:flex; justify-content:space-between; align-items:center; padding:0.5rem;">';
      html += '<strong>Top Assets by Downloads</strong>';
      html += '<div class="analytics-days-selector">';
      html += `<button class="button tiny ${days == 1 ? 'primary' : 'secondary'}" data-days="1">1d</button>`;
      html += `<button class="button tiny ${days == 7 ? 'primary' : 'secondary'}" data-days="7">7d</button>`;
      html += `<button class="button tiny ${days == 30 ? 'primary' : 'secondary'}" data-days="30">30d</button>`;
      html += `<button class="button tiny ${days == 90 ? 'primary' : 'secondary'}" data-days="90">90d</button>`;
      html += '</div>';
      html += '</div>';

      if (assets.length === 0) {
        html += '<div class="empty-state">No download data available.</div>';
      } else {
        html += '<table class="file-table" style="font-size:0.8rem;">';
        html += '<thead><tr><th>File</th><th>Type</th><th>Downloads</th></tr></thead>';
        html += '<tbody>';
        for (const asset of assets) {
          html += `<tr>`;
          html += `<td title="${this.escapeHtml(asset.filename)}">${this.escapeHtml(asset.title || asset.filename)}</td>`;
          html += `<td>${this.escapeHtml(asset.mimeType || '')}</td>`;
          html += `<td>${asset.downloadCount || 0}</td>`;
          html += `</tr>`;
        }
        html += '</tbody></table>';
      }
      html += '</div>';

      analyticsTab.innerHTML = html;
      analyticsTab.dataset.days = days;

      // Wire day selector buttons
      analyticsTab.querySelectorAll('.analytics-days-selector .button').forEach((btn) => {
        btn.addEventListener('click', () => {
          analyticsTab.dataset.days = btn.dataset.days;
          this.loadAnalytics();
        });
      });

    } catch (err) {
      console.error('Error loading analytics', err);
      analyticsTab.innerHTML = '<div class="empty-state">Failed to load analytics.</div>';
    }
  }

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  setUnsavedChanges(unsaved) {
    this.unsavedChanges = unsaved;
    this.editor.setUnsavedChanges(unsaved);
  }
}
