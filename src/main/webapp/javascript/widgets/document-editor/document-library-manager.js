/**
 * Handles folder loading/search and selection in the document editor
 */

class DocumentLibraryManager {
  constructor(editor) {
    this.editor = editor;
    this.folders = [];
    this.searchInput = document.getElementById('document-search');
    this.listContainer = document.getElementById('folder-list-container');
  }

  init() {
    if (this.searchInput) {
      this.searchInput.addEventListener('input', () => this.loadFolders());
    }
    this.loadFolders();
  }

  async loadFolders() {
    try {
      const searchTerm = this.searchInput ? this.searchInput.value.trim() : '';
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentLibrary`, globalThis.location.origin);
      if (searchTerm) {
        url.searchParams.set('search', searchTerm);
      }
      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const payload = await response.json();
      this.folders = payload.folders || [];
      this.render();
    } catch (err) {
      console.error('Unable to load folders', err);
      if (this.listContainer) {
        this.listContainer.innerHTML = '<div class="empty-state">Unable to load folders</div>';
      }
    } finally {
      this.editor.hideLoading();
    }
  }

  render() {
    if (!this.listContainer) {
      return;
    }
    this.listContainer.innerHTML = '';
    if (!this.folders.length) {
      this.listContainer.innerHTML = '<div class="empty-state">No folders found</div>';
      return;
    }

    this.folders.forEach((folder) => {
      const item = document.createElement('div');
      item.className = 'folder-item';
      item.dataset.folderId = folder.id;

      const name = document.createElement('div');
      name.className = 'folder-name';
      name.textContent = folder.name || 'Untitled Folder';
      item.appendChild(name);

      const meta = document.createElement('div');
      meta.className = 'folder-meta';
      meta.innerHTML = `<span>${folder.fileCount || 0} files</span>`;
      if (folder.hasCategories) {
        meta.innerHTML += '<span>Categories</span>';
      }
      if (folder.hasAllowedGroups) {
        meta.innerHTML += '<span>Restricted</span>';
      }
      item.appendChild(meta);

      item.addEventListener('click', () => {
        this.selectFolder(folder.id);
      });

      this.listContainer.appendChild(item);
    });
  }

  selectFolder(folderId) {
    if (!this.listContainer) {
      return;
    }
    this.listContainer.querySelectorAll('.folder-item').forEach((el) => {
      el.classList.toggle('active', Number(el.dataset.folderId) === Number(folderId));
    });
    this.editor.fileManager.setFolder(folderId);
  }
}
