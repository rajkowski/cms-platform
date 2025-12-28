/**
 * Canvas Controller
 * Controls canvas interactions and rendering
 * 
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */

class CanvasController {
  constructor(editor) {
    this.editor = editor;
    this.canvas = null;
    this.selectedElement = null;
    this.selectedContext = null; // { type, rowId, columnId, widgetId }
  }
  
  /**
   * Initialize canvas controller
   */
  init() {
    this.canvas = document.getElementById('editor-canvas');
    console.log('Canvas Controller initialized');
  }
  
  /**
   * Render entire layout
   */
  renderLayout(structure) {
    // Clear canvas
    this.canvas.innerHTML = '';
    
    if (!structure || !structure.rows || structure.rows.length === 0) {
      this.renderEmptyCanvas();
      return;
    }
    
    // Render each row
    for (const row of structure.rows) {
      this.renderRow(row.id, row);
    }
  }
  
  /**
   * Render empty canvas placeholder
   */
  renderEmptyCanvas() {
    this.canvas.innerHTML = `
      <div class="empty-canvas" style="cursor: pointer;">
        <i class="fa-solid fa-plus-circle fa-3x margin-bottom-10"></i>
        <h5>Start Building Your Page</h5>
        <p>Click "Add Row" to begin or drag widgets from the palette</p>
      </div>
    `;
    this.canvas.querySelector('.empty-canvas').addEventListener('click', () => {
      this.editor.addRow();
    });
  }
  
  /**
   * Render a row
   */
  renderRow(rowId, row) {
    // Find existing row element or create new one
    let rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
    
    if (!rowElement) {
      rowElement = document.createElement('div');
      rowElement.className = 'canvas-row';
      rowElement.setAttribute('data-row-id', rowId);
      this.canvas.appendChild(rowElement);
    } else {
      rowElement.innerHTML = '';
    }
    
    // Add row class if specified
    if (row.cssClass) {
      rowElement.className = 'canvas-row ' + row.cssClass;
    }
    
    // Make the row draggable
    rowElement.draggable = true;
    this.editor.dragDropManager.registerDraggable(rowElement, 'row', { rowId: rowId });

    // Add row controls
    const controls = this.createRowControls(rowId);
    rowElement.appendChild(controls);
    
    // Create grid container
    const gridContainer = document.createElement('div');
    gridContainer.className = 'grid-x grid-margin-x';
    
    // Render columns
    for (const column of row.columns) {
      const columnElement = this.renderColumn(rowId, column);
      gridContainer.appendChild(columnElement);
    }
    
    rowElement.appendChild(gridContainer);
    
    // Make row clickable for selection
    rowElement.addEventListener('click', (e) => {
      if (e.target === rowElement || e.target === gridContainer) {
        this.selectElement(rowElement, { type: 'row', rowId });
      }
    });
  }
  
  /**
   * Create row controls
   */
  createRowControls(rowId) {
    const controls = document.createElement('div');
    controls.className = 'row-controls';

    // Settings button
    const settingsBtn = document.createElement('button');
    settingsBtn.className = 'control-btn';
    settingsBtn.innerHTML = '<i class="fa fa-cog"></i>';
    settingsBtn.title = 'Row Settings';
    settingsBtn.onclick = (e) => {
      e.stopPropagation();
      this.showRowSettings(rowId);
    };

    // Delete button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'control-btn danger';
    deleteBtn.innerHTML = '<i class="fa fa-trash"></i>';
    deleteBtn.title = 'Delete Row';
    deleteBtn.onclick = (e) => {
      e.stopPropagation();
      this.deleteRow(rowId);
    };

    controls.appendChild(settingsBtn);
    controls.appendChild(deleteBtn);

    return controls;
  }  /**
   * Render a column
   */
  renderColumn(rowId, column) {
    const columnElement = document.createElement('div');
    columnElement.className = 'canvas-column ' + column.cssClass;
    columnElement.setAttribute('data-column-id', column.id);
    
    // Make it a drop zone for widgets
    const dropZoneId = this.editor.dragDropManager.registerDropZone(columnElement, {
      accept: ['widget'],
      onDrop: (e, type, data) => {
        this.editor.dragDropManager.handleColumnDrop(columnElement, rowId, column.id);
      }
    });
    
    // Render widgets
    if (column.widgets && column.widgets.length > 0) {
      for (const widget of column.widgets) {
        const widgetElement = this.renderWidget(rowId, column.id, widget);
        columnElement.appendChild(widgetElement);
      }
    } else {
      columnElement.innerHTML = '<div style="text-align:center;color:#999;padding:20px;">Drop widget here</div>';
    }
    
    // Make column clickable for selection
    columnElement.addEventListener('click', (e) => {
      if (e.target === columnElement) {
        this.selectElement(columnElement, { type: 'column', rowId, columnId: column.id });
      }
    });
    
    return columnElement;
  }
  
  /**
   * Render a widget
   */
  renderWidget(rowId, columnId, widget) {
    const widgetElement = document.createElement('div');
    widgetElement.className = 'canvas-widget';
    widgetElement.setAttribute('data-widget-id', widget.id);

    // Make the widget draggable
    widgetElement.draggable = true;
    this.editor.dragDropManager.registerDraggable(widgetElement, 'widget', {
      type: widget.type,
      widgetId: widget.id,
      sourceRowId: rowId,
      sourceColumnId: columnId
    });
    
    // Get widget definition from registry
    const definition = this.editor.widgetRegistry.get(widget.type);
    const widgetName = definition ? definition.name : widget.type;
    const widgetIcon = definition ? definition.icon : 'fa-cube';
    
    // Ensure default properties are set before rendering preview
    if (!widget.properties || Object.keys(widget.properties).length === 0) {
      widget.properties = this.editor.getLayoutManager().getDefaultProperties(widget.type);
    }
    
    // Create widget preview after defaults are set
    const preview = this.renderWidgetPreview(widget);
    widgetElement.innerHTML = `
      <div style="display:flex;align-items:center;margin-bottom:10px;">
        <i class="fa ${widgetIcon}" style="margin-right:8px;color:#007bff;"></i>
        <strong>${widgetName}</strong>
      </div>
      ${preview}
    `;
    
    // Add widget controls
    const controls = this.createWidgetControls(rowId, columnId, widget.id);
    widgetElement.appendChild(controls);
    
    // Make widget clickable for selection
    widgetElement.addEventListener('click', (e) => {
      e.stopPropagation();
      this.selectElement(widgetElement, { type: 'widget', rowId, columnId, widgetId: widget.id });
    });
    
    return widgetElement;
  }
  
  /**
   * Render widget preview
   */
  renderWidgetPreview(widget) {
    const props = widget.properties;
    
    if (!props || Object.keys(props).length === 0) {
      return '<div style="font-size:12px;color:#999;">Widget configuration</div>';
    }
    
    const previewValue = this.getPreviewValue(props);
    return previewValue ? `<div style="font-size:12px;color:#666;">${previewValue}</div>` : '<div style="font-size:12px;color:#999;">Widget configuration</div>';
  }
  
  /**
   * Get the display value for widget preview
   */
  getPreviewValue(props) {
    // Check for priority properties first
    const priorityKeys = ['uniqueId', 'name', 'title', 'heading', 'text', 'label'];
    for (const key of priorityKeys) {
      if (props[key]) {
        return (key === 'label' && props.value) ? props[key] + ': ' + props.value : props[key];
      }
    }
    
    // Check for any property key containing 'UniqueId'
    const uniqueIdKey = Object.keys(props).find(key => key.includes('UniqueId') && props[key]);
    if (uniqueIdKey) return props[uniqueIdKey];
    
    // Return the first non-empty string property value
    const firstString = Object.values(props).find(value => typeof value === 'string' && value.trim());
    return firstString || null;
  }
  
  /**
   * Create widget controls
   */
  createWidgetControls(rowId, columnId, widgetId) {
    const controls = document.createElement('div');
    controls.className = 'widget-controls';
    
    // Edit button
    const editBtn = document.createElement('button');
    editBtn.className = 'control-btn';
    editBtn.innerHTML = '<i class="fa fa-edit"></i>';
    editBtn.title = 'Edit Widget';
    editBtn.onclick = (e) => {
      e.stopPropagation();
      const element = document.querySelector(`[data-widget-id="${widgetId}"]`);
      this.selectElement(element, { type: 'widget', rowId, columnId, widgetId });
    };
    
    // Delete button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'control-btn danger';
    deleteBtn.innerHTML = '<i class="fa fa-trash"></i>';
    deleteBtn.title = 'Delete Widget';
    deleteBtn.onclick = (e) => {
      e.stopPropagation();
      this.deleteWidget(rowId, columnId, widgetId);
    };
    
    controls.appendChild(editBtn);
    controls.appendChild(deleteBtn);
    
    return controls;
  }
  
  /**
   * Select an element
   */
  selectElement(element, context) {
    // Remove previous selection
    if (this.selectedElement) {
      this.selectedElement.classList.remove('selected');
    }
    
    // Set new selection
    this.selectedElement = element;
    this.selectedContext = context;
    
    if (element) {
      element.classList.add('selected');
      
      // Update properties panel
      this.editor.getPropertiesPanel().show(context);
    }
  }
  
  /**
   * Get selected element
   */
  getSelectedElement() {
    return this.selectedElement;
  }
  
  /**
   * Delete selected element
   */
  deleteElement(element) {
    if (!this.selectedContext) return;
    
    const { type, rowId, columnId, widgetId } = this.selectedContext;
    
    if (type === 'row') {
      this.deleteRow(rowId);
    } else if (type === 'widget') {
      this.deleteWidget(rowId, columnId, widgetId);
    }
  }
  
  /**
   * Delete a row
   */
  deleteRow(rowId) {
    if (confirm('Are you sure you want to delete this row?')) {
      this.editor.getLayoutManager().removeRow(rowId);
      
      // Remove from DOM
      const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
      if (rowElement) {
        rowElement.remove();
      }
      
      // Clear selection
      this.selectedElement = null;
      this.selectedContext = null;
      this.editor.getPropertiesPanel().clear();
      
      // Save to history
      this.editor.saveToHistory();
      
      // Check if canvas is empty
      if (this.editor.getLayoutManager().getStructure().rows.length === 0) {
        this.renderEmptyCanvas();
      }
    }
  }
  
  /**
   * Delete a widget
   */
  deleteWidget(rowId, columnId, widgetId) {
    if (confirm('Are you sure you want to delete this widget?')) {
      this.editor.getLayoutManager().removeWidget(rowId, columnId, widgetId);
      
      // Re-render the row
      const row = this.editor.getLayoutManager().getRow(rowId);
      this.renderRow(rowId, row);
      
      // Clear selection
      this.selectedElement = null;
      this.selectedContext = null;
      this.editor.getPropertiesPanel().clear();
      
      // Save to history
      this.editor.saveToHistory();
    }
  }
  
  /**
   * Show row settings
   */
  showRowSettings(rowId) {
    this.editor.getPropertiesPanel().show({
      type: 'row',
      rowId: rowId
    });
  }


}
