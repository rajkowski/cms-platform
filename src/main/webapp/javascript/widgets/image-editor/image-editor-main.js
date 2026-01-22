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

    const newFromClipboardBtn = document.getElementById('new-from-clipboard-btn');
    if (newFromClipboardBtn) {
      newFromClipboardBtn.addEventListener('click', () => this.createFromClipboard());
    }

    const newFromStockBtn = document.getElementById('new-from-stock-btn');
    if (newFromStockBtn) {
      newFromStockBtn.addEventListener('click', () => this.createFromStockPhoto());
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
  async handleFileImport(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      alert('Please select a valid image file.');
      return;
    }

    console.log('Importing file:', file.name);
    
    try {
      this.showLoading();

      // Upload the file
      const formData = new FormData();
      formData.append('file', file);

      const response = await fetch(`${this.config.apiBaseUrl}/imageUpload`, {
        method: 'POST',
        credentials: 'same-origin',
        body: formData
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      // Reload library and select the new image
      await this.imageLibrary.loadImages();
      if (result.imageId) {
        this.imageLibrary.selectImage(result.imageId);
      }

      alert('Image imported successfully!');

    } catch (error) {
      console.error('Error importing file:', error);
      alert('Failed to import image: ' + error.message);
    } finally {
      this.hideLoading();
      event.target.value = '';
    }
  }

  /**
   * Create new image from clipboard
   */
  async createFromClipboard() {
    try {
      const items = await navigator.clipboard.read();
      
      for (const item of items) {
        if (item.types.includes('image/png')) {
          const blob = await item.getType('image/png');
          await this.createImageFromBlob(blob, 'clipboard-image.png');
          return;
        }
      }
      
      alert('No image found in clipboard. Copy an image first.');
    } catch (error) {
      console.error('Error reading from clipboard:', error);
      alert('Failed to read from clipboard. Make sure you have copied an image and granted clipboard permissions.');
    }
  }

  /**
   * Create new image from stock photo
   */
  createFromStockPhoto() {
    // TODO: Integrate with stock photo API (e.g., Unsplash, Pexels)
    alert('Stock photo integration coming soon! This will allow you to search and import photos from stock photo services.');
  }

  /**
   * Create new image from blob
   */
  async createImageFromBlob(blob, filename) {
    try {
      this.showLoading();

      const formData = new FormData();
      formData.append('file', blob, filename);

      const response = await fetch(`${this.config.apiBaseUrl}/imageUpload`, {
        method: 'POST',
        credentials: 'same-origin',
        body: formData
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      // Reload library and select the new image
      await this.imageLibrary.loadImages();
      if (result.imageId) {
        this.imageLibrary.selectImage(result.imageId);
      }

      alert('Image created successfully!');

    } catch (error) {
      console.error('Error creating image:', error);
      alert('Failed to create image: ' + error.message);
    } finally {
      this.hideLoading();
    }
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
    
    const saveBtn = document.getElementById('save-btn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';
    
    try {
      // Show loading state
      if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Saving...';
      }
      this.showLoading();

      // Get the current image data from viewer (if modified)
      const imageBlob = await this.imageViewer.getImageBlob();
      const hasImageChanges = this.imageViewer.hasTransformations() || imageBlob;

      // Save image file if there are visual changes
      if (hasImageChanges && imageBlob) {
        await this.saveImageFile(imageBlob);
      }

      // Note: Metadata fields (title, altText, description) would require database schema changes
      // For now, only the visual changes are saved as a new image version

      // Success
      this.clearModified();
      alert('Image changes saved successfully!');

      // Reload the image library to show updated thumbnail
      if (hasImageChanges) {
        this.imageLibrary.loadImages();
      }

    } catch (error) {
      console.error('Error saving changes:', error);
      alert('Failed to save changes: ' + error.message);
    } finally {
      // Restore button state
      if (saveBtn) {
        saveBtn.innerHTML = originalText;
        saveBtn.disabled = false;
      }
      this.hideLoading();
    }
  }

  /**
   * Save image file
   */
  async saveImageFile(blob) {
    const currentImage = this.imageProperties.getCurrentImage();
    if (!currentImage || !currentImage.id) {
      throw new Error('No image selected');
    }

    const formData = new FormData();
    formData.append('imageId', currentImage.id);
    formData.append('file', blob, currentImage.filename || 'image.png');

    const response = await fetch(`${this.config.apiBaseUrl}/imageUpload`, {
      method: 'POST',
      credentials: 'same-origin',
      body: formData
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();
    
    if (result.error) {
      throw new Error(result.error);
    }

    return result;
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
