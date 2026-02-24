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
    this.listContainer.querySelectorAll('.parent-folder').forEach((el) => {
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
    if (folder) {
      // Determine if this is a repository or subfolder and show appropriate section
      if (this.parentFolderId === -1) {
        // Root folder (repository)
        this.editor.showRepositoryProperties(folder);
        // Also load full details via folder details manager
        if (this.editor.folderDetails) {
          this.editor.folderDetails.loadFolder(folderId);
        }
      } else {
        // Subfolder
        this.editor.showSubfolderProperties(folder);
      }
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

  reload() {
    this.loadFolders();
  }

  createFolder() {
    const modal = document.getElementById('new-folder-modal');
    if (!modal) {
      alert('Modal not found');
      return;
    }

    // Reset form
    const form = document.getElementById('new-folder-form');
    if (form) {
      form.reset();
    }

    // Clear error messages if any
    const errorContainer = modal.querySelector('.error-message');
    if (errorContainer) {
      errorContainer.remove();
    }

    // Load user groups into permissions table
    this.loadGroupsForModal();

    // Show modal using Foundation
    const modalInstance = new Foundation.Reveal($(modal));
    modalInstance.open();

    // Setup form submission
    const saveBtn = document.getElementById('save-folder-btn');
    const handler = (e) => {
      e.preventDefault();

      const name = document.getElementById('folder-name').value.trim();
      const summary = document.getElementById('folder-summary').value.trim();
      const guestPublic = document.getElementById('folder-guest-public').checked;
      const userPrivacy = document.getElementById('folder-user-privacy').value;

      if (!name) {
        alert('Repository name is required');
        return;
      }

      if (!userPrivacy) {
        alert('User access level is required');
        return;
      }

      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('name', name);
      if (summary) {
        formData.append('summary', summary);
      }
      formData.append('guestPrivacyType', guestPublic ? 'public' : 'private');
      formData.append('userPrivacyType', userPrivacy);

      fetch(`${this.editor.config.apiBaseUrl}/documentCreateFolder`, {
        method: 'POST',
        credentials: 'same-origin',
        body: formData
      })
        .then((res) => res.json())
        .then(async (data) => {
          if (data.success === false || data.error) {
            alert('Error creating repository: ' + (data.message || 'Unknown error'));
            return;
          }
          // Save group permissions for the new folder
          if (data.folderId) {
            await this.saveGroupPermissionsForFolder(data.folderId);
          }
          modalInstance.close();
          this.loadFolders();
        })
        .catch((err) => {
          console.error('Error creating repository:', err);
          alert('Error creating repository');
        });
    };

    saveBtn.addEventListener('click', handler);

    // Cleanup on modal close
    const closeBtn = modal.querySelector('[data-close]');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        saveBtn.removeEventListener('click', handler);
      });
    }
  }

  async loadGroupsForModal() {
    const tbody = document.getElementById('folder-group-permissions-body');
    if (!tbody) {
      return;
    }
    tbody.innerHTML = '<tr><td colspan="5">Loading groups...</td></tr>';
    try {
      const url = new URL(`${this.editor.config.apiBaseUrl}/documentUserGroups`, globalThis.location.origin);
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const data = await response.json();
      const groups = data.groups || [];
      if (groups.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No user groups found</td></tr>';
        return;
      }
      tbody.innerHTML = '';
      groups.forEach((group) => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${this.escapeHtml(group.name)}</td>
          <td>
            <select class="group-access-level" data-group-id="${group.id}">
              <option value="">-- None --</option>
              <option value="1">Public</option>
              <option value="2">Public Read Only</option>
              <option value="3">Protected</option>
              <option value="4">Private</option>
            </select>
          </td>
          <td><input type="checkbox" class="group-add-perm" data-group-id="${group.id}" /></td>
          <td><input type="checkbox" class="group-edit-perm" data-group-id="${group.id}" /></td>
          <td><input type="checkbox" class="group-delete-perm" data-group-id="${group.id}" /></td>
        `;
        tbody.appendChild(row);
      });
    } catch (err) {
      console.error('Error loading user groups', err);
      tbody.innerHTML = '<tr><td colspan="5">Failed to load groups</td></tr>';
    }
  }

  async saveGroupPermissionsForFolder(folderId) {
    const tbody = document.getElementById('folder-group-permissions-body');
    if (!tbody) return;
    const rows = tbody.querySelectorAll('tr');
    const promises = [];
    rows.forEach((row) => {
      const accessSelect = row.querySelector('.group-access-level');
      if (!accessSelect || !accessSelect.value) return;
      const groupId = accessSelect.dataset.groupId;
      const privacyType = accessSelect.value;
      const addPerm = row.querySelector('.group-add-perm')?.checked || false;
      const editPerm = row.querySelector('.group-edit-perm')?.checked || false;
      const deletePerm = row.querySelector('.group-delete-perm')?.checked || false;
      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('folderId', folderId);
      formData.append('groupId', groupId);
      formData.append('privacyType', privacyType);
      formData.append('addPermission', addPerm);
      formData.append('editPermission', editPerm);
      formData.append('deletePermission', deletePerm);
      promises.push(
        fetch(`${this.editor.config.apiBaseUrl}/folderGroupSave`, {
          method: 'POST',
          credentials: 'same-origin',
          body: formData
        }).catch((err) => console.error('Error saving group permission:', err))
      );
    });
    await Promise.all(promises);
  }

  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  createSubfolder() {
    const modal = document.getElementById('new-subfolder-modal');
    if (!modal) {
      alert('Modal not found');
      return;
    }

    // Reset form
    const form = document.getElementById('new-subfolder-form');
    if (form) {
      form.reset();
    }

    // Show modal using Foundation
    const modalInstance = new Foundation.Reveal($(modal));
    modalInstance.open();

    // Setup form submission
    const saveBtn = document.getElementById('save-subfolder-btn');
    const handler = (e) => {
      e.preventDefault();

      const name = document.getElementById('subfolder-name').value.trim();
      const summary = document.getElementById('subfolder-summary').value.trim();
      const startDate = document.getElementById('subfolder-start-date').value;

      if (!name) {
        alert('Folder name is required');
        return;
      }

      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('folderId', this.parentFolderId);
      formData.append('name', name);
      if (summary) {
        formData.append('summary', summary);
      }
      if (startDate) {
        formData.append('startDate', startDate);
      }

      fetch('/json/documentCreateSubfolder', {
        method: 'POST',
        credentials: 'same-origin',
        body: formData
      })
        .then((res) => res.json())
        .then((data) => {
          if (data.success === false || data.error) {
            alert('Error creating folder: ' + (data.message || 'Unknown error'));
            return;
          }
          modalInstance.close();
          this.loadSubfolders(this.parentFolderId);
        })
        .catch((err) => {
          console.error('Error creating folder:', err);
          alert('Error creating folder');
        });
    };

    saveBtn.addEventListener('click', handler);

    // Cleanup on modal close
    const closeBtn = modal.querySelector('[data-close]');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        saveBtn.removeEventListener('click', handler);
      });
    }
  }
}
