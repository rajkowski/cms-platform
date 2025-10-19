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
    
    // History management for undo/redo
    this.history = [];
    this.historyIndex = -1;
    this.maxHistorySize = 50;
    
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
    
    // Set up event listeners
    this.setupEventListeners();
    
    // Load existing layout if available
    if (this.config.hasExistingLayout && this.config.existingXml) {
      this.loadExistingLayout(this.config.existingXml);
    }
    
    // Save initial state to history
    this.saveToHistory();
    
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
  async addRow() {
    console.log('Adding new row...');
    
    try {
      const columnLayout = await this.showColumnLayoutPicker();
      if (!columnLayout) {
        console.log('Row addition cancelled.');
        return; // User cancelled
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
    }
  }

  /**
   * Show a modal to select column layout
   * @returns {Promise<Array|null>} A promise that resolves with the selected layout or null if cancelled
   */
  showColumnLayoutPicker() {
    return new Promise((resolve) => {
      const modal = document.getElementById('layout-picker-modal');
      const optionsContainer = document.getElementById('layout-picker-options');
      const cancelBtn = document.getElementById('cancel-layout-picker');
      
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
      
      // Populate options
      optionsContainer.innerHTML = '';
      layouts.forEach(layout => {
        const option = document.createElement('div');
        option.className = 'layout-option';
        
        const preview = document.createElement('div');
        preview.className = 'layout-preview';
        layout.classes.forEach(cssClass => {
          const col = document.createElement('div');
          col.className = 'layout-preview-col';
          // Set flex-basis based on column class
          const match = cssClass.match(/small-(\d+)/);
          if (match) {
            const width = (parseInt(match[1]) / 12) * 100;
            col.style.flexBasis = `${width}%`;
          }
          preview.appendChild(col);
        });
        
        const name = document.createElement('div');
        name.textContent = layout.name;
        name.style.fontSize = '11px';
        name.style.color = '#6c757d';
        
        option.appendChild(preview);
        option.appendChild(name);
        
        option.addEventListener('click', () => {
          modal.style.display = 'none';
          resolve(layout.classes);
        });
        
        optionsContainer.appendChild(option);
      });
      
      // Show modal
      modal.style.display = 'flex';
      
      // Handle cancellation
      const cancelHandler = () => {
        modal.style.display = 'none';
        resolve(null);
      };
      
      cancelBtn.onclick = cancelHandler;
      modal.onclick = (e) => {
        if (e.target === modal) {
          cancelHandler();
        }
      };
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
    alert('Preview functionality coming soon!\n\nGenerated XML:\n' + xml);
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
