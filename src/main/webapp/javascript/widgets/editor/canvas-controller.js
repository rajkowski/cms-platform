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
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return null;
    
    const columnElement = document.createElement('div');
    columnElement.className = 'canvas-column ' + column.cssClass;
    columnElement.setAttribute('data-column-id', column.id);
    
    // Add column controls
    const controls = this.createColumnControls(rowId, column.id, columnElement);
    columnElement.appendChild(controls);
    
    // Add resize handles (left and right)
    const leftResizeHandle = document.createElement('div');
    leftResizeHandle.className = 'column-resize-handle left';
    leftResizeHandle.setAttribute('data-column-id', column.id);
    leftResizeHandle.setAttribute('data-side', 'left');
    
    const rightResizeHandle = document.createElement('div');
    rightResizeHandle.className = 'column-resize-handle right';
    rightResizeHandle.setAttribute('data-column-id', column.id);
    rightResizeHandle.setAttribute('data-side', 'right');
    
    // Set up resize handlers
    this.setupColumnResize(leftResizeHandle, rowId, column.id, 'left');
    this.setupColumnResize(rightResizeHandle, rowId, column.id, 'right');
    
    columnElement.appendChild(leftResizeHandle);
    columnElement.appendChild(rightResizeHandle);
    
    // Make it a drop zone for widgets
    const dropZoneId = this.editor.dragDropManager.registerDropZone(columnElement, {
      accept: ['widget'],
      onDrop: (e, type, data) => {
        this.editor.dragDropManager.handleColumnDrop(columnElement, rowId, column.id);
      }
    });
    
    // Render widgets
    let emptyMsg = null;
    if (column.widgets && column.widgets.length > 0) {
      for (const widget of column.widgets) {
        const widgetElement = this.renderWidget(rowId, column.id, widget);
        columnElement.appendChild(widgetElement);
      }
    } else {
      emptyMsg = document.createElement('div');
      emptyMsg.style.cssText = 'text-align:center;color:var(--editor-text-muted);padding:20px;';
      emptyMsg.textContent = 'Drop widget here';
      columnElement.appendChild(emptyMsg);
    }
    
    // Make column clickable for selection
    columnElement.addEventListener('click', (e) => {
      if (e.target === columnElement || (emptyMsg && e.target === emptyMsg)) {
        this.selectElement(columnElement, { type: 'column', rowId, columnId: column.id });
      }
    });
    
    return columnElement;
  }
  
  /**
   * Create column controls
   */
  createColumnControls(rowId, columnId, columnElement) {
    const controls = document.createElement('div');
    controls.className = 'column-controls';
    controls.style.cssText = 'position:absolute;top:5px;right:5px;display:none;z-index:1;';
    
    // Add Column button (before this one)
    const addBeforeBtn = document.createElement('button');
    addBeforeBtn.className = 'control-btn';
    addBeforeBtn.innerHTML = '<i class="fa fa-plus"></i>';
    addBeforeBtn.title = 'Add Column Before';
    addBeforeBtn.onclick = (e) => {
      e.stopPropagation();
      this.addColumnBefore(rowId, columnId);
    };
    
    // Add Column button (after this one)
    const addAfterBtn = document.createElement('button');
    addAfterBtn.className = 'control-btn';
    addAfterBtn.innerHTML = '<i class="fa fa-plus"></i>';
    addAfterBtn.title = 'Add Column After';
    addAfterBtn.onclick = (e) => {
      e.stopPropagation();
      this.addColumnAfter(rowId, columnId);
    };
    
    // Move Column Left
    const moveLeftBtn = document.createElement('button');
    moveLeftBtn.className = 'control-btn';
    moveLeftBtn.innerHTML = '<i class="fa fa-arrow-left"></i>';
    moveLeftBtn.title = 'Move Column Left';
    moveLeftBtn.onclick = (e) => {
      e.stopPropagation();
      this.moveColumnLeft(rowId, columnId);
    };
    
    // Move Column Right
    const moveRightBtn = document.createElement('button');
    moveRightBtn.className = 'control-btn';
    moveRightBtn.innerHTML = '<i class="fa fa-arrow-right"></i>';
    moveRightBtn.title = 'Move Column Right';
    moveRightBtn.onclick = (e) => {
      e.stopPropagation();
      this.moveColumnRight(rowId, columnId);
    };
    
    // Delete Column button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'control-btn danger';
    deleteBtn.innerHTML = '<i class="fa fa-trash"></i>';
    deleteBtn.title = 'Delete Column';
    deleteBtn.onclick = (e) => {
      e.stopPropagation();
      this.deleteColumn(rowId, columnId);
    };
    
    controls.appendChild(addBeforeBtn);
    controls.appendChild(addAfterBtn);
    controls.appendChild(moveLeftBtn);
    controls.appendChild(moveRightBtn);
    controls.appendChild(deleteBtn);
    
    // Show controls on hover
    columnElement.addEventListener('mouseenter', () => {
      controls.style.display = 'block';
    });
    columnElement.addEventListener('mouseleave', () => {
      controls.style.display = 'none';
    });
    
    return controls;
  }
  
  /**
   * Setup column resize functionality
   */
  setupColumnResize(handle, rowId, columnId, side) {

    // @todo handle edge cases where no adjacent column exists (e.g., first or last column, only one column)
    // @todo handle case where there are more than 2 columns in a row
    // @todo implement independent small, medium, large resizing based on selected viewport mode state

    let isResizing = false;
    let startX = 0;
    let startColSize = 0;
    let startAdjSize = 0;
    let adjacentColumn = null;
    let rowElement = null;
    let gridContainer = null;
    
    handle.addEventListener('mousedown', (e) => {
      e.stopPropagation();
      e.preventDefault(); // Prevent default drag behavior
      
      console.log('Column resize started', { rowId, columnId, side });
      
      isResizing = true;
      startX = e.clientX;
      
      // Find the row element and disable its dragging temporarily
      rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
      if (rowElement) {
        rowElement.draggable = false;
        rowElement.style.pointerEvents = 'none'; // Prevent row interactions during resize
        gridContainer = rowElement.querySelector('.grid-x');
      }
      
      if (!gridContainer) {
        console.error('Grid container not found');
        isResizing = false;
        return;
      }
      
      const row = this.editor.getLayoutManager().getRow(rowId);
      if (!row) {
        console.error('Row not found');
        isResizing = false;
        return;
      }
      
      const columnIndex = row.columns.findIndex(c => c.id === columnId);
      if (columnIndex === -1) {
        console.error('Column not found');
        isResizing = false;
        return;
      }
      
      const column = row.columns[columnIndex];
      
      // Find adjacent column
      if (side === 'left' && columnIndex > 0) {
        adjacentColumn = row.columns[columnIndex - 1];
      } else if (side === 'right' && columnIndex < row.columns.length - 1) {
        adjacentColumn = row.columns[columnIndex + 1];
      }
      
      // @todo handle edge cases where no adjacent column exists (e.g., first or last column, only one column)
      if (!adjacentColumn) {
        console.log('No adjacent column found', { side, columnIndex, totalColumns: row.columns.length });
        isResizing = false;
        return;
      }
      
      // Get current grid sizes
      const parseSize = (cssClass) => {
        const match = cssClass.match(/small-(\d+)/);
        return match ? parseInt(match[1], 10) : 12;
      };
      
      startColSize = parseSize(column.cssClass);
      startAdjSize = parseSize(adjacentColumn.cssClass);
      
      console.log('Resize initialized', { startColSize, startAdjSize, rowWidth: gridContainer.offsetWidth });
      
      handle.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    });
    
    const mousemoveHandler = (e) => {
      if (!isResizing || !adjacentColumn || !gridContainer) return;
      
      e.preventDefault(); // Prevent any default behavior
      e.stopPropagation(); // Stop event from bubbling to row
      
      // Calculate pixel difference
      const diff = side === 'left' ? (startX - e.clientX) : (e.clientX - startX);
      
      // Get the actual row width (grid container width)
      const rowWidth = gridContainer.offsetWidth;

      if (rowWidth === 0) {
        console.warn('Row width is 0, cannot resize');
        return;
      }
      
      // Calculate how many grid units this pixel difference represents
      // Each grid unit is rowWidth / 12
      const gridUnitWidth = rowWidth / 12;
      const gridUnitsChange = diff / gridUnitWidth;
      
      // Calculate new sizes
      let newColSize = startColSize;
      let newAdjSize = startAdjSize;

      console.log('Calculating new sizes', { rowWidth, diff, gridUnitsChange, startColSize, startAdjSize });
      
      if (side === 'right' || side === 'left') {
        // Column grows, adjacent shrinks
        newColSize = startColSize + gridUnitsChange;
        newAdjSize = startAdjSize - gridUnitsChange;
      } else {
        // Column shrinks, adjacent grows
        newColSize = startColSize - gridUnitsChange;
        newAdjSize = startAdjSize + gridUnitsChange;
      }

      // Round to nearest integer and clamp to valid range
      newColSize = Math.round(newColSize);
      newAdjSize = Math.round(newAdjSize);
      
      // Ensure minimum size of 1 and maximum of 12
      newColSize = Math.max(1, Math.min(12, newColSize));
      newAdjSize = Math.max(1, Math.min(12, newAdjSize));
      
      // Ensure total doesn't exceed 12 (adjust if needed)
      const total = newColSize + newAdjSize;
      if (total > 12) {
        const excess = total - 12;
        if (newColSize > newAdjSize) {
          newColSize -= excess;
        } else {
          newAdjSize -= excess;
        }
      } else if (total < 12) {
        const deficit = 12 - total;
        if (newColSize > newAdjSize) {
          newAdjSize += deficit;
        } else {
          newColSize += deficit;
        }
      }
      
      // Ensure sizes are still valid after adjustment
      newColSize = Math.max(1, Math.min(12, newColSize));
      newAdjSize = Math.max(1, Math.min(12, newAdjSize));
      
      console.log('Checking column sizes', { newColSize, newAdjSize, diff, gridUnitsChange });

      // Only update if sizes actually changed
      if (newColSize !== startColSize || newAdjSize !== startAdjSize) {
        console.log('Updating column sizes', { newColSize, newAdjSize, diff, gridUnitsChange });
        // Update the column classes immediately for visual feedback
        this.updateColumnSizes(rowId, columnId, adjacentColumn.id, newColSize, newAdjSize);
        // Update start sizes for next iteration
        startColSize = newColSize;
        startAdjSize = newAdjSize;
      }
    };
    
    document.addEventListener('mousemove', mousemoveHandler);
    
    document.addEventListener('mouseup', () => {
      if (isResizing) {
        isResizing = false;
        handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Re-enable row dragging
        if (rowElement) {
          rowElement.draggable = true;
          rowElement.style.pointerEvents = '';
        }
        
        // Remove mousemove handler
        document.removeEventListener('mousemove', mousemoveHandler);
        
        adjacentColumn = null;
        rowElement = null;
        gridContainer = null;
        
        // Re-render the row to ensure classes are properly applied
        const row = this.editor.getLayoutManager().getRow(rowId);
        this.renderRow(rowId, row);
        this.editor.saveToHistory();
      }
    });
  }
  
  /**
   * Update column sizes in the data model
   */
  updateColumnSizes(rowId, columnId, adjacentColumnId, newColSize, newAdjSize) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    const column = row.columns.find(c => c.id === columnId);
    const adjacent = row.columns.find(c => c.id === adjacentColumnId);
    
    if (!column || !adjacent) {
      console.error('Column or adjacent column not found', { columnId, adjacentColumnId });
      return;
    }
    
    console.log('updateColumnSizes called', { newColSize, newAdjSize, currentColClass: column.cssClass, currentAdjClass: adjacent.cssClass });
    
    // Get existing classes (preserve medium, large, and other classes)
    const getSizeClasses = (cssClass) => {
      const classes = cssClass.split(' ').filter(c => c.trim());
      return {
        small: classes.find(c => c.startsWith('small-')) || 'small-12',
        medium: classes.find(c => c.startsWith('medium-')) || '',
        large: classes.find(c => c.startsWith('large-')) || '',
        other: classes.filter(c => !c.startsWith('small-') && !c.startsWith('medium-') && !c.startsWith('large-') && c !== 'cell' && c !== 'canvas-column')
      };
    };
    
    const colClasses = getSizeClasses(column.cssClass);
    const adjClasses = getSizeClasses(adjacent.cssClass);
    
    // Update small sizes
    colClasses.small = `small-${newColSize}`;
    adjClasses.small = `small-${newAdjSize}`;
    
    // Rebuild CSS classes (without canvas-column, that's added separately)
    column.cssClass = [colClasses.small, colClasses.medium, colClasses.large, ...colClasses.other, 'cell'].filter(c => c).join(' ');
    adjacent.cssClass = [adjClasses.small, adjClasses.medium, adjClasses.large, ...adjClasses.other, 'cell'].filter(c => c).join(' ');
    
    console.log('Updated CSS classes', { newColClass: column.cssClass, newAdjClass: adjacent.cssClass });
    
    // Update the DOM immediately for visual feedback
    const columnElement = document.querySelector(`[data-column-id="${columnId}"]`);
    const adjacentElement = document.querySelector(`[data-column-id="${adjacentColumnId}"]`);
    
    if (!columnElement || !adjacentElement) {
      console.error('Column elements not found in DOM', { columnElement: !!columnElement, adjacentElement: !!adjacentElement });
      return;
    }
    
    // Update column element
    const updateElementClasses = (element, newCssClass) => {
      if (!element) return;
      
      // Remove all grid-related classes
      const classesToRemove = [];
      for (let i = 0; i < element.classList.length; i++) {
        const cls = element.classList[i];
        if (cls.startsWith('small-') || cls.startsWith('medium-') || cls.startsWith('large-') || cls === 'cell') {
          classesToRemove.push(cls);
        }
      }
      classesToRemove.forEach(cls => element.classList.remove(cls));
      
      // Add new grid classes
      const newClasses = newCssClass.split(' ').filter(c => c.trim());
      newClasses.forEach(cls => {
        if (cls) {
          element.classList.add(cls);
        }
      });
    };
    
    console.log('Updating column element', { 
      columnId,
      oldClassName: columnElement.className, 
      newClassName: column.cssClass
    });
    
    updateElementClasses(columnElement, column.cssClass);
    
    console.log('Updating adjacent element', { 
      adjacentColumnId,
      oldClassName: adjacentElement.className, 
      newClassName: adjacent.cssClass
    });
    
    updateElementClasses(adjacentElement, adjacent.cssClass);
    
    // Force a reflow to ensure Foundation recalculates the grid
    if (columnElement) {
      void columnElement.offsetHeight;
    }
    if (adjacentElement) {
      void adjacentElement.offsetHeight;
    }

    // Tell the layout manager about the change
    this.editor.getLayoutManager().updateRow(rowId, row);
  }
  
  
  /**
   * Add column before the specified column
   */
  addColumnBefore(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    const columnIndex = row.columns.findIndex(c => c.id === columnId);
    if (columnIndex === -1) return;
    
    // Create new column with default size
    const newColumnId = 'col-' + (this.editor.getLayoutManager().nextColumnId++);
    const newColumn = {
      id: newColumnId,
      cssClass: 'small-6 cell',
      widgets: []
    };
    
    row.columns.splice(columnIndex, 0, newColumn);
    
    // Adjust existing column sizes to fit
    this.adjustColumnSizes(row);
    
    // Re-render
    this.renderRow(rowId, row);
    this.editor.saveToHistory();
  }
  
  /**
   * Add column after the specified column
   */
  addColumnAfter(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    const columnIndex = row.columns.findIndex(c => c.id === columnId);
    if (columnIndex === -1) return;
    
    // Create new column with default size
    const newColumnId = 'col-' + (this.editor.getLayoutManager().nextColumnId++);
    const newColumn = {
      id: newColumnId,
      cssClass: 'small-6 cell',
      widgets: []
    };
    
    row.columns.splice(columnIndex + 1, 0, newColumn);
    
    // Adjust existing column sizes to fit
    this.adjustColumnSizes(row);
    
    // Re-render
    this.renderRow(rowId, row);
    this.editor.saveToHistory();
  }
  
  /**
   * Adjust column sizes to fit within 12-column grid
   */
  adjustColumnSizes(row) {
    const numColumns = row.columns.length;
    if (numColumns === 0) return;
    
    // Calculate base size per column
    const baseSize = Math.floor(12 / numColumns);
    const remainder = 12 % numColumns;
    
    row.columns.forEach((col, index) => {
      const size = baseSize + (index < remainder ? 1 : 0);
      const classes = col.cssClass.split(' ').filter(c => !c.startsWith('small-') && !c.startsWith('medium-') && !c.startsWith('large-'));
      col.cssClass = `small-${size} ${classes.join(' ')}`.trim();
      if (!col.cssClass.includes('cell')) {
        col.cssClass += ' cell';
      }
    });
  }
  
  /**
   * Move column left
   */
  moveColumnLeft(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    const columnIndex = row.columns.findIndex(c => c.id === columnId);
    if (columnIndex <= 0) return; // Can't move left if already first
    
    // Swap with previous column
    [row.columns[columnIndex - 1], row.columns[columnIndex]] = [row.columns[columnIndex], row.columns[columnIndex - 1]];
    
    // Re-render
    this.renderRow(rowId, row);
    this.editor.saveToHistory();
  }
  
  /**
   * Move column right
   */
  moveColumnRight(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    const columnIndex = row.columns.findIndex(c => c.id === columnId);
    if (columnIndex === -1 || columnIndex >= row.columns.length - 1) return; // Can't move right if already last
    
    // Swap with next column
    [row.columns[columnIndex], row.columns[columnIndex + 1]] = [row.columns[columnIndex + 1], row.columns[columnIndex]];
    
    // Re-render
    this.renderRow(rowId, row);
    this.editor.saveToHistory();
  }
  
  /**
   * Delete a column
   */
  deleteColumn(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    if (row.columns.length <= 1) {
      alert('Cannot delete the last column in a row. Delete the row instead.');
      return;
    }
    
    if (confirm('Are you sure you want to delete this column? All widgets in it will be removed.')) {
      const columnIndex = row.columns.findIndex(c => c.id === columnId);
      if (columnIndex !== -1) {
        row.columns.splice(columnIndex, 1);
        
        // Adjust remaining column sizes
        this.adjustColumnSizes(row);
        
        // Re-render
        this.renderRow(rowId, row);
        this.editor.saveToHistory();
        
        // Clear selection
        this.selectedElement = null;
        this.selectedContext = null;
        this.editor.getPropertiesPanel().clear();
      }
    }
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
      const defaultProps = this.editor.getLayoutManager().getDefaultProperties(widget.type);
      widget.properties = defaultProps;
      console.log('Applied default properties to widget:', widget.id, widget.properties);
    } else {
      console.log('Widget already has properties:', widget.id, widget.properties);
    }
    
    // Create widget preview after defaults are set
    const preview = this.renderWidgetPreview(widget);
    console.log('Preview for widget', widget.id, ':', preview);
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
      console.log("No properties found for widget:", widget.id);
      return '<div style="font-size:12px;color:#999;">Widget configuration</div>';
    }
    
    const previewValue = this.getPreviewValue(props);
    if (previewValue) {
      console.log("Preview value for widget", widget.id, ":", previewValue);
      return `<div style="font-size:12px;color: var(--editor-text-muted);">${previewValue}</div>`;
    }
    console.log("No preview value could be determined for widget:", widget.id);
    return '<div style="font-size:12px;color:#999;">Widget configuration</div>';
  }
  
  /**
   * Get the display value for widget preview
   */
  getPreviewValue(props) {
    // Check for priority properties first
    const priorityKeys = ['uniqueId', 'name', 'title', 'heading', 'text', 'label'];
    for (const key of priorityKeys) {
      if (props[key]) {
        const value = (key === 'label' && props.value) ? props[key] + ': ' + props.value : props[key];
        console.log(`Found preview value in "${key}":`, value);
        return value;
      }
    }
    
    // Check for any property key containing 'UniqueId'
    const uniqueIdKey = Object.keys(props).find(key => key.includes('UniqueId') && props[key]);
    if (uniqueIdKey) {
      console.log(`Found preview value in "${uniqueIdKey}":`, props[uniqueIdKey]);
      return props[uniqueIdKey];
    }
    
    // Return the first non-empty string property value
    const firstString = Object.values(props).find(value => typeof value === 'string' && value.trim());
    if (firstString) {
      console.log('Found preview value from first string property:', firstString);
    } else {
      console.log('No preview value found in properties:', Object.keys(props));
    }
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
