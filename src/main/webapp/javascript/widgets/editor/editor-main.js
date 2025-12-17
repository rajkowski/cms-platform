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
    this.layoutManager = new LayoutManager(this);
    this.widgetRegistry = new WidgetRegistry();
    this.canvasController = new CanvasController(this);
    this.propertiesPanel = new PropertiesPanel(this);
    this.pagesTabManager = new PagesTabManager(this);
    
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
    
    console.log('Editor initialized successfully');
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
      designerData: document.getElementById('designer-data')
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
    
    try {
      // Parse XML and load into layout manager
      this.layoutManager.fromXML(xml);
      
      // Render all rows and columns
      this.canvasController.renderLayout(this.layoutManager.getStructure());
      
      console.log('Layout loaded successfully');
    } catch (error) {
      console.error('Error loading layout:', error);
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
    
    // Update undo/redo buttons
    this.updateHistoryButtons();
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
   * Preview the page
   */
  preview() {
    console.log('Opening preview...');
    
    // Generate XML
    const xml = this.layoutManager.toXML();
    
    // Open preview in new window/tab
    // For now, just show alert with XML
    alert('Generated XML:\n\n' + xml);
  }
  
  /**
   * Save the page
   */
  save() {
    console.log('Saving page...');
    
    // Disable save button to prevent double-submission
    this.elements.saveBtn.disabled = true;
    this.elements.saveBtn.textContent = 'Saving...';
    
    try {
      // Convert layout to JSON for backend
      const layoutData = this.layoutManager.getStructure();
      const jsonData = JSON.stringify(layoutData);
      
      // Set form data
      this.elements.designerData.value = jsonData;
      
      // Always update the web page link to the currently selected page
      const pageLink = this.pagesTabManager.getSelectedPageLink();
      if (pageLink) {
        console.log('Setting web page link to:', pageLink);
        // Update the web page hidden input with the selected page
        const webPageInput = document.querySelector('input[name="webPageLink"]');
        if (webPageInput) {
          webPageInput.value = pageLink;
        }
      }
      
      // Submit form
      this.elements.editorForm.submit();
      
    } catch (error) {
      console.error('Error saving page:', error);
      alert('An error occurred while saving the page: ' + error.message);
      
      // Re-enable save button
      this.elements.saveBtn.disabled = false;
      this.elements.saveBtn.textContent = 'Save';
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
