/**
 * Handles folder loading/search, subfolder navigation, and selection in the document editor
 */

class DocumentLibraryManager {
  constructor(editor) {
    this.editor = editor;
    this.folders = [];
    this.subfolders = [];
    this.currentFolderId = -1;
    this.breadcrumbs = [];
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
      const url = new URL(
        this.editor.config.apiBaseUrl + '/documentLibrary',
        globalThis.location.origin
      );
      if (searchTerm) {
        url.searchParams.set('search', searchTerm);
      }
      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
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

    // Render breadcrumbs if in subfolder view
    if (this.breadcrumbs.length > 0) {
      const breadcrumbContainer = document.createElement('div');
      breadcrumbContainer.className = 'folder-breadcrumbs';
      
      const homeBtn = document.createElement('button');
      homeBtn.className = 'breadcrumb-btn';
      homeBtn.innerHTML = '<i class="fas fa-home"></i> All Folders';
      homeBtn.addEventListener('click', () => this.navigateToRoot());
      breadcrumbContainer.appendChild(homeBtn);

      this.breadcrumbs.forEach((crumb, index) => {
        const separator = document.createElement('span');
        separator.className = 'breadcrumb-separator';
        separator.textContent = '/';
        breadcrumbContainer.appendChild(separator);

        const crumbBtn = document.createElement('button');
        crumbBtn.className = 'breadcrumb-btn';
        crumbBtn.textContent = crumb.name;
        if (index < this.breadcrumbs.length - 1) {
          crumbBtn.addEventListener('click', () => this.navigateToFolder(crumb.id, crumb.name));
        } else {
          crumbBtn.classList.add('active');
        }
        breadcrumbContainer.appendChild(crumbBtn);
      });

      this.listContainer.appendChild(breadcrumbContainer);
    }

    // Render folders or subfolders
    const items = this.currentFolderId === -1 ? this.folders : this.subfolders;
    if (!items.length) {
      const emptyMsg = this.currentFolderId === -1 ? 'No folders found' : 'No subfolders in this folder';
      this.listContainer.innerHTML += '<div class="empty-state">' + emptyMsg + '</div>';
      return;
    }

    items.forEach((folder) => {
      const item = document.createElement('div');
      item.className = 'folder-item';
      item.dataset.folderId = folder.id;

      const header = document.createElement('div');
      header.className = 'folder-header';

      const icon = document.createElement('span');
      icon.className = 'folder-icon';
      icon.innerHTML = '<i class="fas fa-folder"></i>';
      header.appendChild(icon);

      const name = document.createElement('div');
      name.className = 'folder-name';
      name.textContent = folder.name || 'Untitled Folder';
      header.appendChild(name);

      // Add subfolder indicator if at root level
      if (this.currentFolderId === -1) {
        const navBtn = document.createElement('button');
        navBtn.className = 'folder-nav-btn';
        navBtn.innerHTML = '<i class="fas fa-chevron-right"></i>';
        navBtn.title = 'View subfolders';
        navBtn.addEventListener('click', (e) => {
          e.stopPropagation();
          this.navigateToFolder(folder.id, folder.name);
        });
        header.appendChild(navBtn);
      }

      item.appendChild(header);

      const meta = document.createElement('div');
      meta.className = 'folder-meta';
      meta.innerHTML = '<span>' + (folder.fileCount || 0) + ' files</span>';
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

    // Add "New Subfolder" button if in folder view
    if (this.currentFolderId > 0) {
      const addBtn = document.createElement('button');
      addBtn.className = 'button tiny primary add-subfolder-btn';
      addBtn.innerHTML = '<i class="fas fa-plus"></i> New Subfolder';
      addBtn.addEventListener('click', () => this.createSubfolder());
      this.listContainer.appendChild(addBtn);
    }
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

  async navigateToFolder(folderId, folderName) {
    this.currentFolderId = folderId;
    this.breadcrumbs.push({ id: folderId, name: folderName });
    await this.loadSubfolders(folderId);
  }

  navigateToRoot() {
    this.currentFolderId = -1;
    this.breadcrumbs = [];
    this.subfolders = [];
    this.render();
  }

  async loadSubfolders(folderId) {
    try {
      const url = new URL(
        this.editor.config.apiBaseUrl + '/documentSubfolders',
        globalThis.location.origin
      );
      url.searchParams.set('folderId', folderId);

      this.editor.showLoading();
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error('HTTP ' + response.status);
      }
      const payload = await response.json();
      this.subfolders = payload.subfolders || [];
      this.render();
    } catch (err) {
      console.error('Unable to load subfolders', err);
      if (this.listContainer) {
        this.listContainer.innerHTML = '<div class="empty-state">Unable to load subfolders</div>';
      }
    } finally {
      this.editor.hideLoading();
    }
  }

  createSubfolder() {
    const name = prompt('Enter subfolder name:');
    if (!name || !name.trim()) {
      return;
    }

    fetch('/json/documentCreateSubfolder', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        folderId: this.currentFolderId,
        name: name.trim()
      }),
      credentials: 'same-origin'
    })
      .then(response => response.json())
      .then(result => {
        if (result.status === 0) {
          alert('Error: ' + (result.message || 'Could not create subfolder'));
          return;
        }
        // Reload subfolders
        this.loadSubfolders(this.currentFolderId);
      })
      .catch(err => {
        console.error('Create subfolder failed', err);
        alert('Failed to create subfolder');
      });
  }
}
