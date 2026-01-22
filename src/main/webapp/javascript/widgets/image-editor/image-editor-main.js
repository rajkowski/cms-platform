/**
 * Main Image Editor Controller
 * Coordinates all editor modules and manages the overall editing workflow
 * 
 * @author matt rajkowski
 * @created 1/21/26 9:40 PM
 */

class ImageEditor {
  constructor(config) {
    this.config = config;
    this.imageLibrary = new ImageLibraryManager(this);
    this.imageViewer = new ImageViewerManager(this);
    this.imageProperties = new ImagePropertiesManager(this);
    this.modified = false;
  }

  /**
   * Initialize the editor
   */
  init() {
    console.log('Initializing Visual Image Editor...');
    
    // Initialize sub-modules
    this.imageLibrary.init();
    this.imageViewer.init();
    this.imageProperties.init();
    
    // Set up event listeners
    this.setupEventListeners();
    
    // If a specific image was requested, select it
    if (this.config.selectedImageId > 0) {
      this.imageLibrary.selectImage(this.config.selectedImageId);
    }
  }

  /**
   * Setup global event listeners
   */
  setupEventListeners() {
    // Dark mode toggle
    const darkModeToggle = document.getElementById('dark-mode-toggle');
    if (darkModeToggle) {
      darkModeToggle.addEventListener('click', () => this.toggleDarkMode());
    }

    // Toolbar buttons
    const saveBtn = document.getElementById('save-btn');
    if (saveBtn) {
      saveBtn.addEventListener('click', () => this.saveChanges());
    }

    const reloadBtn = document.getElementById('reload-btn');
    if (reloadBtn) {
      reloadBtn.addEventListener('click', () => this.reloadCurrentImage());
    }

    const importBtn = document.getElementById('import-btn');
    if (importBtn) {
      importBtn.addEventListener('click', () => this.triggerFileImport());
    }

    const fileInput = document.getElementById('image-file-input');
    if (fileInput) {
      fileInput.addEventListener('change', (e) => this.handleFileImport(e));
    }

    // Warn on exit if unsaved changes
    window.addEventListener('beforeunload', (e) => {
      if (this.hasUnsavedChanges()) {
        e.preventDefault();
        e.returnValue = '';
      }
    });

    // Confirm exit links
    document.querySelectorAll('.confirm-exit').forEach(link => {
      link.addEventListener('click', (e) => {
        if (this.hasUnsavedChanges()) {
          e.preventDefault();
          this.showUnsavedChangesModal(() => {
            window.location.href = link.href;
          });
        }
      });
    });
  }

  /**
   * Toggle dark mode
   */
  toggleDarkMode() {
    const html = document.documentElement;
    const icon = document.querySelector('#dark-mode-toggle i');
    const isDark = html.getAttribute('data-theme') === 'dark';
    
    if (isDark) {
      html.removeAttribute('data-theme');
      localStorage.setItem('image-editor-theme', 'light');
      if (icon) icon.classList.replace('fa-sun', 'fa-moon');
    } else {
      html.setAttribute('data-theme', 'dark');
      localStorage.setItem('image-editor-theme', 'dark');
      if (icon) icon.classList.replace('fa-moon', 'fa-sun');
    }
  }

  /**
   * Trigger file import
   */
  triggerFileImport() {
    const fileInput = document.getElementById('image-file-input');
    if (fileInput) {
      fileInput.click();
    }
  }

  /**
   * Handle file import
   */
  handleFileImport(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      alert('Please select a valid image file.');
      return;
    }

    console.log('Importing file:', file.name);
    // TODO: Implement file upload logic
    alert('File upload feature coming soon!');
    
    // Reset file input
    event.target.value = '';
  }

  /**
   * Save changes
   */
  async saveChanges() {
    if (!this.modified) {
      console.log('No changes to save');
      return;
    }

    console.log('Saving changes...');
    
    // Get form data from properties panel
    const formData = this.imageProperties.getFormData();
    
    // TODO: Implement save logic
    alert('Save functionality coming soon!');
    
    // After successful save:
    // this.clearModified();
  }

  /**
   * Reload current image
   */
  reloadCurrentImage() {
    const selectedImageId = this.imageLibrary.getSelectedImageId();
    if (selectedImageId) {
      if (this.hasUnsavedChanges()) {
        if (confirm('You have unsaved changes. Discard them and reload?')) {
          this.imageViewer.loadImage(selectedImageId);
          this.imageProperties.loadImage(selectedImageId);
          this.clearModified();
        }
      } else {
        this.imageViewer.loadImage(selectedImageId);
        this.imageProperties.loadImage(selectedImageId);
      }
    }
  }

  /**
   * Mark editor as having unsaved changes
   */
  markAsModified() {
    if (!this.modified) {
      this.modified = true;
      this.updateSaveButton();
    }
  }

  /**
   * Clear modification state
   */
  clearModified() {
    this.modified = false;
    this.updateSaveButton();
  }

  /**
   * Update save button state
   */
  updateSaveButton() {
    const saveBtn = document.getElementById('save-btn');
    if (saveBtn) {
      saveBtn.disabled = !this.modified;
    }
  }

  /**
   * Check if there are unsaved changes
   */
  hasUnsavedChanges() {
    return this.modified;
  }

  /**
   * Show unsaved changes modal
   */
  showUnsavedChangesModal(onContinue) {
    // TODO: Use Foundation modal
    const shouldContinue = confirm('You have unsaved changes. Do you want to discard them?');
    if (shouldContinue && onContinue) {
      onContinue();
    }
  }

  /**
   * Show loading indicator
   */
  showLoading() {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) {
      indicator.style.display = 'block';
    }
  }

  /**
   * Hide loading indicator
   */
  hideLoading() {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) {
      indicator.style.display = 'none';
    }
  }
}
