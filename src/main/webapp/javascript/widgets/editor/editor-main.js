/**
 * Main Page Editor Controller
 * Coordinates all editor modules and manages the overall editing workflow
 * 
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */

class PageEditor {
  constructor(config) {
    this.config = config;
    this.dragDropManager = new DragDropManager(this);
    this.widgetRegistry = new WidgetRegistry();
    this.layoutManager = new LayoutManager(this, this.widgetRegistry);
    this.canvasController = new CanvasController(this);
    this.propertiesPanel = new PropertiesPanel(this);
    this.pagesTabManager = new PagesTabManager(this);
    this.viewportManager = new ViewportManager(this);
    this.rightPanelTabs = new RightPanelTabs(this);
    this.infoTabManager = new InfoTabManager(this, this.rightPanelTabs);
    this.cssTabManager = new CSSTabManager(this, this.rightPanelTabs);
    this.xmlTabManager = new XMLTabManager(this, this.rightPanelTabs);
    
    // History management for undo/redo
    this.history = [];
    this.historyIndex = -1;
    this.maxHistorySize = 50;
    
    // Track the saved state for dirty detection
    this.lastSavedState = null;
    
    // References to DOM elements
    this.elements = {};
  }
  
  /**
   * Initialize the editor
   */
  init() {
    console.log('Initializing Visual Page Editor...');
    
    // Cache DOM element references
    this.cacheElements();
    
    // Populate the widget palette
    this.populateWidgetPalette();
    
    // Initialize sub-modules
    this.dragDropManager.init();
    this.canvasController.init();
    this.propertiesPanel.init();
    this.pagesTabManager.init();
    this.viewportManager.init();
    this.rightPanelTabs.init();
    this.infoTabManager.init();
    this.cssTabManager.init();
    this.xmlTabManager.init();
    
    // Connect PropertiesPanel to RightPanelTabs
    this.propertiesPanel.setRightPanelTabs(this.rightPanelTabs);
    
    // Set up event listeners
    this.setupEventListeners();
    
    // Load existing layout if available
    if (this.config.hasExistingLayout && this.config.existingXml) {
      this.loadExistingLayout(this.config.existingXml);
    }
    
    // Load initial page data for Info, CSS, and XML tabs
    // Only load if we have a webPageLink and it's not a new page (webPageId > 0)
    if (this.config.webPageLink) {
      const isNewPage = !this.config.webPageId || this.config.webPageId <= 0;
      
      if (isNewPage) {
        // For new pages, set up the pages tab manager to know it's a new page
        this.pagesTabManager.selectedPageId = 'new';
        this.pagesTabManager.selectedPageLink = this.config.webPageLink;
        console.log('Editor initialized for new page:', this.config.webPageLink);
      }
      
      // Load Info tab data (handles both new and existing pages)
      this.infoTabManager.loadPageInfo(this.config.webPageLink);
      // Load CSS tab data (handles both new and existing pages)
      this.cssTabManager.loadStylesheet(this.config.webPageLink);
      // Update XML tab with initial canvas data
      setTimeout(() => {
        this.xmlTabManager.updateFromCanvas();
      }, 100);
    }
    
    // Save initial state to history
    this.saveToHistory();
    
    // Set baseline for dirty detection AFTER loading initial data
    // This ensures the Publish button is only active for new pages or when changes are made
    setTimeout(() => {
      this.setSavedState();
      this.updateSaveIndicator();
    }, 200);
    
    console.log('Editor initialized successfully');
  }
  
  /**
   * Update the save button indicator to show unsaved changes
   */
  updateSaveIndicator() {
    const isNewPage = this.pagesTabManager?.selectedPageId === 'new';
    const isDirty = this.isDirty();
    
    // For new pages, always show the Publish button as active (they need to be saved)
    // For existing pages, only show active when there are changes
    if (isNewPage || isDirty) {
      if (!this.elements.saveBtn.dataset.dirty) {
        this.elements.saveBtn.dataset.dirty = 'true';
        this.elements.saveBtn.innerHTML = '<i class="far fa-circle-dot"></i> Publish <span style="color: #ff6b6b;">●</span>';
      }
    } else {
      if (this.elements.saveBtn.dataset.dirty) {
        delete this.elements.saveBtn.dataset.dirty;
        this.elements.saveBtn.innerHTML = '<i class="fa-solid fa-save"></i> Publish';
      }
    }
  }

  /**
   * Show the loading indicator in the toolbar
   */
  showLoadingIndicator(text = 'Loading...') {
    if (this.elements.loadingIndicator) {
      this.elements.loadingIndicator.style.display = 'inline-block';
    }
  }

  /**
   * Hide the loading indicator in the toolbar
   */
  hideLoadingIndicator() {
    if (this.elements.loadingIndicator) {
      this.elements.loadingIndicator.style.display = 'none';
    }
  }
  
  /**
   * Cache references to frequently accessed DOM elements
   */
  cacheElements() {
    this.elements = {
      canvas: document.getElementById('editor-canvas'),
      palette: document.getElementById('widget-palette'),
      properties: document.getElementById('properties-panel'),
      toolbar: document.getElementById('editor-toolbar'),
      addPageBtn: document.getElementById('add-page-btn'),
      addRowBtn: document.getElementById('add-row-btn'),
      undoBtn: document.getElementById('undo-btn'),
      redoBtn: document.getElementById('redo-btn'),
      saveBtn: document.getElementById('save-btn'),
      widgetSearch: document.getElementById('widget-search'),
      editorForm: document.getElementById('editor-form'),
      designerData: document.getElementById('designer-data'),
      loadingIndicator: document.getElementById('loading-indicator'),
    };
  }
  
  /**
   * Set up all event listeners
   */
  setupEventListeners() {
    // Toolbar buttons
    this.elements.addPageBtn.addEventListener('click', () => this.handleAddPage());
    this.elements.addRowBtn.addEventListener('click', () => this.addRow());
    this.elements.undoBtn.addEventListener('click', () => this.undo());
    this.elements.redoBtn.addEventListener('click', () => this.redo());
    this.elements.saveBtn.addEventListener('click', () => this.save());
    
    // Widget search
    this.elements.widgetSearch.addEventListener('input', (e) => this.filterWidgets(e.target.value));
    
    // Keyboard shortcuts
    document.addEventListener('keydown', (e) => this.handleKeyboardShortcuts(e));

    // Click handler for initial empty canvas
    const emptyCanvas = this.elements.canvas.querySelector('.empty-canvas');
    if (emptyCanvas) {
      emptyCanvas.addEventListener('click', () => this.addRow());
    }
  }
  
  /**
   * Populate the widget palette from the registry
   */
  populateWidgetPalette() {
    const container = document.getElementById('widget-list-container');
    if (!container) return;

    const categories = this.widgetRegistry.getCategories();
    let html = '';

    categories.forEach(category => {
      html += `<div class="widget-palette-category">${category}</div>`;
      const widgets = this.widgetRegistry.getByCategory(category);
      
      widgets.forEach(widget => {
        // Find the widget type (key) for the given widget definition
        let widgetType = null;
        for (let [key, value] of this.widgetRegistry.widgets.entries()) {
          if (value === widget) {
            widgetType = key;
            break;
          }
        }

        if (widgetType) {
          html += `
            <div class="widget-palette-item" draggable="true" data-widget-type="${widgetType}">
              <i class="far ${widget.icon || 'fa-puzzle-piece'}"></i> <strong>${widget.name}</strong>
              <div style="font-size: 11px; color: #6c757d;">${widget.description || ''}</div>
            </div>
          `;
        }
      });
    });

    container.innerHTML = html;
    
    // Re-initialize drag and drop for the new items
    this.dragDropManager.initPaletteItems();
  }

  /**
   * Create a new page and switch to editing it
   */
  createNewPage(title, link) {
    console.log('Creating new page:', title, link);
    
    // Store the title for later use
    this.newPageTitle = title;
    
    // Update the editor to work on the new page
    this.config.webPageLink = link;
    this.config.existingXml = '';
    this.config.hasExistingLayout = false;
    
    // Clear current layout
    this.layoutManager.structure = { rows: [] };
    
    // Update the pages tab to show the new page with title
    this.pagesTabManager.selectedPageId = 'new';
    this.pagesTabManager.selectedPageLink = link;
    
    // Re-render the pages list to include the new page with title
    const currentPages = this.pagesTabManager.pages || [];
    this.pagesTabManager.renderPageList(currentPages);
    
    // Set up empty canvas for new page
    const canvas = this.elements.canvas;
    canvas.innerHTML = `
      <div class="empty-canvas" style="cursor: pointer;">
        <i class="far fa-plus-circle fa-3x margin-bottom-10"></i>
        <h5>Start Building "${this.escapeHtml(title)}"</h5>
        <p>Click "Add Row" to begin or drag widgets from the palette</p>
      </div>
    `;
    
    // Set up click handler for empty canvas
    const emptyCanvas = canvas.querySelector('.empty-canvas');
    if (emptyCanvas) {
      emptyCanvas.addEventListener('click', () => this.addRow());
    }
    
    // Reset history for new page
    this.history = [];
    this.historyIndex = -1;
    this.saveToHistory();
    
    // Set baseline for dirty detection
    this.setSavedState();
    
    // Update save indicator
    this.updateSaveIndicator();
    
    // Show loading indicator briefly for feedback
    this.showLoadingIndicator('Setting up new page...');
    setTimeout(() => {
      this.hideLoadingIndicator();
    }, 500);
    
    // Dispatch page changed event
    document.dispatchEvent(new CustomEvent('pageChanged', { detail: { pageLink: link } }));
    
    console.log('New page created and ready for editing');
  }

  /**
   * Handle the Add Page button click
   */
  async handleAddPage() {
    console.log('Add Page button clicked');
    
    // Check if editor has unsaved changes
    if (this.isDirty()) {
      console.log('Editor has unsaved changes, showing confirmation dialog');
      const confirmed = await this.showConfirmDialog('You have unsaved changes. Are you sure you want to create a new page?');
      console.log('Confirmation result:', confirmed);
      
      if (!confirmed) {
        console.log('User cancelled, not showing Add Page modal');
        return;
      }
      
      console.log('User confirmed, proceeding to show Add Page modal');
    }
    
    // Small delay to ensure any previous modals are fully cleaned up
    setTimeout(() => {
      // Trigger the modal show function (defined in the JSP)
      if (typeof window.showAddPageModal === 'function') {
        console.log('Calling showAddPageModal');
        window.showAddPageModal();
      } else {
        console.error('showAddPageModal function not found');
      }
    }, 50);
  }

  /**
   * Add a new row to the canvas
   */
  async addRow(columnLayout = null) {
    console.log('Adding new row...');
    
    try {
      if (!columnLayout) {
        columnLayout = await this.showColumnLayoutPicker();
      }
      
      if (!columnLayout) {
        console.log('Row addition cancelled.');
        return; // User cancelled or no layout provided
      }
      
      // Create row in layout manager
      const rowId = this.layoutManager.addRow(columnLayout);
      
      // If this is the first row, re-render the whole layout to clear the placeholder
      if (this.layoutManager.getStructure().rows.length === 1) {
        this.canvasController.renderLayout(this.layoutManager.getStructure());
      } else {
        // Otherwise, just render the new row for better performance
        this.canvasController.renderRow(rowId, this.layoutManager.getRow(rowId));
      }
      
      // Save state
      this.saveToHistory();
      
    } catch (error) {
      console.error('Error adding row:', error);
      alert('Error adding row: ' + error.message);
    }
  }

  /**
   * Show a custom alert dialog
   * @param {string} message - The alert message
   * @returns {Promise<void>} A promise that resolves when the dialog is closed
   */
  showAlertDialog(message) {
    return new Promise((resolve) => {
      // Create modal overlay
      const modalOverlay = document.createElement('div');
      modalOverlay.className = 'modal-overlay active';
      modalOverlay.style.cssText = `
        z-index: 10000;
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
      `;

      // Create modal content
      const modalContent = document.createElement('div');
      modalContent.className = 'modal-content';
      modalContent.style.cssText = `
        max-width: 400px;
        background: white;
        padding: 30px;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        text-align: center;
      `;

      // Add message
      const messageEl = document.createElement('p');
      messageEl.textContent = message;
      messageEl.style.cssText = 'margin: 0 0 20px 0; font-size: 16px; line-height: 1.4;';
      modalContent.appendChild(messageEl);

      // OK button
      const okBtn = document.createElement('button');
      okBtn.textContent = 'OK';
      okBtn.className = 'btn btn-primary';
      okBtn.style.cssText = 'padding: 8px 16px; border: none; background: #007bff; color: white; cursor: pointer; border-radius: 4px;';

      const closeHandler = () => {
        document.body.removeChild(modalOverlay);
        document.removeEventListener('keydown', escHandler);
        resolve();
      };

      okBtn.addEventListener('click', closeHandler);
      modalContent.appendChild(okBtn);
      modalOverlay.appendChild(modalContent);

      // Handler for the Escape key
      const escHandler = (e) => {
        if (e.key === 'Escape') {
          closeHandler();
        }
      };

      // Close on overlay click
      modalOverlay.addEventListener('click', (e) => {
        if (e.target === modalOverlay) {
          closeHandler();
        }
      });

      // Add to DOM and show
      document.body.appendChild(modalOverlay);
      document.addEventListener('keydown', escHandler);
    });
  }

  /**
   * Show a custom confirm dialog
   * @param {string} message - The confirmation message
   * @returns {Promise<boolean>} A promise that resolves with true if confirmed, false if cancelled
   */
  showConfirmDialog(message) {
    return new Promise((resolve) => {
      // Create modal overlay with unique ID to ensure we can find and remove it
      const modalOverlay = document.createElement('div');
      const modalId = 'confirm-dialog-' + Date.now();
      modalOverlay.id = modalId;
      modalOverlay.className = 'modal-overlay active confirm-dialog-modal';
      modalOverlay.style.cssText = `
        z-index: 10001;
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
      `;

      // Create modal content
      const modalContent = document.createElement('div');
      modalContent.className = 'modal-content';
      modalContent.style.cssText = `
        max-width: 400px;
        background: var(--editor-bg);
        color: var(--editor-text);
        padding: 30px;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        text-align: center;
        position: relative;
        z-index: 10002;
      `;

      // Add message
      const messageEl = document.createElement('p');
      messageEl.textContent = message;
      messageEl.style.cssText = 'margin: 0 0 20px 0; font-size: 16px; line-height: 1.4; color: var(--editor-text);';
      modalContent.appendChild(messageEl);

      // Create button container
      const buttonContainer = document.createElement('div');
      buttonContainer.style.cssText = 'display: flex; gap: 10px; justify-content: center;';

      // Cancel button
      const cancelBtn = document.createElement('button');
      cancelBtn.textContent = 'Cancel';
      cancelBtn.className = 'btn btn-secondary';
      cancelBtn.style.cssText = 'padding: 8px 16px; border: 1px solid var(--editor-border); background: var(--editor-panel-bg); color: var(--editor-text); cursor: pointer; border-radius: 4px;';

      // Confirm button
      const confirmBtn = document.createElement('button');
      confirmBtn.textContent = 'Confirm';
      confirmBtn.className = 'btn btn-danger';
      confirmBtn.style.cssText = 'padding: 8px 16px; border: none; background: #dc3545; color: white; cursor: pointer; border-radius: 4px;';

      let isResolved = false; // Prevent multiple resolutions

      const closeHandler = (confirmed) => {
        if (isResolved) return; // Prevent multiple calls
        isResolved = true;
        
        console.log('Closing confirm dialog with result:', confirmed);
        
        // Remove event listeners first
        document.removeEventListener('keydown', escHandler);
        cancelBtn.removeEventListener('click', cancelHandler);
        confirmBtn.removeEventListener('click', confirmHandler);
        modalOverlay.removeEventListener('click', overlayClickHandler);
        
        // Find and remove the modal by ID
        const modalToRemove = document.getElementById(modalId);
        if (modalToRemove && document.body.contains(modalToRemove)) {
          document.body.removeChild(modalToRemove);
          console.log('Confirm dialog removed from DOM');
        }
        
        // Also try to remove by class name as backup
        const confirmDialogs = document.querySelectorAll('.confirm-dialog-modal');
        confirmDialogs.forEach(dialog => {
          if (document.body.contains(dialog)) {
            document.body.removeChild(dialog);
          }
        });
        
        // Small delay to ensure DOM cleanup before resolving
        setTimeout(() => {
          resolve(confirmed);
        }, 10);
      };

      // Define handlers separately to ensure proper cleanup
      const cancelHandler = () => closeHandler(false);
      const confirmHandler = () => closeHandler(true);
      const overlayClickHandler = (e) => {
        if (e.target === modalOverlay) {
          closeHandler(false);
        }
      };

      // Handler for the Escape key
      const escHandler = (e) => {
        if (e.key === 'Escape') {
          closeHandler(false);
        }
      };

      // Add event listeners
      cancelBtn.addEventListener('click', cancelHandler);
      confirmBtn.addEventListener('click', confirmHandler);
      modalOverlay.addEventListener('click', overlayClickHandler);
      document.addEventListener('keydown', escHandler);

      buttonContainer.appendChild(cancelBtn);
      buttonContainer.appendChild(confirmBtn);
      modalContent.appendChild(buttonContainer);
      modalOverlay.appendChild(modalContent);

      // Add to DOM and show
      document.body.appendChild(modalOverlay);
      
      // Focus the confirm button for better UX
      setTimeout(() => {
        if (!isResolved) {
          confirmBtn.focus();
        }
      }, 100);
      
      console.log('Confirm dialog created and added to DOM');
    });
  }

  /**
   * Show a modal to select column layout
   * @returns {Promise<Array|null>} A promise that resolves with the selected layout or null if cancelled
   */
  showColumnLayoutPicker() {
    return new Promise((resolve) => {
      // Define layouts
      const layouts = [
        { name: '1 Column', classes: ['small-12'] },
        { name: '2 Columns', classes: ['small-6', 'small-6'] },
        { name: '3 Columns', classes: ['small-4', 'small-4', 'small-4'] },
        { name: '4 Columns', classes: ['small-3', 'small-3', 'small-3', 'small-3'] },
        { name: '33 / 67', classes: ['small-4', 'small-8'] },
        { name: '67 / 33', classes: ['small-8', 'small-4'] },
        { name: '25 / 75', classes: ['small-3', 'small-9'] },
        { name: '75 / 25', classes: ['small-9', 'small-3'] }
      ];

      // Create modal overlay
      const modalOverlay = document.createElement('div');
      modalOverlay.className = 'modal-overlay active';
      modalOverlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0,0,0,0.5);
        display: flex;
        z-index: 10000;
        justify-content: center;
        align-items: center;
      `;

      // Create modal content
      const modalContent = document.createElement('div');
      modalContent.className = 'modal-content';
      modalContent.style.cssText = `
        background: white;
        padding: 30px;
        border-radius: 8px;
        box-shadow: 0 5px 15px rgba(0,0,0,0.3);
        width: 90%;
        max-width: 600px;
      `;

      // Add title
      const title = document.createElement('h4');
      title.textContent = 'Select Column Layout';
      title.style.marginTop = '0';
      modalContent.appendChild(title);

      // Create layout picker container
      const layoutPicker = document.createElement('div');
      layoutPicker.className = 'layout-picker';
      layoutPicker.style.cssText = `
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
        gap: 20px;
      `;

      // Add layout options
      layouts.forEach(layout => {
        const option = document.createElement('div');
        option.className = 'layout-option';
        option.style.cssText = `
          border: 2px solid #dee2e6;
          border-radius: 4px;
          padding: 15px;
          cursor: pointer;
          transition: all 0.2s;
          text-align: center;
        `;

        // Create preview
        const preview = document.createElement('div');
        preview.className = 'layout-preview';
        preview.style.cssText = `
          display: flex;
          gap: 5px;
          height: 40px;
          margin-bottom: 10px;
        `;

        layout.classes.forEach(cssClass => {
          const col = document.createElement('div');
          col.className = 'layout-preview-col';
          col.style.cssText = `
            background: #ced4da;
            border-radius: 2px;
            flex-grow: 1;
          `;
          // Set flex-basis based on column class
          const match = cssClass.match(/small-(\d+)/);
          if (match) {
            const width = (parseInt(match[1]) / 12) * 100;
            col.style.flexBasis = `${width}%`;
          }
          preview.appendChild(col);
        });

        // Create label
        const label = document.createElement('div');
        label.textContent = layout.name;
        label.style.cssText = `
          font-size: 11px;
          color: #6c757d;
          text-align: center;
        `;

        option.appendChild(preview);
        option.appendChild(label);

        // Add hover effect
        option.addEventListener('mouseenter', () => {
          option.style.borderColor = '#007bff';
          option.style.background = '#f8f9fa';
        });

        option.addEventListener('mouseleave', () => {
          option.style.borderColor = '#dee2e6';
          option.style.background = '';
        });

        // Handle selection
        option.addEventListener('click', () => {
          document.body.removeChild(modalOverlay);
          document.removeEventListener('keydown', escHandler);
          resolve(layout.classes);
        });

        layoutPicker.appendChild(option);
      });

      modalContent.appendChild(layoutPicker);

      // Add cancel button
      const buttonContainer = document.createElement('div');
      buttonContainer.style.cssText = `
        text-align: right;
        margin-top: 20px;
      `;

      const cancelBtn = document.createElement('button');
      cancelBtn.textContent = 'Cancel';
      cancelBtn.className = 'button tiny secondary radius';
      cancelBtn.style.cssText = `
        padding: 8px 15px;
      `;

      const cancelHandler = () => {
        document.body.removeChild(modalOverlay);
        document.removeEventListener('keydown', escHandler);
        resolve(null);
      };

      cancelBtn.addEventListener('click', cancelHandler);
      buttonContainer.appendChild(cancelBtn);
      modalContent.appendChild(buttonContainer);

      modalOverlay.appendChild(modalContent);

      // Handler for the Escape key
      const escHandler = (e) => {
        if (e.key === 'Escape') {
          cancelHandler();
        }
      };

      // Close on overlay click
      modalOverlay.addEventListener('click', (e) => {
        if (e.target === modalOverlay) {
          cancelHandler();
        }
      });

      // Add to DOM and show
      document.body.appendChild(modalOverlay);
      document.addEventListener('keydown', escHandler);
    });
  }  /**
   * Load existing layout from XML
   */
  loadExistingLayout(xml) {
    console.log('Loading existing layout from XML...');
    
    // Show loading indicator
    this.showLoadingIndicator('Loading layout...');
    
    try {
      // Parse XML and load into layout manager
      this.layoutManager.fromXML(xml);
      
      // Render all rows and columns
      this.canvasController.renderLayout(this.layoutManager.getStructure());
      
      // Hide loading indicator
      this.hideLoadingIndicator();
      
      // Dispatch layout changed event for XML tab sync
      document.dispatchEvent(new CustomEvent('layoutChanged', {
        detail: { source: 'canvas' }
      }));
      
      console.log('Layout loaded successfully');
    } catch (error) {
      console.error('Error loading layout:', error);
      
      // Hide loading indicator
      this.hideLoadingIndicator();
      
      alert('Could not load existing layout. Starting with blank canvas.');
    }
  }
  
  /**
   * Filter widgets in palette
   */
  filterWidgets(searchTerm) {
    const items = this.elements.palette.querySelectorAll('.widget-palette-item');
    const term = searchTerm.toLowerCase();
    
    items.forEach(item => {
      const text = item.textContent.toLowerCase();
      item.style.display = text.includes(term) ? 'block' : 'none';
    });
  }
  
  /**
   * Handle keyboard shortcuts
   */
  handleKeyboardShortcuts(e) {
    // Ctrl/Cmd + Z: Undo
    if ((e.ctrlKey || e.metaKey) && e.key === 'z' && !e.shiftKey) {
      e.preventDefault();
      this.undo();
    }
    
    // Ctrl/Cmd + Shift + Z or Ctrl/Cmd + Y: Redo
    if (((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === 'z') ||
        ((e.ctrlKey || e.metaKey) && e.key === 'y')) {
      e.preventDefault();
      this.redo();
    }
    
    // Ctrl/Cmd + S: Save
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault();
      this.save();
    }
    
    // Delete: Remove selected element
    if (e.key === 'Delete' || e.key === 'Backspace') {
      const selected = this.canvasController.getSelectedElement();
      if (selected && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
        e.preventDefault();
        this.canvasController.deleteElement(selected);
        this.saveToHistory();
      }
    }
  }
  
  /**
   * Save current state to history
   */
  saveToHistory() {
    // Remove any future states if we're not at the end
    if (this.historyIndex < this.history.length - 1) {
      this.history = this.history.slice(0, this.historyIndex + 1);
    }
    
    // Add current state
    const state = this.layoutManager.getStructure();
    this.history.push(JSON.parse(JSON.stringify(state)));
    
    // Limit history size
    if (this.history.length > this.maxHistorySize) {
      this.history.shift();
    } else {
      this.historyIndex++;
    }
    
    // Update undo/redo buttons and save indicator
    this.updateHistoryButtons();
    this.updateSaveIndicator();
    
    // Dispatch layout changed event for XML tab sync
    document.dispatchEvent(new CustomEvent('layoutChanged', {
      detail: { source: 'canvas' }
    }));
  }
  
  /**
   * Undo last action
   */
  undo() {
    if (this.historyIndex > 0) {
      this.historyIndex--;
      this.restoreState(this.history[this.historyIndex]);
      this.updateHistoryButtons();
    }
  }
  
  /**
   * Redo last undone action
   */
  redo() {
    if (this.historyIndex < this.history.length - 1) {
      this.historyIndex++;
      this.restoreState(this.history[this.historyIndex]);
      this.updateHistoryButtons();
    }
  }
  
  /**
   * Restore a saved state
   */
  restoreState(state) {
    this.layoutManager.setStructure(state);
    this.canvasController.renderLayout(state);
    
    // Dispatch layout changed event for XML tab sync
    document.dispatchEvent(new CustomEvent('layoutChanged', {
      detail: { source: 'canvas' }
    }));
  }
  
  /**
   * Update undo/redo button states
   */
  updateHistoryButtons() {
    this.elements.undoBtn.disabled = this.historyIndex <= 0;
    this.elements.redoBtn.disabled = this.historyIndex >= this.history.length - 1;
  }
  
  /**
   * Save the page using AJAX - unified save for layout, info, and CSS
   * Implements Requirements 6.1, 6.2, 6.3, 6.4, 6.5, 6.6
   */
  async save() {
    console.log('Saving page (unified save)...');
    
    // Show loading indicator in toolbar
    this.showLoadingIndicator('Saving...');
    
    // Disable save button to prevent double-submission
    this.elements.saveBtn.disabled = true;
    const originalSaveContent = this.elements.saveBtn.innerHTML;
    this.elements.saveBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Saving...';
    
    // Track what needs to be saved and results
    const saveResults = {
      layout: { needed: false, success: false, error: null },
      info: { needed: false, success: false, error: null },
      css: { needed: false, success: false, error: null }
    };
    
    try {
      // Determine what needs to be saved (Requirements 6.1)
      const layoutDirty = this.isLayoutDirty();
      const dirtyTabs = this.rightPanelTabs.getDirtyTabs();
      const infoDirty = dirtyTabs.includes('info') || this.infoTabManager.hasChanges();
      const cssDirty = dirtyTabs.includes('css') || this.cssTabManager.hasChanges();
      
      saveResults.layout.needed = layoutDirty;
      saveResults.info.needed = infoDirty;
      saveResults.css.needed = cssDirty;
      
      console.log('Save analysis:', { layoutDirty, infoDirty, cssDirty });
      
      // If nothing needs saving, show message and return
      if (!layoutDirty && !infoDirty && !cssDirty) {
        this.hideLoadingIndicator();
        this.elements.saveBtn.disabled = false;
        this.elements.saveBtn.innerHTML = originalSaveContent;
        this.showSaveToast('No changes to save', 'info');
        return;
      }
      
      // Save layout if dirty (Requirements 6.2)
      if (layoutDirty) {
        try {
          await this.saveLayout();
          saveResults.layout.success = true;
        } catch (error) {
          saveResults.layout.error = error.message;
          console.error('Error saving layout:', error);
        }
      }
      
      // Save page info if dirty (Requirements 6.3)
      if (infoDirty) {
        try {
          const infoResult = await this.infoTabManager.save();
          if (infoResult.success) {
            saveResults.info.success = true;
          } else {
            saveResults.info.error = infoResult.message || 'Failed to save page info';
          }
        } catch (error) {
          saveResults.info.error = error.message;
          console.error('Error saving page info:', error);
        }
      }
      
      // Save CSS if dirty (Requirements 6.4)
      if (cssDirty) {
        try {
          const cssResult = await this.cssTabManager.save();
          if (cssResult.success) {
            saveResults.css.success = true;
          } else {
            saveResults.css.error = cssResult.message || 'Failed to save CSS';
          }
        } catch (error) {
          saveResults.css.error = error.message;
          console.error('Error saving CSS:', error);
        }
      }
      
      // Process results and show feedback (Requirements 6.5, 6.6)
      this.processSaveResults(saveResults, originalSaveContent);
      
    } catch (error) {
      console.error('Error during unified save:', error);
      this.hideLoadingIndicator();
      this.elements.saveBtn.disabled = false;
      this.elements.saveBtn.innerHTML = originalSaveContent;
      this.showSaveToast('Error saving: ' + error.message, 'error');
    }
  }
  
  /**
   * Save just the layout data
   * @returns {Promise<Object>} The save result
   */
  async saveLayout() {
    return new Promise((resolve, reject) => {
      try {
        // Get the current layout state as JSON
        const layoutData = this.layoutManager.getStructure();
        const jsonData = JSON.stringify(layoutData);

        console.log('Prepared layout data for saving:', jsonData);
        
        // Get the selected page link
        const pageLink = this.pagesTabManager.getSelectedPageLink();
        
        // Create form data for submission
        const formData = new FormData();
        formData.append('token', document.querySelector('input[name="token"]')?.value || '');
        formData.append('webPageLink', pageLink);
        formData.append('designerData', jsonData);
        
        // Send the AJAX request to the save endpoint
        fetch('/json/saveWebPage', {
          method: 'POST',
          body: formData
        })
        .then(response => {
          if (!response.ok) {
            throw new Error('HTTP error, status = ' + response.status);
          }
          return response.json();
        })
        .then(data => {
          // Success! Update the saved state
          this.setSavedState();
          
          // If this was a new page, update the pages list
          if (this.pagesTabManager.selectedPageId === 'new') {
            const pageData = {
              id: data.webPageId || 'saved',
              link: this.pagesTabManager.getSelectedPageLink(),
              title: data.title || this.pagesTabManager.extractTitleFromLink(this.pagesTabManager.getSelectedPageLink())
            };
            this.pagesTabManager.updateNewPageAfterSave(pageData);
            
            // Refresh the pages list to get the updated list from server
            setTimeout(() => {
              this.pagesTabManager.refreshPagesList();
            }, 1000);
          }
          
          console.log('Layout saved successfully', data);
          resolve(data);
        })
        .catch(error => {
          console.error('Error saving layout:', error);
          reject(error);
        });
        
      } catch (error) {
        console.error('Error preparing layout save:', error);
        reject(error);
      }
    });
  }
  
  /**
   * Process save results and show appropriate feedback
   * @param {Object} results - The save results object
   * @param {string} originalSaveContent - The original save button content
   */
  processSaveResults(results, originalSaveContent) {
    // Hide loading indicator
    this.hideLoadingIndicator();
    
    // Count successes and failures
    const savedItems = [];
    const failedItems = [];
    
    if (results.layout.needed) {
      if (results.layout.success) {
        savedItems.push('Layout');
      } else {
        failedItems.push({ name: 'Layout', error: results.layout.error });
      }
    }
    
    if (results.info.needed) {
      if (results.info.success) {
        savedItems.push('Page Info');
      } else {
        failedItems.push({ name: 'Page Info', error: results.info.error });
      }
    }
    
    if (results.css.needed) {
      if (results.css.success) {
        savedItems.push('CSS');
      } else {
        failedItems.push({ name: 'CSS', error: results.css.error });
      }
    }
    
    // Update save button and show feedback
    this.elements.saveBtn.disabled = false;
    
    if (failedItems.length === 0) {
      // All saves successful
      this.elements.saveBtn.innerHTML = '<i class="far fa-check"></i> Saved!';
      this.updateSaveIndicator();
      
      // Show success toast with what was saved (Requirements 6.5)
      const message = savedItems.length === 1 
        ? `${savedItems[0]} saved successfully`
        : `Saved: ${savedItems.join(', ')}`;
      this.showSaveToast(message, 'success');
      
      // Reset button after delay
      setTimeout(() => {
        this.elements.saveBtn.innerHTML = '<i class="fa-solid fa-save"></i> Publish';
        this.updateSaveIndicator();
      }, 2000);
      
    } else if (savedItems.length > 0) {
      // Partial success - some saved, some failed (Requirements 6.6)
      this.elements.saveBtn.innerHTML = '<i class="far fa-exclamation-triangle"></i> Partial Save';
      
      // Show warning toast
      const successMsg = savedItems.length > 0 ? `Saved: ${savedItems.join(', ')}. ` : '';
      const failMsg = `Failed: ${failedItems.map(f => f.name).join(', ')}`;
      this.showSaveToast(successMsg + failMsg, 'warning');
      
      // Reset button after delay
      setTimeout(() => {
        this.elements.saveBtn.innerHTML = originalSaveContent;
        this.updateSaveIndicator();
      }, 3000);
      
    } else {
      // All saves failed (Requirements 6.6)
      this.elements.saveBtn.innerHTML = '<i class="far fa-exclamation-triangle"></i> Save Failed';
      
      // Show error toast with details
      const errorDetails = failedItems.map(f => `${f.name}: ${f.error}`).join('; ');
      this.showSaveToast('Save failed: ' + errorDetails, 'error');
      
      // Reset button after delay
      setTimeout(() => {
        this.elements.saveBtn.innerHTML = originalSaveContent;
      }, 3000);
    }
  }
  
  /**
   * Show a toast notification for save feedback
   * @param {string} message - The message to display
   * @param {string} type - The type of toast: 'success', 'error', 'warning', 'info'
   */
  showSaveToast(message, type = 'info') {
    // Remove any existing toast
    const existingToast = document.getElementById('save-toast');
    if (existingToast) {
      existingToast.remove();
    }
    
    // Create toast element
    const toast = document.createElement('div');
    toast.id = 'save-toast';
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
    const duration = type === 'error' ? 6000 : type === 'warning' ? 5000 : 3000;
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

  /**
   * Check if the editor has unsaved changes
   * Checks layout changes and all tab dirty states
   */
  isDirty() {
    // Check layout dirty state
    const layoutDirty = this.isLayoutDirty();
    
    // Check tab dirty states
    const tabsDirty = this.rightPanelTabs ? this.rightPanelTabs.isDirty() : false;
    
    // Also check individual tab managers for changes
    const infoDirty = this.infoTabManager ? this.infoTabManager.hasChanges() : false;
    const cssDirty = this.cssTabManager ? this.cssTabManager.hasChanges() : false;
    
    const dirty = layoutDirty || tabsDirty || infoDirty || cssDirty;
    
    console.log('isDirty:', dirty, { layoutDirty, tabsDirty, infoDirty, cssDirty });
    
    return dirty;
  }
  
  /**
   * Check if only the layout has unsaved changes (not tabs)
   * @returns {boolean} True if layout has unsaved changes
   */
  isLayoutDirty() {
    if (this.lastSavedState === null) {
      return false;
    }
    const currentState = JSON.stringify(this.layoutManager.getStructure());
    return currentState !== this.lastSavedState;
  }
  
  /**
   * Set the baseline saved state for dirty detection
   */
  setSavedState() {
    this.lastSavedState = JSON.stringify(this.layoutManager.getStructure());
    console.log('Saved state baseline set');
  }
  
  /**
   * Get viewport manager instance
   */
  getViewportManager() {
    return this.viewportManager;
  }
  
  /**
   * Get layout manager instance
   */
  getLayoutManager() {
    return this.layoutManager;
  }
  
  /**
   * Get canvas controller instance
   */
  getCanvasController() {
    return this.canvasController;
  }
  
  /**
   * Get properties panel instance
   */
  getPropertiesPanel() {
    return this.propertiesPanel;
  }
  
  /**
   * Get right panel tabs instance
   */
  getRightPanelTabs() {
    return this.rightPanelTabs;
  }
  
  /**
   * Get info tab manager instance
   */
  getInfoTabManager() {
    return this.infoTabManager;
  }
  
  /**
   * Get CSS tab manager instance
   */
  getCSSTabManager() {
    return this.cssTabManager;
  }
  
  /**
   * Get XML tab manager instance
   */
  getXMLTabManager() {
    return this.xmlTabManager;
  }
  
  /**
   * Get viewport manager instance
   */
  getViewportManager() {
    return this.viewportManager;
  }

  /**
   * Escape HTML special characters
   * @param {string} str - The string to escape
   * @returns {string} The escaped string
   */
  escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
}
