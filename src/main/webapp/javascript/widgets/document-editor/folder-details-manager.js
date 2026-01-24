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
      
      const titleEl = document.getElementById('properties-panel-title');
      if (titleEl) {
        titleEl.textContent = 'Folder: ' + (this.currentFolder.name || 'Untitled');
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
    html += '<button id="edit-folder-details-btn" class="button tiny primary"><i class="fas fa-edit"></i> Edit Details</button>';
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

      const response = await fetch(`${this.editor.config.apiBaseUrl}/folderSave`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload),
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      if (result.status === 0 || result.error) {
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
      alert('Error saving folder: ' + err.message);
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

  setUnsavedChanges(unsaved) {
    this.unsavedChanges = unsaved;
    this.editor.setUnsavedChanges(unsaved);
  }
}
