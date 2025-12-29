/**
 * Drag and Drop Manager
 * Handles all drag-and-drop interactions for widgets, rows, and columns
 * 
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */

class DragDropManager {
  constructor(editor) {
    this.editor = editor;
    this.draggedElement = null;
    this.draggedType = null; // 'widget', 'row', 'column'
    this.draggedData = null;
    this.dropZones = new Map();
    this.widgetPlaceholder = null;
    this.rowPlaceholder = null;
    this.draggedItem = null;
  }
  
  /**
   * Initialize drag and drop functionality
   */
  init() {
    this.initPaletteItems();
    this.initCanvasListeners();
  }
  
  /**
   * Initialize palette item listeners
   */
  initPaletteItems() {
    const paletteItems = document.querySelectorAll('.widget-palette-item, .layout-palette-item');
    paletteItems.forEach(item => {
      item.addEventListener('dragstart', (e) => this.handleDragStart(e));
      item.addEventListener('dragend', (e) => this.handleDragEnd(e));
    });
  }

  /**
   * Initialize canvas listeners
   */
  initCanvasListeners() {
    console.log('Initializing Drag and Drop Manager...');
    
    // Set up canvas drop zones
    this.setupCanvasDropZones();
  }

  /**
   * Register a draggable element
   */
  registerDraggable(element, type, data) {
    element.addEventListener('dragstart', (e) => {
      e.stopPropagation();
      this.draggedElement = element;
      this.draggedType = type;
      this.draggedData = data;
      
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('text/plain', JSON.stringify(data));
      
      // Use a timeout to avoid the element disappearing immediately
      setTimeout(() => {
        element.style.opacity = '0.5';
      }, 0);
      
      console.log('Drag started:', type, data);
    });
    
    element.addEventListener('dragend', (e) => {
      e.stopPropagation();
      element.style.opacity = '1';
      this.draggedElement = null;
      this.draggedType = null;
      this.draggedData = null;
      
      // Clear any lingering drag-over styles
      document.querySelectorAll('.drag-over').forEach(el => el.classList.remove('drag-over'));
      
      // Remove placeholders
      if (this.widgetPlaceholder) {
        this.widgetPlaceholder.remove();
        this.widgetPlaceholder = null;
      }
      if (this.rowPlaceholder) {
        this.rowPlaceholder.remove();
        this.rowPlaceholder = null;
      }

      console.log('Drag ended');
    });
  }
  
  /**
   * Set up draggable widgets in the palette
   */
  setupPaletteWidgets() {
    // Draggable widgets
    const paletteItems = document.querySelectorAll('.widget-palette-item');
    paletteItems.forEach(item => {
      item.addEventListener('dragstart', (e) => {
        const widgetType = item.getAttribute('data-widget-type');
        this.draggedType = 'widget';
        this.draggedData = { type: widgetType };
        e.dataTransfer.effectAllowed = 'copy';
        e.dataTransfer.setData('text/plain', widgetType);
        item.style.opacity = '0.5';
        console.log('Drag started:', widgetType);
      });
      
      item.addEventListener('dragend', () => {
        item.style.opacity = '1';
        this.draggedType = null;
        this.draggedData = null;
        console.log('Drag ended');
      });
    });

    // Draggable layouts
    const layoutItems = document.querySelectorAll('.layout-palette-item');
    layoutItems.forEach(item => {
      item.addEventListener('dragstart', (e) => {
        const layout = item.getAttribute('data-layout');
        this.draggedType = 'layout';
        this.draggedData = { layout: layout };
        e.dataTransfer.effectAllowed = 'copy';
        e.dataTransfer.setData('text/plain', layout);
        item.style.opacity = '0.5';
        console.log('Drag started: layout', layout);
      });

      item.addEventListener('dragend', () => {
        item.style.opacity = '1';
        this.draggedType = null;
        this.draggedData = null;
        console.log('Drag ended');
      });
    });
  }
  
  /**
   * Set up drop zones in the canvas
   */
  setupCanvasDropZones() {
    const canvas = document.getElementById('editor-canvas');
    
    // Canvas-level drop zone (for rows and widgets)
    this.registerDropZone(canvas, {
      accept: ['widget', 'row', 'layout'],
      onDrop: (e, type, data) => this.handleCanvasDrop(e, type, data)
    });
  }
  
  /**
   * Register a drop zone
   */
  registerDropZone(element, config) {
    const zoneId = this.generateId();
    this.dropZones.set(zoneId, { element, config });
    
    element.setAttribute('data-dropzone-id', zoneId);
    
    let dragEnterCounter = 0;

    // Drag over
    element.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.stopPropagation();
      
      // Check if this drop zone accepts the dragged type
      if (this.canAcceptDrop(config, this.draggedType)) {
        e.dataTransfer.dropEffect = 'copy';

        if (this.draggedType === 'widget' && element.classList.contains('canvas-column')) {
          this.handleWidgetDragOver(e, element);
        } else if ((this.draggedType === 'row' || this.draggedType === 'layout') && element.id === 'editor-canvas') {
          this.handleRowDragOver(e, element);
        }
      } else {
        e.dataTransfer.dropEffect = 'none';
      }
    });
    
    // Drag enter
    element.addEventListener('dragenter', (e) => {
      e.preventDefault();
      e.stopPropagation();
      
      if (this.canAcceptDrop(config, this.draggedType)) {
        dragEnterCounter++;
        element.classList.add('drag-over');
      }
    });
    
    // Drag leave
    element.addEventListener('dragleave', (e) => {
      e.preventDefault();
      e.stopPropagation();
      
      if (this.canAcceptDrop(config, this.draggedType)) {
        dragEnterCounter--;
        if (dragEnterCounter === 0) {
          element.classList.remove('drag-over');
          if (this.widgetPlaceholder) {
            this.widgetPlaceholder.remove();
            this.widgetPlaceholder = null;
          }
          if (this.rowPlaceholder) {
            this.rowPlaceholder.remove();
            this.rowPlaceholder = null;
          }
        }
      }
    });
    
    // Drop
    element.addEventListener('drop', (e) => {
      e.preventDefault();
      e.stopPropagation();
      
      dragEnterCounter = 0;
      element.classList.remove('drag-over');
      
      if (this.canAcceptDrop(config, this.draggedType)) {
        if (config.onDrop) {
          config.onDrop(e, this.draggedType, this.draggedData);
        }
        // Handle row reordering
        if (this.draggedType === 'row' && config.accept.includes('row')) {
          this.handleRowDrop(e, element);
        }
      }

      // Clean up placeholders
      if (this.widgetPlaceholder) {
        this.widgetPlaceholder.remove();
        this.widgetPlaceholder = null;
      }
      if (this.rowPlaceholder) {
        this.rowPlaceholder.remove();
        this.rowPlaceholder = null;
      }
    });
    
    return zoneId;
  }
  
  /**
   * Unregister a drop zone
   */
  unregisterDropZone(zoneId) {
    this.dropZones.delete(zoneId);
  }
  
  /**
   * Check if a drop zone can accept a dragged element
   */
  canAcceptDrop(config, draggedType) {
    if (!config.accept || config.accept.length === 0) {
      return true;
    }
    return config.accept.includes(draggedType);
  }
  
  /**
   * Handle drop on canvas
   */
  handleCanvasDrop(e, type, data) {
    console.log('Dropped on canvas:', type, data);
    
    if (type === 'layout') {
      const targetRowId = this.rowPlaceholder ? this.rowPlaceholder.nextElementSibling?.getAttribute('data-row-id') : null;
      const columnLayout = this.draggedData.layout.split(',');
      const rowId = this.editor.getLayoutManager().addRow(columnLayout, targetRowId);
      
      this.editor.getCanvasController().renderLayout(this.editor.getLayoutManager().getStructure());
      this.editor.saveToHistory();

    } else if (type === 'widget') {
      // Create a new row with one column and add the widget
      const columnLayout = ['small-12'];
      const rowId = this.editor.getLayoutManager().addRow(columnLayout);
      const row = this.editor.getLayoutManager().getRow(rowId);
      
      // Add widget to the first column
      const newWidgetId = this.editor.getLayoutManager().addWidget(rowId, row.columns[0].id, this.draggedData.type);

      // If this is the first row, re-render the whole layout to clear the placeholder
      if (this.editor.getLayoutManager().getStructure().rows.length === 1) {
        this.editor.getCanvasController().renderLayout(this.editor.getLayoutManager().getStructure());
      } else {
        // Otherwise, just render the new row for better performance
        this.editor.getCanvasController().renderRow(rowId, row);
      }
      
      // Highlight the newly added widget and show its properties
      setTimeout(() => {
        const newWidgetElement = document.querySelector(`[data-widget-id="${newWidgetId}"]`);
        if (newWidgetElement) {
          this.editor.getCanvasController().selectElement(newWidgetElement, {
            type: 'widget',
            rowId: rowId,
            columnId: row.columns[0].id,
            widgetId: newWidgetId
          });
        }
      }, 0);
      
      // Save to history
      this.editor.saveToHistory();
    }
  }
  
  /**
   * Handle drop of a row
   */
  handleRowDrop(e, dropZoneElement) {
    if (!this.draggedElement) return;
    
    const draggedRowId = this.draggedData.rowId;
    const targetRowId = this.rowPlaceholder ? this.rowPlaceholder.nextElementSibling?.getAttribute('data-row-id') : null;

    if (draggedRowId !== targetRowId) {
      this.editor.getLayoutManager().moveRow(draggedRowId, targetRowId);
      this.editor.getCanvasController().renderLayout(this.editor.getLayoutManager().getStructure());
      this.editor.saveToHistory();
    }
  }
  
  /**
   * Find closest row to a vertical position
   */
  findClosestRow(y, canvasElement) {
    const rows = canvasElement.querySelectorAll('.canvas-row');
    let closest = {
      element: null,
      offset: Number.NEGATIVE_INFINITY,
      position: 'after'
    };

    rows.forEach(row => {
      // Skip the currently dragged row
      if (row === this.draggedElement) return;

      const box = row.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;

      if (offset < 0 && offset > closest.offset) {
        closest = { element: row, offset: offset, position: 'before' };
      }
    });

    if (closest.element) {
      return closest;
    }

    // If no row is found to be 'before', find the closest 'after'
    let closestAfter = {
      element: null,
      offset: Number.POSITIVE_INFINITY,
      position: 'after'
    };

    rows.forEach(row => {
      // Skip the currently dragged row
      if (row === this.draggedElement) return;
      
      const box = row.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;
      if (offset > 0 && offset < closestAfter.offset) {
        closestAfter = { element: row, offset: offset, position: 'after' };
      }
    });

    return closestAfter.element ? closestAfter : { element: null, position: 'after' };
  }

  /**
   * Handle drop on a specific column
   */
  handleColumnDrop(columnElement, rowId, columnId) {
    console.log('Dropped on column:', rowId, columnId);
    
    if (this.draggedType === 'widget') {
      const targetWidgetId = this.widgetPlaceholder ? this.widgetPlaceholder.nextElementSibling?.getAttribute('data-widget-id') : null;
      let newWidgetId = null;

      // If the widget is from the palette, it has a 'type'
      // If it's from the canvas, it has 'widgetId', 'sourceRowId', 'sourceColumnId'
      if (this.draggedData.widgetId) {
        // Moving an existing widget
        this.editor.getLayoutManager().moveWidget(
          this.draggedData.sourceRowId,
          this.draggedData.sourceColumnId,
          this.draggedData.widgetId,
          rowId,
          columnId,
          targetWidgetId
        );
        newWidgetId = this.draggedData.widgetId;
      } else {
        // Adding a new widget from the palette
        newWidgetId = this.editor.getLayoutManager().addWidget(rowId, columnId, this.draggedData.type, targetWidgetId);
      }
      
      // Re-render the whole layout to reflect the change
      this.editor.getCanvasController().renderLayout(this.editor.getLayoutManager().getStructure());
      
      // Highlight the newly added/moved widget and show its properties
      setTimeout(() => {
        const newWidgetElement = document.querySelector(`[data-widget-id="${newWidgetId}"]`);
        if (newWidgetElement) {
          this.editor.getCanvasController().selectElement(newWidgetElement, {
            type: 'widget',
            rowId: rowId,
            columnId: columnId,
            widgetId: newWidgetId
          });
        }
      }, 0);
      
      // Save to history
      this.editor.saveToHistory();
    }
  }

  /**
   * Handle row drag over for visual feedback
   */
  handleRowDragOver(e, canvasElement) {
    if (!this.rowPlaceholder) {
      this.rowPlaceholder = document.createElement('div');
      this.rowPlaceholder.className = 'row-drag-placeholder';
    }

    const closestRow = this.findClosestRow(e.clientY, canvasElement);
    if (closestRow.element) {
      if (closestRow.position === 'before') {
        closestRow.element.parentNode.insertBefore(this.rowPlaceholder, closestRow.element);
      } else {
        closestRow.element.parentNode.insertBefore(this.rowPlaceholder, closestRow.element.nextSibling);
      }
    } else {
      canvasElement.appendChild(this.rowPlaceholder);
    }
  }

  /**
   * Find the closest widget to a vertical position within a column
   */
  findClosestWidget(y, columnElement) {
    const widgets = columnElement.querySelectorAll('.canvas-widget');
    let closest = {
      element: null,
      offset: Number.NEGATIVE_INFINITY,
      position: 'after'
    };

    widgets.forEach(widget => {
      // Skip the currently dragged widget
      if (widget === this.draggedElement) return;

      const box = widget.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;

      if (offset < 0 && offset > closest.offset) {
        closest = { element: widget, offset: offset, position: 'before' };
      }
    });

    if (closest.element) {
      return closest;
    }

    // If no widget is found to be 'before', find the closest 'after'
    let closestAfter = {
      element: null,
      offset: Number.POSITIVE_INFINITY,
      position: 'after'
    };

    widgets.forEach(widget => {
      if (widget === this.draggedElement) return;
      
      const box = widget.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;
      if (offset > 0 && offset < closestAfter.offset) {
        closestAfter = { element: widget, offset: offset, position: 'after' };
      }
    });

    return closestAfter.element ? closestAfter : { element: null, position: 'after' };
  }

  /**
   * Handle widget drag over for visual feedback
   */
  handleWidgetDragOver(e, columnElement) {
    if (!this.widgetPlaceholder) {
      this.widgetPlaceholder = document.createElement('div');
      this.widgetPlaceholder.className = 'widget-drag-placeholder';
    }

    const closestWidget = this.findClosestWidget(e.clientY, columnElement);
    if (closestWidget.element) {
      if (closestWidget.position === 'before') {
        closestWidget.element.parentNode.insertBefore(this.widgetPlaceholder, closestWidget.element);
      } else {
        closestWidget.element.parentNode.insertBefore(this.widgetPlaceholder, closestWidget.element.nextSibling);
      }
    } else {
      columnElement.appendChild(this.widgetPlaceholder);
    }
  }

  /**
   * Handle drag start for any draggable item
   */
  handleDragStart(e) {
    const target = e.currentTarget;
    target.style.opacity = '0.5';
    this.draggedItem = target;

    const widgetType = target.getAttribute('data-widget-type');
    const layout = target.getAttribute('data-layout');
    const rowId = target.getAttribute('data-row-id');
    const columnId = target.getAttribute('data-column-id');
    const widgetId = target.getAttribute('data-widget-id');

    if (widgetType) {
      this.draggedType = 'widget';
      this.draggedData = { type: widgetType };
      e.dataTransfer.effectAllowed = 'copy';
    } else if (layout) {
      this.draggedType = 'layout';
      this.draggedData = { layout: layout };
      e.dataTransfer.effectAllowed = 'copy';
    } else if (rowId && !columnId) {
      this.draggedType = 'row';
      this.draggedData = { rowId: rowId };
      e.dataTransfer.effectAllowed = 'move';
      this.draggedElement = target;
    } else if (widgetId) {
      this.draggedType = 'widget';
      this.draggedData = { 
        widgetId: widgetId,
        sourceRowId: rowId,
        sourceColumnId: columnId
      };
      e.dataTransfer.effectAllowed = 'move';
      this.draggedElement = target;
    }
    
    e.dataTransfer.setData('text/plain', JSON.stringify(this.draggedData));
    console.log('Drag started:', this.draggedType, this.draggedData);
  }

  /**
   * Handle drag end for any draggable item
   */
  handleDragEnd(e) {
    if (this.draggedItem) {
      this.draggedItem.style.opacity = '1';
    }
    
    this.draggedItem = null;
    this.draggedType = null;
    this.draggedData = null;
    this.draggedElement = null;

    // Clean up placeholders
    if (this.widgetPlaceholder) {
      this.widgetPlaceholder.remove();
      this.widgetPlaceholder = null;
    }
    if (this.rowPlaceholder) {
      this.rowPlaceholder.remove();
      this.rowPlaceholder = null;
    }

    // Clean up drag-over styles
    document.querySelectorAll('.drag-over').forEach(el => el.classList.remove('drag-over'));

    console.log('Drag ended');
  }

  /**
   * Generate a unique ID
   */
  generateId() {
    return `zone-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}
