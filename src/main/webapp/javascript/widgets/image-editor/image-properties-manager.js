/**
 * Image Properties Manager for Visual Image Editor
 * Handles displaying and editing image metadata in the right pane
 * 
 * @author matt rajkowski
 * @created 1/21/26 9:55 PM
 */

class ImagePropertiesManager {
  constructor(editor) {
    this.editor = editor;
    this.currentImage = null;
  }

  /**
   * Initialize the properties panel
   */
  init() {
    console.log('Initializing Image Properties Manager...');
    this.setupEventListeners();
  }

  /**
   * Setup event listeners
   */
  setupEventListeners() {
    // Detect changes to form fields
    const form = document.getElementById('image-metadata-form');
    if (form) {
      form.addEventListener('input', () => {
        this.onMetadataChanged();
      });
    }

    // Save metadata button
    const saveMetadataBtn = document.getElementById('save-metadata-btn');
    if (saveMetadataBtn) {
      saveMetadataBtn.addEventListener('click', () => {
        this.saveMetadata();
      });
    }

    // Copy URL button
    const copyUrlBtn = document.getElementById('copy-url-btn');
    if (copyUrlBtn) {
      copyUrlBtn.addEventListener('click', () => {
        this.copyUrlToClipboard();
      });
    }

    // Delete image button
    const deleteBtn = document.getElementById('delete-image-btn');
    if (deleteBtn) {
      deleteBtn.addEventListener('click', () => {
        this.deleteImage();
      });
    }

    // Upload new version button (in Versions tab)
    const uploadVersionBtn = document.getElementById('upload-new-version-btn');
    if (uploadVersionBtn) {
      uploadVersionBtn.addEventListener('click', () => {
        this.uploadNewVersion();
      });
    }
  }

  /**
   * Load and display image properties
   */
  async loadImage(imageId) {
    console.log('Loading image properties:', imageId);

    try {
      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageContent?imageId=${imageId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const imageData = await response.json();
      
      if (imageData.error) {
        throw new Error(imageData.error);
      }

      this.currentImage = imageData;
      this.displayProperties(imageData);
      
      // Load versions and update tab badge
      await this.loadVersions();

    } catch (error) {
      console.error('Error loading image properties:', error);
      this.showErrorState('Failed to load image properties.');
    }
  }

  /**
   * Display image properties in the form
   */
  displayProperties(imageData) {
    // Hide no-image message, show form
    const noImageDiv = document.querySelector('#properties-tab-content .no-image-selected');
    const form = document.getElementById('image-metadata-form');
    
    if (noImageDiv) noImageDiv.classList.add('hidden');
    if (form) form.classList.remove('hidden');

    // Populate form fields
    this.setFieldValue('image-title', imageData.title || imageData.filename || '');
    this.setFieldValue('image-filename', imageData.filename || '');
    this.setFieldValue('image-alt-text', imageData.altText || '');
    this.setFieldValue('image-description', imageData.description || '');

    // Display read-only URL with link
    const urlLink = document.getElementById('image-url');
    if (urlLink && imageData.url) {
      const fullUrl = imageData.url.startsWith('http') ? imageData.url : globalThis.location.origin + imageData.url;
      urlLink.href = fullUrl;
      urlLink.textContent = imageData.url;
      urlLink.style.display = 'inline';
    }

    // Display read-only metadata
    this.setTextContent('image-dimensions', 
      imageData.width && imageData.height ? `${imageData.width} × ${imageData.height} px` : '-');
    
    this.setTextContent('image-file-size', this.formatFileSize(imageData.fileLength));
    this.setTextContent('image-file-type', imageData.fileType || '-');
    this.setTextContent('image-created', this.formatDate(imageData.created));
    this.setTextContent('image-modified', this.formatDate(imageData.modified) || '-');

      // Display thumbnail information if available
      const thumbnailGroup = document.getElementById('thumbnail-info-group');
      const thumbnailInfo = document.getElementById('image-thumbnail-info');
      if (imageData.hasThumbnail && imageData.thumbnailWidth && imageData.thumbnailHeight) {
        if (thumbnailGroup) thumbnailGroup.style.display = 'block';
        if (thumbnailInfo) {
          const thumbSize = this.formatFileSize(imageData.thumbnailFileLength);
          thumbnailInfo.textContent = `${imageData.thumbnailWidth} × ${imageData.thumbnailHeight} px (${thumbSize})`;
        }
      } else {
        if (thumbnailGroup) thumbnailGroup.style.display = 'none';
      }

    // Show versions tab content
    const versionsNoImage = document.querySelector('#versions-tab-content .no-image-selected');
    const versionsContainer = document.getElementById('versions-list-container');
    if (versionsNoImage) versionsNoImage.classList.add('hidden');
    if (versionsContainer) versionsContainer.classList.remove('hidden');

    // Disable save button initially
    const saveBtn = document.getElementById('save-metadata-btn');
    if (saveBtn) saveBtn.disabled = true;
  }

  /**
   * Clear properties panel
   */
  clear() {
    this.currentImage = null;

    const noImageDiv = document.querySelector('#properties-tab-content .no-image-selected');
    const form = document.getElementById('image-metadata-form');
    
    if (noImageDiv) noImageDiv.classList.remove('hidden');
    if (form) form.classList.add('hidden');

    const versionsNoImage = document.querySelector('#versions-tab-content .no-image-selected');
    const versionsContainer = document.getElementById('versions-list-container');
    if (versionsNoImage) versionsNoImage.classList.remove('hidden');
    if (versionsContainer) versionsContainer.classList.add('hidden');
  }

  /**
   * Show error state
   */
  showErrorState(message) {
    const noImageDiv = document.querySelector('#properties-content .no-image-selected');
    if (noImageDiv) {
      noImageDiv.innerHTML = `<p style="color: var(--alert-color);">${message}</p>`;
      noImageDiv.classList.remove('hidden');
    }

    const form = document.getElementById('image-metadata-form');
    if (form) form.classList.add('hidden');
  }

  /**
   * Get form data for saving
   */
  getFormData() {
    return {
      id: this.currentImage ? this.currentImage.id : null,
      title: this.getFieldValue('image-title'),
      filename: this.getFieldValue('image-filename'),
      altText: this.getFieldValue('image-alt-text'),
      description: this.getFieldValue('image-description')
    };
  }

  /**
   * Set a form field value
   */
  setFieldValue(fieldId, value) {
    const field = document.getElementById(fieldId);
    if (field) {
      field.value = value || '';
    }
  }

  /**
   * Get a form field value
   */
  getFieldValue(fieldId) {
    const field = document.getElementById(fieldId);
    return field ? field.value : '';
  }

  /**
   * Set text content of an element
   */
  setTextContent(elementId, text) {
    const element = document.getElementById(elementId);
    if (element) {
      element.textContent = text;
    }
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '-';
    
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let unitIndex = 0;

    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }

    return `${size.toFixed(1)} ${units[unitIndex]}`;
  }

  /**
   * Format date for display
   */
  formatDate(dateString) {
    if (!dateString) return null;
    
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    } catch (error) {
      console.warn('Error formatting date:', error);
      return dateString;
    }
  }

  /**
   * Copy image URL to clipboard
   */
  async copyUrlToClipboard() {
    const urlLink = document.getElementById('image-url');
    if (!urlLink || !urlLink.href) {
      return;
    }

    try {
      await navigator.clipboard.writeText(urlLink.href);
      this.showToast('URL copied to clipboard!', 'success');
    } catch (error) {
      console.error('Error copying URL to clipboard:', error);
      this.showToast('Failed to copy URL', 'error');
    }
  }

  /**
   * Handle metadata field changes
   */
  onMetadataChanged() {
    const saveBtn = document.getElementById('save-metadata-btn');
    if (saveBtn) {
      saveBtn.disabled = false;
    }
  }

  /**
   * Save metadata (title, alt text, description)
   */
  async saveMetadata() {
    if (!this.currentImage || !this.currentImage.id) {
      alert('No image selected');
      return;
    }

    const saveBtn = document.getElementById('save-metadata-btn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';
    
    try {
      if (saveBtn) {
        saveBtn.disabled = true;
        saveBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Saving...';
      }

      const formData = this.getFormData();
      
      const params = new URLSearchParams();
      params.append('token', this.editor.token);
      params.append('id', formData.id);
      params.append('title', formData.title || '');
      params.append('filename', formData.filename || '');
      params.append('altText', formData.altText || '');
      params.append('description', formData.description || '');
      
      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageMetadata`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'same-origin',
        body: params.toString()
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      this.showToast('Properties saved successfully!', 'success');
      
      // Keep the save button disabled until next change
      if (saveBtn) {
        saveBtn.disabled = true;
      }

    } catch (error) {
      console.error('Error saving metadata:', error);
      alert('Failed to save properties: ' + error.message);
    } finally {
      if (saveBtn) {
        saveBtn.innerHTML = originalText;
      }
    }
  }

  /**
   * Delete the current image
   */
  async deleteImage() {
    if (!this.currentImage || !this.currentImage.id) {
      alert('No image selected');
      return;
    }

    const confirmed = confirm(`Are you sure you want to delete "${this.currentImage.filename}"? This action cannot be undone.`);
    if (!confirmed) return;

    try {
      this.editor.showLoading();

      const params = new URLSearchParams();
      params.append('token', this.editor.token);
      params.append('id', this.currentImage.id);
      
      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageDelete`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'same-origin',
        body: params.toString()
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      this.showToast('Image deleted successfully!', 'success');
      
      // Remove from library and clear viewer/properties
      this.editor.imageLibrary.removeImage(this.currentImage.id);
      this.clear();

    } catch (error) {
      console.error('Error deleting image:', error);
      alert('Failed to delete image: ' + error.message);
    } finally {
      this.editor.hideLoading();
    }
  }

  /**
   * Load versions for the current image
   */
  async loadVersions() {
    if (!this.currentImage || !this.currentImage.id) {
      return;
    }

    console.log('Loading versions for image:', this.currentImage.id);

    const versionsList = document.getElementById('versions-list');
    if (!versionsList) return;

    versionsList.innerHTML = `
      <div class="loading-message">
        <i class="far fa-spinner fa-spin"></i>
        <p>Loading versions...</p>
      </div>
    `;

    try {
      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageVersions?imageId=${this.currentImage.id}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();
      
      if (data.error) {
        throw new Error(data.error);
      }

      this.displayVersions(data.versions || []);
      
      // Update Versions tab with count badge
      const versionsTab = document.querySelector('[data-tab="versions"]');
      if (versionsTab && data.versions && data.versions.length > 0) {
        const versionCount = data.versions.length;
        const tabText = versionsTab.innerHTML.replace(/\s*<span class="badge">.*?<\/span>/, ''); // Remove existing badge
        versionsTab.innerHTML = `${tabText} <span class="badge">${versionCount}</span>`;
      }

    } catch (error) {
      console.error('Error loading versions:', error);
      versionsList.innerHTML = `
        <div class="no-versions-message">
          <p style="color: var(--alert-color);">Failed to load versions.</p>
        </div>
      `;
    }
  }

  /**
   * Display version history with thumbnails
   */
  displayVersions(versions) {
    const versionsList = document.getElementById('versions-list');
    if (!versionsList) return;

    if (versions.length === 0) {
      versionsList.innerHTML = `
        <div class="no-versions-message">
          <p>No version history available.</p>
        </div>
      `;
      return;
    }

    let html = '<div class="versions-list-items">';
    
    versions.forEach((version, index) => {
      const isCurrent = !!version.isCurrent;
      const thumbnailUrl = `${this.editor.config.contextPath}${version.url || this.currentImage.url}`;
      
      html += `
        <div class="version-item ${isCurrent ? 'current' : ''}">
          <div class="version-thumbnail">
            <img src="${thumbnailUrl}" alt="Version ${version.versionNumber}" 
                 style="max-width: 60px; max-height: 60px; border-radius: 4px; border: 1px solid #e0e0e0;" />
          </div>
          <div class="version-content">
            <div class="version-header">
              <span class="version-number">Version ${version.versionNumber || versions.length - index}</span>
              ${isCurrent ? '<span class="badge success">Current</span>' : ''}
            </div>
            <div class="version-details">
              <div class="version-info">
                <small>${this.formatDate(version.created)}</small>
                <small>${this.formatFileSize(version.fileLength)}</small>
                ${version.width && version.height ? `<small>${version.width}×${version.height}</small>` : ''}
              </div>
              <div class="version-actions">
                ${!isCurrent ? `
                  <button class="button tiny secondary no-gap" onclick="imageEditor.imageProperties.revertToVersion(${version.id})" title="Make this version current">
                    <i class="far fa-undo"></i> Make Current
                  </button>
                  <button class="button tiny alert no-gap" onclick="imageEditor.imageProperties.deleteVersion(${version.id})" title="Delete this version">
                    <i class="far fa-trash"></i>
                  </button>
                ` : ''}
              </div>
            </div>
          </div>
        </div>
      `;
    });
    
    html += '</div>';
    versionsList.innerHTML = html;
  }

  /**
   * Upload new version
   */
  uploadNewVersion() {
    console.log('Upload new version for image:', this.currentImage ? this.currentImage.id : 'none');
    // Trigger the file input for image version upload
    const fileInput = document.getElementById('image-file-input');
    if (fileInput) {
      // Set a data attribute to indicate this is for version upload
      fileInput.dataset.uploadType = 'version';
      fileInput.dataset.imageId = this.currentImage ? this.currentImage.id : '';
      fileInput.click();
    }
  }

  /**
   * Revert to a specific version
   */
  async revertToVersion(versionId) {
    if (!confirm('Revert to this version? This will make it the current image.')) {
      return;
    }

    try {
      this.editor.showLoading();

      const params = new URLSearchParams();
      params.append('token', this.editor.token);
      params.append('imageId', this.currentImage.id);
      params.append('versionId', versionId);

      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageVersionRevert`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'same-origin',
        body: params.toString()
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      this.showToast('Reverted to selected version successfully!', 'success');
      
      // Reload the image and versions
      await this.loadImage(this.currentImage.id);
      await this.loadVersions();
      this.editor.imageViewer.loadImage(this.currentImage.id);
      this.editor.imageLibrary.loadImages();

    } catch (error) {
      console.error('Error reverting to version:', error);
      alert('Failed to revert to version: ' + error.message);
    } finally {
      this.editor.hideLoading();
    }
  }

  /**
   * Delete a specific version
   */
  async deleteVersion(versionId) {
    if (!confirm('Delete this version? This action cannot be undone.')) {
      return;
    }

    try {
      this.editor.showLoading();

      const params = new URLSearchParams();
      params.append('token', this.editor.token);
      params.append('versionId', versionId);

      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageVersionDelete`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'same-origin',
        body: params.toString()
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      
      if (result.error) {
        throw new Error(result.error);
      }

      this.showToast('Version deleted successfully!', 'success');
      
      // Reload versions list
      await this.loadVersions();

    } catch (error) {
      console.error('Error deleting version:', error);
      alert('Failed to delete version: ' + error.message);
    } finally {
      this.editor.hideLoading();
    }
  }

  /**
   * Get current image data
   */
  getCurrentImage() {
    return this.currentImage;
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
    if (type === 'success') icon = 'fa-check-circle';
    else if (type === 'error') icon = 'fa-exclamation-circle';
    else if (type === 'warning') icon = 'fa-exclamation-triangle';
    
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
