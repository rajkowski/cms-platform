/**
 * Font Awesome Icon Picker Modal
 * Universal modal for selecting Font Awesome icons
 * 
 * @author AI Assistant
 * @created 1/10/26
 */

class IconPickerModal {
  constructor() {
    this.modal = null;
    this.callback = null;
    this.icons = this.getIconList();
  }

  /**
   * Show the icon picker modal
   * @param {Function} callback - Function to call when icon is selected, receives icon class (e.g., 'fa-home')
   * @param {string} currentIcon - Currently selected icon (optional)
   */
  show(callback, currentIcon = '') {
    this.callback = callback;
    
    if (!this.modal) {
      this.createModal();
    }
    
    // Reset search and display all icons
    const searchInput = this.modal.querySelector('#icon-search');
    if (searchInput) {
      searchInput.value = '';
    }
    this.renderIcons(this.icons, currentIcon);
    
    this.modal.style.display = 'flex';
    
    // Focus on search input
    setTimeout(() => {
      if (searchInput) {
        searchInput.focus();
      }
    }, 100);
  }

  /**
   * Hide the modal
   */
  hide() {
    if (this.modal) {
      this.modal.style.display = 'none';
    }
  }

  /**
   * Create the modal DOM structure
   */
  createModal() {
    this.modal = document.createElement('div');
    this.modal.id = 'icon-picker-modal';
    this.modal.className = 'modal-overlay';
    this.modal.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.5);
      display: none;
      align-items: center;
      justify-content: center;
      z-index: 99999;
    `;
    
    this.modal.innerHTML = `
      <div class="modal-content" style="width: 90%; max-width: 900px; max-height: 80vh; display: flex; flex-direction: column; overflow: hidden; background: #fff; border-radius: 8px; padding: 30px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);">
        <h4 style="margin: 0 0 15px 0; flex-shrink: 0;">Choose an Icon</h4>
        <div style="margin-bottom: 15px; flex-shrink: 0;">
          <input type="text" id="icon-search" class="property-input" placeholder="Search icons..." style="width: 100%; padding: 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 14px;" />
        </div>
        <div id="icon-grid" style="flex: 1; overflow-y: auto; border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #f9f9f9; min-height: 200px;">
          <!-- Icons will be rendered here -->
        </div>
        <div style="text-align: right; margin-top: 15px; display: flex; gap: 10px; justify-content: flex-end; flex-shrink: 0;">
          <button type="button" id="clear-icon-btn" class="button tiny secondary radius">Clear Icon</button>
          <button type="button" id="cancel-icon-picker" class="button tiny secondary radius">Cancel</button>
        </div>
      </div>
    `;
    
    document.body.appendChild(this.modal);
    
    // Set up event listeners
    this.setupEventListeners();
  }

  /**
   * Set up event listeners
   */
  setupEventListeners() {
    // Cancel button
    const cancelBtn = this.modal.querySelector('#cancel-icon-picker');
    if (cancelBtn) {
      cancelBtn.addEventListener('click', () => this.hide());
    }
    
    // Clear icon button
    const clearBtn = this.modal.querySelector('#clear-icon-btn');
    if (clearBtn) {
      clearBtn.addEventListener('click', () => {
        if (this.callback) {
          this.callback('');
        }
        this.hide();
      });
    }
    
    // Search input
    const searchInput = this.modal.querySelector('#icon-search');
    if (searchInput) {
      searchInput.addEventListener('input', (e) => {
        const query = e.target.value.toLowerCase().trim();
        const filtered = query ? this.icons.filter(icon => 
          icon.name.includes(query) || icon.label.toLowerCase().includes(query)
        ) : this.icons;
        this.renderIcons(filtered);
      });
    }
    
    // Close on overlay click
    this.modal.addEventListener('click', (e) => {
      if (e.target === this.modal) {
        this.hide();
      }
    });
  }

  /**
   * Render icons in the grid
   */
  renderIcons(icons, currentIcon = '') {
    const grid = this.modal.querySelector('#icon-grid');
    if (!grid) return;
    
    if (icons.length === 0) {
      grid.innerHTML = '<div style="text-align: center; padding: 40px; color: #999;">No icons found</div>';
      return;
    }
    
    grid.innerHTML = '';
    
    // Create grid container
    const gridContainer = document.createElement('div');
    gridContainer.style.cssText = `
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
      gap: 10px;
    `;
    
    icons.forEach(icon => {
      const iconItem = document.createElement('div');
      const isSelected = currentIcon === icon.class;
      iconItem.className = 'icon-picker-item';
      iconItem.style.cssText = `
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 15px 10px;
        border: 2px solid ${isSelected ? '#007bff' : '#ddd'};
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.2s;
        background: ${isSelected ? '#e7f3ff' : '#fff'};
        min-height: 80px;
      `;
      
      iconItem.innerHTML = `
        <i class="fa ${icon.class}" style="font-size: 24px; margin-bottom: 8px; color: #333;"></i>
        <div style="font-size: 10px; text-align: center; color: #666; word-break: break-word;">${icon.label}</div>
      `;
      
      // Hover effect
      iconItem.addEventListener('mouseenter', () => {
        if (!isSelected) {
          iconItem.style.borderColor = '#007bff';
          iconItem.style.background = '#f0f7ff';
        }
      });
      
      iconItem.addEventListener('mouseleave', () => {
        if (!isSelected) {
          iconItem.style.borderColor = '#ddd';
          iconItem.style.background = '#fff';
        }
      });
      
      // Click handler
      iconItem.addEventListener('click', () => {
        if (this.callback) {
          this.callback(icon.class);
        }
        this.hide();
      });
      
      gridContainer.appendChild(iconItem);
    });
    
    grid.appendChild(gridContainer);
  }

  /**
   * Get list of common Font Awesome icons
   * This is a curated list of commonly used icons
   */
  getIconList() {
    return [
      { name: 'home', label: 'Home', class: 'fa-home' },
      { name: 'user', label: 'User', class: 'fa-user' },
      { name: 'users', label: 'Users', class: 'fa-users' },
      { name: 'cog', label: 'Settings', class: 'fa-cog' },
      { name: 'search', label: 'Search', class: 'fa-search' },
      { name: 'envelope', label: 'Email', class: 'fa-envelope' },
      { name: 'phone', label: 'Phone', class: 'fa-phone' },
      { name: 'calendar', label: 'Calendar', class: 'fa-calendar' },
      { name: 'clock', label: 'Clock', class: 'fa-clock' },
      { name: 'map-marker', label: 'Location', class: 'fa-map-marker-alt' },
      { name: 'heart', label: 'Heart', class: 'fa-heart' },
      { name: 'star', label: 'Star', class: 'fa-star' },
      { name: 'bookmark', label: 'Bookmark', class: 'fa-bookmark' },
      { name: 'tag', label: 'Tag', class: 'fa-tag' },
      { name: 'tags', label: 'Tags', class: 'fa-tags' },
      { name: 'file', label: 'File', class: 'fa-file' },
      { name: 'folder', label: 'Folder', class: 'fa-folder' },
      { name: 'download', label: 'Download', class: 'fa-download' },
      { name: 'upload', label: 'Upload', class: 'fa-upload' },
      { name: 'image', label: 'Image', class: 'fa-image' },
      { name: 'video', label: 'Video', class: 'fa-video' },
      { name: 'music', label: 'Music', class: 'fa-music' },
      { name: 'camera', label: 'Camera', class: 'fa-camera' },
      { name: 'shopping-cart', label: 'Cart', class: 'fa-shopping-cart' },
      { name: 'credit-card', label: 'Credit Card', class: 'fa-credit-card' },
      { name: 'dollar-sign', label: 'Dollar', class: 'fa-dollar-sign' },
      { name: 'chart-bar', label: 'Chart', class: 'fa-chart-bar' },
      { name: 'chart-line', label: 'Line Chart', class: 'fa-chart-line' },
      { name: 'chart-pie', label: 'Pie Chart', class: 'fa-chart-pie' },
      { name: 'database', label: 'Database', class: 'fa-database' },
      { name: 'server', label: 'Server', class: 'fa-server' },
      { name: 'cloud', label: 'Cloud', class: 'fa-cloud' },
      { name: 'lock', label: 'Lock', class: 'fa-lock' },
      { name: 'unlock', label: 'Unlock', class: 'fa-unlock' },
      { name: 'key', label: 'Key', class: 'fa-key' },
      { name: 'shield', label: 'Shield', class: 'fa-shield-alt' },
      { name: 'check', label: 'Check', class: 'fa-check' },
      { name: 'times', label: 'Close', class: 'fa-times' },
      { name: 'plus', label: 'Plus', class: 'fa-plus' },
      { name: 'minus', label: 'Minus', class: 'fa-minus' },
      { name: 'edit', label: 'Edit', class: 'fa-edit' },
      { name: 'trash', label: 'Trash', class: 'fa-trash' },
      { name: 'save', label: 'Save', class: 'fa-save' },
      { name: 'print', label: 'Print', class: 'fa-print' },
      { name: 'share', label: 'Share', class: 'fa-share' },
      { name: 'link', label: 'Link', class: 'fa-link' },
      { name: 'external-link', label: 'External', class: 'fa-external-link-alt' },
      { name: 'arrow-up', label: 'Arrow Up', class: 'fa-arrow-up' },
      { name: 'arrow-down', label: 'Arrow Down', class: 'fa-arrow-down' },
      { name: 'arrow-left', label: 'Arrow Left', class: 'fa-arrow-left' },
      { name: 'arrow-right', label: 'Arrow Right', class: 'fa-arrow-right' },
      { name: 'chevron-up', label: 'Chevron Up', class: 'fa-chevron-up' },
      { name: 'chevron-down', label: 'Chevron Down', class: 'fa-chevron-down' },
      { name: 'chevron-left', label: 'Chevron Left', class: 'fa-chevron-left' },
      { name: 'chevron-right', label: 'Chevron Right', class: 'fa-chevron-right' },
      { name: 'bars', label: 'Menu', class: 'fa-bars' },
      { name: 'ellipsis-h', label: 'More', class: 'fa-ellipsis-h' },
      { name: 'ellipsis-v', label: 'More Vertical', class: 'fa-ellipsis-v' },
      { name: 'info-circle', label: 'Info', class: 'fa-info-circle' },
      { name: 'question-circle', label: 'Question', class: 'fa-question-circle' },
      { name: 'exclamation-circle', label: 'Warning', class: 'fa-exclamation-circle' },
      { name: 'exclamation-triangle', label: 'Alert', class: 'fa-exclamation-triangle' },
      { name: 'bell', label: 'Bell', class: 'fa-bell' },
      { name: 'comment', label: 'Comment', class: 'fa-comment' },
      { name: 'comments', label: 'Comments', class: 'fa-comments' },
      { name: 'thumbs-up', label: 'Like', class: 'fa-thumbs-up' },
      { name: 'thumbs-down', label: 'Dislike', class: 'fa-thumbs-down' },
      { name: 'flag', label: 'Flag', class: 'fa-flag' },
      { name: 'trophy', label: 'Trophy', class: 'fa-trophy' },
      { name: 'gift', label: 'Gift', class: 'fa-gift' },
      { name: 'lightbulb', label: 'Idea', class: 'fa-lightbulb' },
      { name: 'rocket', label: 'Rocket', class: 'fa-rocket' },
      { name: 'globe', label: 'Globe', class: 'fa-globe' },
      { name: 'wifi', label: 'WiFi', class: 'fa-wifi' },
      { name: 'bluetooth', label: 'Bluetooth', class: 'fa-bluetooth' },
      { name: 'mobile', label: 'Mobile', class: 'fa-mobile-alt' },
      { name: 'laptop', label: 'Laptop', class: 'fa-laptop' },
      { name: 'desktop', label: 'Desktop', class: 'fa-desktop' },
      { name: 'tablet', label: 'Tablet', class: 'fa-tablet-alt' },
      { name: 'book', label: 'Book', class: 'fa-book' },
      { name: 'graduation-cap', label: 'Education', class: 'fa-graduation-cap' },
      { name: 'briefcase', label: 'Business', class: 'fa-briefcase' },
      { name: 'building', label: 'Building', class: 'fa-building' },
      { name: 'hospital', label: 'Hospital', class: 'fa-hospital' },
      { name: 'plane', label: 'Plane', class: 'fa-plane' },
      { name: 'car', label: 'Car', class: 'fa-car' },
      { name: 'bicycle', label: 'Bicycle', class: 'fa-bicycle' },
      { name: 'bus', label: 'Bus', class: 'fa-bus' },
      { name: 'train', label: 'Train', class: 'fa-train' },
      { name: 'ship', label: 'Ship', class: 'fa-ship' },
      { name: 'coffee', label: 'Coffee', class: 'fa-coffee' },
      { name: 'utensils', label: 'Food', class: 'fa-utensils' },
      { name: 'pizza-slice', label: 'Pizza', class: 'fa-pizza-slice' },
      { name: 'hamburger', label: 'Burger', class: 'fa-hamburger' },
      { name: 'tree', label: 'Tree', class: 'fa-tree' },
      { name: 'leaf', label: 'Leaf', class: 'fa-leaf' },
      { name: 'sun', label: 'Sun', class: 'fa-sun' },
      { name: 'moon', label: 'Moon', class: 'fa-moon' },
      { name: 'cloud-sun', label: 'Partly Cloudy', class: 'fa-cloud-sun' },
      { name: 'snowflake', label: 'Snow', class: 'fa-snowflake' },
      { name: 'bolt', label: 'Lightning', class: 'fa-bolt' },
      { name: 'fire', label: 'Fire', class: 'fa-fire' },
      { name: 'water', label: 'Water', class: 'fa-water' }
    ];
  }
}

// Create global instance
window.iconPickerModal = new IconPickerModal();
