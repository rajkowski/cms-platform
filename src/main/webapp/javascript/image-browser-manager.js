/**
 * Image Browser Manager for TinyMCE
 * Handles loading, displaying, searching, and paging images in the TinyMCE image browser
 * 
 * @author github copilot
 * @created 2/14/26
 */

class ImageBrowserManager {
  constructor(config) {
    this.config = config;
    this.currentPage = 1;
    this.pageSize = 20;
    this.totalImages = 0;
    this.searchTerm = '';
    this.images = [];
    this.loading = false;
    this.hasMore = true;
    this.lazyLoadObserver = null;
  }

  /**
   * Initialize the image browser
   */
  init() {
    console.log('Initializing Image Browser Manager...');
    this.setupEventListeners();
    this.setupLazyLoading();
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
        this.currentPage = 1;
        this.images = [];
        this.hasMore = true;
        this.loadImages();
      }, 300));
    }

    // Clear search button
    const clearSearchBtn = document.getElementById('clear-search-btn');
    if (clearSearchBtn) {
      clearSearchBtn.addEventListener('click', () => {
        document.getElementById('image-search').value = '';
        this.searchTerm = '';
        this.currentPage = 1;
        this.images = [];
        this.hasMore = true;
        this.loadImages();
      });
    }
  }

  /**
   * Setup lazy loading with Intersection Observer
   */
  setupLazyLoading() {
    const container = document.getElementById('image-grid-container');
    if (!container) return;

    // Create a sentinel element for lazy loading trigger
    const sentinel = document.createElement('div');
    sentinel.id = 'lazy-load-sentinel';
    sentinel.style.height = '1px';
    sentinel.style.clear = 'both';
    container.appendChild(sentinel);

    // Create Intersection Observer
    this.lazyLoadObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && !this.loading && this.hasMore) {
            console.log('Lazy loading more images...');
            this.loadMoreImages();
          }
        });
      },
      {
        root: null,
        rootMargin: '100px',
        threshold: 0.1
      }
    );

    // Start observing the sentinel
    this.lazyLoadObserver.observe(sentinel);
  }

  /**
   * Load images from the server
   */
  async loadImages(append = false) {
    if (this.loading) return;

    this.loading = true;
    if (!append) {
      this.showLoadingState();
    }

    try {
      const params = new URLSearchParams({
        page: this.currentPage,
        limit: this.pageSize
      });

      if (this.searchTerm) {
        params.append('search', this.searchTerm);
      }

      const response = await fetch(`${this.config.apiBaseUrl}/imageLibrary?${params}`, {
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

      if (append) {
        this.images = [...this.images, ...(data.images || [])];
      } else {
        this.images = data.images || [];
      }
      this.totalImages = data.total || 0;

      // Check if there are more images to load
      this.hasMore = this.images.length < this.totalImages;

      this.updateImageCount();
      this.renderImages(append);

    } catch (error) {
      console.error('Error loading images:', error);
      this.showErrorState('Failed to load images. Please try again.');
    } finally {
      this.loading = false;
    }
  }

  /**
   * Load more images for lazy loading
   */
  async loadMoreImages() {
    if (!this.hasMore || this.loading) return;

    this.currentPage++;
    await this.loadImages(true);
  }

  /**
   * Update the image count display
   */
  updateImageCount() {
    const countDisplay = document.getElementById('image-count');
    if (countDisplay) {
      if (this.searchTerm) {
        countDisplay.textContent = `Found ${this.totalImages} image${this.totalImages !== 1 ? 's' : ''}`;
      } else {
        countDisplay.textContent = `${this.totalImages} image${this.totalImages !== 1 ? 's' : ''}`;
      }
    }
  }

  /**
   * Show loading state
   */
  showLoadingState() {
    const container = document.getElementById('image-grid-container');
    if (container) {
      container.innerHTML = `
        <div class="loading-message" style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: #999;">
          <p>Loading images...</p>
        </div>
      `;
    }
  }

  /**
   * Show error state
   */
  showErrorState(message) {
    const container = document.getElementById('image-grid-container');
    if (container) {
      container.innerHTML = `
        <div class="error-message" style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: #999;">
          <p>${this.escapeHtml(message)}</p>
        </div>
      `;
    }
  }

  /**
   * Render images in the grid
   */
  renderImages(append = false) {
    const container = document.getElementById('image-grid-container');
    if (!container) return;

    if (this.images.length === 0 && !append) {
      this.showEmptyState();
      return;
    }

    // Get sentinel element BEFORE clearing (if it exists)
    let sentinel = document.getElementById('lazy-load-sentinel');
    
    if (!append) {
      // If we're not appending, preserve the sentinel before clearing
      if (sentinel && sentinel.parentNode === container) {
        sentinel.remove();
      }
      container.innerHTML = '';
    } else {
      // If appending, just remove sentinel temporarily to re-add at the end
      if (sentinel && sentinel.parentNode === container) {
        sentinel.remove();
      }
    }

    const startIndex = append ? this.images.length - this.pageSize : 0;
    const imagesToRender = append ? this.images.slice(startIndex) : this.images;

    imagesToRender.forEach(image => {
      const imageItem = this.createImageGridItem(image);
      container.appendChild(imageItem);
    });

    // Re-append or recreate sentinel for lazy loading
    if (!sentinel) {
      // Recreate sentinel if it was lost
      sentinel = document.createElement('div');
      sentinel.id = 'lazy-load-sentinel';
      sentinel.style.height = '1px';
      sentinel.style.clear = 'both';
      // Re-observe the new sentinel
      if (this.lazyLoadObserver) {
        this.lazyLoadObserver.observe(sentinel);
      }
    }
    container.appendChild(sentinel);
  }

  /**
   * Show empty state when no images found
   */
  showEmptyState() {
    const container = document.getElementById('image-grid-container');
    if (container) {
      if (this.searchTerm) {
        container.innerHTML = `
          <div class="empty-state" style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: #999;">
            <p>No images found matching "${this.escapeHtml(this.searchTerm)}"</p>
          </div>
        `;
      } else {
        container.innerHTML = `
          <div class="empty-state" style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: #999;">
            <p>No images were found.</p>
          </div>
        `;
      }
    }
  }

  /**
   * Create an image grid item element
   */
  createImageGridItem(image) {
    const cell = document.createElement('div');
    cell.className = 'cell card';

    const browserDiv = document.createElement('div');
    browserDiv.className = 'image-browser';

    const img = document.createElement('img');
    const imageSrc = `${this.config.contextPath}/assets/img/${image.url}`;
    img.src = imageSrc;
    img.dataset.src = imageSrc;
    img.alt = image.filename;
    img.loading = 'lazy';
    img.onclick = () => this.selectImage(imageSrc);

    browserDiv.appendChild(img);
    cell.appendChild(browserDiv);

    const cardSection = document.createElement('div');
    cardSection.className = 'card-section';

    const infoDiv = document.createElement('div');
    
    const filenameSmall = document.createElement('small');
    filenameSmall.textContent = image.filename;
    infoDiv.appendChild(filenameSmall);
    infoDiv.appendChild(document.createElement('br'));

    const dimensionsSmall = document.createElement('small');
    dimensionsSmall.style.color = '#999999';
    dimensionsSmall.textContent = `${image.width}x${image.height}`;
    infoDiv.appendChild(dimensionsSmall);
    infoDiv.appendChild(document.createTextNode(' '));

    const sizeSmall = document.createElement('small');
    sizeSmall.style.color = '#999999';
    sizeSmall.textContent = this.formatFileSize(image.fileLength);
    infoDiv.appendChild(sizeSmall);

    cardSection.appendChild(infoDiv);
    cell.appendChild(cardSection);

    return cell;
  }

  /**
   * Select an image and return it to TinyMCE
   */
  selectImage(imageUrl) {
    if (typeof window.mySubmit === 'function') {
      window.mySubmit(imageUrl);
    }
  }

  /**
   * Format file size
   */
  formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 10) / 10 + ' ' + sizes[i];
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
}
