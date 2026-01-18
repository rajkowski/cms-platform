/**
 * PropertyEditorBridge - Handles integration with the existing Property Editor
 * 
 * Translates element information to Property Editor API calls and manages
 * the opening of element properties based on element type and ID.
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4
 */
class PropertyEditorBridge {
  
  /**
   * Initialize the Property Editor bridge
   * @param {Object} propertyEditorAPI - Property Editor API interface
   * @param {Object} [options={}] - Optional configuration
   * @param {HTMLElement} [options.iframeElement] - Optional iframe element for accessing preview DOM
   */
  constructor(propertyEditorAPI, options) {
    this.propertyEditorAPI = propertyEditorAPI;
    this.isInitialized = false;
    this.errorCount = 0;
    this.maxErrors = 5; // Maximum errors before showing persistent warning
    
    // Store iframe context if provided
    const opts = options || {};
    this.iframeElement = opts.iframeElement || null;
    this.iframeDoc = this.iframeElement && this.iframeElement.contentDocument ? this.iframeElement.contentDocument : null;
    
    // Validate API availability
    this._validateAPI();
  }
  
  /**
   * Refresh the iframe document reference (call this when preview is reloaded)
   * @returns {HTMLDocument|null} The updated iframe document or null
   */
  refreshIframeDocReference() {
    if (this.iframeElement) {
      this.iframeDoc = this.iframeElement.contentDocument || this.iframeElement.contentWindow?.document || null;
      if (!this.iframeDoc) {
        console.warn('PropertyEditorBridge: Could not refresh iframe document reference');
      }
    }
    return this.iframeDoc;
  }
  
  /**
   * Get the canvas element from the main editor
   * @private
   * @param {string} dataId - The data-id attribute value (row-X, col-X-Y, or widget-X-Y-Z)
   * @returns {HTMLElement|null} The canvas element or null
   */
  _getCanvasElement(dataId) {
    if (!dataId) return null;
    
    // Try to find element in the canvas editor area
    const canvasElement = document.querySelector(`[data-id="${dataId}"]`);
    return canvasElement;
  }
  
  /**
   * Parse data ID to extract type, row, column, widget info
   * @private
   * @param {string} dataId - The data-id attribute value
   * @returns {Object|null} Context object or null
   */
  _parseDataId(dataId) {
    if (!dataId) return null;
    
    const parts = dataId.split('-');
    if (parts.length < 2) return null;
    
    const type = parts[0]; // 'row', 'col', or 'widget'
    
    if (type === 'row' && parts.length >= 2) {
      return {
        type: 'row',
        rowId: dataId
      };
    } else if (type === 'col' && parts.length >= 3) {
      // col-0-1 format
      return {
        type: 'column',
        rowId: `row-${parts[1]}`,
        columnId: dataId
      };
    } else if (type === 'widget' && parts.length >= 4) {
      // widget-0-0-1 format
      return {
        type: 'widget',
        rowId: `row-${parts[1]}`,
        columnId: `col-${parts[1]}-${parts[2]}`,
        widgetId: dataId
      };
    }
    
    return null;
  }
  
  /**
   * Open widget properties in the Property Editor
   * @param {string} widgetId - Unique widget identifier
   * @param {string} [rowId] - Optional row ID from iframe context
   * @param {string} [columnId] - Optional column ID from iframe context
   * @returns {boolean} True if successfully opened, false otherwise
   */
  openWidgetProperties(widgetId, rowId, columnId) {
    // Check if we've exceeded error threshold
    if (this.errorCount >= this.maxErrors) {
      this._showUserError('Property Editor integration has encountered too many errors and is temporarily disabled. Please refresh the page.');
      return false;
    }
    
    if (!this._validateWidgetId(widgetId)) {
      console.warn('PropertyEditorBridge: Invalid widget ID provided:', widgetId);
      this._showUserError('Invalid widget selected. Please try selecting a different widget.');
      return false;
    }
    
    // Check API availability before attempting to use it
    if (!this._checkAPIAvailability()) {
      this._showUserError('Property Editor is not available. Please ensure the editor is properly loaded.');
      return false;
    }
    
    try {
      // Get the canvas controller from the main editor
      const editor = window.pageEditor;
      const canvasController = editor && editor.getCanvasController ? editor.getCanvasController() : null;
      
      // Map preview ID to canvas ID
      const canvasWidgetId = this._mapPreviewIdToCanvasId(widgetId, 'widget');
      const canvasRowId = rowId ? this._mapPreviewIdToCanvasId(rowId, 'row') : null;
      const canvasColumnId = columnId ? this._mapPreviewIdToCanvasId(columnId, 'column') : null;
      
      console.debug('PropertyEditorBridge: ID mapping results:', {
        previewIds: { widgetId, rowId, columnId },
        canvasIds: { canvasWidgetId, canvasRowId, canvasColumnId }
      });
      
      if (!canvasController) {
        console.error('PropertyEditorBridge: Canvas controller not available');
        this._handleError('Canvas controller not found', new Error('Canvas controller not available'));
        this._showUserError('Editor canvas is not available. Please refresh the page.');
        return false;
      }
      
      // Find the widget element in the main canvas
      // Main canvas uses data-widget-id attribute
      const widgetElement = document.querySelector(`[data-widget-id="${canvasWidgetId}"]`);
      if (!widgetElement) {
        console.error('PropertyEditorBridge: Widget element not found in main canvas:', canvasWidgetId, 'from preview ID:', widgetId);
        this._handleError('Widget not found in main canvas', new Error(`Widget ${canvasWidgetId} not found`));
        this._showUserError('Could not locate the selected widget in the main editor. It may have been removed or modified.');
        return false;
      }
      
      // Use the canvas controller to select the element
      // This will automatically highlight it and show its properties
      const context = {
        type: 'widget',
        rowId: canvasRowId,
        columnId: canvasColumnId,
        widgetId: canvasWidgetId
      };
      
      console.debug('PropertyEditorBridge: Calling canvasController.selectElement with context:', context);
      canvasController.selectElement(widgetElement, context);
      console.debug('PropertyEditorBridge: Widget selected in canvas:', canvasWidgetId, 'from preview ID:', widgetId);
      return true;
      
    } catch (error) {
      console.error('PropertyEditorBridge: Error opening widget properties:', error);
      this._handleError('Error opening widget properties', error);
      this._showUserError('Error opening widget properties. Please try again or refresh the page if the problem persists.');
      return false;
    }
  }
  
  /**
   * Open column properties in the Property Editor
   * @param {string} columnId - Unique column identifier
   * @param {string} [rowId] - Optional row ID (if available from iframe context)
   * @returns {boolean} True if successfully opened, false otherwise
   */
  openColumnProperties(columnId, rowId) {
    // Check if we've exceeded error threshold
    if (this.errorCount >= this.maxErrors) {
      this._showUserError('Property Editor integration has encountered too many errors and is temporarily disabled. Please refresh the page.');
      return false;
    }
    
    if (!this._validateColumnId(columnId)) {
      console.warn('PropertyEditorBridge: Invalid column ID provided:', columnId);
      this._showUserError('Invalid column selected. Please try selecting a different column.');
      return false;
    }
    
    // Check API availability before attempting to use it
    if (!this._checkAPIAvailability()) {
      this._showUserError('Property Editor is not available. Please ensure the editor is properly loaded.');
      return false;
    }
    
    try {
      // Get the canvas controller from the main editor
      const editor = window.pageEditor;
      const canvasController = editor && editor.getCanvasController ? editor.getCanvasController() : null;

      // Map preview IDs to canvas IDs
      const canvasColumnId = this._mapPreviewIdToCanvasId(columnId, 'column');
      const canvasRowId = rowId ? this._mapPreviewIdToCanvasId(rowId, 'row') : null;
      
      if (!canvasController) {
        console.error('PropertyEditorBridge: Canvas controller not available');
        this._handleError('Canvas controller not found', new Error('Canvas controller not available'));
        this._showUserError('Editor canvas is not available. Please refresh the page.');
        return false;
      }
      
      // Find the column element in the main canvas
      // Main canvas uses data-column-id attribute
      const columnElement = document.querySelector(`[data-column-id="${canvasColumnId}"]`);
      if (!columnElement) {
        console.error('PropertyEditorBridge: Column element not found in main canvas:', canvasColumnId, 'from preview ID:', columnId);
        this._handleError('Column not found in main canvas', new Error(`Column ${canvasColumnId} not found`));
        this._showUserError('Could not locate the selected column in the main editor. It may have been removed or modified.');
        return false;
      }
      
      // If canvasRowId is not provided, find it from the column element
      let actualRowId = canvasRowId;
      if (!actualRowId) {
        // Try to find parent row element (using data-row-id)
        const rowElement = columnElement.closest('[data-row-id]');
        if (rowElement) {
          actualRowId = rowElement.getAttribute('data-row-id');
        }
      }
      
      if (!actualRowId) {
        console.error('PropertyEditorBridge: Could not determine row ID for column:', canvasColumnId);
        this._handleError('Could not find row for column', new Error(`Row not found for column ${canvasColumnId}`));
        this._showUserError('Could not locate the parent row for this column.');
        return false;
      }
      
      // Use the canvas controller to select the element
      const context = {
        type: 'column',
        rowId: actualRowId,
        columnId: canvasColumnId
      };
      
      canvasController.selectElement(columnElement, context);
      console.debug('PropertyEditorBridge: Column selected in canvas:', canvasColumnId, 'from preview ID:', columnId);
      return true;
      
    } catch (error) {
      console.error('PropertyEditorBridge: Error opening column properties:', error);
      this._handleError('Error opening column properties', error);
      this._showUserError('Error opening column properties. Please try again or refresh the page if the problem persists.');
      return false;
    }
  }
  
  /**
   * Open row properties in the Property Editor
   * @param {string} rowId - Unique row identifier
   * @returns {boolean} True if successfully opened, false otherwise
   */
  openRowProperties(rowId) {
    // Check if we've exceeded error threshold
    if (this.errorCount >= this.maxErrors) {
      this._showUserError('Property Editor integration has encountered too many errors and is temporarily disabled. Please refresh the page.');
      return false;
    }
    
    if (!this._validateRowId(rowId)) {
      console.warn('PropertyEditorBridge: Invalid row ID provided:', rowId);
      this._showUserError('Invalid row selected. Please try selecting a different row.');
      return false;
    }
    
    // Check API availability before attempting to use it
    if (!this._checkAPIAvailability()) {
      this._showUserError('Property Editor is not available. Please ensure the editor is properly loaded.');
      return false;
    }
    
    try {
      // Get the canvas controller from the main editor
      const editor = window.pageEditor;
      const canvasController = editor && editor.getCanvasController ? editor.getCanvasController() : null;

      // Map preview ID to canvas ID
      const canvasRowId = this._mapPreviewIdToCanvasId(rowId, 'row');
      
      if (!canvasController) {
        console.error('PropertyEditorBridge: Canvas controller not available');
        this._handleError('Canvas controller not found', new Error('Canvas controller not available'));
        this._showUserError('Editor canvas is not available. Please refresh the page.');
        return false;
      }
      
      // Find the row element in the main canvas
      // Main canvas uses data-row-id attribute
      const rowElement = document.querySelector(`[data-row-id="${canvasRowId}"]`);
      if (!rowElement) {
        console.error('PropertyEditorBridge: Row element not found in main canvas:', canvasRowId, 'from preview ID:', rowId);
        this._handleError('Row not found in main canvas', new Error(`Row ${canvasRowId} not found`));
        this._showUserError('Could not locate the selected row in the main editor. It may have been removed or modified.');
        return false;
      }
      
      // Use the canvas controller to select the element
      const context = {
        type: 'row',
        rowId: canvasRowId
      };
      
      canvasController.selectElement(rowElement, context);
      console.debug('PropertyEditorBridge: Row selected in canvas:', canvasRowId, 'from preview ID:', rowId);
      return true;
      
    } catch (error) {
      console.error('PropertyEditorBridge: Error opening row properties:', error);
      this._handleError('Error opening row properties', error);
      this._showUserError('Error opening row properties. Please try again or refresh the page if the problem persists.');
      return false;
    }
  }
  
  /**
   * Check if Property Editor is currently open
   * @returns {boolean} True if Property Editor is open
   */
  isPropertyEditorOpen() {
    try {
      if (this.propertyEditorAPI && typeof this.propertyEditorAPI.isOpen === 'function') {
        return this.propertyEditorAPI.isOpen();
      } else if (typeof window.isPropertyEditorOpen === 'function') {
        return window.isPropertyEditorOpen();
      } else {
        // Fallback: check for common Property Editor DOM elements
        return document.querySelector('.property-editor, #property-editor, .properties-panel') !== null;
      }
    } catch (error) {
      console.error('PropertyEditorBridge: Error checking Property Editor state:', error);
      return false;
    }
  }
  
  /**
   * Get currently editing element information
   * @returns {Object|null} Current element info or null
   */
  getCurrentEditingElement() {
    try {
      if (this.propertyEditorAPI && typeof this.propertyEditorAPI.getCurrentElement === 'function') {
        return this.propertyEditorAPI.getCurrentElement();
      } else if (typeof window.getCurrentEditingElement === 'function') {
        return window.getCurrentEditingElement();
      } else {
        return null;
      }
    } catch (error) {
      console.error('PropertyEditorBridge: Error getting current editing element:', error);
      return null;
    }
  }
  
  /**
   * Validate Property Editor API availability
   * @private
   */
  _validateAPI() {
    if (!this.propertyEditorAPI) {
      console.warn('PropertyEditorBridge: Property Editor API not provided. Will attempt fallback methods.');
      this.isInitialized = false;
    } else if (typeof this.propertyEditorAPI !== 'object') {
      console.warn('PropertyEditorBridge: Property Editor API is not an object:', typeof this.propertyEditorAPI);
      this.isInitialized = false;
    } else {
      console.debug('PropertyEditorBridge: Property Editor API provided and appears valid');
      this.isInitialized = true;
    }
  }
  
  /**
   * Check if Property Editor API is currently available
   * @private
   * @returns {boolean} True if API is available
   */
  _checkAPIAvailability() {
    // Check if API object still exists and has expected methods
    if (this.propertyEditorAPI && typeof this.propertyEditorAPI === 'object') {
      return true;
    }
    
    // Check for global fallback functions
    if (typeof window.openWidgetProperties === 'function' ||
        typeof window.openColumnProperties === 'function' ||
        typeof window.openRowProperties === 'function') {
      return true;
    }
    
    console.warn('PropertyEditorBridge: Property Editor API is not available');
    return false;
  }
  
  /**
   * Validate widget ID format and content
   * @private
   * @param {string} widgetId - Widget ID to validate
   * @returns {boolean} True if valid
   */
  _validateWidgetId(widgetId) {
    if (!widgetId) {
      console.debug('PropertyEditorBridge: Widget ID is null or undefined');
      return false;
    }
    
    if (typeof widgetId !== 'string') {
      console.debug('PropertyEditorBridge: Widget ID is not a string:', typeof widgetId);
      return false;
    }
    
    const trimmedId = widgetId.trim();
    if (trimmedId.length === 0) {
      console.debug('PropertyEditorBridge: Widget ID is empty or whitespace only');
      return false;
    }
    
    // Additional validation: check for reasonable ID format
    if (trimmedId.length > 100) {
      console.warn('PropertyEditorBridge: Widget ID is unusually long:', trimmedId.length);
      return false;
    }
    
    return true;
  }
  
  /**
   * Validate column ID format and content
   * @private
   * @param {string} columnId - Column ID to validate
   * @returns {boolean} True if valid
   */
  _validateColumnId(columnId) {
    if (!columnId) {
      console.debug('PropertyEditorBridge: Column ID is null or undefined');
      return false;
    }
    
    if (typeof columnId !== 'string') {
      console.debug('PropertyEditorBridge: Column ID is not a string:', typeof columnId);
      return false;
    }
    
    const trimmedId = columnId.trim();
    if (trimmedId.length === 0) {
      console.debug('PropertyEditorBridge: Column ID is empty or whitespace only');
      return false;
    }
    
    // Additional validation: check for reasonable ID format
    if (trimmedId.length > 100) {
      console.warn('PropertyEditorBridge: Column ID is unusually long:', trimmedId.length);
      return false;
    }
    
    return true;
  }
  
  /**
   * Validate row ID format and content
   * @private
   * @param {string} rowId - Row ID to validate
   * @returns {boolean} True if valid
   */
  _validateRowId(rowId) {
    if (!rowId) {
      console.debug('PropertyEditorBridge: Row ID is null or undefined');
      return false;
    }
    
    if (typeof rowId !== 'string') {
      console.debug('PropertyEditorBridge: Row ID is not a string:', typeof rowId);
      return false;
    }
    
    const trimmedId = rowId.trim();
    if (trimmedId.length === 0) {
      console.debug('PropertyEditorBridge: Row ID is empty or whitespace only');
      return false;
    }
    
    // Additional validation: check for reasonable ID format
    if (trimmedId.length > 100) {
      console.warn('PropertyEditorBridge: Row ID is unusually long:', trimmedId.length);
      return false;
    }
    
    return true;
  }
  
  /**
   * Show user-friendly error message
   * @private
   * @param {string} message - Error message to display
   */
  _showUserError(message) {
    console.error('PropertyEditorBridge: ' + message);
    
    // Try multiple methods to show user error, in order of preference
    try {
      // Method 1: Custom notification system
      if (typeof window.showNotification === 'function') {
        window.showNotification(message, 'error');
        return;
      }
      
      // Method 2: Toast notification system
      if (typeof window.showToast === 'function') {
        window.showToast(message, 'error');
        return;
      }
      
      // Method 3: jQuery-based notification
      if (typeof $ !== 'undefined' && $.fn.notify) {
        $.notify(message, 'error');
        return;
      }
      
      // Method 4: Foundation framework notification
      if (typeof Foundation !== 'undefined' && Foundation.utils && Foundation.utils.S) {
        const $notification = $('<div data-alert class="alert-box alert">' + message + '<a href="#" class="close">&times;</a></div>');
        $('body').prepend($notification);
        setTimeout(() => $notification.fadeOut(), 5000);
        return;
      }
      
      // Method 5: Simple DOM-based notification
      this._showDOMNotification(message);
      
    } catch (error) {
      console.error('PropertyEditorBridge: Error showing user notification:', error);
      
      // Final fallback: browser alert (not ideal but ensures user sees the message)
      if (typeof window.alert === 'function') {
        window.alert('Property Editor Error: ' + message);
      }
    }
  }
  
  /**
   * Show a simple DOM-based notification
   * @private
   * @param {string} message - Message to display
   */
  _showDOMNotification(message) {
    try {
      // Create notification element
      const notification = document.createElement('div');
      notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #f04124;
        color: white;
        padding: 15px 20px;
        border-radius: 4px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        z-index: 10000;
        max-width: 400px;
        font-family: Arial, sans-serif;
        font-size: 14px;
        line-height: 1.4;
      `;
      notification.textContent = message;
      
      // Add close button
      const closeBtn = document.createElement('span');
      closeBtn.innerHTML = '&times;';
      closeBtn.style.cssText = `
        float: right;
        margin-left: 10px;
        cursor: pointer;
        font-size: 18px;
        font-weight: bold;
      `;
      closeBtn.onclick = () => {
        if (notification.parentNode) {
          notification.parentNode.removeChild(notification);
        }
      };
      notification.appendChild(closeBtn);
      
      // Add to DOM
      document.body.appendChild(notification);
      
      // Auto-remove after 8 seconds
      setTimeout(() => {
        if (notification.parentNode) {
          notification.parentNode.removeChild(notification);
        }
      }, 8000);
      
    } catch (error) {
      console.error('PropertyEditorBridge: Error creating DOM notification:', error);
    }
  }
  
  /**
   * Find widget context (row and column IDs) for a given widget ID
   * @private
   * @param {string} widgetId - Widget ID to find context for
   * @returns {Object|null} Context object with rowId and columnId, or null if not found
   */
  _findWidgetContext(widgetId) {
    try {
      // Try to find the widget element in the DOM
      const widgetElement = document.querySelector(`[data-widget="${widgetId}"]`);
      if (!widgetElement) {
        console.debug('PropertyEditorBridge: Widget element not found in DOM:', widgetId);
        return null;
      }
      
      // Find parent column and row elements
      const columnElement = widgetElement.closest('[data-col-id]');
      const rowElement = widgetElement.closest('[data-row-id]');
      
      if (columnElement && rowElement) {
        const rowId = rowElement.getAttribute('data-row-id');
        const columnId = columnElement.getAttribute('data-col-id');
        
        // Validate that both IDs are valid
        if (rowId && rowId.trim().length > 0 && columnId && columnId.trim().length > 0) {
          return {
            rowId: rowId.trim(),
            columnId: columnId.trim()
          };
        } else {
          console.warn('PropertyEditorBridge: Found parent elements but they have invalid IDs:', { rowId, columnId });
          return null;
        }
      }
      
      console.debug('PropertyEditorBridge: Could not find both parent column and row for widget:', widgetId);
      return null;
    } catch (error) {
      console.error('PropertyEditorBridge: Error finding widget context:', error);
      this._handleError('Error in _findWidgetContext', error);
      return null;
    }
  }
  
  /**
   * Find column context (row ID) for a given column ID
   * @private
   * @param {string} columnId - Column ID to find context for
   * @returns {Object|null} Context object with rowId, or null if not found
   */
  _findColumnContext(columnId) {
    try {
      // Determine which document to search
      const searchDoc = this.iframeDoc || document;
      const searchDocName = this.iframeDoc ? 'iframe' : 'main';
      
      // Try to find the column element in the DOM
      const columnElement = searchDoc.querySelector(`[data-col-id="${columnId}"]`);
      if (!columnElement) {
        console.debug(`PropertyEditorBridge: Column element not found in ${searchDocName} DOM:`, columnId);
        return null;
      }
      
      // Find parent row element
      const rowElement = columnElement.closest('[data-row-id]');
      
      if (rowElement) {
        const rowId = rowElement.getAttribute('data-row-id');
        if (rowId && rowId.trim().length > 0) {
          return {
            rowId: rowId.trim()
          };
        } else {
          console.warn('PropertyEditorBridge: Row element found but has invalid data-row-id');
          return null;
        }
      }
      
      console.debug(`PropertyEditorBridge: No parent row found for column in ${searchDocName} DOM:`, columnId);
      return null;
    } catch (error) {
      console.error('PropertyEditorBridge: Error finding column context:', error);
      this._handleError('Error in _findColumnContext', error);
      return null;
    }
  }
  
  /**
   * Map preview ID to main canvas ID
   * Preview uses: widget-0-1-0, col-0-0, row-0 (0-indexed, per-column widget numbering)
   * Main canvas uses: widget-1, col-1, row-1 (1-indexed, global widget numbering)
   * @private
   * @param {string} previewId - ID from preview (e.g., widget-0-1-0, col-0-0, row-0)
   * @param {string} type - Element type (widget, column, row)
   * @returns {string} Mapped ID for main canvas
   */
  _mapPreviewIdToCanvasId(previewId, type) {
    if (!previewId || typeof previewId !== 'string') {
      return previewId;
    }
    
    const parts = previewId.split('-');
    
    if (type === 'widget' && parts[0] === 'widget' && parts.length >= 4) {
      // widget-0-1-0 format in preview
      const rowIndex = parseInt(parts[1], 10);
      const colIndex = parseInt(parts[2], 10);
      const widgetIndex = parseInt(parts[3], 10);
      return this._findCanvasWidgetId(rowIndex, colIndex, widgetIndex);
    } else if (type === 'column' && parts[0] === 'col' && parts.length >= 3) {
      // col-0-0 format in preview -> main canvas column id
      const rowIndex = parseInt(parts[1], 10);
      const colIndex = parseInt(parts[2], 10);
      return this._findCanvasColumnId(rowIndex, colIndex);
    } else if (type === 'row' && parts[0] === 'row' && parts.length >= 2) {
      // row-0 (0-indexed) -> row-1 (1-indexed)
      const rowIndex = parseInt(parts[1], 10);
      return `row-${rowIndex + 1}`;
    }
    
    // Might already be a canvas ID
    return previewId;
  }
  
  /**
   * Find canvas widget ID by position in preview
   * @private
   * @param {number} rowIndex - Row index (0-based)
   * @param {number} colIndex - Column index within row (0-based)
   * @param {number} widgetIndex - Widget index within column (0-based)
   * @returns {string} Canvas widget ID (e.g., widget-5)
   */
  _findCanvasWidgetId(rowIndex, colIndex, widgetIndex) {
    try {
      const rows = document.querySelectorAll('[data-row-id]');
      if (rowIndex >= rows.length) {
        return `widget-${widgetIndex + 1}`;
      }
      const row = rows[rowIndex];
      const columns = row.querySelectorAll('[data-column-id]');
      if (colIndex >= columns.length) {
        return `widget-${widgetIndex + 1}`;
      }
      const column = columns[colIndex];
      const widgets = column.querySelectorAll('[data-widget-id]');
      if (widgetIndex >= widgets.length) {
        return `widget-${widgetIndex + 1}`;
      }
      const widget = widgets[widgetIndex];
      const widgetId = widget.getAttribute('data-widget-id');
      if (widgetId) return widgetId;
    } catch (error) {
      console.error('PropertyEditorBridge: Error finding canvas widget ID:', error);
    }
    return `widget-${widgetIndex + 1}`;
  }
  
  /**
   * Find canvas column ID by position in preview
   * @private
   * @param {number} rowIndex - Row index (0-based)
   * @param {number} colIndex - Column index within row (0-based)
   * @returns {string} Canvas column ID
   */
  _findCanvasColumnId(rowIndex, colIndex) {
    try {
      const rows = document.querySelectorAll('[data-row-id]');
      if (rowIndex >= rows.length) {
        return `col-${colIndex + 1}`;
      }
      const row = rows[rowIndex];
      const columns = row.querySelectorAll('[data-column-id]');
      if (colIndex >= columns.length) {
        return `col-${colIndex + 1}`;
      }
      const column = columns[colIndex];
      const columnId = column.getAttribute('data-column-id');
      if (columnId) return columnId;
    } catch (error) {
      console.error('PropertyEditorBridge: Error finding canvas column ID:', error);
    }
    return `col-${colIndex + 1}`;
  }
  
  /**
   * Handle errors and track error count
   * @private
   * @param {string} context - Context where error occurred
   * @param {Error} error - The error object
   */
  _handleError(context, error) {
    this.errorCount++;
    console.error(`PropertyEditorBridge: ${context} (Error #${this.errorCount}):`, error);
    
    // Log additional debugging information
    console.debug('PropertyEditorBridge: Current API state:', {
      hasAPI: !!this.propertyEditorAPI,
      isInitialized: this.isInitialized,
      errorCount: this.errorCount
    });
    
    // If too many errors, show persistent warning
    if (this.errorCount >= this.maxErrors) {
      console.error('PropertyEditorBridge: Maximum error threshold reached');
      this._showUserError('Property Editor integration has encountered multiple errors. Some features may not work correctly. Please refresh the page to reset.');
    }
  }
  
  /**
   * Reset error count (can be called by parent to re-enable after fixing issues)
   */
  resetErrorCount() {
    this.errorCount = 0;
    console.debug('PropertyEditorBridge: Error count reset');
  }
}

// Export for module systems or make globally available
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PropertyEditorBridge;
} else if (typeof window !== 'undefined') {
  window.PropertyEditorBridge = PropertyEditorBridge;
}