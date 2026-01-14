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
    this.addViewportIndicator();
    this.setupViewportChangeListener();
    console.log('Canvas Controller initialized');
  }

  /**
   * Add viewport indicator to canvas
   */
  addViewportIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'viewport-indicator';
    indicator.id = 'viewport-indicator';
    this.canvas.appendChild(indicator);
    this.updateViewportIndicator();
  }

  /**
   * Update viewport indicator text
   */
  updateViewportIndicator() {
    const indicator = document.getElementById('viewport-indicator');
    if (indicator) {
      const currentViewport = this.editor.getViewportManager().getCurrentViewport();
      const config = this.editor.getViewportManager().getViewportConfig(currentViewport);
      indicator.textContent = config.name + ' View';
    }
  }

  /**
   * Setup listener for viewport changes
   */
  setupViewportChangeListener() {
    document.addEventListener('viewportChanged', (e) => {
      this.updateViewportIndicator();
      
      // Re-render the layout to apply viewport-specific column classes
      const structure = this.editor.getLayoutManager().getStructure();
      this.renderLayout(structure);
      
      console.log('Canvas updated for viewport change:', e.detail.current);
    });
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
      console.log('Row delete button clicked');
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
    columnElement.className = 'canvas-column cell ' + column.cssClass;
    columnElement.setAttribute('data-column-id', column.id);
    columnElement.setAttribute('data-row-id', rowId);
    
    // Create content wrapper for proper spacing
    const contentWrapper = document.createElement('div');
    contentWrapper.className = 'column-content';
    contentWrapper.style.cssText = 'padding: 10px; min-height: 40px;';
    
    // Add column controls to content wrapper
    const controls = this.createColumnControls(rowId, column.id, columnElement);
    
    // Add resize handles to the column element (outside content wrapper)
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
    
    contentWrapper.appendChild(leftResizeHandle);
    contentWrapper.appendChild(rightResizeHandle);
    columnElement.appendChild(controls);
    columnElement.appendChild(contentWrapper);

    
    // Make it a drop zone for widgets
    const dropZoneId = this.editor.dragDropManager.registerDropZone(columnElement, {
      accept: ['widget'],
      onDrop: (e, type, data) => {
        this.editor.dragDropManager.handleColumnDrop(columnElement, rowId, column.id);
      }
    });
    
    // Render widgets in content wrapper
    let emptyMsg = null;
    if (column.widgets && column.widgets.length > 0) {
      for (const widget of column.widgets) {
        const widgetElement = this.renderWidget(rowId, column.id, widget);
        contentWrapper.appendChild(widgetElement);
      }
    } else {
      emptyMsg = document.createElement('div');
      emptyMsg.style.cssText = 'text-align:center;color:var(--editor-text-muted);padding:20px;';
      emptyMsg.textContent = 'Drop widget here';
      contentWrapper.appendChild(emptyMsg);
    }
    
    // Make column clickable for selection
    columnElement.addEventListener('click', (e) => {
      if (e.target === columnElement || e.target === contentWrapper || (emptyMsg && e.target === emptyMsg)) {
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
    controls.style.cssText = 'position:absolute;top:3px;right:5px;z-index:1;';
    
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

    // Settings button
    const settingsBtn = document.createElement('button');
    settingsBtn.className = 'control-btn';
    settingsBtn.innerHTML = '<i class="fa fa-cog"></i>';
    settingsBtn.title = 'Column Settings';
    settingsBtn.onclick = (e) => {
      e.stopPropagation();
      this.selectElement(columnElement, { type: 'column', rowId, columnId: columnId });
    };
    
    // Delete Column button
    const deleteBtn = document.createElement('button');
    deleteBtn.className = 'control-btn danger';
    deleteBtn.innerHTML = '<i class="fa fa-trash"></i>';
    deleteBtn.title = 'Delete Column';
    deleteBtn.onclick = (e) => {
      console.log('Column delete button clicked');
      e.stopPropagation();
      this.deleteColumn(rowId, columnId);
    };
    
    controls.appendChild(moveLeftBtn);
    controls.appendChild(moveRightBtn);
    controls.appendChild(addBeforeBtn);
    controls.appendChild(addAfterBtn);
    controls.appendChild(settingsBtn);
    controls.appendChild(deleteBtn);
    
    return controls;
  }
  
  /**
   * Setup column resize functionality
   */
  setupColumnResize(handle, rowId, columnId, side) {
    let isResizing = false;
    let startX = 0;
    let startColSize = 0;
    let startAdjSize = 0;
    let adjacentColumn = null;
    let lastUpdateX = 0; // Track last update position
    let rowElement = null;
    let gridContainer = null;
    let columnIndex = -1;
    let totalColumns = 0;
    
    handle.addEventListener('mousedown', (e) => {
      e.stopPropagation();
      e.preventDefault();
      
      console.log('Column resize started', { rowId, columnId, side });
      
      isResizing = true;
      startX = e.clientX;
      lastUpdateX = e.clientX; // Initialize last update position
      
      // Find the row element and disable its dragging temporarily
      rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
      if (rowElement) {
        rowElement.draggable = false;
        rowElement.style.pointerEvents = 'none';
        gridContainer = rowElement.querySelector('.grid-x');
      }
      
      if (!gridContainer) {
        console.error('Grid container not found');
        
        // Clean up UI state before returning
        isResizing = false;
        handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Re-enable row dragging
        if (rowElement) {
          rowElement.draggable = true;
          rowElement.style.pointerEvents = '';
        }
        
        return;
      }
      
      const row = this.editor.getLayoutManager().getRow(rowId);
      if (!row) {
        console.error('Row not found');
        
        // Clean up UI state before returning
        isResizing = false;
        handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Re-enable row dragging
        if (rowElement) {
          rowElement.draggable = true;
          rowElement.style.pointerEvents = '';
        }
        
        return;
      }
      
      columnIndex = row.columns.findIndex(c => c.id === columnId);
      totalColumns = row.columns.length;
      
      if (columnIndex === -1) {
        console.error('Column not found');
        
        // Clean up UI state before returning
        isResizing = false;
        handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Re-enable row dragging
        if (rowElement) {
          rowElement.draggable = true;
          rowElement.style.pointerEvents = '';
        }
        
        return;
      }
      
      const column = row.columns[columnIndex];
      
      // Enhanced logic to handle edge cases
      if (totalColumns === 1) {
        // Single column - can't resize
        console.log('Cannot resize single column');
        
        // Clean up UI state before returning
        isResizing = false;
        handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Re-enable row dragging
        if (rowElement) {
          rowElement.draggable = true;
          rowElement.style.pointerEvents = '';
        }
        
        return;
      }
      
      // Find adjacent column - enhanced logic for last column
      if (side === 'left' && columnIndex > 0) {
        // Left handle - resize with left neighbor
        adjacentColumn = row.columns[columnIndex - 1];
      } else if (side === 'right' && columnIndex < row.columns.length - 1) {
        // Right handle - resize with right neighbor
        adjacentColumn = row.columns[columnIndex + 1];
      } else if (side === 'right' && columnIndex === row.columns.length - 1) {
        // Special case: right handle on last column - allow minimizing by creating space for a new column
        console.log('Right handle on last column - allowing resize to make room for new column');
        adjacentColumn = null; // We'll handle this case specially
      } else {
        // Edge case: no appropriate neighbor for this handle
        console.log('No appropriate adjacent column for this handle');
        
        // Clean up UI state before returning
        isResizing = false;
        handle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Re-enable row dragging
        if (rowElement) {
          rowElement.draggable = true;
          rowElement.style.pointerEvents = '';
        }
        
        return;
      }
      
      if (!adjacentColumn && !(side === 'right' && columnIndex === row.columns.length - 1)) {
        console.log('No adjacent column available for resizing');
        isResizing = false;
        return;
      }
      
      // Get current grid sizes for the current viewport
      const parseSize = (cssClass) => {
        const currentViewport = this.editor.getViewportManager().getCurrentViewport();
        const classes = cssClass.split(' ');
        const viewportClass = classes.find(c => c.startsWith(currentViewport + '-'));
        
        if (viewportClass) {
          const match = viewportClass.match(/(\w+)-(\d+)/);
          return match ? parseInt(match[2], 10) : 12;
        }
        
        // Fallback to small if current viewport not found
        const smallClass = classes.find(c => c.startsWith('small-'));
        if (smallClass) {
          const match = smallClass.match(/small-(\d+)/);
          return match ? parseInt(match[1], 10) : 12;
        }
        
        return 12;
      };
      
      startColSize = parseSize(column.cssClass);
      startAdjSize = adjacentColumn ? parseSize(adjacentColumn.cssClass) : 0;
      
      console.log('Resize initialized', { 
        startColSize, 
        startAdjSize, 
        columnIndex, 
        totalColumns, 
        side,
        adjacentColumnId: adjacentColumn ? adjacentColumn.id : 'none (last column resize)',
        rowWidth: gridContainer.offsetWidth 
      });
      
      handle.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    });
    
    const mousemoveHandler = (e) => {
      if (!isResizing || !gridContainer) return;
      
      // Get the current row data
      const row = this.editor.getLayoutManager().getRow(rowId);
      if (!row) {
        console.error('Row not found during resize');
        return;
      }
      
      // Special case: last column resize without adjacent column
      if (!adjacentColumn && !(side === 'right' && columnIndex === row.columns.length - 1)) {
        return;
      }
      
      e.preventDefault();
      e.stopPropagation();
      
      // Calculate pixel difference from last update (not from start)
      const pixelDiff = e.clientX - lastUpdateX;
      
      // Get the actual row width (grid container width)
      const rowWidth = gridContainer.offsetWidth;
      if (rowWidth === 0) {
        console.warn('Row width is 0, cannot resize');
        return;
      }
      
      // Calculate how many grid units this pixel difference represents
      const gridUnitWidth = rowWidth / 12;
      
      // Add sensitivity control - require more pixels per grid unit change
      const sensitivity = 2; // Require 2x the grid unit width to change by 1 unit
      let gridUnitsChange = pixelDiff / (gridUnitWidth * sensitivity);
      
      // Only round when we have a significant change (at least 0.5 units)
      if (Math.abs(gridUnitsChange) < 0.5) {
        return; // Don't update for very small movements
      }
      
      // Round to nearest integer for actual grid changes
      gridUnitsChange = Math.round(gridUnitsChange);
      
      // Skip if no actual change
      if (gridUnitsChange === 0) {
        return;
      }
      
      // Handle special case: last column resize (no adjacent column)
      if (!adjacentColumn && side === 'right' && columnIndex === row.columns.length - 1) {
        // Right handle on last column - allow minimizing to make room for new column
        let newColSize = startColSize + gridUnitsChange;
        
        // Calculate current total used by all columns
        let currentTotal = 0;
        for (const col of row.columns) {
          const colSize = this.parseColumnSize(col.cssClass);
          currentTotal += colSize;
        }
        
        // Ensure the new size doesn't make total exceed 12 or go below 1
        const otherColumnsTotal = currentTotal - startColSize;
        const maxAllowedSize = 12 - otherColumnsTotal;
        newColSize = Math.max(1, Math.min(maxAllowedSize, newColSize));
        
        // Only update if size actually changed
        if (newColSize !== startColSize) {
          console.log('Updating last column size', { 
            side,
            pixelDiff, 
            gridUnitsChange, 
            startColSize,
            newColSize,
            currentTotal,
            otherColumnsTotal,
            maxAllowedSize
          });
          
          // Update just this column
          this.updateSingleColumnSize(rowId, columnId, newColSize);
          
          // Update start size and last update position for next iteration
          startColSize = newColSize;
          lastUpdateX = e.clientX;
        }
        return;
      }
      
      // Normal case: resize with adjacent column
      let newColSize, newAdjSize;
      
      if (side === 'right') {
        // Right handle: moving right increases column, moving left decreases column
        newColSize = startColSize + gridUnitsChange;
        newAdjSize = startAdjSize - gridUnitsChange;
      } else if (side === 'left') {
        // Left handle: moving right decreases column (expanding left neighbor), moving left increases column
        newColSize = startColSize - gridUnitsChange;
        newAdjSize = startAdjSize + gridUnitsChange;
      }
      
      // Ensure minimum size of 1 and maximum of 11 (leave at least 1 for adjacent)
      newColSize = Math.max(1, Math.min(11, newColSize));
      newAdjSize = Math.max(1, Math.min(11, newAdjSize));
      
      // Ensure total equals 12 (Foundation grid total)
      const total = newColSize + newAdjSize;
      if (total !== 12) {
        // Adjust the adjacent column to maintain total of 12
        newAdjSize = 12 - newColSize;
        // Ensure adjacent doesn't go below 1
        if (newAdjSize < 1) {
          newAdjSize = 1;
          newColSize = 11;
        }
      }
      
      // Only update if sizes actually changed
      if (newColSize !== startColSize || newAdjSize !== startAdjSize) {
        console.log('Updating column sizes', { 
          side,
          pixelDiff, 
          gridUnitsChange, 
          startColSize,
          startAdjSize,
          newColSize, 
          newAdjSize,
          total: newColSize + newAdjSize
        });
        
        // Update the column classes immediately for visual feedback
        this.updateColumnSizes(rowId, columnId, adjacentColumn.id, newColSize, newAdjSize);
        
        // Update start sizes and last update position for next iteration
        startColSize = newColSize;
        startAdjSize = newAdjSize;
        lastUpdateX = e.clientX; // Update the reference point
      }
    };
    
    document.addEventListener('mousemove', mousemoveHandler);
    
    const mouseupHandler = () => {
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
        
        // Remove event handlers
        document.removeEventListener('mousemove', mousemoveHandler);
        document.removeEventListener('mouseup', mouseupHandler);
        
        adjacentColumn = null;
        rowElement = null;
        gridContainer = null;
        
        // Re-render the row to ensure classes are properly applied
        const row = this.editor.getLayoutManager().getRow(rowId);
        if (row) {
          this.renderRow(rowId, row);
          this.editor.saveToHistory();
        }
      }
    };
    
    document.addEventListener('mouseup', mouseupHandler);
    
    // Also add escape key handler to cancel resize
    const escapeHandler = (e) => {
      if (e.key === 'Escape' && isResizing) {
        console.log('Resize cancelled by escape key');
        mouseupHandler(); // Use the same cleanup
        document.removeEventListener('keydown', escapeHandler);
      }
    };
    
    document.addEventListener('keydown', escapeHandler);
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
    
    // Get current viewport from viewport manager
    const currentViewport = this.editor.getViewportManager().getCurrentViewport();
    
    // Simplified class update - just update the current viewport size
    const updateColumnClass = (cssClass, newSize) => {
      const classes = cssClass.split(' ').filter(c => c.trim());
      const otherClasses = [];
      let hasCurrentViewport = false;
      
      // Remove old viewport classes and keep other classes
      classes.forEach(cls => {
        if (cls.match(/^(small|medium|large)-\d+$/)) {
          const [viewport] = cls.split('-');
          if (viewport === currentViewport) {
            hasCurrentViewport = true;
            // Skip this class - we'll add the new size
          } else {
            // Keep other viewport classes
            otherClasses.push(cls);
          }
        } else if (cls !== 'canvas-column') {
          otherClasses.push(cls);
        }
      });
      
      // Add the new size for current viewport
      otherClasses.push(`${currentViewport}-${newSize}`);
      
      return otherClasses.filter(c => c).join(' ');
    };
    
    column.cssClass = updateColumnClass(column.cssClass, newColSize);
    adjacent.cssClass = updateColumnClass(adjacent.cssClass, newAdjSize);
    
    console.log('Updated CSS classes for viewport', currentViewport, { 
      newColClass: column.cssClass, 
      newAdjClass: adjacent.cssClass 
    });
    
    // Update the DOM immediately for visual feedback
    const columnElement = document.querySelector(`[data-column-id="${columnId}"]`);
    const adjacentElement = document.querySelector(`[data-column-id="${adjacentColumnId}"]`);
    
    if (!columnElement || !adjacentElement) {
      console.error('Column elements not found in DOM', { columnElement: !!columnElement, adjacentElement: !!adjacentElement });
      return;
    }
    
    // Update column element classes
    const updateElementClasses = (element, newCssClass, newSize) => {
      if (!element) return;
      
      // Remove current viewport grid classes only
      const classesToRemove = [];
      for (let i = 0; i < element.classList.length; i++) {
        const cls = element.classList[i];
        if (cls.startsWith(`${currentViewport}-`)) {
          classesToRemove.push(cls);
        }
      }
      classesToRemove.forEach(cls => element.classList.remove(cls));
      
      // Add new current viewport class
      element.classList.add(`${currentViewport}-${newSize}`);
    };
    
    console.log('Updating column element', { 
      columnId,
      oldClassName: columnElement.className, 
      newSize: newColSize
    });
    
    updateElementClasses(columnElement, column.cssClass, newColSize);
    
    console.log('Updating adjacent element', { 
      adjacentColumnId,
      oldClassName: adjacentElement.className, 
      newSize: newAdjSize
    });
    
    updateElementClasses(adjacentElement, adjacent.cssClass, newAdjSize);
    
    // Tell the layout manager about the change
    this.editor.getLayoutManager().updateRow(rowId, row);
  }
  
  /**
   * Parse column size from CSS class for current viewport
   */
  parseColumnSize(cssClass) {
    const currentViewport = this.editor.getViewportManager().getCurrentViewport();
    const classes = cssClass.split(' ');
    const viewportClass = classes.find(c => c.startsWith(currentViewport + '-'));
    
    if (viewportClass) {
      const match = viewportClass.match(/(\w+)-(\d+)/);
      return match ? parseInt(match[2], 10) : 12;
    }
    
    // Fallback to small if current viewport not found
    const smallClass = classes.find(c => c.startsWith('small-'));
    if (smallClass) {
      const match = smallClass.match(/small-(\d+)/);
      return match ? parseInt(match[1], 10) : 12;
    }
    
    return 12;
  }

  /**
   * Update a single column size in the data model
   */
  updateSingleColumnSize(rowId, columnId, newSize) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    const column = row.columns.find(c => c.id === columnId);
    
    if (!column) {
      console.error('Column not found', { columnId });
      return;
    }
    
    console.log('updateSingleColumnSize called', { newSize, currentColClass: column.cssClass });
    
    // Get current viewport from viewport manager
    const currentViewport = this.editor.getViewportManager().getCurrentViewport();
    
    // Update column class for current viewport
    const updateColumnClass = (cssClass, newSize) => {
      const classes = cssClass.split(' ').filter(c => c.trim());
      const otherClasses = [];
      
      // Remove old viewport classes and keep other classes
      classes.forEach(cls => {
        if (cls.match(/^(small|medium|large)-\d+$/)) {
          const [viewport] = cls.split('-');
          if (viewport !== currentViewport) {
            // Keep other viewport classes
            otherClasses.push(cls);
          }
          // Skip current viewport class - we'll add the new size
        } else if (cls !== 'canvas-column') {
          otherClasses.push(cls);
        }
      });
      
      // Add the new size for current viewport
      otherClasses.push(`${currentViewport}-${newSize}`);
      
      return otherClasses.filter(c => c).join(' ');
    };
    
    column.cssClass = updateColumnClass(column.cssClass, newSize);
    
    console.log('Updated CSS class for viewport', currentViewport, { 
      newColClass: column.cssClass
    });
    
    // Update the DOM immediately for visual feedback
    const columnElement = document.querySelector(`[data-column-id="${columnId}"]`);
    
    if (!columnElement) {
      console.error('Column element not found in DOM');
      return;
    }
    
    // Update column element classes
    const classesToRemove = [];
    for (let i = 0; i < columnElement.classList.length; i++) {
      const cls = columnElement.classList[i];
      if (cls.startsWith(`${currentViewport}-`)) {
        classesToRemove.push(cls);
      }
    }
    classesToRemove.forEach(cls => columnElement.classList.remove(cls));
    
    // Add new current viewport class
    columnElement.classList.add(`${currentViewport}-${newSize}`);
    
    console.log('Updated single column element', { 
      columnId,
      oldClassName: columnElement.className, 
      newSize: newSize
    });
    
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
    this.adjustColumnSizes(row, rowId);
    
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
    this.adjustColumnSizes(row, rowId);
    
    // Re-render
    this.renderRow(rowId, row);
    this.editor.saveToHistory();
  }
  
  /**
   * Adjust column sizes to fit within 12-column grid
   * Enhanced to use remaining space when available instead of resizing existing columns
   */
  adjustColumnSizes(row, rowId) {
    const numColumns = row.columns.length;
    if (numColumns === 0) return;
    
    // Calculate current total used by existing columns (excluding the last added one)
    let currentTotal = 0;
    let hasNewColumn = false;
    
    // Check if we have a column with default size (likely newly added)
    const defaultSizeColumns = row.columns.filter(col => {
      const size = this.parseColumnSize(col.cssClass);
      return size === 6; // Default size from addColumnBefore/After
    });
    
    // If we have exactly one column with size 6 and others with different sizes,
    // it's likely a newly added column
    if (defaultSizeColumns.length === 1 && numColumns > 1) {
      const newColumn = defaultSizeColumns[0];
      const otherColumns = row.columns.filter(col => col.id !== newColumn.id);
      
      // Calculate space used by existing columns
      let existingTotal = 0;
      otherColumns.forEach(col => {
        existingTotal += this.parseColumnSize(col.cssClass);
      });
      
      // If there's remaining space, use it for the new column
      const remainingSpace = 12 - existingTotal;
      if (remainingSpace > 0 && remainingSpace <= 12) {
        console.log('Using remaining space for new column', {
          existingTotal,
          remainingSpace,
          newColumnId: newColumn.id
        });
        
        // Update the new column to use the remaining space
        this.updateSingleColumnSize(row.id, newColumn.id, remainingSpace);
        return; // Don't resize existing columns
      }
    }
    
    // Fallback to original behavior if no remaining space or other conditions not met
    console.log('No remaining space available, resizing all columns proportionally');
    
    // Calculate base size per column
    const baseSize = Math.floor(12 / numColumns);
    const remainder = 12 % numColumns;
    
    row.columns.forEach((col, index) => {
      const size = baseSize + (index < remainder ? 1 : 0);
      
      // Parse existing classes to preserve non-size classes and inheritance pattern
      const classes = col.cssClass.split(' ').filter(c => c.trim());
      const viewportSizes = {};
      const otherClasses = [];
      
      classes.forEach(cls => {
        if (cls.match(/^(small|medium|large)-\d+$/)) {
          const [viewport, sizeStr] = cls.split('-');
          viewportSizes[viewport] = parseInt(sizeStr, 10);
        } else if (cls !== 'cell') {
          otherClasses.push(cls);
        }
      });
      
      // Update all existing viewport sizes proportionally
      const oldSmallSize = viewportSizes.small || 12;
      const ratio = size / oldSmallSize;
      
      Object.keys(viewportSizes).forEach(viewport => {
        const newSize = Math.round(viewportSizes[viewport] * ratio);
        viewportSizes[viewport] = Math.max(1, Math.min(12, newSize));
      });
      
      // Always ensure we have at least a small size
      if (!viewportSizes.small) {
        viewportSizes.small = size;
      }
      
      // Build class string following Foundation inheritance pattern
      const sizeClasses = [];
      
      // Always include small (base size)
      sizeClasses.push(`small-${viewportSizes.small}`);
      
      // Only include medium if different from small
      if (viewportSizes.medium && viewportSizes.medium !== viewportSizes.small) {
        sizeClasses.push(`medium-${viewportSizes.medium}`);
      }
      
      // Only include large if different from inherited value
      const inheritedLargeSize = viewportSizes.medium || viewportSizes.small;
      if (viewportSizes.large && viewportSizes.large !== inheritedLargeSize) {
        sizeClasses.push(`large-${viewportSizes.large}`);
      }
      
      col.cssClass = [...sizeClasses, ...otherClasses, 'cell'].filter(c => c).join(' ');
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
  async deleteColumn(rowId, columnId) {
    console.log('deleteColumn called with rowId:', rowId, 'columnId:', columnId);
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) {
      console.log('Row not found');
      return;
    }
    
    if (row.columns.length <= 1) {
      this.editor.showAlertDialog('Cannot delete the last column in a row. Delete the row instead.');
      return;
    }
    
    const confirmed = await this.editor.showConfirmDialog('Are you sure you want to delete this column? All widgets in it will be removed.');
    if (confirmed) {
      console.log('User confirmed column deletion');
      const columnIndex = row.columns.findIndex(c => c.id === columnId);
      if (columnIndex !== -1) {
        row.columns.splice(columnIndex, 1);
        console.log('Column removed from row data');
        
        // Adjust remaining column sizes
        this.adjustColumnSizes(row, rowId);
        
        // Re-render
        this.renderRow(rowId, row);
        this.editor.saveToHistory();
        
        // Clear selection
        this.selectedElement = null;
        this.selectedContext = null;
        this.editor.getPropertiesPanel().clear();
        console.log('Column deletion completed');
      } else {
        console.log('Column not found in row');
      }
    } else {
      console.log('User cancelled column deletion');
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
      console.log('Widget delete button clicked');
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
  async deleteRow(rowId) {
    console.log('deleteRow called with rowId:', rowId);
    const confirmed = await this.editor.showConfirmDialog('Are you sure you want to delete this row? All widgets in it will be removed.');
    if (confirmed) {
      console.log('User confirmed deletion, calling removeRow');
      this.editor.getLayoutManager().removeRow(rowId);
      
      // Remove from DOM
      const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
      if (rowElement) {
        rowElement.remove();
        console.log('Row element removed from DOM');
      } else {
        console.log('Row element not found in DOM');
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
    } else {
      console.log('User cancelled deletion');
    }
  }
  
  /**
   * Delete a widget
   */
  async deleteWidget(rowId, columnId, widgetId) {
    console.log('deleteWidget called with rowId:', rowId, 'columnId:', columnId, 'widgetId:', widgetId);
    const confirmed = await this.editor.showConfirmDialog('Are you sure you want to delete this widget?');
    if (confirmed) {
      console.log('User confirmed widget deletion');
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
      console.log('Widget deletion completed');
    } else {
      console.log('User cancelled widget deletion');
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
