/**
 * Viewport Manager
 * Manages viewport modes (small, medium, large) and column sizing for different breakpoints
 * 
 * @author matt rajkowski
 * @created 1/4/26 12:00 PM
 */

class ViewportManager {
  constructor(editor) {
    this.editor = editor;
    this.currentViewport = 'small'; // Default to small viewport
    this.viewports = {
      small: { name: 'Small', icon: 'fa-mobile-alt', maxWidth: '600px' },
      medium: { name: 'Medium', icon: 'fa-tablet-alt', maxWidth: '1024px' },
      large: { name: 'Large', icon: 'fa-desktop', maxWidth: '100%' }
    };
    
    // DOM elements
    this.canvas = null;
    this.viewportButtons = {};
  }

  /**
   * Initialize the viewport manager
   */
  init() {
    this.canvas = document.getElementById('editor-canvas');
    this.createViewportControls();
    this.applyViewportStyles();
    console.log('Viewport Manager initialized');
  }

  /**
   * Create viewport toggle controls in the toolbar
   */
  createViewportControls() {
    const toolbar = document.getElementById('editor-toolbar');
    const middleSection = toolbar.querySelector('.toolbar-section.middle');
    
    if (!middleSection) {
      console.error('Middle toolbar section not found');
      return;
    }

    // Create viewport controls container
    const viewportControls = document.createElement('div');
    viewportControls.className = 'viewport-controls';
    viewportControls.style.cssText = `
      display: flex;
      gap: 5px;
      align-items: center;
      margin-left: 20px;
    `;

    // Create viewport buttons
    Object.keys(this.viewports).forEach(viewportKey => {
      const viewport = this.viewports[viewportKey];
      const button = document.createElement('button');
      button.className = `button tiny no-gap radius viewport-btn ${viewportKey === this.currentViewport ? 'active' : 'secondary'}`;
      button.setAttribute('data-viewport', viewportKey);
      button.innerHTML = `<i class="far ${viewport.icon}"></i> ${viewport.name}`;
      button.title = `Switch to ${viewport.name} viewport`;
      
      button.addEventListener('click', () => this.switchViewport(viewportKey));
      
      this.viewportButtons[viewportKey] = button;
      viewportControls.appendChild(button);
    });

    // Add viewport controls to the middle section
    middleSection.appendChild(viewportControls);
  }

  /**
   * Switch to a different viewport
   */
  switchViewport(viewportKey) {
    if (!this.viewports[viewportKey] || viewportKey === this.currentViewport) {
      return;
    }

    console.log(`Switching viewport from ${this.currentViewport} to ${viewportKey}`);
    
    const previousViewport = this.currentViewport;
    this.currentViewport = viewportKey;

    // Update button states
    Object.keys(this.viewportButtons).forEach(key => {
      const button = this.viewportButtons[key];
      if (key === viewportKey) {
        button.classList.remove('secondary');
        button.classList.add('active');
      } else {
        button.classList.remove('active');
        button.classList.add('secondary');
      }
    });

    // Apply viewport styles
    this.applyViewportStyles();

    // Trigger viewport change event
    const event = new CustomEvent('viewportChanged', {
      detail: { 
        previous: previousViewport, 
        current: viewportKey,
        viewport: this.viewports[viewportKey]
      }
    });
    document.dispatchEvent(event);

    console.log(`Viewport switched to ${viewportKey}`);
  }

  /**
   * Apply viewport-specific styles to the canvas
   */
  applyViewportStyles() {
    if (!this.canvas) return;

    const viewport = this.viewports[this.currentViewport];
    
    // Remove existing viewport classes
    this.canvas.classList.remove('viewport-small', 'viewport-medium', 'viewport-large');
    
    // Add current viewport class
    this.canvas.classList.add(`viewport-${this.currentViewport}`);
    
    // Apply max-width constraint to simulate viewport
    if (this.currentViewport === 'small') {
      this.canvas.style.maxWidth = '640px';
      this.canvas.style.margin = '0 auto';
    } else if (this.currentViewport === 'medium') {
      this.canvas.style.maxWidth = '1024px';
      this.canvas.style.margin = '0 auto';
    } else {
      this.canvas.style.maxWidth = '';
      this.canvas.style.margin = '';
    }

    console.log(`Applied viewport styles for ${this.currentViewport}`);
  }

  /**
   * Get the current viewport
   */
  getCurrentViewport() {
    return this.currentViewport;
  }

  /**
   * Get viewport configuration
   */
  getViewportConfig(viewportKey = null) {
    return viewportKey ? this.viewports[viewportKey] : this.viewports[this.currentViewport];
  }

  /**
   * Update column classes based on current viewport when resizing
   */
  updateColumnForViewport(rowId, columnId, newSize) {
    const layoutManager = this.editor.getLayoutManager();
    const row = layoutManager.getRow(rowId);
    if (!row) return false;

    const column = row.columns.find(c => c.id === columnId);
    if (!column) return false;

    // Parse existing classes
    const classes = column.cssClass.split(' ').filter(c => c.trim());
    const sizePrefix = this.currentViewport + '-';
    
    // Remove existing size class for current viewport
    const filteredClasses = classes.filter(c => !c.startsWith(sizePrefix));
    
    // Add new size class for current viewport
    filteredClasses.push(`${sizePrefix}${newSize}`);
    
    // Update column class
    column.cssClass = filteredClasses.join(' ');
    
    console.log(`Updated column ${columnId} for ${this.currentViewport} viewport: ${column.cssClass}`);
    return true;
  }

  /**
   * Get column size for current viewport, respecting Foundation inheritance
   */
  getColumnSize(column) {
    const classes = column.cssClass.split(' ');
    const currentViewport = this.currentViewport;
    
    // Look for current viewport size first
    const currentSizeClass = classes.find(c => c.startsWith(currentViewport + '-'));
    if (currentSizeClass) {
      const match = currentSizeClass.match(/(\w+)-(\d+)/);
      return match ? parseInt(match[2], 10) : 12;
    }
    
    // Follow Foundation inheritance pattern
    if (currentViewport === 'large') {
      // Large inherits from medium, then small
      const mediumClass = classes.find(c => c.startsWith('medium-'));
      if (mediumClass) {
        const match = mediumClass.match(/medium-(\d+)/);
        return match ? parseInt(match[1], 10) : 12;
      }
    }
    
    if (currentViewport === 'medium' || currentViewport === 'large') {
      // Medium and large (if no medium) inherit from small
      const smallClass = classes.find(c => c.startsWith('small-'));
      if (smallClass) {
        const match = smallClass.match(/small-(\d+)/);
        return match ? parseInt(match[1], 10) : 12;
      }
    }
    
    return 12; // Default fallback
  }

  /**
   * Don't automatically add viewport classes - let Foundation handle inheritance
   */
  ensureViewportClasses(column, baseSize = null) {
    // Foundation CSS handles inheritance automatically
    // Only add viewport classes when they're explicitly different
    return false; // No modification needed
  }
}