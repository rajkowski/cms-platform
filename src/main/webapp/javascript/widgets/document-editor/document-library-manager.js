/**
 * Handles folder loading/search, subfolder navigation, and selection in the document editor
 */

class DocumentLibraryManager {
  constructor(editor) {
    this.editor = editor;
    this.token = editor.config.token;
    this.folders = [];
    this.subfolders = [];
    this.parentFolderId = -1;
    this.currentSubFolderId = -1;
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
        this.listContainer.innerHTML = '<div class="empty-state">Unable to load repositories</div>';
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
      homeBtn.innerHTML = '<i class="fas fa-home"></i> All Repositories';
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

    const items = this.parentFolderId === -1 ? this.folders : this.subfolders;
    if (this.parentFolderId === -1 && !items.length) {
      this.listContainer.insertAdjacentHTML('beforeend', '<div class="empty-state">No repositories found</div>');
      return;
    }

    // When a folder is selected, keep it visible above its subfolders
    if (this.parentFolderId > -1) {
      const parentFolder = this.folders.find((f) => Number(f.id) === Number(this.parentFolderId)) || this.breadcrumbs[this.breadcrumbs.length - 1] || {};
      const parentItem = document.createElement('div');
      parentItem.className = 'folder-item parent-folder';
      parentItem.dataset.folderId = parentFolder.id;

      const header = document.createElement('div');
      header.className = 'folder-header';

      const icon = document.createElement('span');
      icon.className = 'folder-icon';
      icon.innerHTML = '<i class="fas fa-book"></i>';
      header.appendChild(icon);

      const name = document.createElement('div');
      name.className = 'folder-name';
      name.textContent = parentFolder.name || 'Selected Repository';
      header.appendChild(name);

      parentItem.appendChild(header);

      const meta = document.createElement('div');
      meta.className = 'folder-meta';
      meta.innerHTML = '<span>' + (parentFolder.fileCount || 0) + ' files</span>';
      parentItem.appendChild(meta);

      parentItem.addEventListener('click', () => {
        this.selectFolder(parentFolder.id);
      });

      this.listContainer.appendChild(parentItem);
    }

    if (this.parentFolderId > -1 && !items.length) {
      this.listContainer.insertAdjacentHTML('beforeend', '<div class="empty-state">No subfolders in this repository</div>');
      return;
    }

    items.forEach((folder) => {
      const item = document.createElement('div');
      if (this.parentFolderId === -1) {
        item.className = 'folder-item';
      } else {
        item.className = 'folder-item subfolder-item';
      }
      item.dataset.folderId = folder.id;

      const header = document.createElement('div');
      header.className = 'folder-header';

      const icon = document.createElement('span');
      icon.className = 'folder-icon';
      if (this.parentFolderId === -1) {
        icon.innerHTML = '<i class="fas fa-book"></i>';
      } else {
        icon.innerHTML = '<i class="fa-regular fa-folder"></i>';
      }
      header.appendChild(icon);

      const name = document.createElement('div');
      name.className = 'folder-name';
      name.textContent = folder.name || 'Untitled Folder';
      header.appendChild(name);

      // Add subfolder indicator if at root level
      if (this.parentFolderId === -1) {
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
    if (this.parentFolderId > 0) {
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
    // Pass parent folder ID when viewing subfolders
    if (this.parentFolderId > -1) {
      // Check if clicking on the parent folder itself or a subfolder
      if (Number(folderId) === Number(this.parentFolderId)) {
        // Clicking the parent folder - reload its file list
        this.currentSubFolderId = -1;
        this.editor.fileManager.setFolder(folderId);
      } else {
        // Selecting a subfolder - get the subfolder's parent folder ID from the subfolder object
        const subfolder = this.subfolders.find((f) => f.id === folderId);
        if (subfolder) {
          this.currentSubFolderId = folderId;
          // Use the subfolder's folderId property as the parent folder ID
          this.editor.fileManager.setFolder(folderId, subfolder.folderId);
        }
      }
    } else {
      // Selecting a root folder (don't change parentFolderId, just load files)
      this.currentSubFolderId = -1;
      this.editor.fileManager.setFolder(folderId);
    }
    // Load folder details for the properties panel
    const folder = this.getCurrentFolder(folderId);
    if (folder && this.editor.folderDetails) {
      this.editor.folderDetails.setFolderAndLoad(folder);
    }
    // Enable file-toolbar buttons when a folder is selected
    this.enableFileToolbar();
  }

  enableFileToolbar() {
    const buttons = [
      'new-subfolder-btn',
      'import-doc-btn',
      'new-url-btn',
      'reload-files-btn'
    ];
    buttons.forEach((btnId) => {
      const btn = document.getElementById(btnId);
      if (btn) {
        btn.disabled = false;
      }
    });
  }

  disableFileToolbar() {
    const buttons = [
      'new-subfolder-btn',
      'import-doc-btn',
      'new-url-btn',
      'reload-files-btn'
    ];
    buttons.forEach((btnId) => {
      const btn = document.getElementById(btnId);
      if (btn) {
        btn.disabled = true;
      }
    });
  }

  getCurrentFolder(folderId) {
    if (this.parentFolderId === -1) {
      return this.folders.find((f) => f.id === folderId);
    }
    return this.subfolders.find((f) => f.id === folderId);
  }

  async navigateToFolder(folderId, folderName) {
    // If clicking on an existing breadcrumb, remove entries after it
    const existingIndex = this.breadcrumbs.findIndex((crumb) => Number(crumb.id) === Number(folderId));
    if (existingIndex >= 0) {
      // Keep only breadcrumbs up to and including this one
      this.breadcrumbs = this.breadcrumbs.slice(0, existingIndex + 1);
    } else {
      // New navigation, add to breadcrumbs
      this.breadcrumbs.push({ id: folderId, name: folderName });
    }
    
    this.parentFolderId = folderId;
    this.currentSubFolderId = -1;
    await this.loadSubfolders(folderId);
  }

  navigateToRoot() {
    this.parentFolderId = -1;
    this.currentSubFolderId = -1;
    this.breadcrumbs = [];
    this.subfolders = [];
    this.render();
    // Disable file-toolbar buttons when no folder is selected
    this.disableFileToolbar();
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

  createFolder() {
    const folderName = prompt('Enter repository name:');
    if (!folderName || !folderName.trim()) {
      return;
    }

    fetch('/json/documentCreateFolder', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: this.token,
        folderName: folderName.trim()
      })
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          this.loadFolders();
        } else {
          alert('Error creating repository: ' + (data.message || 'Unknown error'));
        }
      })
      .catch((err) => {
        console.error('Error creating repository:', err);
        alert('Error creating repository');
      });
  }

  createSubfolder() {
    const folderName = prompt('Enter folder name:');
    if (!folderName) {
      return;
    }

    fetch('/json/documentCreateSubfolder', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: this.token,
        parentFolderId: this.currentFolderId,
        folderName: folderName
      })
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          this.loadSubfolders(this.currentFolderId);
        } else {
          alert('Error creating subfolder: ' + (data.message || 'Unknown error'));
        }
      })
      .catch((err) => {
        console.error('Error creating folder:', err);
        alert('Error creating folder');
      });
  }
}
