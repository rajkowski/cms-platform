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
    this.savedOriginalImageData = null; // Original from file, never modified
    this.originalImageData = null; // Working original, updated after crop
    this.currentImageElement = null;
    this.transformations = {
      rotation: 0,
      flipHorizontal: false,
      flipVertical: false,
      brightness: 0,
      contrast: 0,
      saturation: 0,
      cropData: null
    };
    this.selectionRect = null;
    this.isDragging = false;
    this.dragStart = null;
    this.resizeHandle = null;
    this.HANDLE_SIZE = 20;  // Size for corner handles
    this.EDGE_HANDLE_LENGTH = 50;  // Length of edge handles along the rectangle
    this.zoomLevel = 1;  // Track current zoom level
    this.imageScaleX = 1;  // Scale for fitting image in container
    this.imageScaleY = 1;
    this.canvasContainer = null;  // Reference to canvas container for zoom
  }

  /**
   * Initialize the image viewer
   */
  init() {
    console.log('Initializing Image Viewer Manager...');
    this.canvas = document.getElementById('image-canvas');
    this.canvasContainer = document.getElementById('image-canvas-container');
    if (this.canvas) {
      this.ctx = this.canvas.getContext('2d');
    }
    this.setupEventListeners();
  }

  /**
   * Setup event listeners for image tools
   */
  setupEventListeners() {
    // Rotation buttons
    const rotateLeftBtn = document.getElementById('rotate-left-btn');
    if (rotateLeftBtn) {
      rotateLeftBtn.addEventListener('click', () => this.rotateImage(-90));
    }

    const rotateRightBtn = document.getElementById('rotate-right-btn');
    if (rotateRightBtn) {
      rotateRightBtn.addEventListener('click', () => this.rotateImage(90));
    }

    // Flip buttons
    const flipHorizontalBtn = document.getElementById('flip-horizontal-btn');
    if (flipHorizontalBtn) {
      flipHorizontalBtn.addEventListener('click', () => this.flipImage('horizontal'));
    }

    const flipVerticalBtn = document.getElementById('flip-vertical-btn');
    if (flipVerticalBtn) {
      flipVerticalBtn.addEventListener('click', () => this.flipImage('vertical'));
    }

    // Adjustments button
    const adjustmentsBtn = document.getElementById('adjustments-btn');
    if (adjustmentsBtn) {
      adjustmentsBtn.addEventListener('click', () => this.toggleAdjustments());
    }

    // Reset button
    const resetBtn = document.getElementById('reset-btn');
    if (resetBtn) {
      resetBtn.addEventListener('click', () => this.resetChanges());
    }

    // Selection controls
    const cropSelectionBtn = document.getElementById('crop-selection-btn');
    if (cropSelectionBtn) {
      cropSelectionBtn.addEventListener('click', () => this.cropSelection());
    }

    const copySelectionBtn = document.getElementById('copy-selection-btn');
    if (copySelectionBtn) {
      copySelectionBtn.addEventListener('click', () => this.copySelection());
    }

    const clearSelectionBtn = document.getElementById('clear-selection-btn');
    if (clearSelectionBtn) {
      clearSelectionBtn.addEventListener('click', () => this.clearSelection());
    }

    // Adjustment sliders
    ['brightness', 'contrast', 'saturation'].forEach(adjustment => {
      const slider = document.getElementById(`${adjustment}-slider`);
      const valueSpan = document.getElementById(`${adjustment}-value`);

      if (slider && valueSpan) {
        slider.addEventListener('input', (e) => {
          const value = Number.parseInt(e.target.value, 10);
          valueSpan.textContent = value;
          this.transformations[adjustment] = value;
          this.applyTransformations();
        });
      }
    });

    // Create thumbnail button
    const createThumbnailBtn = document.getElementById('create-thumbnail-btn');
    if (createThumbnailBtn) {
      createThumbnailBtn.addEventListener('click', () => this.createThumbnail());
    }

    // Zoom buttons
    const zoomInBtn = document.getElementById('zoom-in-btn');
    if (zoomInBtn) {
      zoomInBtn.addEventListener('click', () => this.zoomIn());
    }

    const zoomOutBtn = document.getElementById('zoom-out-btn');
    if (zoomOutBtn) {
      zoomOutBtn.addEventListener('click', () => this.zoomOut());
    }

    const zoomFitBtn = document.getElementById('zoom-fit-btn');
    if (zoomFitBtn) {
      zoomFitBtn.addEventListener('click', () => this.zoomToFit());
    }

    const zoomActualBtn = document.getElementById('zoom-actual-btn');
    if (zoomActualBtn) {
      zoomActualBtn.addEventListener('click', () => this.zoomActualSize());
    }

    // Apply adjustments button
    const applyAdjustmentsBtn = document.getElementById('apply-adjustments-btn');
    if (applyAdjustmentsBtn) {
      applyAdjustmentsBtn.addEventListener('click', () => this.applyAdjustments());
    }

    const cancelAdjustmentsBtn = document.getElementById('cancel-adjustments-btn');
    if (cancelAdjustmentsBtn) {
      cancelAdjustmentsBtn.addEventListener('click', () => this.cancelAdjustments());
    }

    // Canvas mouse events for cropping
    if (this.canvas) {
      this.canvas.addEventListener('mousedown', (e) => this.onCanvasMouseDown(e));
      this.canvas.addEventListener('mousemove', (e) => this.onCanvasMouseMove(e));
      this.canvas.addEventListener('mouseup', (e) => this.onCanvasMouseUp(e));

      // Listen for mouse up on document to catch releases outside canvas
      document.addEventListener('mouseup', (e) => this.onCanvasMouseUp(e));
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
        // Store the original image element
        this.currentImageElement = img;

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
        if (canvasContainer) {
          canvasContainer.style.display = 'flex';
          canvasContainer.style.alignItems = 'center';
          canvasContainer.style.justifyContent = 'center';
        }

        // Set canvas size to match image
        this.canvas.width = img.width;
        this.canvas.height = img.height;

        // Draw image
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.drawImage(img, 0, 0);

        // Store original image data for reset functionality
        const originalData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
        this.originalImageData = originalData;
        // Keep a separate copy of the saved original that never changes
        this.savedOriginalImageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);

        // Reset transformations
        this.resetTransformations();

        // Apply zoom to fit for newly loaded images
        this.zoomToFit();

        resolve();
      };

      img.onerror = () => {
        reject(new Error('Failed to load image'));
      };

      img.src = `${this.editor.config.contextPath}${imageData.url}`;
    });
  }

  /**
   * Reset transformations to default values
   */
  resetTransformations() {
    this.transformations = {
      rotation: 0,
      flipHorizontal: false,
      flipVertical: false,
      brightness: 0,
      contrast: 0,
      saturation: 0,
      cropData: null
    };

    // Reset slider values
    ['brightness', 'contrast', 'saturation'].forEach(adjustment => {
      const slider = document.getElementById(`${adjustment}-slider`);
      const valueSpan = document.getElementById(`${adjustment}-value`);

      if (slider) slider.value = 0;
      if (valueSpan) valueSpan.textContent = '0';
    });
  }

  /**
   * Rotate the image
   */
  rotateImage(degrees) {
    this.transformations.rotation = (this.transformations.rotation + degrees) % 360;
    if (this.transformations.rotation < 0) {
      this.transformations.rotation += 360;
    }
    this.applyTransformations();
    this.editor.markAsModified();
  }

  /**
   * Flip the image
   */
  flipImage(direction) {
    if (direction === 'horizontal') {
      this.transformations.flipHorizontal = !this.transformations.flipHorizontal;
    } else if (direction === 'vertical') {
      this.transformations.flipVertical = !this.transformations.flipVertical;
    }
    this.applyTransformations();
    this.editor.markAsModified();
  }

  /**
   * Apply all transformations to the canvas
   */
  applyTransformations() {
    if (!this.originalImageData || !this.currentImageElement) return;

    const img = this.currentImageElement;
    const rotation = this.transformations.rotation;

    // Calculate new canvas dimensions based on rotation
    let canvasWidth = img.width;
    let canvasHeight = img.height;

    if (rotation === 90 || rotation === 270) {
      canvasWidth = img.height;
      canvasHeight = img.width;
    }

    this.canvas.width = canvasWidth;
    this.canvas.height = canvasHeight;

    // Save context state
    this.ctx.save();

    // Move to center for transformations
    this.ctx.translate(canvasWidth / 2, canvasHeight / 2);

    // Apply rotation
    this.ctx.rotate((rotation * Math.PI) / 180);

    // Apply flips
    const scaleX = this.transformations.flipHorizontal ? -1 : 1;
    const scaleY = this.transformations.flipVertical ? -1 : 1;
    this.ctx.scale(scaleX, scaleY);

    // Draw the image centered
    this.ctx.drawImage(img, -img.width / 2, -img.height / 2);

    // Restore context state
    this.ctx.restore();

    // Apply color adjustments
    if (this.transformations.brightness !== 0 ||
      this.transformations.contrast !== 0 ||
      this.transformations.saturation !== 0) {
      this.applyColorAdjustments();
    }
  }

  /**
   * Apply color adjustments (brightness, contrast, saturation)
   */
  applyColorAdjustments() {
    const imageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
    const data = imageData.data;

    const brightness = this.transformations.brightness / 100;
    const contrast = (this.transformations.contrast + 100) / 100;
    const saturation = (this.transformations.saturation + 100) / 100;

    for (let i = 0; i < data.length; i += 4) {
      // Apply brightness
      let r = data[i] + brightness * 255;
      let g = data[i + 1] + brightness * 255;
      let b = data[i + 2] + brightness * 255;

      // Apply contrast
      r = ((r / 255 - 0.5) * contrast + 0.5) * 255;
      g = ((g / 255 - 0.5) * contrast + 0.5) * 255;
      b = ((b / 255 - 0.5) * contrast + 0.5) * 255;

      // Apply saturation
      const gray = 0.2989 * r + 0.587 * g + 0.114 * b;
      r = gray + (r - gray) * saturation;
      g = gray + (g - gray) * saturation;
      b = gray + (b - gray) * saturation;

      // Clamp values
      data[i] = Math.max(0, Math.min(255, r));
      data[i + 1] = Math.max(0, Math.min(255, g));
      data[i + 2] = Math.max(0, Math.min(255, b));
    }

    this.ctx.putImageData(imageData, 0, 0);
  }

  /**
   * Show selection controls - enable toolbar buttons
   */
  showSelectionControls() {
    const cropBtn = document.getElementById('crop-selection-btn');
    const copyBtn = document.getElementById('copy-selection-btn');
    const clearBtn = document.getElementById('clear-selection-btn');

    if (cropBtn) cropBtn.disabled = false;
    if (copyBtn) copyBtn.disabled = false;
    if (clearBtn) clearBtn.disabled = false;
  }

  /**
   * Hide selection controls - disable toolbar buttons
   */
  hideSelectionControls() {
    const copyBtn = document.getElementById('copy-selection-btn');
    const clearBtn = document.getElementById('clear-selection-btn');

    if (copyBtn) copyBtn.disabled = true;
    if (clearBtn) clearBtn.disabled = true;
  }

  /**
   * Convert mouse event coordinates to canvas coordinates
   * Allows coordinates outside canvas bounds for edge snapping
   */
  getCanvasCoordinates(e) {
    const rect = this.canvas.getBoundingClientRect();
    const scaleX = this.canvas.width / rect.width;
    const scaleY = this.canvas.height / rect.height;

    return {
      x: (e.clientX - rect.left) * scaleX,
      y: (e.clientY - rect.top) * scaleY
    };
  }

  /**
   * Canvas mouse down - start selection or resize
   */
  onCanvasMouseDown(e) {
    if (!this.currentImage) return;

    const { x, y } = this.getCanvasCoordinates(e);

    // Check if clicking on a resize handle
    if (this.selectionRect) {
      const handle = this.getHandleAtPoint(x, y);
      if (handle) {
        this.resizeHandle = handle;
        this.isDragging = true;
        return;
      }

      // Check if clicking inside selection to move it
      if (this.isPointInSelection(x, y)) {
        this.isDragging = true;
        this.dragStart = { x, y };
        this.canvas.style.cursor = 'move';
        return;
      }
    }

    // Start new selection with initial 50x50 size centered on click
    this.isDragging = true;
    this.dragStart = { x, y };
    // Center the 50x50 rectangle on the click point
    const rectSize = 50;
    this.selectionRect = {
      x: Math.max(0, Math.min(x - rectSize / 2, this.canvas.width - rectSize)),
      y: Math.max(0, Math.min(y - rectSize / 2, this.canvas.height - rectSize)),
      width: rectSize,
      height: rectSize
    };
    this.canvas.style.cursor = 'crosshair';
  }

  /**
   * Canvas mouse move - update selection or resize
   */
  onCanvasMouseMove(e) {
    if (!this.currentImage) return;

    let { x, y } = this.getCanvasCoordinates(e);

    if (this.isDragging) {
      // Snap to edges when dragging near or past bounds
      const snapX = Math.max(0, Math.min(x, this.canvas.width));
      const snapY = Math.max(0, Math.min(y, this.canvas.height));

      if (this.resizeHandle) {
        // Resize selection with edge snapping
        this.resizeSelection(snapX, snapY);
      } else if (this.selectionRect && this.dragStart &&
        this.selectionRect.width > 50 && this.selectionRect.height > 50 &&
        this.isPointInSelection(this.dragStart.x, this.dragStart.y)) {
        // Move selection
        const dx = snapX - this.dragStart.x;
        const dy = snapY - this.dragStart.y;
        const newX = this.selectionRect.x + dx;
        const newY = this.selectionRect.y + dy;

        // Constrain to canvas boundaries
        this.selectionRect.x = Math.max(0, Math.min(newX, this.canvas.width - this.selectionRect.width));
        this.selectionRect.y = Math.max(0, Math.min(newY, this.canvas.height - this.selectionRect.height));
        this.dragStart = { x: snapX, y: snapY };
      } else {
        // Grow selection from initial 50x50
        const minX = Math.min(this.dragStart.x, snapX);
        const minY = Math.min(this.dragStart.y, snapY);
        const maxX = Math.max(this.dragStart.x, snapX);
        const maxY = Math.max(this.dragStart.y, snapY);

        this.selectionRect = {
          x: Math.max(0, minX),
          y: Math.max(0, minY),
          width: Math.min(maxX, this.canvas.width) - Math.max(0, minX),
          height: Math.min(maxY, this.canvas.height) - Math.max(0, minY)
        };
      }

      // Redraw with selection rectangle
      this.applyTransformations();
      this.drawSelectionRectangle();
    } else if (this.selectionRect) {
      // Update cursor based on handle position
      const handle = this.getHandleAtPoint(x, y);
      if (handle) {
        this.canvas.style.cursor = this.getResizeCursor(handle);
      } else if (this.isPointInSelection(x, y)) {
        this.canvas.style.cursor = 'move';
      } else {
        this.canvas.style.cursor = 'crosshair';
      }
    } else {
      this.canvas.style.cursor = 'crosshair';
    }
  }

  /**
   * Canvas mouse up - finalize selection
   * Can be released outside canvas bounds
   */
  onCanvasMouseUp() {
    if (!this.isDragging) return;

    this.isDragging = false;
    this.resizeHandle = null;

    // Accept selection if it's at least the initial 50x50 size
    if (this.selectionRect && this.selectionRect.width >= 50 && this.selectionRect.height >= 50) {
      this.showSelectionControls();
      this.canvas.style.cursor = 'crosshair';
    } else if (this.selectionRect && (this.selectionRect.width < 50 || this.selectionRect.height < 50)) {
      // Clear selection if smaller than initial size
      this.selectionRect = null;
      this.hideSelectionControls();
      this.applyTransformations();
      this.canvas.style.cursor = 'crosshair';
    }
  }

  /**
   * Check if point is inside selection
   */
  isPointInSelection(x, y) {
    if (!this.selectionRect) return false;
    return x >= this.selectionRect.x &&
      x <= this.selectionRect.x + this.selectionRect.width &&
      y >= this.selectionRect.y &&
      y <= this.selectionRect.y + this.selectionRect.height;
  }

  /**
   * Get which handle (if any) is at the given point
   */
  getHandleAtPoint(x, y) {
    if (!this.selectionRect) return null;

    const handles = this.getHandlePositions();
    const halfSize = this.HANDLE_SIZE / 2;
    const halfLength = this.EDGE_HANDLE_LENGTH / 2;

    for (const [name, pos] of Object.entries(handles)) {
      let hitDetected = false;

      // Edge handles have different dimensions
      if (name === 'n' || name === 's') {
        // North/South: wide horizontally, narrow vertically
        hitDetected = x >= pos.x - halfLength && x <= pos.x + halfLength &&
          y >= pos.y - halfSize && y <= pos.y + halfSize;
      } else if (name === 'w' || name === 'e') {
        // West/East: narrow horizontally, tall vertically
        hitDetected = x >= pos.x - halfSize && x <= pos.x + halfSize &&
          y >= pos.y - halfLength && y <= pos.y + halfLength;
      } else {
        // Corner handles: square
        hitDetected = x >= pos.x - halfSize && x <= pos.x + halfSize &&
          y >= pos.y - halfSize && y <= pos.y + halfSize;
      }

      if (hitDetected) {
        return name;
      }
    }
    return null;
  }

  /**
   * Get positions of all resize handles
   */
  getHandlePositions() {
    const r = this.selectionRect;
    return {
      'nw': { x: r.x, y: r.y },
      'ne': { x: r.x + r.width, y: r.y },
      'sw': { x: r.x, y: r.y + r.height },
      'se': { x: r.x + r.width, y: r.y + r.height },
      'n': { x: r.x + r.width / 2, y: r.y },
      's': { x: r.x + r.width / 2, y: r.y + r.height },
      'w': { x: r.x, y: r.y + r.height / 2 },
      'e': { x: r.x + r.width, y: r.y + r.height / 2 }
    };
  }

  /**
   * Get appropriate cursor for resize handle
   */
  getResizeCursor(handle) {
    const cursors = {
      'nw': 'nw-resize', 'ne': 'ne-resize',
      'sw': 'sw-resize', 'se': 'se-resize',
      'n': 'row-resize', 's': 'row-resize',
      'w': 'col-resize', 'e': 'col-resize'
    };
    return cursors[handle] || 'default';
  }

  /**
   * Resize selection based on handle
   */
  resizeSelection(x, y) {
    if (!this.selectionRect || !this.resizeHandle) return;

    const r = this.selectionRect;
    const handle = this.resizeHandle;

    // Constrain coordinates to canvas
    x = Math.max(0, Math.min(x, this.canvas.width));
    y = Math.max(0, Math.min(y, this.canvas.height));

    // Update rectangle based on which handle is being dragged
    if (handle.includes('n')) {
      const bottom = r.y + r.height;
      r.y = Math.min(y, bottom - 10);
      r.height = Math.max(10, bottom - r.y);
    }
    if (handle.includes('s')) {
      r.height = Math.max(10, y - r.y);
    }
    if (handle.includes('w')) {
      const right = r.x + r.width;
      r.x = Math.min(x, right - 10);
      r.width = Math.max(10, right - r.x);
    }
    if (handle.includes('e')) {
      r.width = Math.max(10, x - r.x);
    }
  }

  /**
   * Draw selection rectangle overlay
   */
  drawSelectionRectangle() {
    if (!this.selectionRect) return;

    this.ctx.save();

    // Draw darkened overlay outside the selection area
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';  // 50% black overlay
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    // Clear the selection area to show the original image
    this.ctx.clearRect(
      this.selectionRect.x,
      this.selectionRect.y,
      this.selectionRect.width,
      this.selectionRect.height
    );

    // Redraw the selection area from the original image data
    if (this.originalImageData) {
      this.ctx.putImageData(
        this.originalImageData,
        0,
        0,
        this.selectionRect.x,
        this.selectionRect.y,
        this.selectionRect.width,
        this.selectionRect.height
      );
    }

    // Draw selection rectangle
    this.ctx.strokeStyle = '#4a9eff';
    this.ctx.lineWidth = 2;
    this.ctx.setLineDash([5, 5]);
    this.ctx.strokeRect(
      this.selectionRect.x,
      this.selectionRect.y,
      this.selectionRect.width,
      this.selectionRect.height
    );

    // Draw handles with white border for better visibility
    const handles = this.getHandlePositions();

    Object.entries(handles).forEach(([name, pos]) => {
      let width, height;

      // Edge handles are longer along their respective edges
      if (name === 'n' || name === 's') {
        // North/South: wide horizontally, narrow vertically
        width = this.EDGE_HANDLE_LENGTH;
        height = this.HANDLE_SIZE;
      } else if (name === 'w' || name === 'e') {
        // West/East: narrow horizontally, tall vertically
        width = this.HANDLE_SIZE;
        height = this.EDGE_HANDLE_LENGTH;
      } else {
        // Corner handles: square
        width = this.HANDLE_SIZE;
        height = this.HANDLE_SIZE;
      }

      // White outline
      this.ctx.fillStyle = '#ffffff';
      this.ctx.fillRect(
        pos.x - width / 2 - 1,
        pos.y - height / 2 - 1,
        width + 2,
        height + 2
      );

      // Blue handle
      this.ctx.fillStyle = '#4a9eff';
      this.ctx.fillRect(
        pos.x - width / 2,
        pos.y - height / 2,
        width,
        height
      );
    });

    this.ctx.restore();
  }

  /**
   * Crop the image to the current selection
   */
  cropSelection() {
    // If no selection exists, create one covering the entire image
    if (!this.selectionRect || this.selectionRect.width === 0 || this.selectionRect.height === 0) {
      this.selectionRect = {
        x: 0,
        y: 0,
        width: this.canvas.width,
        height: this.canvas.height
      };
      this.showSelectionControls();
      this.applyTransformations();
      this.drawSelectionRectangle();
      return;
    }

    // Save the selection bounds before clearing it
    const cropRect = { ...this.selectionRect };

    // Clear selection first so the rectangle is not drawn
    this.selectionRect = null;
    this.isDragging = false;
    this.dragStart = null;
    this.resizeHandle = null;
    this.hideSelectionControls();

    // Redraw the image without the selection rectangle overlay
    this.applyTransformations();

    // Get the cropped image data from the clean canvas
    const croppedData = this.ctx.getImageData(
      cropRect.x,
      cropRect.y,
      cropRect.width,
      cropRect.height
    );

    // Resize canvas to crop dimensions
    this.canvas.width = cropRect.width;
    this.canvas.height = cropRect.height;

    // Draw cropped data
    this.ctx.putImageData(croppedData, 0, 0);

    // Update working original to the cropped version
    this.originalImageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);

    // Create a new image element from the cropped canvas for future transformations
    const croppedImage = new Image();
    croppedImage.onload = () => {
      this.zoomToFit();
    };
    croppedImage.src = this.canvas.toDataURL();
    this.currentImageElement = croppedImage;

    if (croppedImage.complete) {
      this.zoomToFit();
    }

    this.canvas.style.cursor = 'crosshair';

    // Mark as modified
    this.editor.markAsModified();
  }

  /**
   * Copy selection to clipboard
   */
  async copySelection() {
    if (!this.selectionRect || this.selectionRect.width === 0 || this.selectionRect.height === 0) {
      alert('Please select an area to copy');
      return;
    }

    try {
      // Create a temporary canvas with just the selected area
      const tempCanvas = document.createElement('canvas');
      tempCanvas.width = this.selectionRect.width;
      tempCanvas.height = this.selectionRect.height;
      const tempCtx = tempCanvas.getContext('2d');

      // Copy selected area to temp canvas
      const imageData = this.ctx.getImageData(
        this.selectionRect.x,
        this.selectionRect.y,
        this.selectionRect.width,
        this.selectionRect.height
      );
      tempCtx.putImageData(imageData, 0, 0);

      // Convert to blob and copy to clipboard
      tempCanvas.toBlob(async (blob) => {
        try {
          await navigator.clipboard.write([
            new ClipboardItem({ 'image/png': blob })
          ]);
          alert('Selection copied to clipboard!');
        } catch (err) {
          console.error('Failed to copy to clipboard:', err);
          alert('Failed to copy to clipboard. Your browser may not support this feature.');
        }
      });
    } catch (err) {
      console.error('Failed to copy selection:', err);
      alert('Failed to copy selection');
    }
  }

  /**
   * Clear the current selection
   */
  clearSelection() {
    this.selectionRect = null;
    this.isDragging = false;
    this.dragStart = null;
    this.resizeHandle = null;

    // Hide selection controls
    this.hideSelectionControls();

    // Reset cursor
    this.canvas.style.cursor = 'crosshair';

    // Redraw without selection rectangle
    this.applyTransformations();
  }

  /**
   * Toggle adjustments panel
   */
  toggleAdjustments() {
    const panel = document.getElementById('adjustments-panel');
    if (panel) {
      const isVisible = panel.style.display !== 'none';
      panel.style.display = isVisible ? 'none' : 'block';

      const adjustmentsBtn = document.getElementById('adjustments-btn');
      if (adjustmentsBtn) {
        if (isVisible) {
          adjustmentsBtn.classList.remove('active');
        } else {
          adjustmentsBtn.classList.add('active');
        }
      }
    }
  }

  /**
   * Apply adjustments and close panel
   */
  applyAdjustments() {
    this.toggleAdjustments();
    this.editor.markAsModified();
  }

  /**
   * Cancel adjustments and reset
   */
  cancelAdjustments() {
    // Reset adjustment values
    this.transformations.brightness = 0;
    this.transformations.contrast = 0;
    this.transformations.saturation = 0;

    // Reset sliders
    ['brightness', 'contrast', 'saturation'].forEach(adjustment => {
      const slider = document.getElementById(`${adjustment}-slider`);
      const valueSpan = document.getElementById(`${adjustment}-value`);

      if (slider) slider.value = 0;
      if (valueSpan) valueSpan.textContent = '0';
    });

    this.applyTransformations();
    this.toggleAdjustments();
  }

  /**
   * Reset all changes to the original saved file
   */
  resetChanges() {
    if (!this.savedOriginalImageData) return;

    if (confirm('Are you sure you want to reset all changes to the original saved file?')) {
      // Restore from the saved original (never modified)
      this.canvas.width = this.savedOriginalImageData.width;
      this.canvas.height = this.savedOriginalImageData.height;
      this.ctx.putImageData(this.savedOriginalImageData, 0, 0);

      // Update working original to match saved original
      this.originalImageData = this.ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);

      // Reset transformations
      this.resetTransformations();

      // Clear any selection
      this.clearSelection();

      this.editor.clearModified();
    }
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
    this.currentImageElement = null;
    this.resetTransformations();

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
   * Get the current canvas as a data URL
   */
  getImageDataURL() {
    if (!this.canvas) return null;
    return this.canvas.toDataURL('image/png');
  }

  /**
   * Get the current canvas as a blob
   */
  async getImageBlob() {
    if (!this.canvas) return null;

    return new Promise((resolve) => {
      this.canvas.toBlob((blob) => {
        resolve(blob);
      }, 'image/png');
    });
  }

  /**
   * Check if any transformations have been applied
   */
  hasTransformations() {
    return this.transformations.rotation !== 0 ||
      this.transformations.flipHorizontal ||
      this.transformations.flipVertical ||
      this.transformations.brightness !== 0 ||
      this.transformations.contrast !== 0 ||
      this.transformations.saturation !== 0 ||
      this.transformations.cropData !== null;
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
   * Create a thumbnail for the current image
   */
  async createThumbnail() {
    if (!this.currentImage || !this.currentImage.id) {
      console.error('No image loaded to create thumbnail');
      return;
    }

    // Confirm action
    if (!confirm('Create a thumbnail (240x240 max) for this image? This will replace any existing thumbnail.')) {
      return;
    }

    console.log('Creating thumbnail for image:', this.currentImage.id);

    try {
      // Show loading state
      const btn = document.getElementById('create-thumbnail-btn');
      if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<i class="fa-regular fa-spinner fa-spin"></i> Creating...';
      }

      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageThumbnail`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams({
          'token': this.editor.config.token,
          'imageId': this.currentImage.id
        }),
        credentials: 'same-origin'
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();

      if (result.status === 'error') {
        throw new Error(result.message || 'Failed to create thumbnail');
      }

      // Show success message
      if (result.thumbnail) {
        const thumbInfo = `${result.thumbnail.width}x${result.thumbnail.height}, ${this.formatFileSize(result.thumbnail.fileLength)}`;
        this.editor.showToast(`Thumbnail created successfully! Dimensions: ${thumbInfo}`, 'success');

        // Reload the image to update thumbnail info
        this.currentImage.hasThumbnail = true;
        this.currentImage.thumbnailUrl = result.thumbnail.url;
        this.currentImage.thumbnailWidth = result.thumbnail.width;
        this.currentImage.thumbnailHeight = result.thumbnail.height;

        // Refresh the library to show thumbnail
        if (this.editor.imageLibrary) {
          this.editor.imageLibrary.loadImages();
        }
      } else {
        this.editor.showToast('Thumbnail created successfully!', 'success');
      }

    } catch (error) {
      console.error('Error creating thumbnail:', error);
      alert('Failed to create thumbnail: ' + error.message);
    } finally {
      // Restore button state
      const btn = document.getElementById('create-thumbnail-btn');
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-regular fa-image"></i> Thumbnail';
      }
    }
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
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

  /**
   * Zoom in on the image
   */
  zoomIn() {
    this.zoomLevel = Math.min(this.zoomLevel + 0.1, 3);
    this.updateCanvasScale();
  }

  /**
   * Zoom out from the image
   */
  zoomOut() {
    this.zoomLevel = Math.max(this.zoomLevel - 0.1, 0.1);
    this.updateCanvasScale();
  }

  /**
   * Zoom to fit the image in the container
   */
  zoomToFit() {
    this.zoomLevel = 1;
    this.updateCanvasScale();
  }

  /**
   * Zoom to actual size (1:1 pixels)
   */
  zoomActualSize() {
    this.zoomLevel = 1;
    // Remove any scaling constraints
    if (this.canvas) {
      this.canvas.style.maxWidth = 'none';
      this.canvas.style.maxHeight = 'none';
      this.canvas.style.width = this.canvas.width + 'px';
      this.canvas.style.height = this.canvas.height + 'px';
    }
  }

  /**
   * Update canvas scale based on zoom level
   */
  updateCanvasScale() {
    if (!this.canvas || !this.canvasContainer) return;

    console.log("Updating canvas scale, zoom level:", this.zoomLevel);

    // Ensure container is centered for equal padding on all sides
    if (this.zoomLevel <= 1) {
      this.canvasContainer.style.alignItems = 'center';
    } else {
      this.canvasContainer.style.alignItems = 'flex-start';
    }
    this.canvasContainer.style.justifyContent = 'center';

    const containerWidth = this.canvasContainer.clientWidth - 20; // Account for padding
    const containerHeight = this.canvasContainer.clientHeight - 20;

    // Calculate scale to fit in container
    const maxScaleX = containerWidth / this.currentImageElement.width;
    const maxScaleY = containerHeight / this.currentImageElement.height;
    const maxScale = Math.min(maxScaleX, maxScaleY, 1); // Don't exceed container

    // Apply zoom level
    const finalScale = maxScale * this.zoomLevel;

    this.canvas.style.maxWidth = 'none';
    this.canvas.style.maxHeight = 'none';
    this.canvas.style.width = (this.currentImageElement.width * finalScale) + 'px';
    this.canvas.style.height = (this.currentImageElement.height * finalScale) + 'px';
  }
}
