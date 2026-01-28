/**
 * Main controller for the Visual Document Editor
 */

class DocumentEditor {
  constructor(config) {
    this.config = config;
    this.library = new DocumentLibraryManager(this);
    this.fileManager = new DocumentFileManager(this);
    this.folderDetails = new FolderDetailsManager(this);
    this.properties = new DocumentPropertiesManager(this);
    this.loadingIndicator = document.getElementById('loading-indicator');
    this.unsaved = false;
  }

  init() {
    this.applySavedTheme();
    this.library.init();
    this.fileManager.init();
    this.folderDetails.init();
    this.properties.init();
    this.setupEventListeners();

    // Preselect folder/file when provided
    if (this.config.selectedFolderId > 0) {
      this.library.selectFolder(this.config.selectedFolderId);
    }
    if (this.config.selectedFileId > 0) {
      this.fileManager.selectFile(this.config.selectedFileId);
    }
  }

  setupEventListeners() {
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
    if (darkModeToggle) {
      darkModeToggle.addEventListener('click', () => this.toggleDarkMode());
    }

    const reloadBtn = document.getElementById('reload-files-btn');
    if (reloadBtn) {
      reloadBtn.addEventListener('click', () => this.fileManager.reload());
    }

    const newUrlBtn = document.getElementById('new-url-btn');
    if (newUrlBtn) {
      newUrlBtn.addEventListener('click', () => this.handleNewUrl());
    }

    const importBtn = document.getElementById('import-doc-btn');
    if (importBtn) {
      importBtn.addEventListener('click', () => this.handleImport());
    }

    const saveVersionBtn = document.getElementById('save-version-btn');
    const newFolderBtn = document.getElementById('new-folder-btn');
    const newSubfolderBtn = document.getElementById('new-subfolder-btn');

    if (newFolderBtn) {
      newFolderBtn.addEventListener('click', () => this.handleNewFolder());
    }

    if (newSubfolderBtn) {
      newSubfolderBtn.addEventListener('click', () => this.handleNewSubfolder());
    }

    if (saveVersionBtn) {
      saveVersionBtn.addEventListener('click', () => this.fileManager.saveVersion());
    }

    // Tab switching for folder properties
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => this.switchTab(tab.dataset.tab));
    });

    // Unsaved changes guard
    window.addEventListener('beforeunload', (e) => {
      if (this.unsaved) {
        e.preventDefault();
        e.returnValue = '';
      }
    });

    document.querySelectorAll('.confirm-exit').forEach((link) => {
      link.addEventListener('click', (e) => {
        if (!this.unsaved) {
          return;
        }
        e.preventDefault();
        this.showUnsavedWarning(() => {
          globalThis.location.href = link.href;
        });
      });
    });
  }

  applySavedTheme() {
    const savedTheme = localStorage.getItem('editor-theme');
    if (savedTheme === 'dark') {
      document.documentElement.dataset.theme = 'dark';
      const icon = document.querySelector('#dark-mode-toggle-menu i');
      if (icon) {
        icon.classList.replace('fa-moon', 'fa-sun');
      }
    }
  }

  toggleDarkMode() {
    const html = document.documentElement;
    const icon = document.querySelector('#dark-mode-toggle-menu i');
    const isDark = html.dataset.theme === 'dark';

    if (isDark) {
      delete html.dataset.theme;
      localStorage.setItem('editor-theme', 'light');
      if (icon) {
        icon.classList.replace('fa-sun', 'fa-moon');
      }
    } else {
      html.dataset.theme = 'dark';
      localStorage.setItem('editor-theme', 'dark');
      if (icon) {
        icon.classList.replace('fa-moon', 'fa-sun');
      }
    }
  }

  setUnsavedChanges(hasChanges) {
    this.unsaved = hasChanges;
    const indicator = document.getElementById('unsaved-indicator');
    if (!indicator) {
      return;
    }
    if (hasChanges) {
      indicator.classList.add('active');
    } else {
      indicator.classList.remove('active');
    }
  }

  showUnsavedWarning(onProceed) {
    const modal = document.getElementById('unsaved-changes-modal');
    if (!modal) {
      if (confirm('You have unsaved changes. Continue without saving?')) {
        onProceed();
      }
      return;
    }
    const saveBtn = modal.querySelector('#save-and-continue-btn');
    const discardBtn = modal.querySelector('#discard-changes-btn');

    const cleanup = () => {
      saveBtn?.removeEventListener('click', saveHandler);
      discardBtn?.removeEventListener('click', discardHandler);
    };

    const saveHandler = () => {
      cleanup();
      modal.classList.remove('is-open');
      this.fileManager.saveVersion().then(() => {
        this.setUnsavedChanges(false);
        onProceed();
      });
    };
    const discardHandler = () => {
      cleanup();
      modal.classList.remove('is-open');
      this.setUnsavedChanges(false);
      onProceed();
    };

    saveBtn?.addEventListener('click', saveHandler);
    discardBtn?.addEventListener('click', discardHandler);
    modal.classList.add('is-open');
  }

  handleNewUrl() {
    alert('New URL placeholder — hook into FileItem link creation.');
  }

  handleImport() {
    this.fileManager.triggerFileUpload();
  }

  handleNewFolder() {
    this.library.createFolder();
  }

  handleNewSubfolder() {
    this.library.createSubfolder();
  }

  switchTab(tabName) {
    // Update tab buttons
    const tabs = document.querySelectorAll('.tab-btn');
    tabs.forEach((tab) => {
      if (tab.dataset.tab === tabName) {
        tab.classList.add('active');
      } else {
        tab.classList.remove('active');
      }
    });

    // Update content visibility
    const detailsTab = document.getElementById('folder-details-tab');
    const permissionsTab = document.getElementById('folder-permissions-tab');
    const versionsTab = document.getElementById('file-versions-tab');
    
    // Hide all tabs
    if (detailsTab) detailsTab.style.display = 'none';
    if (permissionsTab) permissionsTab.style.display = 'none';
    if (versionsTab) versionsTab.style.display = 'none';

    // Show selected tab
    if (tabName === 'details' && detailsTab) {
      detailsTab.style.display = 'block';
    } else if (tabName === 'permissions' && permissionsTab) {
      permissionsTab.style.display = 'block';
    } else if (tabName === 'versions' && versionsTab) {
      versionsTab.style.display = 'block';
    }
  }

  showRepositoryProperties(folder) {
    // Hide all sections - repositories use folder-details-tab instead
    document.getElementById('repository-properties-section').style.display = 'none';
    document.getElementById('subfolder-properties-section').style.display = 'none';
    document.getElementById('file-properties-section').style.display = 'none';

    // Hide old document-properties-content
    const contentArea = document.getElementById('document-properties-content');
    if (contentArea) {
      contentArea.style.display = 'none';
    }

    // Show tabs for folder (Details and Permissions)
    const tabsNav = document.getElementById('properties-tabs');
    if (tabsNav) {
      tabsNav.style.display = 'flex';
    }

    // Show folder details tab (will be populated by FolderDetailsManager)
    const folderDetailsTab = document.getElementById('folder-details-tab');
    if (folderDetailsTab) {
      folderDetailsTab.style.display = 'block';
    }

    // Hide folder permissions tab initially (will be shown when user clicks Permissions tab)
    const folderPermissionsTab = document.getElementById('folder-permissions-tab');
    if (folderPermissionsTab) {
      folderPermissionsTab.style.display = 'none';
    }

    document.getElementById('properties-panel-title').textContent = 'Repository: ' + (folder.name || 'Untitled');
  }

  showSubfolderProperties(subfolder) {
    // Hide all sections
    document.getElementById('repository-properties-section').style.display = 'none';
    document.getElementById('subfolder-properties-section').style.display = 'block';
    document.getElementById('file-properties-section').style.display = 'none';

    // Hide old document-properties-content
    const contentArea = document.getElementById('document-properties-content');
    if (contentArea) {
      contentArea.style.display = 'none';
    }

    // Hide tabs (subfolders have no permissions tab)
    const tabsNav = document.getElementById('properties-tabs');
    if (tabsNav) {
      tabsNav.style.display = 'none';
    }

    // Hide folder-specific tabs
    const folderDetailsTab = document.getElementById('folder-details-tab');
    if (folderDetailsTab) {
      folderDetailsTab.style.display = 'none';
    }
    const folderPermissionsTab = document.getElementById('folder-permissions-tab');
    if (folderPermissionsTab) {
      folderPermissionsTab.style.display = 'none';
    }

    // Populate subfolder properties
    document.getElementById('subfolder-name-display').value = subfolder.name || '';
    document.getElementById('subfolder-summary-display').value = subfolder.summary || '';
    document.getElementById('subfolder-start-date-display').value = subfolder.startDate || '';

    document.getElementById('properties-panel-title').textContent = 'Folder: ' + (subfolder.name || 'Untitled');
  }

  showFileProperties(file) {
    // Hide all sections
    document.getElementById('repository-properties-section').style.display = 'none';
    document.getElementById('subfolder-properties-section').style.display = 'none';
    document.getElementById('file-properties-section').style.display = 'block';

    // Don't show properties-tabs here - render() will create its own tabs to avoid flickering
    // const tabsNav = document.getElementById('properties-tabs');
    // if (tabsNav) {
    //   tabsNav.style.display = 'flex';
    // }

    // Hide folder-specific tabs
    const folderDetailsTab = document.getElementById('folder-details-tab');
    if (folderDetailsTab) {
      folderDetailsTab.style.display = 'none';
    }
    const folderPermissionsTab = document.getElementById('folder-permissions-tab');
    if (folderPermissionsTab) {
      folderPermissionsTab.style.display = 'none';
    }

    // Show the old document-properties-content for file display
    const contentArea = document.getElementById('document-properties-content');
    if (contentArea) {
      contentArea.style.display = 'block';
    }

    document.getElementById('properties-panel-title').textContent = 'File: ' + (file.title || file.filename || 'Untitled');
  }

  get files() {
    return this.fileManager;
  }

  showLoading() {
    if (this.loadingIndicator) {
      this.loadingIndicator.style.display = 'inline-block';
    }
  }

  hideLoading() {
    if (this.loadingIndicator) {
      this.loadingIndicator.style.display = 'none';
    }
  }
}
