/**
 * Handles file listing, selection, and basic preview wiring
 */

class DocumentFileManager {
  constructor(editor) {
    this.editor = editor;
    this.folderId = -1;
    this.files = [];
    this.tableBody = null;
    this.searchInput = document.getElementById('file-search');
  }

  init() {
    const table = document.getElementById('file-table');
    if (table) {
      this.tableBody = table.querySelector('tbody');
    }
    if (this.searchInput) {
      this.searchInput.addEventListener('input', () => this.reload());
    }
  }

  setFolder(folderId) {
    this.folderId = folderId;
    this.reload();
  }

  async reload() {
    if (this.folderId === -1) {
      return;
    }
    try {
      const searchTerm = this.searchInput ? this.searchInput.value.trim() : '';
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentFileList`, globalThis.location.origin);
      url.searchParams.set('folderId', this.folderId);
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

  render() {
    if (!this.tableBody) {
      return;
    }
    this.tableBody.innerHTML = '';
    if (!this.files.length) {
      this.tableBody.innerHTML = '<tr><td colspan="6" class="empty-state">No files in this folder</td></tr>';
      return;
    }

    this.files.forEach((file) => {
      const row = document.createElement('tr');
      row.dataset.fileId = file.id;

      row.innerHTML = `
        <td>${file.title || file.filename || 'Untitled'}</td>
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

  selectFile(fileId) {
    if (this.tableBody) {
      this.tableBody.querySelectorAll('tr').forEach((row) => {
        row.classList.toggle('active', Number(row.dataset.fileId) === Number(fileId));
      });
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
}
