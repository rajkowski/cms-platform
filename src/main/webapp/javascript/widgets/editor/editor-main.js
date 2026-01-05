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
    
    // Set up event listeners
    this.setupEventListeners();
    
    // Load existing layout if available
    if (this.config.hasExistingLayout && this.config.existingXml) {
      this.loadExistingLayout(this.config.existingXml);
    }
    
    // Save initial state to history
    this.saveToHistory();
    
    // Set baseline for dirty detection
    this.setSavedState();
    
    // Initialize save indicator
    this.updateSaveIndicator();
    
    console.log('Editor initialized successfully');
  }
  
  /**
   * Update the save button indicator to show unsaved changes
   */
  updateSaveIndicator() {
    if (this.isDirty()) {
      if (!this.elements.saveBtn.dataset.dirty) {
        this.elements.saveBtn.dataset.dirty = 'true';
        this.elements.saveBtn.innerHTML = '<i class="far fa-circle-dot"></i> Save <span style="color: #ff6b6b;">●</span>';
      }
    } else {
      if (this.elements.saveBtn.dataset.dirty) {
        delete this.elements.saveBtn.dataset.dirty;
        this.elements.saveBtn.innerHTML = '<i class="fa-solid fa-save"></i> Save';
      }
    }
  }

  /**
   * Show the loading indicator in the toolbar
   */
  showLoadingIndicator(text = 'Loading...') {
    if (this.elements.loadingIndicator && this.elements.loadingText) {
      this.elements.loadingText.textContent = text;
      this.elements.loadingIndicator.style.display = 'flex';
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
      addRowBtn: document.getElementById('add-row-btn'),
      undoBtn: document.getElementById('undo-btn'),
      redoBtn: document.getElementById('redo-btn'),
      previewBtn: document.getElementById('preview-btn'),
      saveBtn: document.getElementById('save-btn'),
      widgetSearch: document.getElementById('widget-search'),
      editorForm: document.getElementById('editor-form'),
      designerData: document.getElementById('designer-data'),
      loadingIndicator: document.getElementById('page-loading-indicator'),
      loadingText: document.getElementById('loading-text')
    };
  }
  
  /**
   * Set up all event listeners
   */
  setupEventListeners() {
    // Toolbar buttons
    this.elements.addRowBtn.addEventListener('click', () => this.addRow());
    this.elements.undoBtn.addEventListener('click', () => this.undo());
    this.elements.redoBtn.addEventListener('click', () => this.redo());
    this.elements.previewBtn.addEventListener('click', () => this.preview());
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
      cancelBtn.className = 'button tiny secondary';
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
  }
  
  /**
   * Update undo/redo button states
   */
  updateHistoryButtons() {
    this.elements.undoBtn.disabled = this.historyIndex <= 0;
    this.elements.redoBtn.disabled = this.historyIndex >= this.history.length - 1;
  }
  
  /**
   * Preview the page (Raw Data)
   */
  preview() {
    console.log('Opening raw data preview...');
    
    try {
      // Generate XML from current layout
      const xml = this.layoutManager.toXML();
      
      // Create a modal to display the raw XML data
      this.showRawDataModal(xml);
      
    } catch (error) {
      console.error('Error generating preview:', error);
      alert('Error generating raw data: ' + error.message);
    }
  }

  /**
   * Show raw data in a modal
   */
  showRawDataModal(xmlData) {
    // Create modal overlay
    const modalOverlay = document.createElement('div');
    modalOverlay.className = 'modal-overlay raw-data-modal active';
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
      background: var(--editor-bg);
      padding: 30px;
      border-radius: 8px;
      box-shadow: 0 5px 15px var(--editor-shadow);
      width: 90%;
      max-width: 800px;
      max-height: 80vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      color: var(--editor-text);
    `;

    // Add title
    const title = document.createElement('h4');
    title.textContent = 'Raw Page Data (XML)';
    title.style.cssText = 'margin-top: 0; margin-bottom: 20px; color: var(--editor-text);';
    modalContent.appendChild(title);

    // Create textarea for XML content
    const xmlTextarea = document.createElement('textarea');
    xmlTextarea.value = xmlData;
    xmlTextarea.style.cssText = `
      width: 100%;
      height: 400px;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      border: 1px solid var(--editor-border);
      border-radius: 4px;
      padding: 10px;
      background: var(--editor-bg);
      color: var(--editor-text);
      resize: vertical;
      margin-bottom: 20px;
    `;
    xmlTextarea.readOnly = true;
    modalContent.appendChild(xmlTextarea);

    // Create button container
    const buttonContainer = document.createElement('div');
    buttonContainer.style.cssText = `
      display: flex;
      justify-content: space-between;
      align-items: center;
    `;

    // Copy button
    const copyBtn = document.createElement('button');
    copyBtn.textContent = 'Copy to Clipboard';
    copyBtn.className = 'button tiny primary';
    copyBtn.style.cssText = 'padding: 8px 15px;';
    copyBtn.addEventListener('click', () => {
      xmlTextarea.select();
      document.execCommand('copy');
      copyBtn.textContent = 'Copied!';
      setTimeout(() => {
        copyBtn.textContent = 'Copy to Clipboard';
      }, 2000);
    });

    // Close button
    const closeBtn = document.createElement('button');
    closeBtn.textContent = 'Close';
    closeBtn.className = 'button tiny secondary';
    closeBtn.style.cssText = 'padding: 8px 15px;';

    const closeHandler = () => {
      document.body.removeChild(modalOverlay);
      document.removeEventListener('keydown', escHandler);
    };

    closeBtn.addEventListener('click', closeHandler);

    buttonContainer.appendChild(copyBtn);
    buttonContainer.appendChild(closeBtn);
    modalContent.appendChild(buttonContainer);

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

    // Focus the textarea for easy copying
    setTimeout(() => {
      xmlTextarea.focus();
    }, 100);
  }
  
  /**
   * Save the page using AJAX
   */
  save() {
    console.log('Saving page...');
    
    // Show loading indicator in toolbar
    this.showLoadingIndicator('Saving...');
    
    // Disable save button to prevent double-submission
    this.elements.saveBtn.disabled = true;
    const originalSaveContent = this.elements.saveBtn.innerHTML;
    this.elements.saveBtn.innerHTML = '<i class="far fa-spinner fa-spin"></i> Saving...';
    
    try {
      // Get the current layout state as JSON
      const layoutData = this.layoutManager.getStructure();
      const jsonData = JSON.stringify(layoutData);

      console.log('Prepared layout data for saving:', jsonData);
      
      // Get the selected page link
      const pageLink = this.pagesTabManager.getSelectedPageLink();
      
      // Create form data for submission
      const formData = new FormData();
      formData.append('widget', document.querySelector('input[name="widget"]')?.value || '');
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
        // Success! Update the saved state and show message
        this.setSavedState();
        this.updateSaveIndicator();
        
        // Hide loading indicator
        this.hideLoadingIndicator();
        
        // Re-enable save button
        this.elements.saveBtn.disabled = false;
        this.elements.saveBtn.innerHTML = '<i class="far fa-check"></i> Saved!';
        
        // Reset the button text after 2 seconds to clean state
        setTimeout(() => {
          this.elements.saveBtn.innerHTML = '<i class="fa-solid fa-save"></i> Save';
          this.updateSaveIndicator();
        }, 2000);
        
        console.log('Page saved successfully', data);
      })
      .catch(error => {
        console.error('Error saving page:', error);
        
        // Hide loading indicator
        this.hideLoadingIndicator();
        
        // Show error message
        this.elements.saveBtn.disabled = false;
        this.elements.saveBtn.innerHTML = '<i class="far fa-exclamation-triangle"></i> Save Failed';
        
        // Reset the button text after 3 seconds
        setTimeout(() => {
          this.elements.saveBtn.innerHTML = originalSaveContent;
        }, 3000);
        
        alert('Error saving page: ' + error.message);
      });
      
    } catch (error) {
      console.error('Error preparing save:', error);
      alert('An error occurred while preparing to save the page: ' + error.message);
      
      // Hide loading indicator and re-enable save button
      this.hideLoadingIndicator();
      this.elements.saveBtn.disabled = false;
      this.elements.saveBtn.innerHTML = originalSaveContent;
    }
  }

  /**
   * Check if the editor has unsaved changes
   */
  isDirty() {
    // If there's no saved state baseline, we can't know if there are changes
    if (this.lastSavedState === null) {
      console.log('isDirty: false (no baseline saved state)');
      return false;
    }
    
    // Get the current layout state
    const currentState = JSON.stringify(this.layoutManager.getStructure());
    const dirty = currentState !== this.lastSavedState;
    
    console.log('isDirty:', dirty);
    
    return dirty;
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
}
