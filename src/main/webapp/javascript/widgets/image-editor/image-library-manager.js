/**
 * Image Library Manager for Visual Image Editor
 * Handles loading, displaying, searching, and selecting images from the library
 * 
 * @author matt rajkowski
 * @created 1/21/26 9:45 PM
 */

class ImageLibraryManager {
  constructor(editor) {
    this.editor = editor;
    this.currentPage = 1;
    this.pageSize = 20;
    this.totalImages = 0;
    this.searchTerm = '';
    this.images = [];
    this.selectedImageId = null;
    this.loading = false;
  }

  /**
   * Initialize the image library
   */
  init() {
    console.log('Initializing Image Library Manager...');
    this.setupEventListeners();
    this.loadImages();
  }

  /**
   * Setup event listeners
   */
  setupEventListeners() {
    // Search input with debouncing
    const searchInput = document.getElementById('image-search');
    if (searchInput) {
      searchInput.addEventListener('input', this.debounce((e) => {
        this.searchTerm = e.target.value.trim();
        this.currentPage = 1; // Reset to first page on new search
        this.loadImages();
      }, 300));
    }

    // Upload first image button (shown when library is empty)
    document.addEventListener('click', (e) => {
      if (e.target.id === 'upload-first-image-btn') {
        this.editor.triggerFileImport();
      }
    });
  }

  /**
   * Load images from the server
   */
  async loadImages() {
    if (this.loading) return;
    
    this.loading = true;
    this.showLoadingState();

    try {
      const params = new URLSearchParams({
        page: this.currentPage,
        limit: this.pageSize
      });

      if (this.searchTerm) {
        params.append('search', this.searchTerm);
      }

      const response = await fetch(`${this.editor.config.apiBaseUrl}/imageLibrary?${params}`, {
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
      console.log('Loaded images:', data);

      this.images = data.images || [];
      this.totalImages = data.total || 0;
      
      this.renderImages();
      this.renderPagination();

    } catch (error) {
      console.error('Error loading images:', error);
      this.showErrorState('Failed to load images. Please try again.');
    } finally {
      this.loading = false;
    }
  }

  /**
   * Show loading state
   */
  showLoadingState() {
    const container = document.querySelector('#image-list-container .image-grid');
    if (container) {
      container.innerHTML = `
        <div class="loading-message">
          <i class="far fa-spinner fa-spin"></i>
          <p>Loading images...</p>
        </div>
      `;
    }
  }

  /**
   * Show error state
   */
  showErrorState(message) {
    const container = document.querySelector('#image-list-container .image-grid');
    if (container) {
      container.innerHTML = `
        <div class="no-images-message">
          <i class="far fa-exclamation-triangle fa-3x"></i>
          <p>${message}</p>
          <button id="retry-load-images-btn" class="button tiny primary">Retry</button>
        </div>
      `;

      // Add retry handler
      const retryBtn = document.getElementById('retry-load-images-btn');
      if (retryBtn) {
        retryBtn.addEventListener('click', () => this.loadImages());
      }
    }
  }

  /**
   * Render images in the grid
   */
  renderImages() {
    const container = document.querySelector('#image-list-container .image-grid');
    if (!container) return;

    if (this.images.length === 0) {
      this.showEmptyState();
      return;
    }

    container.innerHTML = '';

    this.images.forEach(image => {
      const imageItem = this.createImageGridItem(image);
      container.appendChild(imageItem);
    });
  }

  /**
   * Show empty state when no images found
   */
  showEmptyState() {
    const container = document.querySelector('#image-list-container .image-grid');
    if (container) {
      if (this.searchTerm) {
        container.innerHTML = `
          <div class="no-images-message">
            <i class="far fa-search fa-3x"></i>
            <p>No images found matching "${this.escapeHtml(this.searchTerm)}"</p>
            <button id="clear-search-btn" class="button tiny secondary">Clear Search</button>
          </div>
        `;
        
        const clearBtn = document.getElementById('clear-search-btn');
        if (clearBtn) {
          clearBtn.addEventListener('click', () => {
            document.getElementById('image-search').value = '';
            this.searchTerm = '';
            this.currentPage = 1;
            this.loadImages();
          });
        }
      } else {
        container.innerHTML = `
          <div class="no-images-message">
            <i class="far fa-image fa-3x"></i>
            <p>No images in your library</p>
            <button id="upload-first-image-btn" class="button tiny primary">Upload Your First Image</button>
          </div>
        `;
      }
    }
  }

  /**
   * Create an image grid item element
   */
  createImageGridItem(image) {
    const item = document.createElement('div');
    item.className = 'image-grid-item';
    item.dataset.imageId = image.id;
    
    if (this.selectedImageId === image.id) {
      item.classList.add('selected');
    }

    // Create image element
    const img = document.createElement('img');
    img.src = `${this.editor.config.contextPath}${image.url}`;
    img.alt = image.filename;
    img.loading = 'lazy'; // Native lazy loading
    
    // Handle image load errors
    img.onerror = () => {
      // Use a data URI for placeholder instead of referencing a file
      img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2VlZSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LWZhbWlseT0iQXJpYWwiIGZvbnQtc2l6ZT0iMTQiIGZpbGw9IiM5OTkiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5JbWFnZTwvdGV4dD48L3N2Zz4=';
    };

    // Create filename label
    const nameLabel = document.createElement('div');
    nameLabel.className = 'image-name';
    nameLabel.textContent = image.filename;
    nameLabel.title = image.filename;

    item.appendChild(img);
    item.appendChild(nameLabel);

    // Click handler to select image
    item.addEventListener('click', () => {
      this.selectImage(image.id);
    });

    return item;
  }

  /**
   * Select an image
   */
  selectImage(imageId) {
    // Check for unsaved changes before switching
    if (this.selectedImageId !== imageId && this.editor.hasUnsavedChanges()) {
      this.editor.showUnsavedChangesModal(() => {
        this.performImageSelection(imageId);
      });
    } else {
      this.performImageSelection(imageId);
    }
  }

  /**
   * Perform the actual image selection
   */
  performImageSelection(imageId) {
    // Remove previous selection
    const previousSelected = document.querySelector('.image-grid-item.selected');
    if (previousSelected) {
      previousSelected.classList.remove('selected');
    }

    // Add new selection
    const newSelected = document.querySelector(`.image-grid-item[data-image-id="${imageId}"]`);
    if (newSelected) {
      newSelected.classList.add('selected');
    }

    this.selectedImageId = imageId;

    // Load the image in the viewer
    this.editor.imageViewer.loadImage(imageId);
    this.editor.imageProperties.loadImage(imageId);
  }

  /**
   * Render pagination controls
   */
  renderPagination() {
    const paginationContainer = document.getElementById('image-pagination');
    if (!paginationContainer) return;

    const totalPages = Math.ceil(this.totalImages / this.pageSize);

    if (totalPages <= 1) {
      paginationContainer.innerHTML = '';
      return;
    }

    let html = '';

    // Previous button
    if (this.currentPage > 1) {
      html += `<button class="button tiny secondary pagination-btn" data-page="${this.currentPage - 1}">
        <i class="far fa-chevron-left"></i>
      </button>`;
    }

    // Page numbers (show current page and a few around it)
    const startPage = Math.max(1, this.currentPage - 2);
    const endPage = Math.min(totalPages, this.currentPage + 2);

    if (startPage > 1) {
      html += `<button class="button tiny secondary pagination-btn" data-page="1">1</button>`;
      if (startPage > 2) {
        html += `<span class="pagination-ellipsis">...</span>`;
      }
    }

    for (let i = startPage; i <= endPage; i++) {
      const isActive = i === this.currentPage ? 'primary' : 'secondary';
      html += `<button class="button tiny ${isActive} pagination-btn" data-page="${i}">${i}</button>`;
    }

    if (endPage < totalPages) {
      if (endPage < totalPages - 1) {
        html += `<span class="pagination-ellipsis">...</span>`;
      }
      html += `<button class="button tiny secondary pagination-btn" data-page="${totalPages}">${totalPages}</button>`;
    }

    // Next button
    if (this.currentPage < totalPages) {
      html += `<button class="button tiny secondary pagination-btn" data-page="${this.currentPage + 1}">
        <i class="far fa-chevron-right"></i>
      </button>`;
    }

    paginationContainer.innerHTML = html;

    // Add click handlers
    paginationContainer.querySelectorAll('.pagination-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const page = Number.parseInt(btn.dataset.page, 10);
        if (page !== this.currentPage) {
          this.currentPage = page;
          this.loadImages();
        }
      });
    });
  }

  /**
   * Reload the current page of images
   */
  reload() {
    this.loadImages();
  }

  /**
   * Add a newly uploaded image to the library
   */
  addImage(image) {
    // Add to the beginning of the array
    this.images.unshift(image);
    this.totalImages++;
    
    // Re-render
    this.renderImages();
    this.renderPagination();
    
    // Auto-select the new image
    this.selectImage(image.id);
  }

  /**
   * Remove an image from the library
   */
  removeImage(imageId) {
    this.images = this.images.filter(img => img.id !== imageId);
    this.totalImages--;
    
    // If this was the selected image, clear selection
    if (this.selectedImageId === imageId) {
      this.selectedImageId = null;
      this.editor.imageViewer.clear();
      this.editor.imageProperties.clear();
    }
    
    this.renderImages();
    this.renderPagination();
  }

  /**
   * Debounce function for search input
   */
  debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  /**
   * Escape HTML to prevent XSS
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Get the currently selected image ID
   */
  getSelectedImageId() {
    return this.selectedImageId;
  }

  /**
   * Get the currently selected image data
   */
  getSelectedImage() {
    return this.images.find(img => img.id === this.selectedImageId);
  }
}
