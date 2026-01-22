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
        this.editor.markAsModified();
      });
    }

    // View versions button
    const viewVersionsBtn = document.getElementById('view-versions-btn');
    if (viewVersionsBtn) {
      viewVersionsBtn.addEventListener('click', () => {
        this.showVersionsModal();
      });
    }

    // Upload new version button
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
    const noImageDiv = document.querySelector('#properties-content .no-image-selected');
    const form = document.getElementById('image-metadata-form');
    
    if (noImageDiv) noImageDiv.style.display = 'none';
    if (form) form.style.display = 'flex';

    // Populate form fields
    this.setFieldValue('image-title', imageData.title || imageData.filename || '');
    this.setFieldValue('image-filename', imageData.filename || '');
    this.setFieldValue('image-alt-text', imageData.altText || '');
    this.setFieldValue('image-description', imageData.description || '');

    // Display read-only metadata
    this.setTextContent('image-dimensions', 
      imageData.width && imageData.height ? `${imageData.width} × ${imageData.height} px` : '-');
    
    this.setTextContent('image-file-size', this.formatFileSize(imageData.fileLength));
    this.setTextContent('image-file-type', imageData.fileType || '-');
    this.setTextContent('image-created', this.formatDate(imageData.created));
    this.setTextContent('image-modified', this.formatDate(imageData.processed) || '-');
    this.setTextContent('image-version-info', imageData.version || 'Version 1');

    // Reset modification state
    this.editor.clearModified();
  }

  /**
   * Clear properties panel
   */
  clear() {
    this.currentImage = null;

    const noImageDiv = document.querySelector('#properties-content .no-image-selected');
    const form = document.getElementById('image-metadata-form');
    
    if (noImageDiv) noImageDiv.style.display = 'flex';
    if (form) form.style.display = 'none';
  }

  /**
   * Show error state
   */
  showErrorState(message) {
    const noImageDiv = document.querySelector('#properties-content .no-image-selected');
    if (noImageDiv) {
      noImageDiv.innerHTML = `<p style="color: var(--alert-color);">${message}</p>`;
      noImageDiv.style.display = 'flex';
    }

    const form = document.getElementById('image-metadata-form');
    if (form) form.style.display = 'none';
  }

  /**
   * Get form data for saving
   */
  getFormData() {
    return {
      id: this.currentImage ? this.currentImage.id : null,
      title: this.getFieldValue('image-title'),
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
    } catch (e) {
      return dateString;
    }
  }

  /**
   * Show versions modal
   */
  showVersionsModal() {
    console.log('Show versions modal');
    // TODO: Implement versions modal
    alert('Version history feature coming soon!');
  }

  /**
   * Upload new version
   */
  uploadNewVersion() {
    console.log('Upload new version');
    // TODO: Implement upload new version
    alert('Upload new version feature coming soon!');
  }

  /**
   * Get current image data
   */
  getCurrentImage() {
    return this.currentImage;
  }
}
