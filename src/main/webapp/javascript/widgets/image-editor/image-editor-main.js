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
    this.token = config.token;
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
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
    if (darkModeToggle) {
      darkModeToggle.addEventListener('click', () => this.toggleDarkMode());
    }

    // Toolbar buttons
    const saveImageBtn = document.getElementById('save-image-btn');
    if (saveImageBtn) {
      saveImageBtn.addEventListener('click', () => this.saveImageVersion());
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
            globalThis.location.href = link.href;
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
    const isDark = html.dataset.theme === 'dark';
    
    if (isDark) {
      delete html.dataset.theme;
      localStorage.setItem('editor-theme', 'light');
      if (icon) icon.classList.replace('fa-sun', 'fa-moon');
    } else {
      html.dataset.theme = 'dark';
      localStorage.setItem('editor-theme', 'dark');
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
    const files = event.target.files;
    if (!files || files.length === 0) return;

    const fileInput = event.target;
    
    // Check if this is a version upload
    const isVersionUpload = fileInput.dataset.uploadType === 'version';
    const imageId = fileInput.dataset.imageId;
    
    // Clear the dataset flags
    delete fileInput.dataset.uploadType;
    delete fileInput.dataset.imageId;

    // Validate file types
    for (const file of files) {
      if (!file.type.startsWith('image/')) {
        alert('Please select only valid image files.');
        return;
      }
    }

    // Open the upload modal
    const modal = new Foundation.Reveal($('#upload-modal'));
    modal.open();

    const progressContainer = document.getElementById('upload-progress');
    const successContainer = document.getElementById('upload-success');
    const errorContainer = document.getElementById('upload-error');
    const progressBar = document.getElementById('upload-progress-bar');
    const statusText = document.getElementById('upload-status');

    progressContainer.style.display = 'block';
    successContainer.style.display = 'none';
    errorContainer.style.display = 'none';

    console.log('Importing files:', Array.from(files).map(f => f.name).join(', '));
    
    try {
      // Upload the files
      const formData = new FormData();
      formData.append('token', this.token);
      
      // If this is a version upload, add the imageId
      if (isVersionUpload && imageId) {
        formData.append('imageId', imageId);
      }
      
      for (const file of files) {
        formData.append('file', file);
      }

      statusText.textContent = isVersionUpload ? 'Uploading new version...' : `Uploading ${files.length} image(s)...`;
      progressBar.style.width = '0%';

      const response = await fetch(`${this.config.contextPath}/json/imageUpload`, {
        method: 'POST',
        credentials: 'same-origin',
        body: formData
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.success === false || result.error) {
        throw new Error(result.error || 'Upload failed');
      }

      progressBar.style.width = '100%';
      progressContainer.style.display = 'none';
      successContainer.style.display = 'block';

      // For version uploads, reload the image and versions list
      if (isVersionUpload && imageId) {
        await this.imageProperties.loadImage(imageId);
        await this.imageProperties.loadVersions();
        
        setTimeout(() => {
          modal.close();
          this.showToast('New version uploaded successfully!', 'success');
        }, 2000);
      } else {
        // For new image uploads, reload library and select the first uploaded image
        await this.imageLibrary.loadImages();
        if (result.images && result.images.length > 0 && result.images[0].id) {
          this.imageLibrary.selectImage(result.images[0].id);
        }

        setTimeout(() => {
          modal.close();
          this.showToast(`${files.length} image(s) imported successfully!`, 'success');
        }, 2000);
      }

    } catch (error) {
      console.error('Error importing files:', error);
      progressContainer.style.display = 'none';
      errorContainer.style.display = 'block';
      document.getElementById('upload-error-message').textContent = error.message || 'Upload failed';
    } finally {
      event.target.value = '';
    }
  }

  /**
   * Save image as a new version (from viewer modifications)
   */
  async saveImageVersion() {
    console.log('Opening save options modal...');
    
    // Show the save options modal
    const modal = new Foundation.Reveal($('#save-options-modal'));
    modal.open();
    
    // Setup button handlers
    const saveAsVersionBtn = document.getElementById('save-as-version-btn');
    const saveAsCopyBtn = document.getElementById('save-as-copy-btn');
    
    // Remove any existing listeners
    const newSaveAsVersionBtn = saveAsVersionBtn.cloneNode(true);
    const newSaveAsCopyBtn = saveAsCopyBtn.cloneNode(true);
    saveAsVersionBtn.replaceWith(newSaveAsVersionBtn);
    saveAsCopyBtn.replaceWith(newSaveAsCopyBtn);
    
    // Save as version handler
    document.getElementById('save-as-version-btn').addEventListener('click', async () => {
      modal.close();
      await this.performSave(true); // true = save as version
    });
    
    // Save as copy handler
    document.getElementById('save-as-copy-btn').addEventListener('click', async () => {
      modal.close();
      await this.performSave(false); // false = save as new copy
    });
  }

  /**
   * Perform the actual save operation
   * @param {boolean} asVersion - true to save as version, false to save as new copy
   */
  async performSave(asVersion) {
    const saveBtn = document.getElementById('save-image-btn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';
    
    try {
      // Show loading state
      if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Saving...';
      }
      this.showLoading();

      // When saving, hide any active crop boxes or overlays
      this.imageViewer.clearSelection();

      // Get the current image data from viewer (if modified)
      const imageBlob = await this.imageViewer.getImageBlob();
      
      if (!imageBlob) {
        throw new Error('No image data to save');
      }

      // Save the image
      const result = await this.saveImageFile(imageBlob, asVersion);

      // Success
      this.clearModified();
      if (asVersion) {
        this.showToast('Image version saved successfully!', 'success');
        // Reload the image to show the new version
        const currentImageId = this.imageProperties.getCurrentImage()?.id;
        if (currentImageId) {
          await this.imageProperties.loadImage(currentImageId);
          await this.imageProperties.loadVersions();
        }
      } else {
        this.showToast('Image copy created successfully!', 'success');
        // Select the new image
        if (result.imageId) {
          this.imageLibrary.selectImage(result.imageId);
        }
      }

      // Reload the image library to show updated thumbnail
      this.imageLibrary.loadImages();

    } catch (error) {
      console.error('Error saving image:', error);
      alert('Failed to save image: ' + error.message);
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
    // Placeholder: integrate with a stock photo API (e.g., Unsplash, Pexels)
    alert('Stock photo integration coming soon! This will allow you to search and import photos from stock photo services.');
  }

  /**
   * Create new image from blob
   */
  async createImageFromBlob(blob, filename) {
    try {
      this.showLoading();

      const formData = new FormData();
      formData.append('token', this.token);
      formData.append('file', blob, filename);

      const response = await fetch(`${this.config.contextPath}/json/imageUpload`, {
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

      this.showToast('Image created successfully!', 'success');

    } catch (error) {
      console.error('Error creating image:', error);
      alert('Failed to create image: ' + error.message);
    } finally {
      this.hideLoading();
    }
  }

  /**
   * Save image file (used by performSave)
   * @param {Blob} blob - The image data
   * @param {boolean} asVersion - true to save as version of current image, false to save as new copy
   */
  async saveImageFile(blob, asVersion = false) {
    const formData = new FormData();
    formData.append('token', this.token);
    
    if (asVersion) {
      // Save as version of current image
      const currentImage = this.imageProperties.getCurrentImage();
      if (!currentImage || !currentImage.id) {
        throw new Error('No image selected');
      }
      formData.append('imageId', currentImage.id);
      formData.append('file', blob, currentImage.filename || 'image.png');
    } else {
      // Save as new copy
      const currentImage = this.imageProperties.getCurrentImage();
      const filename = currentImage?.filename || 'image.png';
      const baseName = filename.replace(/\.[^/.]+$/, ''); // Remove extension
      const extension = filename.split('.').pop();
      formData.append('file', blob, `${baseName}-copy.${extension}`);
    }

    const response = await fetch(`${this.config.contextPath}/json/imageUpload`, {
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
    
    // Return the result with imageId if new image was created
    if (!asVersion && result.images && result.images.length > 0) {
      return { imageId: result.images[0].id };
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
    const saveImageBtn = document.getElementById('save-image-btn');
    if (saveImageBtn) {
      saveImageBtn.disabled = !this.modified;
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
    // Placeholder: wire up to Foundation modal for consistent UX
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

  /**
   * Show a toast notification
   * @param {string} message - The message to display
   * @param {string} type - The type of toast: 'success', 'error', 'warning', 'info'
   */
  showToast(message, type = 'info') {
    // Remove any existing toast
    const existingToast = document.getElementById('image-editor-toast');
    if (existingToast) {
      existingToast.remove();
    }
    
    // Create toast element
    const toast = document.createElement('div');
    toast.id = 'image-editor-toast';
    toast.className = `save-toast save-toast-${type}`;
    
    // Set icon based on type
    let icon = 'fa-info-circle';
    if (type === 'success') {
      icon = 'fa-check-circle';
    } else if (type === 'error') {
      icon = 'fa-exclamation-circle';
    } else if (type === 'warning') {
      icon = 'fa-exclamation-triangle';
    }
    
    toast.innerHTML = `
      <i class="far ${icon}"></i>
      <span class="save-toast-message">${message}</span>
      <button class="save-toast-close" onclick="this.parentElement.remove()">
        <i class="far fa-times"></i>
      </button>
    `;
    
    // Add toast styles if not already present
    this.ensureToastStyles();
    
    // Add to DOM
    document.body.appendChild(toast);
    
    // Trigger animation
    setTimeout(() => {
      toast.classList.add('show');
    }, 10);
    
    // Auto-remove after delay (longer for errors)
    let duration = 3000;
    if (type === 'error') {
      duration = 6000;
    } else if (type === 'warning') {
      duration = 5000;
    }
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => {
        if (toast.parentElement) {
          toast.remove();
        }
      }, 300);
    }, duration);
  }

  /**
   * Ensure toast styles are added to the document
   */
  ensureToastStyles() {
    if (document.getElementById('save-toast-styles')) {
      return;
    }
    
    const styles = document.createElement('style');
    styles.id = 'save-toast-styles';
    styles.textContent = `
      .save-toast {
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 12px 16px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        gap: 10px;
        font-size: 14px;
        font-weight: 500;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 10001;
        transform: translateY(100px);
        opacity: 0;
        transition: transform 0.3s ease, opacity 0.3s ease;
        max-width: 400px;
      }
      
      .save-toast.show {
        transform: translateY(0);
        opacity: 1;
      }
      
      .save-toast-success {
        background: #d4edda;
        color: #155724;
        border: 1px solid #c3e6cb;
      }
      
      .save-toast-error {
        background: #f8d7da;
        color: #721c24;
        border: 1px solid #f5c6cb;
      }
      
      .save-toast-warning {
        background: #fff3cd;
        color: #856404;
        border: 1px solid #ffeeba;
      }
      
      .save-toast-info {
        background: #d1ecf1;
        color: #0c5460;
        border: 1px solid #bee5eb;
      }
      
      .save-toast-message {
        flex: 1;
      }
      
      .save-toast-close {
        background: none;
        border: none;
        cursor: pointer;
        padding: 4px;
        opacity: 0.7;
        color: inherit;
      }
      
      .save-toast-close:hover {
        opacity: 1;
      }
      
      /* Dark mode support */
      [data-theme="dark"] .save-toast-success {
        background: #1e4620;
        color: #a3d9a5;
        border-color: #2d5a2e;
      }
      
      [data-theme="dark"] .save-toast-error {
        background: #4a1c1c;
        color: #f5a5a5;
        border-color: #6b2c2c;
      }
      
      [data-theme="dark"] .save-toast-warning {
        background: #4a3c1c;
        color: #f5d9a5;
        border-color: #6b5a2c;
      }
      
      [data-theme="dark"] .save-toast-info {
        background: #1c3a4a;
        color: #a5d9f5;
        border-color: #2c5a6b;
      }
    `;
    document.head.appendChild(styles);
  }
}
