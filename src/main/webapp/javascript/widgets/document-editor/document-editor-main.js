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
    const darkModeToggle = document.getElementById('dark-mode-toggle');
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

    if (newFolderBtn) {
      newFolderBtn.addEventListener('click', () => this.handleNewFolder());
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
      const icon = document.querySelector('#dark-mode-toggle i');
      if (icon) {
        icon.classList.replace('fa-moon', 'fa-sun');
      }
    }
  }

  toggleDarkMode() {
    const html = document.documentElement;
    const icon = document.querySelector('#dark-mode-toggle i');
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
    const contentArea = document.getElementById('document-properties-content');

    if (tabName === 'details') {
      if (detailsTab) detailsTab.style.display = 'flex';
      if (permissionsTab) permissionsTab.style.display = 'none';
      if (contentArea) contentArea.style.display = 'none';
    } else if (tabName === 'permissions') {
      if (detailsTab) detailsTab.style.display = 'none';
      if (permissionsTab) permissionsTab.style.display = 'flex';
      if (contentArea) contentArea.style.display = 'none';
    }
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
