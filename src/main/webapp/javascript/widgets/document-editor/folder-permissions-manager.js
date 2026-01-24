/**
 * Manages folder group permissions display and editing
 * Handles viewing, adding, editing, and deleting folder group access
 */

class FolderPermissionsManager {
  constructor(folderDetailsManager) {
    this.folderDetailsManager = folderDetailsManager;
    this.editor = folderDetailsManager.editor;
    this.token = folderDetailsManager.editor.config.token;
    this.currentFolder = null;
    this.folderGroups = [];
    this.allGroups = [];
    this.containerEl = document.getElementById('folder-permissions-tab');
    this.isEditing = false;
  }

  init() {
    // Initialize event listeners for permissions tab
    const addGroupBtn = document.getElementById('add-folder-group-btn');
    if (addGroupBtn) {
      addGroupBtn.addEventListener('click', () => this.openAddGroupModal());
    }
  }

  async loadFolderGroups(folderId) {
    if (!folderId || folderId === -1) {
      return;
    }
    try {
      const url = new URL(
        `${this.editor.config.apiBaseUrl}/folderGroupsList`,
        globalThis.location.origin
      );
      url.searchParams.set('folderId', folderId);
      
      const response = await fetch(url.toString(), { credentials: 'same-origin' });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      
      const payload = await response.json();
      this.folderGroups = payload.folderGroups || [];
      this.allGroups = payload.allGroups || [];
      this.currentFolder = payload.folder;
    } catch (err) {
      console.error('Unable to load folder groups', err);
      this.folderGroups = [];
      this.allGroups = [];
    }
  }

  render() {
    if (!this.containerEl) {
      return;
    }

    if (!this.currentFolder) {
      this.containerEl.innerHTML = '<div class="empty-state">Select a folder to manage permissions</div>';
      return;
    }

    let html = '<div class="folder-permissions-content">';
    
    // Privacy summary
    html += '<div class="property-section">';
    html += '<h4>Folder Access</h4>';
    html += '<p class="info-text">Configure which user groups can access and modify files in this folder.</p>';
    html += '</div>';

    // Groups list
    html += '<div class="property-section">';
    html += '<div class="section-header">';
    html += '<h5>Allowed Groups</h5>';
    html += '<button id="add-folder-group-btn" class="button tiny primary no-gap"><i class="fas fa-plus"></i> Add Group</button>';
    html += '</div>';

    if (this.folderGroups && this.folderGroups.length > 0) {
      html += '<table class="folder-groups-table">';
      html += '<thead>';
      html += '<tr>';
      html += '<th>Group Name</th>';
      html += '<th>Privacy Type</th>';
      html += '<th>Permissions</th>';
      html += '<th style="width: 100px;">Actions</th>';
      html += '</tr>';
      html += '</thead>';
      html += '<tbody>';

      this.folderGroups.forEach((fg) => {
        const groupName = this.getGroupName(fg.groupId);
        const privacyLabel = this.getPrivacyTypeLabel(fg.privacyType);
        const perms = this.getPermissionsList(fg);

        html += `<tr data-group-id="${fg.groupId}" data-folder-group-id="${fg.id}">`;
        html += `<td>${groupName || 'Unknown Group'}</td>`;
        html += `<td><span class="badge">${privacyLabel}</span></td>`;
        html += `<td><small>${perms || 'View Only'}</small></td>`;
        html += `<td>`;
        html += `<button class="button tiny secondary edit-group-btn" title="Edit"><i class="fas fa-edit"></i></button>`;
        html += `<button class="button tiny alert delete-group-btn" title="Delete"><i class="fas fa-trash"></i></button>`;
        html += `</td>`;
        html += `</tr>`;
      });

      html += '</tbody>';
      html += '</table>';
    } else {
      html += '<div class="empty-state" style="padding: 20px; text-align: center;">';
      html += '<p>No groups have access to this folder yet.</p>';
      html += '</div>';
    }
    html += '</div>';

    html += '</div>';
    this.containerEl.innerHTML = html;

    // Attach event listeners
    this.attachEventListeners();
  }

  attachEventListeners() {
    const addBtn = document.getElementById('add-folder-group-btn');
    if (addBtn) {
      addBtn.removeEventListener('click', () => this.openAddGroupModal());
      addBtn.addEventListener('click', () => this.openAddGroupModal());
    }

    const editBtns = this.containerEl.querySelectorAll('.edit-group-btn');
    editBtns.forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const row = e.target.closest('tr');
        const folderGroupId = row.dataset.folderGroupId;
        const groupId = row.dataset.groupId;
        this.openEditGroupModal(folderGroupId, groupId);
      });
    });

    const deleteBtns = this.containerEl.querySelectorAll('.delete-group-btn');
    deleteBtns.forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const row = e.target.closest('tr');
        const folderGroupId = row.dataset.folderGroupId;
        this.deleteGroup(folderGroupId);
      });
    });
  }

  openAddGroupModal() {
    this.openGroupModal(null);
  }

  openEditGroupModal(folderGroupId, groupId) {
    const fg = this.folderGroups.find(g => g.id === Number(folderGroupId));
    this.openGroupModal(fg);
  }

  openGroupModal(folderGroup) {
    const modal = new Foundation.Reveal($('#folder-group-modal'));
    const form = document.getElementById('folder-group-form');
    
    if (!form) return;

    this.populateGroupForm(folderGroup);
    
    // Handle save
    const saveBtn = document.getElementById('save-folder-group-btn');
    if (saveBtn) {
      saveBtn.onclick = () => this.saveGroup(folderGroup);
    }

    modal.open();
  }

  populateGroupForm(folderGroup) {
    // Set form title
    const titleEl = document.getElementById('folder-group-modal-title');
    if (titleEl) {
      titleEl.textContent = folderGroup ? 'Edit Group Access' : 'Add Group Access';
    }

    // Reset form
    const form = document.getElementById('folder-group-form');
    if (form) form.reset();

    this.populateGroupSelect(folderGroup);
    this.setPrivacyType(folderGroup);
    this.setPermissionCheckboxes(folderGroup);
  }

  populateGroupSelect(folderGroup) {
    const groupSelect = document.getElementById('folder-group-select');
    if (!groupSelect) return;

    groupSelect.innerHTML = '<option value="">-- Select Group --</option>';
    this.allGroups.forEach((group) => {
      const option = document.createElement('option');
      option.value = group.id;
      option.textContent = group.name;
      groupSelect.appendChild(option);
    });

    if (folderGroup) {
      groupSelect.value = folderGroup.groupId;
      groupSelect.disabled = true;
    } else {
      groupSelect.disabled = false;
    }
  }

  setPrivacyType(folderGroup) {
    const privacySelect = document.getElementById('folder-group-privacy-type');
    if (privacySelect && folderGroup) {
      privacySelect.value = folderGroup.privacyType;
    }
  }

  setPermissionCheckboxes(folderGroup) {
    if (!folderGroup) return;

    const addPerm = document.getElementById('folder-group-add-permission');
    const editPerm = document.getElementById('folder-group-edit-permission');
    const deletePerm = document.getElementById('folder-group-delete-permission');

    if (addPerm) addPerm.checked = folderGroup.addPermission;
    if (editPerm) editPerm.checked = folderGroup.editPermission;
    if (deletePerm) deletePerm.checked = folderGroup.deletePermission;
  }

  async saveGroup(existingGroup) {
    const groupSelect = document.getElementById('folder-group-select');
    const privacySelect = document.getElementById('folder-group-privacy-type');
    const addPerm = document.getElementById('folder-group-add-permission');
    const editPerm = document.getElementById('folder-group-edit-permission');
    const deletePerm = document.getElementById('folder-group-delete-permission');

    const groupId = Number(groupSelect.value);
    if (!groupId) {
      alert('Please select a group');
      return;
    }

    try {
      const payload = {
        token: this.token,
        folderId: this.currentFolder.id,
        groupId: groupId,
        privacyType: Number(privacySelect.value) || 0,
        addPermission: addPerm ? addPerm.checked : false,
        editPermission: editPerm ? editPerm.checked : false,
        deletePermission: deletePerm ? deletePerm.checked : false
      };

      if (existingGroup) {
        payload.id = existingGroup.id;
      }

      const response = await fetch(`${this.editor.config.apiBaseUrl}/folderGroupSave`, {
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
        alert(result.message || 'Failed to save group access');
        return;
      }

      // Close modal and reload
      const modal = new Foundation.Reveal($('#folder-group-modal'));
      modal.close();
      
      // Reload folder groups
      await this.loadFolderGroups(this.currentFolder.id);
      this.render();
      this.folderDetailsManager.setUnsavedChanges(true);

    } catch (err) {
      console.error('Error saving group', err);
      alert('Error saving group access: ' + err.message);
    }
  }

  async deleteGroup(folderGroupId) {
    if (!confirm('Are you sure you want to remove this group\'s access?')) {
      return;
    }

    try {
      const payload = {
        token: this.token,
        id: Number(folderGroupId),
        folderId: this.currentFolder.id
      };

      const response = await fetch(`${this.editor.config.apiBaseUrl}/folderGroupDelete`, {
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
        alert(result.message || 'Failed to delete group access');
        return;
      }

      // Reload folder groups
      await this.loadFolderGroups(this.currentFolder.id);
      this.render();
      this.folderDetailsManager.setUnsavedChanges(true);

    } catch (err) {
      console.error('Error deleting group', err);
      alert('Error deleting group access: ' + err.message);
    }
  }

  getGroupName(groupId) {
    const group = this.allGroups.find(g => g.id === groupId);
    return group ? group.name : null;
  }

  getPrivacyTypeLabel(type) {
    const types = {
      0: 'Undefined',
      1: 'Public',
      2: 'Public Read Only',
      3: 'Protected',
      4: 'Private'
    };
    return types[type] || 'Unknown';
  }

  getPermissionsList(fg) {
    const perms = [];
    if (fg.addPermission) perms.push('Add');
    if (fg.editPermission) perms.push('Edit');
    if (fg.deletePermission) perms.push('Delete');
    return perms.length > 0 ? perms.join(', ') : 'View Only';
  }

  setFolderAndLoad(folder) {
    this.currentFolder = folder;
    this.loadFolderGroups(folder.id).then(() => {
      this.render();
    });
  }
}
