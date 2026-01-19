/**
 * Font Awesome Icon Picker Modal
 * Universal modal for selecting Font Awesome icons
 * 
 * @author Matt Rajkowski
 * @created 01/10/26 12:00 PM
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
    
    // Add escape key listener when modal is shown
    this.escapeKeyHandler = (e) => {
      console.log('Icon picker - Key pressed:', e.key, 'Modal visible:', this.modal.style.display !== 'none');
      if (e.key === 'Escape' || e.keyCode === 27) {
        e.preventDefault();
        e.stopPropagation();
        console.log('Icon picker - Escape key detected, hiding modal');
        this.hide();
      }
    };
    document.addEventListener('keydown', this.escapeKeyHandler, true);
    
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
    // Remove escape key listener
    if (this.escapeKeyHandler) {
      document.removeEventListener('keydown', this.escapeKeyHandler, true);
      console.log('Icon picker - Escape key listener removed');
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
      { name: 'water', label: 'Water', class: 'fa-water' },
      { name: 'anchor', label: 'Anchor', class: 'fa-anchor' },
      { name: 'atom', label: 'Atom', class: 'fa-atom' },
      { name: 'award', label: 'Award', class: 'fa-award' },
      { name: 'balance-scale', label: 'Balance', class: 'fa-balance-scale' },
      { name: 'band-aid', label: 'Band Aid', class: 'fa-band-aid' },
      { name: 'battery-full', label: 'Battery Full', class: 'fa-battery-full' },
      { name: 'battery-half', label: 'Battery Half', class: 'fa-battery-half' },
      { name: 'battery-empty', label: 'Battery Empty', class: 'fa-battery-empty' },
      { name: 'bed', label: 'Bed', class: 'fa-bed' },
      { name: 'beer', label: 'Beer', class: 'fa-beer' },
      { name: 'binoculars', label: 'Binoculars', class: 'fa-binoculars' },
      { name: 'birthday-cake', label: 'Birthday Cake', class: 'fa-birthday-cake' },
      { name: 'bomb', label: 'Bomb', class: 'fa-bomb' },
      { name: 'bone', label: 'Bone', class: 'fa-bone' },
      { name: 'brain', label: 'Brain', class: 'fa-brain' },
      { name: 'bread-slice', label: 'Bread', class: 'fa-bread-slice' },
      { name: 'broom', label: 'Broom', class: 'fa-broom' },
      { name: 'brush', label: 'Brush', class: 'fa-brush' },
      { name: 'bug', label: 'Bug', class: 'fa-bug' },
      { name: 'calculator', label: 'Calculator', class: 'fa-calculator' },
      { name: 'candy-cane', label: 'Candy Cane', class: 'fa-candy-cane' },
      { name: 'carrot', label: 'Carrot', class: 'fa-carrot' },
      { name: 'cat', label: 'Cat', class: 'fa-cat' },
      { name: 'chess', label: 'Chess', class: 'fa-chess' },
      { name: 'child', label: 'Child', class: 'fa-child' },
      { name: 'clipboard', label: 'Clipboard', class: 'fa-clipboard' },
      { name: 'cloud-download', label: 'Cloud Download', class: 'fa-cloud-download-alt' },
      { name: 'cloud-upload', label: 'Cloud Upload', class: 'fa-cloud-upload-alt' },
      { name: 'cocktail', label: 'Cocktail', class: 'fa-cocktail' },
      { name: 'code', label: 'Code', class: 'fa-code' },
      { name: 'compass', label: 'Compass', class: 'fa-compass' },
      { name: 'cookie', label: 'Cookie', class: 'fa-cookie' },
      { name: 'copy', label: 'Copy', class: 'fa-copy' },
      { name: 'cut', label: 'Cut', class: 'fa-cut' },
      // Business Icons
      { name: 'briefcase', label: 'Briefcase', class: 'fa-briefcase' },
      { name: 'building', label: 'Building', class: 'fa-building' },
      { name: 'city', label: 'City', class: 'fa-city' },
      { name: 'handshake', label: 'Handshake', class: 'fa-handshake' },
      { name: 'chart-line', label: 'Growth Chart', class: 'fa-chart-line' },
      { name: 'chart-bar', label: 'Bar Chart', class: 'fa-chart-bar' },
      { name: 'chart-pie', label: 'Pie Chart', class: 'fa-chart-pie' },
      { name: 'presentation', label: 'Presentation', class: 'fa-chalkboard' },
      { name: 'meeting', label: 'Meeting', class: 'fa-users' },
      { name: 'contract', label: 'Contract', class: 'fa-file-contract' },
      { name: 'signature', label: 'Signature', class: 'fa-signature' },
      { name: 'stamp', label: 'Stamp', class: 'fa-stamp' },
      { name: 'balance-scale', label: 'Legal', class: 'fa-balance-scale' },
      { name: 'gavel', label: 'Gavel', class: 'fa-gavel' },
      { name: 'bullseye', label: 'Target', class: 'fa-bullseye' },
      { name: 'crosshairs', label: 'Focus', class: 'fa-crosshairs' },
      { name: 'rocket', label: 'Launch', class: 'fa-rocket' },
      { name: 'lightbulb', label: 'Innovation', class: 'fa-lightbulb' },
      { name: 'cogs', label: 'Operations', class: 'fa-cogs' },
      { name: 'network-wired', label: 'Network', class: 'fa-network-wired' },
      { name: 'project-diagram', label: 'Strategy', class: 'fa-project-diagram' },
      { name: 'sitemap', label: 'Structure', class: 'fa-sitemap' },
      { name: 'chess', label: 'Strategy', class: 'fa-chess' },
      { name: 'chess-king', label: 'Leadership', class: 'fa-chess-king' },
      { name: 'crown', label: 'Executive', class: 'fa-crown' },
      { name: 'medal', label: 'Achievement', class: 'fa-medal' },
      { name: 'trophy', label: 'Success', class: 'fa-trophy' },
      { name: 'award', label: 'Award', class: 'fa-award' },
      { name: 'certificate', label: 'Certification', class: 'fa-certificate' },
      { name: 'clipboard-check', label: 'Compliance', class: 'fa-clipboard-check' },
      { name: 'shield-alt', label: 'Protection', class: 'fa-shield-alt' },
      { name: 'user-shield', label: 'Security', class: 'fa-user-shield' },
      { name: 'lock', label: 'Secure', class: 'fa-lock' },
      { name: 'key', label: 'Access', class: 'fa-key' },
      { name: 'id-card', label: 'Identity', class: 'fa-id-card' },
      { name: 'passport', label: 'Passport', class: 'fa-passport' },
      { name: 'plane', label: 'Business Travel', class: 'fa-plane' },
      { name: 'suitcase', label: 'Travel', class: 'fa-suitcase' },
      { name: 'hotel', label: 'Hotel', class: 'fa-hotel' },
      { name: 'taxi', label: 'Transportation', class: 'fa-taxi' },
      { name: 'subway', label: 'Transit', class: 'fa-subway' },
      { name: 'map-marked-alt', label: 'Location', class: 'fa-map-marked-alt' },
      { name: 'globe-americas', label: 'Global', class: 'fa-globe-americas' },
      { name: 'language', label: 'Language', class: 'fa-language' },
      { name: 'translate', label: 'Translate', class: 'fa-language' },
      { name: 'phone-alt', label: 'Business Phone', class: 'fa-phone-alt' },
      { name: 'fax', label: 'Fax', class: 'fa-fax' },
      { name: 'envelope-open', label: 'Mail', class: 'fa-envelope-open' },
      { name: 'mail-bulk', label: 'Bulk Mail', class: 'fa-mail-bulk' },
      { name: 'newspaper', label: 'News', class: 'fa-newspaper' },
      { name: 'ad', label: 'Advertisement', class: 'fa-ad' },
      { name: 'bullhorn', label: 'Marketing', class: 'fa-bullhorn' },
      { name: 'broadcast-tower', label: 'Broadcasting', class: 'fa-broadcast-tower' },
      { name: 'satellite', label: 'Satellite', class: 'fa-satellite' },
      { name: 'wifi', label: 'Connectivity', class: 'fa-wifi' },
      { name: 'ethernet', label: 'Ethernet', class: 'fa-ethernet' },
      { name: 'server', label: 'Server', class: 'fa-server' },
      { name: 'database', label: 'Database', class: 'fa-database' },
      { name: 'cloud', label: 'Cloud', class: 'fa-cloud' },
      { name: 'cloud-upload-alt', label: 'Cloud Upload', class: 'fa-cloud-upload-alt' },
      { name: 'cloud-download-alt', label: 'Cloud Download', class: 'fa-cloud-download-alt' },
      { name: 'sync', label: 'Sync', class: 'fa-sync' },
      { name: 'sync-alt', label: 'Refresh', class: 'fa-sync-alt' },
      { name: 'redo', label: 'Redo', class: 'fa-redo' },
      { name: 'undo', label: 'Undo', class: 'fa-undo' },
      { name: 'history', label: 'History', class: 'fa-history' },
      { name: 'clock', label: 'Time', class: 'fa-clock' },
      { name: 'stopwatch', label: 'Timer', class: 'fa-stopwatch' },
      { name: 'hourglass-half', label: 'Processing', class: 'fa-hourglass-half' },
      { name: 'circle-notch', label: 'Progress', class: 'fa-circle-notch' },
      { name: 'tasks', label: 'Project Tasks', class: 'fa-tasks' },
      { name: 'project-diagram', label: 'Project Plan', class: 'fa-project-diagram' },
      { name: 'calendar-alt', label: 'Schedule', class: 'fa-calendar-alt' },
      { name: 'calendar-week', label: 'Weekly View', class: 'fa-calendar-week' },
      { name: 'calendar-day', label: 'Daily View', class: 'fa-calendar-day' },
      { name: 'business-time', label: 'Business Hours', class: 'fa-business-time' },
      { name: 'user-clock', label: 'Time Management', class: 'fa-user-clock' },
      { name: 'user-tie', label: 'Professional', class: 'fa-user-tie' },
      { name: 'user-friends', label: 'Partnership', class: 'fa-user-friends' },
      { name: 'people-carry', label: 'Collaboration', class: 'fa-people-carry' },
      { name: 'hands-helping', label: 'Support', class: 'fa-hands-helping' },
      { name: 'handshake', label: 'Deal', class: 'fa-handshake' },
      { name: 'hand-holding-heart', label: 'Care', class: 'fa-hand-holding-heart' },
      { name: 'thumbs-up', label: 'Approval', class: 'fa-thumbs-up' },
      { name: 'check-circle', label: 'Verified', class: 'fa-check-circle' },
      { name: 'check-double', label: 'Double Check', class: 'fa-check-double' },
      { name: 'clipboard-check', label: 'Audit', class: 'fa-clipboard-check' },
      { name: 'file-alt', label: 'Document', class: 'fa-file-alt' },
      { name: 'file-pdf', label: 'PDF', class: 'fa-file-pdf' },
      { name: 'file-word', label: 'Word Doc', class: 'fa-file-word' },
      { name: 'file-excel', label: 'Excel', class: 'fa-file-excel' },
      { name: 'file-powerpoint', label: 'PowerPoint', class: 'fa-file-powerpoint' },
      { name: 'file-archive', label: 'Archive', class: 'fa-file-archive' },
      { name: 'folder-open', label: 'Open Folder', class: 'fa-folder-open' },
      { name: 'archive', label: 'Archive', class: 'fa-archive' },
      { name: 'box', label: 'Package', class: 'fa-box' },
      { name: 'shipping-fast', label: 'Express', class: 'fa-shipping-fast' },
      { name: 'truck', label: 'Delivery', class: 'fa-truck' },
      { name: 'warehouse', label: 'Storage', class: 'fa-warehouse' },
      { name: 'industry', label: 'Manufacturing', class: 'fa-industry' },
      { name: 'tools', label: 'Maintenance', class: 'fa-tools' },
      { name: 'wrench', label: 'Service', class: 'fa-wrench' },
      { name: 'cog', label: 'Configuration', class: 'fa-cog' },
      { name: 'sliders-h', label: 'Settings', class: 'fa-sliders-h' },
      { name: 'filter', label: 'Filter', class: 'fa-filter' },
      { name: 'sort', label: 'Sort', class: 'fa-sort' },
      { name: 'sort-up', label: 'Sort Ascending', class: 'fa-sort-up' },
      { name: 'sort-down', label: 'Sort Descending', class: 'fa-sort-down' },
      { name: 'search-plus', label: 'Zoom In', class: 'fa-search-plus' },
      { name: 'search-minus', label: 'Zoom Out', class: 'fa-search-minus' },
      { name: 'expand', label: 'Expand', class: 'fa-expand' },
      { name: 'compress', label: 'Compress', class: 'fa-compress' },
      { name: 'arrows-alt', label: 'Full Screen', class: 'fa-arrows-alt' },
      { name: 'external-link-alt', label: 'External Link', class: 'fa-external-link-alt' },
      { name: 'share-alt', label: 'Share', class: 'fa-share-alt' },
      { name: 'share-square', label: 'Share Square', class: 'fa-share-square' },
      { name: 'bookmark', label: 'Bookmark', class: 'fa-bookmark' },
      { name: 'star', label: 'Favorite', class: 'fa-star' },
      { name: 'flag', label: 'Flag', class: 'fa-flag' },
      { name: 'bell', label: 'Notification', class: 'fa-bell' },
      { name: 'exclamation-triangle', label: 'Warning', class: 'fa-exclamation-triangle' },
      { name: 'info-circle', label: 'Information', class: 'fa-info-circle' },
      { name: 'question-circle', label: 'Help', class: 'fa-question-circle' },
      { name: 'life-ring', label: 'Support', class: 'fa-life-ring' },
      { name: 'headset', label: 'Customer Service', class: 'fa-headset' },
      { name: 'phone-volume', label: 'Call Center', class: 'fa-phone-volume' },
      { name: 'comments', label: 'Discussion', class: 'fa-comments' },
      { name: 'comment-dots', label: 'Chat', class: 'fa-comment-dots' },
      { name: 'video', label: 'Video Call', class: 'fa-video' },
      { name: 'microphone', label: 'Audio', class: 'fa-microphone' },
      { name: 'volume-up', label: 'Volume', class: 'fa-volume-up' },
      { name: 'rss', label: 'RSS Feed', class: 'fa-rss' },
      { name: 'podcast', label: 'Podcast', class: 'fa-podcast' },
      { name: 'blog', label: 'Blog', class: 'fa-blog' },
      { name: 'pen', label: 'Write', class: 'fa-pen' },
      { name: 'pen-fancy', label: 'Signature', class: 'fa-pen-fancy' },
      { name: 'highlighter', label: 'Highlight', class: 'fa-highlighter' },
      { name: 'marker', label: 'Marker', class: 'fa-marker' },
      { name: 'sticky-note', label: 'Note', class: 'fa-sticky-note' },
      { name: 'paperclip', label: 'Attachment', class: 'fa-paperclip' },
      { name: 'thumbtack', label: 'Pin', class: 'fa-thumbtack' },
      { name: 'pushpin', label: 'Push Pin', class: 'fa-map-pin' },
      // Finance Icons
      { name: 'coins', label: 'Coins', class: 'fa-coins' },
      { name: 'money-bill', label: 'Money Bill', class: 'fa-money-bill' },
      { name: 'money-bill-wave', label: 'Money Wave', class: 'fa-money-bill-wave' },
      { name: 'euro-sign', label: 'Euro', class: 'fa-euro-sign' },
      { name: 'pound-sign', label: 'Pound', class: 'fa-pound-sign' },
      { name: 'yen-sign', label: 'Yen', class: 'fa-yen-sign' },
      { name: 'receipt', label: 'Receipt', class: 'fa-receipt' },
      { name: 'file-invoice', label: 'Invoice', class: 'fa-file-invoice' },
      { name: 'file-invoice-dollar', label: 'Invoice Dollar', class: 'fa-file-invoice-dollar' },
      { name: 'wallet', label: 'Wallet', class: 'fa-wallet' },
      { name: 'cash-register', label: 'Cash Register', class: 'fa-cash-register' },
      { name: 'hand-holding-usd', label: 'Holding Money', class: 'fa-hand-holding-usd' },
      { name: 'percentage', label: 'Percentage', class: 'fa-percentage' },
      { name: 'chart-area', label: 'Area Chart', class: 'fa-chart-area' },
      { name: 'trending-up', label: 'Trending Up', class: 'fa-chart-line' },
      { name: 'trending-down', label: 'Trending Down', class: 'fa-chart-line' },
      { name: 'exchange-alt', label: 'Exchange', class: 'fa-exchange-alt' },
      { name: 'donate', label: 'Donate', class: 'fa-donate' },
      { name: 'funnel-dollar', label: 'Funnel Dollar', class: 'fa-funnel-dollar' },
      { name: 'money-check', label: 'Check', class: 'fa-money-check' },
      { name: 'money-check-alt', label: 'Check Alt', class: 'fa-money-check-alt' },
      { name: 'landmark', label: 'Bank', class: 'fa-landmark' },
      { name: 'university', label: 'University', class: 'fa-university' },
      { name: 'vault', label: 'Vault', class: 'fa-vault' },
      // HR Icons
      { name: 'user-tie', label: 'Employee', class: 'fa-user-tie' },
      { name: 'user-friends', label: 'Team', class: 'fa-user-friends' },
      { name: 'user-plus', label: 'Add User', class: 'fa-user-plus' },
      { name: 'user-minus', label: 'Remove User', class: 'fa-user-minus' },
      { name: 'user-check', label: 'Approved User', class: 'fa-user-check' },
      { name: 'user-times', label: 'Rejected User', class: 'fa-user-times' },
      { name: 'user-clock', label: 'Time Tracking', class: 'fa-user-clock' },
      { name: 'id-card', label: 'ID Card', class: 'fa-id-card' },
      { name: 'id-badge', label: 'Badge', class: 'fa-id-badge' },
      { name: 'handshake', label: 'Handshake', class: 'fa-handshake' },
      { name: 'clipboard-list', label: 'Checklist', class: 'fa-clipboard-list' },
      { name: 'clipboard-check', label: 'Completed', class: 'fa-clipboard-check' },
      { name: 'tasks', label: 'Tasks', class: 'fa-tasks' },
      { name: 'calendar-check', label: 'Schedule', class: 'fa-calendar-check' },
      { name: 'calendar-times', label: 'Time Off', class: 'fa-calendar-times' },
      { name: 'chalkboard-teacher', label: 'Training', class: 'fa-chalkboard-teacher' },
      { name: 'user-graduate', label: 'Graduate', class: 'fa-user-graduate' },
      { name: 'certificate', label: 'Certificate', class: 'fa-certificate' },
      { name: 'bullhorn', label: 'Announcement', class: 'fa-bullhorn' },
      { name: 'comments-dollar', label: 'Payroll', class: 'fa-comments-dollar' },
      { name: 'user-cog', label: 'User Settings', class: 'fa-user-cog' },
      { name: 'user-shield', label: 'User Security', class: 'fa-user-shield' },
      { name: 'people-carry', label: 'Teamwork', class: 'fa-people-carry' },
      { name: 'sitemap', label: 'Organization', class: 'fa-sitemap' },
      { name: 'project-diagram', label: 'Org Chart', class: 'fa-project-diagram' },
      // Supply Chain Icons
      { name: 'truck', label: 'Truck', class: 'fa-truck' },
      { name: 'shipping-fast', label: 'Fast Shipping', class: 'fa-shipping-fast' },
      { name: 'dolly', label: 'Dolly', class: 'fa-dolly' },
      { name: 'dolly-flatbed', label: 'Flatbed', class: 'fa-dolly-flatbed' },
      { name: 'pallet', label: 'Pallet', class: 'fa-pallet' },
      { name: 'boxes', label: 'Boxes', class: 'fa-boxes' },
      { name: 'box', label: 'Box', class: 'fa-box' },
      { name: 'box-open', label: 'Open Box', class: 'fa-box-open' },
      { name: 'warehouse', label: 'Warehouse', class: 'fa-warehouse' },
      { name: 'industry', label: 'Industry', class: 'fa-industry' },
      { name: 'route', label: 'Route', class: 'fa-route' },
      { name: 'map-marked-alt', label: 'Delivery Map', class: 'fa-map-marked-alt' },
      { name: 'clipboard-list', label: 'Inventory', class: 'fa-clipboard-list' },
      { name: 'barcode', label: 'Barcode', class: 'fa-barcode' },
      { name: 'qrcode', label: 'QR Code', class: 'fa-qrcode' },
      { name: 'weight-hanging', label: 'Weight Scale', class: 'fa-weight-hanging' },
      { name: 'balance-scale-left', label: 'Scale Left', class: 'fa-balance-scale-left' },
      { name: 'balance-scale-right', label: 'Scale Right', class: 'fa-balance-scale-right' },
      { name: 'truck-loading', label: 'Loading', class: 'fa-truck-loading' },
      { name: 'truck-moving', label: 'Moving', class: 'fa-truck-moving' },
      { name: 'people-arrows', label: 'Distribution', class: 'fa-people-arrows' },
      { name: 'stream', label: 'Supply Stream', class: 'fa-stream' },
      // R&D Icons
      { name: 'flask', label: 'Research', class: 'fa-flask' },
      { name: 'vial', label: 'Vial', class: 'fa-vial' },
      { name: 'vials', label: 'Vials', class: 'fa-vials' },
      { name: 'dna', label: 'DNA', class: 'fa-dna' },
      { name: 'microscope', label: 'Microscope', class: 'fa-microscope' },
      { name: 'atom', label: 'Atom', class: 'fa-atom' },
      { name: 'radiation', label: 'Radiation', class: 'fa-radiation' },
      { name: 'radiation-alt', label: 'Radiation Alt', class: 'fa-radiation-alt' },
      { name: 'biohazard', label: 'Biohazard', class: 'fa-biohazard' },
      { name: 'prescription-bottle', label: 'Medicine', class: 'fa-prescription-bottle' },
      { name: 'pills', label: 'Pills', class: 'fa-pills' },
      { name: 'syringe', label: 'Syringe', class: 'fa-syringe' },
      { name: 'stethoscope', label: 'Stethoscope', class: 'fa-stethoscope' },
      { name: 'heartbeat', label: 'Heartbeat', class: 'fa-heartbeat' },
      { name: 'brain', label: 'Brain Research', class: 'fa-brain' },
      { name: 'eye-dropper', label: 'Dropper', class: 'fa-eye-dropper' },
      { name: 'mortar-pestle', label: 'Mortar Pestle', class: 'fa-mortar-pestle' },
      { name: 'seedling', label: 'Growth', class: 'fa-seedling' },
      { name: 'leaf', label: 'Bio Research', class: 'fa-leaf' },
      { name: 'dna', label: 'Genetics', class: 'fa-dna' },
      { name: 'microscope', label: 'Analysis', class: 'fa-microscope' },
      { name: 'chart-line', label: 'Research Data', class: 'fa-chart-line' },
      { name: 'project-diagram', label: 'Research Plan', class: 'fa-project-diagram' },
      { name: 'lightbulb', label: 'Innovation', class: 'fa-lightbulb' },
      // Manufacturing Icons
      { name: 'cogs', label: 'Machinery', class: 'fa-cogs' },
      { name: 'wrench', label: 'Wrench', class: 'fa-wrench' },
      { name: 'screwdriver', label: 'Screwdriver', class: 'fa-screwdriver' },
      { name: 'hammer', label: 'Hammer', class: 'fa-hammer' },
      { name: 'tools', label: 'Tools', class: 'fa-tools' },
      { name: 'toolbox', label: 'Toolbox', class: 'fa-toolbox' },
      { name: 'hard-hat', label: 'Hard Hat', class: 'fa-hard-hat' },
      { name: 'industry', label: 'Factory', class: 'fa-industry' },
      { name: 'oil-can', label: 'Oil Can', class: 'fa-oil-can' },
      { name: 'bolt', label: 'Bolt', class: 'fa-bolt' },
      { name: 'gear', label: 'Gear', class: 'fa-cog' },
      { name: 'gears', label: 'Gears', class: 'fa-cogs' },
      { name: 'robot', label: 'Robot', class: 'fa-robot' },
      { name: 'microchip', label: 'Microchip', class: 'fa-microchip' },
      { name: 'memory', label: 'Memory', class: 'fa-memory' },
      { name: 'hdd', label: 'Hard Drive', class: 'fa-hdd' },
      { name: 'plug', label: 'Plug', class: 'fa-plug' },
      { name: 'battery-three-quarters', label: 'Battery', class: 'fa-battery-three-quarters' },
      { name: 'tachometer-alt', label: 'Performance', class: 'fa-tachometer-alt' },
      { name: 'sliders-h', label: 'Controls', class: 'fa-sliders-h' },
      { name: 'play-circle', label: 'Start Production', class: 'fa-play-circle' },
      { name: 'stop-circle', label: 'Stop Production', class: 'fa-stop-circle' },
      // Website Icons
      { name: 'code', label: 'Code', class: 'fa-code' },
      { name: 'code-branch', label: 'Git Branch', class: 'fa-code-branch' },
      { name: 'github', label: 'GitHub', class: 'fa-github' },
      { name: 'gitlab', label: 'GitLab', class: 'fa-gitlab' },
      { name: 'bitbucket', label: 'Bitbucket', class: 'fa-bitbucket' },
      { name: 'terminal', label: 'Terminal', class: 'fa-terminal' },
      { name: 'window-maximize', label: 'Browser', class: 'fa-window-maximize' },
      { name: 'mobile-alt', label: 'Mobile Web', class: 'fa-mobile-alt' },
      { name: 'tablet-alt', label: 'Tablet Web', class: 'fa-tablet-alt' },
      { name: 'desktop', label: 'Desktop Web', class: 'fa-desktop' },
      { name: 'paint-brush', label: 'Design', class: 'fa-paint-brush' },
      { name: 'palette', label: 'Color Palette', class: 'fa-palette' },
      // Consumer Goods Icons
      { name: 'shopping-bag', label: 'Shopping Bag', class: 'fa-shopping-bag' },
      { name: 'shopping-basket', label: 'Basket', class: 'fa-shopping-basket' },
      { name: 'store', label: 'Store', class: 'fa-store' },
      { name: 'store-alt', label: 'Shop', class: 'fa-store-alt' },
      { name: 'cash-register', label: 'Register', class: 'fa-cash-register' },
      { name: 'receipt', label: 'Receipt', class: 'fa-receipt' },
      { name: 'tags', label: 'Price Tags', class: 'fa-tags' },
      { name: 'percent', label: 'Discount', class: 'fa-percent' },
      { name: 'gift', label: 'Gift', class: 'fa-gift' },
      { name: 'tshirt', label: 'T-Shirt', class: 'fa-tshirt' },
      { name: 'shoe-prints', label: 'Shoes', class: 'fa-shoe-prints' },
      { name: 'glasses', label: 'Glasses', class: 'fa-glasses' },
      { name: 'ring', label: 'Ring', class: 'fa-ring' },
      { name: 'gem', label: 'Jewelry', class: 'fa-gem' },
      { name: 'mobile-alt', label: 'Phone', class: 'fa-mobile-alt' },
      { name: 'laptop', label: 'Laptop', class: 'fa-laptop' },
      { name: 'tv', label: 'TV', class: 'fa-tv' },
      { name: 'gamepad', label: 'Gaming', class: 'fa-gamepad' },
      { name: 'headphones', label: 'Headphones', class: 'fa-headphones' },
      { name: 'camera', label: 'Camera', class: 'fa-camera' },
      { name: 'blender', label: 'Blender', class: 'fa-blender' },
      { name: 'couch', label: 'Furniture', class: 'fa-couch' },
      { name: 'bed', label: 'Bed', class: 'fa-bed' },
      { name: 'chair', label: 'Chair', class: 'fa-chair' },
      { name: 'bath', label: 'Bath', class: 'fa-bath' },
      // File and User Icons
      { name: 'file', label: 'File', class: 'fa-file' },
      { name: 'file-circle-plus', label: 'Add File', class: 'fa-file-circle-plus' },
      { name: 'file-alt', label: 'Text File', class: 'fa-file-alt' },
      { name: 'file-pdf', label: 'PDF File', class: 'fa-file-pdf' },
      { name: 'file-word', label: 'Word Document', class: 'fa-file-word' },
      { name: 'file-excel', label: 'Excel File', class: 'fa-file-excel' },
      { name: 'file-powerpoint', label: 'PowerPoint', class: 'fa-file-powerpoint' },
      { name: 'file-image', label: 'Image File', class: 'fa-file-image' },
      { name: 'file-video', label: 'Video File', class: 'fa-file-video' },
      { name: 'file-audio', label: 'Audio File', class: 'fa-file-audio' },
      { name: 'file-archive', label: 'Archive File', class: 'fa-file-archive' },
      { name: 'file-code', label: 'Code File', class: 'fa-file-code' },
      { name: 'file-csv', label: 'CSV File', class: 'fa-file-csv' },
      { name: 'file-download', label: 'Download File', class: 'fa-file-download' },
      { name: 'file-upload', label: 'Upload File', class: 'fa-file-upload' },
      { name: 'file-import', label: 'Import File', class: 'fa-file-import' },
      { name: 'file-export', label: 'Export File', class: 'fa-file-export' },
      { name: 'file-medical', label: 'Medical File', class: 'fa-file-medical' },
      { name: 'file-contract', label: 'Contract', class: 'fa-file-contract' },
      { name: 'file-signature', label: 'Signature File', class: 'fa-file-signature' },
      { name: 'folder', label: 'Folder', class: 'fa-folder' },
      { name: 'folder-open', label: 'Open Folder', class: 'fa-folder-open' },
      { name: 'folder-plus', label: 'Add Folder', class: 'fa-folder-plus' },
      { name: 'folder-minus', label: 'Remove Folder', class: 'fa-folder-minus' },
      { name: 'folder-tree', label: 'Folder Tree', class: 'fa-folder-tree' },
      { name: 'copy', label: 'Copy', class: 'fa-copy' },
      { name: 'paste', label: 'Paste', class: 'fa-paste' },
      { name: 'cut', label: 'Cut', class: 'fa-cut' },
      { name: 'user', label: 'User', class: 'fa-user' },
      { name: 'user-plus', label: 'Add User', class: 'fa-user-plus' },
      { name: 'user-minus', label: 'Remove User', class: 'fa-user-minus' },
      { name: 'user-times', label: 'Delete User', class: 'fa-user-times' },
      { name: 'user-check', label: 'Verified User', class: 'fa-user-check' },
      { name: 'user-edit', label: 'Edit User', class: 'fa-user-edit' },
      { name: 'user-cog', label: 'User Settings', class: 'fa-user-cog' },
      { name: 'user-lock', label: 'Locked User', class: 'fa-user-lock' },
      { name: 'user-shield', label: 'Protected User', class: 'fa-user-shield' },
      { name: 'user-secret', label: 'Secret User', class: 'fa-user-secret' },
      { name: 'user-tie', label: 'Business User', class: 'fa-user-tie' },
      { name: 'user-graduate', label: 'Graduate', class: 'fa-user-graduate' },
      { name: 'user-md', label: 'Doctor', class: 'fa-user-md' },
      { name: 'user-nurse', label: 'Nurse', class: 'fa-user-nurse' },
      { name: 'user-injured', label: 'Injured User', class: 'fa-user-injured' },
      { name: 'user-astronaut', label: 'Astronaut', class: 'fa-user-astronaut' },
      { name: 'user-ninja', label: 'Ninja', class: 'fa-user-ninja' },
      { name: 'users', label: 'Users Group', class: 'fa-users' },
      { name: 'users-cog', label: 'Manage Users', class: 'fa-users-cog' },
      { name: 'user-friends', label: 'Friends', class: 'fa-user-friends' },
      { name: 'user-circle', label: 'User Circle', class: 'fa-user-circle' },
      { name: 'address-book', label: 'Address Book', class: 'fa-address-book' },
      { name: 'address-card', label: 'Contact Card', class: 'fa-address-card' },
      { name: 'id-card', label: 'ID Card', class: 'fa-id-card' },
      { name: 'id-badge', label: 'ID Badge', class: 'fa-id-badge' },
      // Fun
      { name: 'crown', label: 'Crown', class: 'fa-crown' },
      { name: 'cube', label: 'Cube', class: 'fa-cube' },
      { name: 'cubes', label: 'Cubes', class: 'fa-cubes' },
      { name: 'dice', label: 'Dice', class: 'fa-dice' },
      { name: 'dog', label: 'Dog', class: 'fa-dog' },
      { name: 'dove', label: 'Dove', class: 'fa-dove' },
      { name: 'dragon', label: 'Dragon', class: 'fa-dragon' },
      { name: 'drum', label: 'Drum', class: 'fa-drum' },
      { name: 'dumbbell', label: 'Dumbbell', class: 'fa-dumbbell' },
      { name: 'egg', label: 'Egg', class: 'fa-egg' },
      { name: 'eye', label: 'Eye', class: 'fa-eye' },
      { name: 'eye-slash', label: 'Eye Slash', class: 'fa-eye-slash' },
      { name: 'feather', label: 'Feather', class: 'fa-feather' },
      { name: 'fingerprint', label: 'Fingerprint', class: 'fa-fingerprint' },
      { name: 'fish', label: 'Fish', class: 'fa-fish' },
      { name: 'fist-raised', label: 'Fist', class: 'fa-fist-raised' },
      { name: 'flask', label: 'Flask', class: 'fa-flask' },
      { name: 'flower', label: 'Flower', class: 'fa-seedling' },
      { name: 'football-ball', label: 'Football', class: 'fa-football-ball' },
      { name: 'frog', label: 'Frog', class: 'fa-frog' },
      { name: 'gamepad', label: 'Gamepad', class: 'fa-gamepad' },
      { name: 'gem', label: 'Gem', class: 'fa-gem' },
      { name: 'ghost', label: 'Ghost', class: 'fa-ghost' },
      { name: 'glasses', label: 'Glasses', class: 'fa-glasses' },
      { name: 'guitar', label: 'Guitar', class: 'fa-guitar' },
      { name: 'hammer', label: 'Hammer', class: 'fa-hammer' },
      { name: 'hand-paper', label: 'Hand Paper', class: 'fa-hand-paper' },
      { name: 'hand-peace', label: 'Peace', class: 'fa-hand-peace' },
      { name: 'hand-point-up', label: 'Point Up', class: 'fa-hand-point-up' },
      { name: 'hand-rock', label: 'Rock', class: 'fa-hand-rock' },
      { name: 'hand-scissors', label: 'Scissors', class: 'fa-hand-scissors' },
      { name: 'hat-wizard', label: 'Wizard Hat', class: 'fa-hat-wizard' },
      { name: 'headphones', label: 'Headphones', class: 'fa-headphones' },
      { name: 'hiking', label: 'Hiking', class: 'fa-hiking' },
      { name: 'hippo', label: 'Hippo', class: 'fa-hippo' },
      { name: 'horse', label: 'Horse', class: 'fa-horse' },
      { name: 'hourglass', label: 'Hourglass', class: 'fa-hourglass' },
      { name: 'ice-cream', label: 'Ice Cream', class: 'fa-ice-cream' },
      { name: 'igloo', label: 'Igloo', class: 'fa-igloo' },
      { name: 'infinity', label: 'Infinity', class: 'fa-infinity' },
      { name: 'kiwi-bird', label: 'Kiwi', class: 'fa-kiwi-bird' },
      { name: 'lemon', label: 'Lemon', class: 'fa-lemon' },
      { name: 'magic', label: 'Magic', class: 'fa-magic' },
      { name: 'magnet', label: 'Magnet', class: 'fa-magnet' },
      { name: 'mask', label: 'Mask', class: 'fa-mask' },
      { name: 'medal', label: 'Medal', class: 'fa-medal' },
      { name: 'microphone', label: 'Microphone', class: 'fa-microphone' },
      { name: 'microscope', label: 'Microscope', class: 'fa-microscope' },
      { name: 'mountain', label: 'Mountain', class: 'fa-mountain' },
      { name: 'mouse', label: 'Mouse', class: 'fa-mouse' },
      { name: 'mug-hot', label: 'Hot Mug', class: 'fa-mug-hot' },
      { name: 'palette', label: 'Palette', class: 'fa-palette' },
      { name: 'paper-plane', label: 'Paper Plane', class: 'fa-paper-plane' },
      { name: 'paw', label: 'Paw', class: 'fa-paw' },
      { name: 'pepper-hot', label: 'Hot Pepper', class: 'fa-pepper-hot' },
      { name: 'piggy-bank', label: 'Piggy Bank', class: 'fa-piggy-bank' },
      { name: 'puzzle-piece', label: 'Puzzle', class: 'fa-puzzle-piece' },
      { name: 'rainbow', label: 'Rainbow', class: 'fa-rainbow' },
      { name: 'robot', label: 'Robot', class: 'fa-robot' },
      { name: 'running', label: 'Running', class: 'fa-running' },
      { name: 'seedling', label: 'Seedling', class: 'fa-seedling' },
      { name: 'skull', label: 'Skull', class: 'fa-skull' },
      { name: 'smile', label: 'Smile', class: 'fa-smile' },
      { name: 'snowman', label: 'Snowman', class: 'fa-snowman' },
      { name: 'spider', label: 'Spider', class: 'fa-spider' },
      { name: 'stopwatch', label: 'Stopwatch', class: 'fa-stopwatch' },
      { name: 'swimming-pool', label: 'Pool', class: 'fa-swimming-pool' },
      { name: 'thermometer', label: 'Thermometer', class: 'fa-thermometer-half' },
      { name: 'toilet-paper', label: 'Toilet Paper', class: 'fa-toilet-paper' },
      { name: 'tools', label: 'Tools', class: 'fa-tools' },
      { name: 'tooth', label: 'Tooth', class: 'fa-tooth' },
      { name: 'tornado', label: 'Tornado', class: 'fa-tornado' },
      { name: 'umbrella', label: 'Umbrella', class: 'fa-umbrella' },
      { name: 'virus', label: 'Virus', class: 'fa-virus' },
      { name: 'volcano', label: 'Volcano', class: 'fa-volcano' },
      { name: 'weight', label: 'Weight', class: 'fa-weight' },
      { name: 'wine-glass', label: 'Wine', class: 'fa-wine-glass' },
      { name: 'yin-yang', label: 'Yin Yang', class: 'fa-yin-yang' },
    ];
  }
}

// Create global instance
window.iconPickerModal = new IconPickerModal();
