/**
 * Image Viewer Manager for Visual Image Editor
 * Handles displaying and editing images in the center pane
 * 
 * @author matt rajkowski
 * @created 1/21/26 9:50 PM
 */

class ImageViewerManager {
  constructor(editor) {
    this.editor = editor;
    this.currentImage = null;
    this.canvas = null;
    this.ctx = null;
    this.originalImageData = null;
  }

  /**
   * Initialize the image viewer
   */
  init() {
    console.log('Initializing Image Viewer Manager...');
    this.canvas = document.getElementById('image-canvas');
    if (this.canvas) {
      this.ctx = this.canvas.getContext('2d');
    }
  }

  /**
   * Load and display an image
   */
  async loadImage(imageId) {
    console.log('Loading image in viewer:', imageId);
    
    this.showLoadingState();

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
      await this.displayImage(imageData);
      this.enableTools();

    } catch (error) {
      console.error('Error loading image:', error);
      this.showErrorState('Failed to load image. Please try again.');
    }
  }

  /**
   * Display the image on the canvas
   */
  async displayImage(imageData) {
    return new Promise((resolve, reject) => {
      const img = new Image();
      
      img.onload = () => {
        // Update title
        const titleElement = document.getElementById('image-viewer-title');
        if (titleElement) {
          titleElement.textContent = imageData.filename;
          titleElement.title = imageData.filename;
        }

        // Show canvas container
        const noImageDiv = document.querySelector('#image-viewer-content .no-image-selected');
        const canvasContainer = document.getElementById('image-canvas-container');
        
        if (noImageDiv) noImageDiv.style.display = 'none';
        if (canvasContainer) canvasContainer.style.display = 'flex';

        // Set canvas size to match image
        this.canvas.width = img.width;
        this.canvas.height = img.height;

        // Draw image
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.drawImage(img, 0, 0);

        // Store original image data for reset functionality
        this.originalImageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);

        resolve();
      };

      img.onerror = () => {
        reject(new Error('Failed to load image'));
      };

      img.src = `${this.editor.config.contextPath}${imageData.url}`;
    });
  }

  /**
   * Show loading state
   */
  showLoadingState() {
    const noImageDiv = document.querySelector('#image-viewer-content .no-image-selected');
    if (noImageDiv) {
      noImageDiv.innerHTML = `
        <i class="far fa-spinner fa-spin fa-5x"></i>
        <p>Loading image...</p>
      `;
      noImageDiv.style.display = 'flex';
    }

    const canvasContainer = document.getElementById('image-canvas-container');
    if (canvasContainer) {
      canvasContainer.style.display = 'none';
    }
  }

  /**
   * Show error state
   */
  showErrorState(message) {
    const noImageDiv = document.querySelector('#image-viewer-content .no-image-selected');
    if (noImageDiv) {
      noImageDiv.innerHTML = `
        <i class="far fa-exclamation-triangle fa-5x"></i>
        <p>${message}</p>
      `;
      noImageDiv.style.display = 'flex';
    }

    const canvasContainer = document.getElementById('image-canvas-container');
    if (canvasContainer) {
      canvasContainer.style.display = 'none';
    }

    this.disableTools();
  }

  /**
   * Clear the viewer
   */
  clear() {
    this.currentImage = null;
    
    const titleElement = document.getElementById('image-viewer-title');
    if (titleElement) {
      titleElement.textContent = 'Select an Image';
    }

    const noImageDiv = document.querySelector('#image-viewer-content .no-image-selected');
    if (noImageDiv) {
      noImageDiv.innerHTML = `
        <i class="far fa-image fa-5x"></i>
        <p>Select an image from the library to begin editing</p>
      `;
      noImageDiv.style.display = 'flex';
    }

    const canvasContainer = document.getElementById('image-canvas-container');
    if (canvasContainer) {
      canvasContainer.style.display = 'none';
    }

    if (this.ctx && this.canvas) {
      this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    }

    this.disableTools();
  }

  /**
   * Enable image editing tools
   */
  enableTools() {
    const toolButtons = document.querySelectorAll('#image-tools .tool-btn');
    toolButtons.forEach(btn => {
      btn.disabled = false;
    });
  }

  /**
   * Disable image editing tools
   */
  disableTools() {
    const toolButtons = document.querySelectorAll('#image-tools .tool-btn');
    toolButtons.forEach(btn => {
      btn.disabled = true;
    });
  }

  /**
   * Get current image data
   */
  getCurrentImage() {
    return this.currentImage;
  }
}
