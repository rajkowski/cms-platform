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

    // View toggle button
    const viewToggleBtn = document.getElementById('view-toggle-btn');
    if (viewToggleBtn) {
      viewToggleBtn.addEventListener('click', () => this.toggleView());
    }

    // Close preview button
    const closePreviewBtn = document.getElementById('close-preview-btn');
    if (closePreviewBtn) {
      closePreviewBtn.addEventListener('click', () => this.closePreview());
    }

    // File upload
    const uploadInput = document.getElementById('file-upload-input');
    if (uploadInput) {
      uploadInput.addEventListener('change', (e) => this.handleFileUpload(e));
    }
  }

  toggleView() {
    if (this.viewMode === 'table') {
      this.showPreview();
    } else {
      this.closePreview();
    }
  }

  showPreview() {
    if (!this.currentFile) {
      return;
    }
    this.viewMode = 'preview';
    document.getElementById('file-list-container').style.display = 'none';
    document.getElementById('file-preview-container').style.display = 'block';
    this.renderPreview(this.currentFile);
  }

  closePreview() {
    this.viewMode = 'table';
    document.getElementById('file-list-container').style.display = 'block';
    document.getElementById('file-preview-container').style.display = 'none';
  }

  renderPreview(file) {
    const previewContent = document.getElementById('file-preview-content');
    if (!previewContent) {
      return;
    }

    const mimeType = file.mimeType || '';
    const fileUrl = file.url || '';

    if (!fileUrl) {
      previewContent.innerHTML = '<div class="empty-state">No preview available</div>';
      return;
    }

    // Image preview
    if (mimeType.startsWith('image/')) {
      previewContent.innerHTML = `<img src="/assets/view/${fileUrl}" alt="${file.title || file.filename}" />`;
    }
    // PDF preview
    else if (mimeType === 'application/pdf') {
      previewContent.innerHTML = `<iframe src="/assets/view/${fileUrl}" type="application/pdf"></iframe>`;
    }
    // URL preview
    else if (mimeType === 'text/uri-list') {
      previewContent.innerHTML = `<div class="url-preview"><a href="${file.filename}" target="_blank">${file.filename}</a></div>`;
    }
    // Text preview
    else if (mimeType.startsWith('text/') || mimeType === 'application/json') {
      fetch(fileUrl)
        .then(response => response.text())
        .then(text => {
          previewContent.innerHTML = `<pre>${this.escapeHtml(text)}</pre>`;
        })
        .catch(() => {
          previewContent.innerHTML = '<div class="empty-state">Unable to load text preview</div>';
        });
    }
    // Default message
    else {
      previewContent.innerHTML = `<div class="empty-state">Preview not available for this file type<br><a href="${fileUrl}" target="_blank">Download File</a></div>`;
    }
  }

  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
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
        formData.append('files', file);
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

    // Clear input
    event.target.value = '';
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
    this.reload();
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

        row.innerHTML = `
          <td><span class="file-icon"><i class="fa-regular fa-folder"></i></span> <strong>${subfolder.name || 'Untitled'}</strong></td>
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
