/**
 * Handles file listing, selection, preview, and file uploads
 */

class DocumentFileManager {
  constructor(editor) {
    this.editor = editor;
    this.token = editor.config.token;
    this.folderId = -1;
    this.parentFolderId = -1;
    this.subFolderId = -1;
    this.files = [];
    this.subfolders = [];
    this.tableBody = null;
    this.searchInput = document.getElementById('file-search');
    this.viewMode = 'table'; // 'table' or 'preview'
    this.currentFile = null;
  }

  init() {
    const table = document.getElementById('file-table');
    if (table) {
      this.tableBody = table.querySelector('tbody');
    }
    if (this.searchInput) {
      this.searchInput.addEventListener('input', () => this.reload());
    }

    // File upload
    const uploadInput = document.getElementById('file-upload-input');
    if (uploadInput) {
      uploadInput.addEventListener('change', (e) => this.handleFileUpload(e));
    }

    // Drag-and-drop upload on file browser panel
    this.initDragDrop();
  }

  get currentFolderId() {
    return this.folderId;
  }

  get currentSubFolderId() {
    return this.subFolderId;
  }

  initDragDrop() {
    const panel = document.getElementById('document-browser-panel');
    const overlay = document.getElementById('file-drop-overlay');
    if (!panel) {
      return;
    }

    let dragCounter = 0;

    panel.addEventListener('dragenter', (e) => {
      e.preventDefault();
      e.stopPropagation();
      dragCounter++;
      if (this.folderId > -1 && overlay) {
        overlay.style.display = 'flex';
      }
    });

    panel.addEventListener('dragleave', (e) => {
      e.preventDefault();
      e.stopPropagation();
      dragCounter--;
      if (dragCounter <= 0) {
        dragCounter = 0;
        if (overlay) overlay.style.display = 'none';
      }
    });

    panel.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.stopPropagation();
      e.dataTransfer.dropEffect = 'copy';
    });

    panel.addEventListener('drop', (e) => {
      e.preventDefault();
      e.stopPropagation();
      dragCounter = 0;
      if (overlay) overlay.style.display = 'none';
      if (this.folderId === -1) {
        alert('Please select a repository before uploading.');
        return;
      }
      const files = e.dataTransfer.files;
      if (files && files.length > 0) {
        this.uploadFiles(files);
      }
    });
  }

  triggerFileUpload() {
    const uploadInput = document.getElementById('file-upload-input');
    if (uploadInput) {
      uploadInput.click();
    }
  }

  async handleFileUpload(event) {
    const files = event.target.files;
    if (!files || files.length === 0 || this.folderId === -1) {
      return;
    }
    await this.uploadFiles(files);
    // Clear input
    event.target.value = '';
  }

  async uploadFiles(files) {
    if (!files || files.length === 0 || this.folderId === -1) {
      return;
    }

    const modal = new Foundation.Reveal($('#upload-modal'));
    modal.open();

    const progressContainer = document.getElementById('upload-progress');
    const successContainer = document.getElementById('upload-success');
    const errorContainer = document.getElementById('upload-error');
    const progressBar = document.getElementById('upload-progress-bar');
    const statusText = document.getElementById('upload-status');

    progressContainer.style.display = 'block';
    successContainer.style.display = 'none';
    errorContainer.style.display = 'none';

    try {
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('folderId', this.folderId);
      for (const file of files) {
        formData.append('file', file);
      }

      statusText.textContent = `Uploading ${files.length} file(s)...`;
      progressBar.value = 0;

      const response = await fetch('/json/documentUpload', {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const result = await response.json();
      if (result.success === false || result.error) {
        throw new Error(result.message || 'Upload failed');
      }

      progressBar.value = 100;
      progressContainer.style.display = 'none';
      successContainer.style.display = 'block';

      setTimeout(() => {
        modal.close();
        this.reload();
      }, 2000);

    } catch (err) {
      console.error('Upload failed', err);
      progressContainer.style.display = 'none';
      errorContainer.style.display = 'block';
      document.getElementById('upload-error-message').textContent = err.message || 'Upload failed';
    }
  }

  setFolder(folderId, parentFolderId) {
    this.folderId = folderId;
    if (parentFolderId) {
      // When parentFolderId is provided, this is a subfolder selection
      this.parentFolderId = parentFolderId;
      this.subFolderId = folderId;
    } else {
      // When selecting a root folder, reset subfolder tracking
      this.parentFolderId = -1;
      this.subFolderId = -1;
    }
    
    // Update the document browser title with the folder icon and name
    this.updateBrowserTitle(folderId, parentFolderId);
    
    this.reload();
  }

  updateBrowserTitle(folderId, parentFolderId) {
    const titleElement = document.getElementById('document-browser-title');
    if (!titleElement) {
      return;
    }

    // Get the folder object from the library manager
    const folder = this.editor.library.getCurrentFolder(folderId);
    if (!folder) {
      titleElement.innerHTML = 'Files';
      return;
    }

    // Determine icon and title based on folder type
    if (parentFolderId && parentFolderId > -1) {
      // This is a subfolder - show both parent repo and subfolder
      const parentFolder = this.editor.library.folders.find(f => f.id === parentFolderId);
      if (parentFolder) {
        const repoIcon = '<i class="fas fa-book"></i>';
        const subfolderIcon = '<i class="fa-regular fa-folder"></i>';
        titleElement.innerHTML = `${repoIcon} ${parentFolder.name || 'Untitled'} <i class="fas fa-chevron-right" style="font-size: 0.8em; margin: 0 0.25em;"></i> ${subfolderIcon} ${folder.name || 'Untitled'}`;
      } else {
        // Fallback if parent not found
        const subfolderIcon = '<i class="fa-regular fa-folder"></i>';
        titleElement.innerHTML = `${subfolderIcon} ${folder.name || 'Untitled'}`;
      }
    } else {
      // This is a root repository folder
      const repoIcon = '<i class="fas fa-book"></i>';
      titleElement.innerHTML = `${repoIcon} ${folder.name || 'Untitled'}`;
    }
  }

  async reload() {
    if (this.folderId === -1) {
      return;
    }
    try {
      const searchTerm = this.searchInput ? this.searchInput.value.trim() : '';
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentFileList`, globalThis.location.origin);
      if (this.subFolderId > -1) {
        url.searchParams.set('folderId', this.parentFolderId);
        url.searchParams.set('subFolderId', this.subFolderId);
      } else {
        url.searchParams.set('folderId', this.folderId);
      }
      url.searchParams.set('page', 1);
      url.searchParams.set('limit', 50);
      if (searchTerm) {
        url.searchParams.set('search', searchTerm);
      }

      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const payload = await response.json();
      this.files = payload.files || [];
      
      // Load subfolders if viewing a root folder
      if (this.subFolderId === -1 && this.parentFolderId === -1) {
        await this.loadSubfoldersForDisplay(this.folderId);
      }
      
      this.render();
    } catch (err) {
      console.error('Unable to load files', err);
      if (this.tableBody) {
        this.tableBody.innerHTML = '<tr><td colspan="6" class="empty-state">Unable to load files</td></tr>';
      }
    } finally {
      this.editor.hideLoading();
    }
  }

  async loadSubfoldersForDisplay(folderId) {
    try {
      const url = new URL(
        this.editor.config.apiBaseUrl + '/documentSubfolders',
        globalThis.location.origin
      );
      url.searchParams.set('folderId', folderId);

      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const payload = await response.json();
      this.subfolders = payload.subfolders || [];
    } catch (err) {
      console.error('Unable to load subfolders for display', err);
      this.subfolders = [];
    }
  }

  render() {
    if (!this.tableBody) {
      return;
    }
    this.tableBody.innerHTML = '';

    // If viewing a subfolder, show parent navigation
    if (this.subFolderId > -1 && this.parentFolderId > -1) {
      const row = document.createElement('tr');
      row.classList.add('parent-folder-row');
      row.innerHTML = `
        <td colspan="6">
          <span class="file-icon"><i class="fa-regular fa-folder-open"></i></span>
          <strong style="cursor: pointer;" class="parent-folder-link">.. (Parent Folder)</strong>
        </td>
      `;
      row.addEventListener('click', () => {
        this.navigateToParentFolder();
      });
      this.tableBody.appendChild(row);
    }

    // Render subfolders first (if viewing a root folder)
    if (this.subfolders && this.subfolders.length > 0 && this.subFolderId === -1) {
      this.subfolders.forEach((subfolder) => {
        const row = document.createElement('tr');
        row.dataset.subFolderId = subfolder.id;
        row.classList.add('subfolder-row');

        const startDateStr = subfolder.startDate ? this.formatDate(subfolder.startDate) : '—';
        const fileCountText = subfolder.fileCount || 0;
        const fileCountLabel = fileCountText === 1 ? 'file' : 'files';
        const titleWithCount = `${subfolder.name || 'Untitled'} (${fileCountText} ${fileCountLabel})`;

        row.innerHTML = `
          <td><span class="file-icon"><i class="fa-regular fa-folder"></i></span> <strong>${titleWithCount}</strong></td>
          <td>—</td>
          <td>Folder</td>
          <td>—</td>
          <td>—</td>
          <td>${startDateStr}</td>
        `;

        row.addEventListener('click', () => {
          this.navigateToSubfolder(subfolder.id);
        });

        this.tableBody.appendChild(row);
      });
    }

    // Then render files
    if (!this.files.length && (!this.subfolders || this.subfolders.length === 0)) {
      this.tableBody.innerHTML = '<tr><td colspan="6" class="empty-state">No files or folders in this folder</td></tr>';
      return;
    }

    this.files.forEach((file) => {
      const row = document.createElement('tr');
      row.dataset.fileId = file.id;

      row.innerHTML = `
        <td><span class="file-icon">${this.getMimeIcon(file.mimeType, file.filename)}</span> ${file.title || file.filename || 'Untitled'}</td>
        <td>${file.version || ''}</td>
        <td>${file.mimeType || ''}</td>
        <td>${file.fileLength ? this.formatSize(file.fileLength) : ''}</td>
        <td>${file.downloadCount || 0}</td>
        <td>${file.modified ? this.formatDate(file.modified) : ''}</td>
      `;

      row.addEventListener('click', () => {
        this.selectFile(file.id);
      });

      this.tableBody.appendChild(row);
    });
  }

  navigateToSubfolder(subFolderId) {
    // Drill into subfolder
    this.editor.fileManager.setFolder(subFolderId, this.folderId);
    
    // Update breadcrumb navigation
    const breadcrumbNav = document.getElementById('breadcrumb-navigation');
    if (breadcrumbNav && this.subfolders) {
      const subfolder = this.subfolders.find(f => f.id === subFolderId);
      if (subfolder) {
        breadcrumbNav.style.display = 'block';
        const breadcrumbPath = document.getElementById('breadcrumb-path');
        if (breadcrumbPath) {
          breadcrumbPath.innerHTML = `<span class="breadcrumb-item">${subfolder.name}</span>`;
        }
      }
    }
  }

  navigateToParentFolder() {
    // Navigate back to parent folder
    if (this.parentFolderId > -1) {
      // Hide breadcrumb
      const breadcrumbNav = document.getElementById('breadcrumb-navigation');
      if (breadcrumbNav) {
        breadcrumbNav.style.display = 'none';
      }
      // Reset to parent folder
      this.setFolder(this.parentFolderId);
    }
  }

  selectFile(fileId) {
    if (this.tableBody) {
      this.tableBody.querySelectorAll('tr').forEach((row) => {
        row.classList.toggle('active', Number(row.dataset.fileId) === Number(fileId));
      });
    }
    // Store current file for preview
    this.currentFile = this.files.find(f => f.id === fileId);
    if (this.currentFile) {
      this.editor.showFileProperties(this.currentFile);
    }
    this.editor.properties.loadFile(fileId);
  }

  async saveVersion() {
    // Placeholder for upload new version wiring
    alert('Save version is not wired yet.');
  }

  downloadFile() {
    if (!this.currentFile) {
      alert('No file selected');
      return;
    }
    if (!this.currentFile.url) {
      alert('File URL not available');
      return;
    }
    // Open download in new window/tab
    window.open(`/assets/file/${this.currentFile.url}`, '_blank');
  }

  addVersion() {
    if (!this.currentFile) {
      alert('No file selected');
      return;
    }
    // URL files need a different version update flow
    if (this.currentFile.mimeType === 'text/uri-list' || this.currentFile.fileType === 'URL') {
      this.addUrlVersion();
      return;
    }
    // Open hidden file input for version upload
    const versionInput = document.createElement('input');
    versionInput.type = 'file';
    versionInput.style.display = 'none';
    versionInput.addEventListener('change', async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      await this.uploadNewVersion(file);
      versionInput.remove();
    });
    document.body.appendChild(versionInput);
    versionInput.click();
  }

  addUrlVersion() {
    if (!this.currentFile) return;
    const modal = document.getElementById('update-url-version-modal');
    if (!modal) {
      // Fallback to browser prompt
      const newUrl = prompt('Enter the updated URL:', this.currentFile.filename || '');
      if (newUrl === null) return;
      if (!newUrl.trim()) { alert('URL cannot be empty'); return; }
      this.saveUrlVersion(null, newUrl);
      return;
    }
    const urlInput = modal.querySelector('#update-url-link');
    const versionInput = modal.querySelector('#update-url-version');
    if (urlInput) urlInput.value = this.currentFile.filename || '';
    if (versionInput) versionInput.value = '';
    const saveBtn = modal.querySelector('#save-url-version-btn');
    if (saveBtn) {
      saveBtn.onclick = () => this.saveUrlVersion(modal);
    }
    if (typeof $ !== 'undefined' && typeof Foundation !== 'undefined') {
      new Foundation.Reveal($(modal)).open();
    } else {
      modal.style.display = 'block';
      modal.classList.add('is-open');
    }
  }

  async saveUrlVersion(modal, fallbackUrl) {
    const urlInput = modal ? modal.querySelector('#update-url-link') : null;
    const versionInput = modal ? modal.querySelector('#update-url-version') : null;
    const url = urlInput ? urlInput.value.trim() : (fallbackUrl || '');
    if (!url) {
      alert('URL is required.');
      return;
    }
    const version = versionInput ? versionInput.value.trim() : '';
    try {
      this.editor.showLoading();
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('fileId', this.currentFile.id);
      formData.append('url', url);
      if (version) formData.append('version', version);
      const response = await fetch(`${this.editor.config.apiBaseUrl}/documentAddUrlVersion`, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const result = await response.json();
      if (result.success) {
        this.editor.closeModal(modal);
        this.reload();
        if (this.editor.properties && this.currentFile) {
          this.editor.properties.loadFile(this.currentFile.id);
        }
      } else {
        alert(result.message || 'Failed to update URL version.');
      }
    } catch (err) {
      console.error('Error updating URL version', err);
      alert('Error updating URL version: ' + err.message);
    } finally {
      this.editor.hideLoading();
    }
  }

  async uploadNewVersion(file) {
    try {
      this.editor.showLoading();
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('fileId', this.currentFile.id);
      formData.append('file', file);
      const response = await fetch(`${this.editor.config.apiBaseUrl}/documentAddVersion`, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const result = await response.json();
      if (result.success) {
        this.reload();
        if (this.editor.properties && this.currentFile) {
          this.editor.properties.loadFile(this.currentFile.id);
        }
      } else {
        alert(result.message || 'Failed to add version.');
      }
    } catch (err) {
      console.error('Error uploading version', err);
      alert('Error uploading version: ' + err.message);
    } finally {
      this.editor.hideLoading();
    }
  }

  moveFile() {
    if (!this.currentFile) {
      alert('No file selected');
      return;
    }
    const modal = document.getElementById('move-file-modal');
    if (!modal) {
      return;
    }
    // Populate folder dropdown
    const folderSelect = modal.querySelector('#move-target-folder');
    if (folderSelect) {
      folderSelect.innerHTML = '<option value="">-- Select Repository --</option>';
      const folders = this.editor.library.folders || [];
      folders.forEach((f) => {
        const opt = document.createElement('option');
        opt.value = f.id;
        opt.textContent = f.name || 'Untitled';
        folderSelect.appendChild(opt);
      });
    }
    // Reset subfolder
    const subGroup = modal.querySelector('#move-target-subfolder-group');
    const subSelect = modal.querySelector('#move-target-subfolder');
    if (subGroup) subGroup.style.display = 'none';
    if (subSelect) subSelect.innerHTML = '<option value="">-- Root of Repository --</option>';
    // Wire folder change to load subfolders
    if (folderSelect) {
      folderSelect.onchange = async () => {
        const targetId = parseInt(folderSelect.value);
        if (!targetId) {
          if (subGroup) subGroup.style.display = 'none';
          return;
        }
        try {
          const url = new URL(`${this.editor.config.apiBaseUrl}/documentSubfolders`, globalThis.location.origin);
          url.searchParams.set('folderId', targetId);
          const resp = await fetch(url.toString(), { credentials: 'same-origin' });
          const data = await resp.json();
          const subs = data.subfolders || [];
          if (subs.length > 0 && subSelect) {
            subSelect.innerHTML = '<option value="">-- Root of Repository --</option>';
            subs.forEach((s) => {
              const opt = document.createElement('option');
              opt.value = s.id;
              opt.textContent = s.name;
              subSelect.appendChild(opt);
            });
            if (subGroup) subGroup.style.display = 'block';
          } else {
            if (subGroup) subGroup.style.display = 'none';
          }
        } catch (err) {
          console.error('Error loading subfolders for move', err);
        }
      };
    }
    const confirmBtn = modal.querySelector('#confirm-move-btn');
    if (confirmBtn) {
      confirmBtn.onclick = () => this.confirmMoveFile(modal);
    }
    if (typeof $ !== 'undefined' && typeof Foundation !== 'undefined') {
      new Foundation.Reveal($(modal)).open();
    } else {
      modal.style.display = 'block';
      modal.classList.add('is-open');
    }
  }

  async confirmMoveFile(modal) {
    const folderSelect = modal.querySelector('#move-target-folder');
    const subSelect = modal.querySelector('#move-target-subfolder');
    const targetFolderId = parseInt(folderSelect ? folderSelect.value : 0);
    const targetSubFolderId = subSelect ? parseInt(subSelect.value) || -1 : -1;
    if (!targetFolderId) {
      alert('Please select a target repository.');
      return;
    }
    try {
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('fileId', this.currentFile.id);
      formData.append('targetFolderId', targetFolderId);
      if (targetSubFolderId > 0) formData.append('targetSubFolderId', targetSubFolderId);
      const response = await fetch(`${this.editor.config.apiBaseUrl}/documentMoveFile`, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });
      const result = await response.json();
      if (result.success) {
        this.editor.closeModal(modal);
        this.reload();
      } else {
        alert(result.message || 'Failed to move file.');
      }
    } catch (err) {
      console.error('Error moving file', err);
      alert('Error moving file: ' + err.message);
    }
  }

  async deleteFile() {
    if (!this.currentFile) {
      alert('No file selected');
      return;
    }
    if (!confirm(`Are you sure you want to delete "${this.currentFile.title || this.currentFile.filename}"?`)) {
      return;
    }
    try {
      this.editor.showLoading();
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('fileId', this.currentFile.id);
      const response = await fetch(`${this.editor.config.apiBaseUrl}/documentDeleteFile`, {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
      });
      const result = await response.json();
      if (result.success) {
        this.currentFile = null;
        // Clear the properties panel
        const contentArea = document.getElementById('document-properties-content');
        if (contentArea) contentArea.innerHTML = '<div class="empty-state">Select a file to view details</div>';
        const fileSection = document.getElementById('file-properties-section');
        if (fileSection) fileSection.style.display = 'none';
        this.reload();
      } else {
        alert(result.message || 'Failed to delete file.');
      }
    } catch (err) {
      console.error('Error deleting file', err);
      alert('Error deleting file: ' + err.message);
    } finally {
      this.editor.hideLoading();
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

  formatDate(dateStr) {
    if (!dateStr) {
      return '';
    }
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    } catch {
      return dateStr;
    }
  }

  getMimeIcon(mimeType, filename) {
    const type = (mimeType || '').toLowerCase();
    const name = (filename || '').toLowerCase();
    if (type === 'text/uri-list') return '<i class="fas fa-link"></i>';
    if (type.startsWith('image/')) return '<i class="fas fa-file-image"></i>';
    if (type === 'application/pdf') return '<i class="fas fa-file-pdf"></i>';
    if (type.startsWith('video/')) return '<i class="fas fa-file-video"></i>';
    if (type.startsWith('audio/')) return '<i class="fas fa-file-audio"></i>';
    if (type.startsWith('text/') || type === 'application/json') return '<i class="fas fa-file-alt"></i>';
    if (type.includes('spreadsheet') || type.includes('excel') || type === 'application/vnd.ms-excel') return '<i class="fas fa-file-excel"></i>';
    if (type.includes('presentation') || type.includes('powerpoint')) return '<i class="fas fa-file-powerpoint"></i>';
    if (type.includes('word')) return '<i class="fas fa-file-word"></i>';
    if (type.includes('zip') || type.includes('compressed') || type.includes('tar')) return '<i class="fas fa-file-archive"></i>';
    if (type.includes('xml') || type.includes('html') || type.includes('javascript')) return '<i class="fas fa-file-code"></i>';
    if (name.endsWith('.drawio')) return '<i class="fas fa-project-diagram"></i>';
    return '<i class="fas fa-file"></i>';
  }
}
