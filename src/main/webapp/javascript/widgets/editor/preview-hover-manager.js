/**
 * PreviewHoverManager - Main controller for the Visual Editor Preview Hover feature
 * 
 * Orchestrates hover functionality by coordinating ElementDetector, HoverOverlay,
 * and PropertyEditorBridge components. Manages preview mode state transitions
 * and provides the main interface for the hover system.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.5, 2.1, 3.1, 3.2, 3.3, 3.4, 5.4, 6.1, 6.2, 6.3, 6.4
 */
class PreviewHoverManager {
  
  /**
   * Initialize the preview hover manager
   * @param {HTMLElement} previewContainer - The preview container element
   * @param {Object} propertyEditorAPI - Property Editor API interface (PropertiesPanel instance)
   */
  constructor(previewContainer, propertyEditorAPI) {
    this.previewContainer = previewContainer;
    this.propertyEditorAPI = propertyEditorAPI;

    // Determine context (iframe-aware)
    const isIframe = !!(previewContainer && previewContainer.tagName && previewContainer.tagName.toLowerCase() === 'iframe');
    this.isIframeContainer = isIframe;
    
    const parentDoc = (typeof document !== 'undefined') ? document : null;
    const parentWin = (typeof window !== 'undefined') ? window : null;
    
    let ctxDoc;
    let ctxWin;
    if (isIframe) {
      // For iframe containers, use parent document/window for overlay rendering
      ctxWin = parentWin;
      ctxDoc = parentDoc;
    } else {
      ctxDoc = (previewContainer && previewContainer.ownerDocument) ? previewContainer.ownerDocument : parentDoc;
      ctxWin = (ctxDoc && ctxDoc.defaultView) ? ctxDoc.defaultView : parentWin;
    }
    
    const iframeEl = isIframe ? previewContainer : (parentDoc ? parentDoc.getElementById('preview-iframe') : null);
    
    // Initialize components (always use parent context for overlay)
    this.elementDetector = new ElementDetector(previewContainer, { document: ctxDoc, window: ctxWin, parentDocument: parentDoc, parentWindow: parentWin, iframeElement: iframeEl });
    this.hoverOverlay = new HoverOverlay({ document: ctxDoc, window: ctxWin, iframeElement: iframeEl });
    this.propertyEditorBridge = new PropertyEditorBridge(propertyEditorAPI, { iframeElement: iframeEl });
    
    // Internal state
    this.hoverState = {
      isActive: false,
      currentElement: {
        type: null,
        element: null,
        id: null,
        boundingBox: null
      },
      isOutlineVisible: false,
      isButtonVisible: false
    };
    
    // Separate state for locked (selected) element
    this.lockedElement = {
      type: null,
      element: null,
      id: null,
      boundingBox: null
    };
    
    this.isEnabled = false;
    
    // Page change listener state
    this._pageChangeListenerActive = false;
    this._onPageChanged = null;
    
    // Set up postMessage listener for iframe hover events
    if (isIframe) {
      this._setupIframeMessageListener();
    }
    
    // Bind event handlers
    this._onElementDetected = this._onElementDetected.bind(this);
    this._onElementLost = this._onElementLost.bind(this);
    this._onActionButtonClick = this._onActionButtonClick.bind(this);
    this._onActionButtonLeave = this._onActionButtonLeave.bind(this);
    
    // Set up component callbacks
    this._setupComponentCallbacks();
    
    console.log('PreviewHoverManager initialized');
  }
  
  /**
   * Set up postMessage listener for iframe hover events
   * @private
   */
  _setupIframeMessageListener() {
    if (!this.previewContainer || !window) {
      return;
    }
    
    window.addEventListener('message', (event) => {
      if (!event.data || !event.data.type) {
        return;
      }
      
      // Handle iframe hover detection messages
      if (event.data.type === 'previewHover:elementDetected' && event.data.data) {
        this._handleIframeElementDetected(event.data.data);
      } else if (event.data.type === 'previewHover:elementLost') {
        this._handleIframeElementLost();
      }
    });
  }

  /**
   * Handle iframe element detected events
   * @private
   * @param {Object} data - Detected element data from iframe
   */
  _handleIframeElementDetected(data) {
    console.debug('PreviewHoverManager: Received element detected from iframe:', data.type, data.id);

    // Check if this is the locked element - if so, don't update hover state
    if (this.lockedElement &&
        this.lockedElement.type === data.type &&
        this.lockedElement.id === data.id) {
      console.debug('PreviewHoverManager: Detected locked element from iframe, ignoring detection');
      return; // This is the locked element, don't change anything
    }

    // Check if same element
    if (this.hoverState.currentElement.type === data.type &&
        this.hoverState.currentElement.id === data.id) {
      return; // No change
    }

    this.hoverState.currentElement = {
      type: data.type,
      element: null,
      id: data.id,
      rowId: data.rowId,
      columnId: data.columnId,
      boundingBox: data.boundingBox
    };

    // Show outline using bounding box coordinates
    this.hoverOverlay.showOutlineFromBox(data.boundingBox, data.type, this.previewContainer);
    this.hoverState.isOutlineVisible = true;

    // Set up button click callback before rendering
    this.hoverOverlay.setActionButtonClickCallback(() => {
      this._onActionButtonClick();
    });

    // Render action button for all types (widget, column, row)
    this.hoverOverlay.renderActionButtonFromBox(data.boundingBox, data.type, this.previewContainer);
    this.hoverState.isButtonVisible = true;
  }

  /**
   * Handle iframe element lost events
   * @private
   */
  _handleIframeElementLost() {
    console.debug('PreviewHoverManager: Received element lost from iframe');

    // Check if the lost element is NOT the locked element before hiding
    if (this.hoverState.currentElement.id &&
        !(this.lockedElement && this.lockedElement.id === this.hoverState.currentElement.id)) {
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      this._resetCurrentElement();
    } else if (this.lockedElement && this.lockedElement.id === this.hoverState.currentElement.id) {
      console.debug('PreviewHoverManager: Element lost from iframe but element is locked, keeping outline visible');
    }

    // Re-show locked outline and action button when applicable
    if (this.lockedElement && this.lockedElement.type) {
      if (this.lockedElement.element) {
        this.hoverOverlay.showLockedOutline(this.lockedElement.element, this.lockedElement.type);
        if (this.lockedElement.type === 'widget') {
          this.hoverOverlay.renderActionButton(this.lockedElement.element, this.lockedElement.type);
        }
      } else if (this.lockedElement.boundingBox) {
        this.hoverOverlay.showLockedOutlineFromBox(this.lockedElement.boundingBox, this.lockedElement.type, this.previewContainer);
        this.hoverOverlay.renderActionButtonFromBox(this.lockedElement.boundingBox, this.lockedElement.type, this.previewContainer);
      }
    }
  }
  
  /**
   * Enable hover functionality
   * Activates hover detection and visual feedback
   * @param {boolean} forceSendEnable - If true, always send enable message even if already enabled (for iframe reattachment)
   */
  enable(forceSendEnable = false) {
    if (this.isEnabled && !forceSendEnable) {
      return;
    }
    
    try {
      if (this.isIframeContainer) {
        // For iframe: send enable message to iframe's hover bridge
        if (this.previewContainer.contentWindow) {
          console.debug('PreviewHoverManager: Sending enable message to iframe bridge');
          this.previewContainer.contentWindow.postMessage({ type: 'previewHover:enable' }, '*');
        }
      } else {
        // For non-iframe: activate element detection directly
        this.elementDetector.attachMouseListeners();
      }
      
      // Update state
      this.isEnabled = true;
      this.hoverState.isActive = true;
      
      console.log('PreviewHoverManager enabled');
    } catch (error) {
      console.error('PreviewHoverManager: Error enabling hover functionality:', error);
    }
  }
  
  /**
   * Disable hover functionality
   * Deactivates hover detection and cleans up any visible elements
   */
  disable() {
    if (!this.isEnabled) {
      return;
    }
    
    try {
      if (this.isIframeContainer) {
        // For iframe: send disable message to iframe's hover bridge
        if (this.previewContainer.contentWindow) {
          this.previewContainer.contentWindow.postMessage({ type: 'previewHover:disable' }, '*');
        }
      } else {
        // For non-iframe: deactivate element detection
        this.elementDetector.detachMouseListeners();
      }
      
      // Clean up any visible hover elements
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      
      // Remove page change listener when disabling
      this._removePageChangeListener();
      
      // Reset state
      this.isEnabled = false;
      this.hoverState.isActive = false;
      this._resetCurrentElement();
      
      console.log('PreviewHoverManager disabled');
    } catch (error) {
      console.error('PreviewHoverManager: Error disabling hover functionality:', error);
    }
  }
  
  /**
   * Check if hover functionality is currently enabled
   * @returns {boolean} True if hover is active
   */
  isHoverEnabled() {
    return this.isEnabled;
  }
  
  /**
   * Reinitialize bridge after iframe navigation
   * Forces a clean enable message to be sent to the iframe
   */
  reinitializeBridge() {
    console.log('PreviewHoverManager: Reinitializing bridge after navigation');
    try {
      // Clean up any visible hover elements
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      this._resetCurrentElement();
      
      // Force send enable message to iframe even if already enabled
      this.enable(true);
    } catch (error) {
      console.error('PreviewHoverManager: Error reinitializing bridge:', error);
    }
  }
  
  /**
   * Refresh iframe references after preview reload
   * Call this when the preview has been reloaded with new content
   */
  refreshIframeReferences() {
    console.log('PreviewHoverManager: Refreshing iframe references after preview reload');
    
    // Refresh iframe element reference if needed
    if (!this.previewContainer || this.previewContainer.tagName.toLowerCase() !== 'iframe') {
      const iframeEl = document.getElementById('preview-iframe');
      if (iframeEl) {
        this.previewContainer = iframeEl;
      }
    }
    
    // Refresh PropertyEditorBridge iframe document reference
    if (this.propertyEditorBridge) {
      this.propertyEditorBridge.refreshIframeDocReference();
    }
    
    // Reset error count to allow fresh attempts after page reload
    if (this.propertyEditorBridge) {
      this.propertyEditorBridge.resetErrorCount();
    }
  }
  
  /**
   * Handle preview mode changes
   * @param {boolean} isPreviewMode - True if entering preview mode, false if exiting
   */
  handlePreviewModeChange(isPreviewMode) {
    console.log('PreviewHoverManager: Preview mode change:', isPreviewMode);
    
    if (isPreviewMode) {
      this.enable();
      // If there's a locked selection, redraw it after enabling preview
      if (this.lockedElement && this.lockedElement.type) {
        console.log('PreviewHoverManager: Redrawing locked outline after preview mode enabled');
        setTimeout(() => {
          this._redrawLockedOutline();
        }, 100);
      }
      // Set up page change listener when entering preview mode
      this._setupPageChangeListener();
    } else {
      this.disable();
      // Remove page change listener when leaving preview mode
      this._removePageChangeListener();
    }
  }
  
  /**
   * Set up listener for page changes - auto-unselect when user switches pages
   * @private
   */
  _setupPageChangeListener() {
    if (this._pageChangeListenerActive) {
      return; // Already set up
    }
    
    this._onPageChanged = () => {
      console.log('PreviewHoverManager: Page changed, clearing selections');
      // Hide locked outline
      this.hoverOverlay.hideLockedOutline();
      // Clear locked element
      this.lockedElement = {
        type: null,
        element: null,
        id: null,
        boundingBox: null
      };
      // Clear any active hover states
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      this._resetCurrentElement();
      
      // Clear properties panel if available
      if (this.propertyEditorAPI && typeof this.propertyEditorAPI.clear === 'function') {
        this.propertyEditorAPI.clear();
      }
    };
    
    // Listen for layout editor deselection (when user clicks empty space in layout editor)
    this._onLayoutEditorDeselect = () => {
      console.log('PreviewHoverManager: Layout editor deselected, clearing preview lock');
      // Hide locked outline
      this.hoverOverlay.hideLockedOutline();
      // Clear locked element
      this.lockedElement = {
        type: null,
        element: null,
        id: null,
        boundingBox: null
      };
      // Clear any active hover states
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      this._resetCurrentElement();
      
      // Clear properties panel if available
      if (this.propertyEditorAPI && typeof this.propertyEditorAPI.clear === 'function') {
        this.propertyEditorAPI.clear();
      }
    };
    
    // Listen for viewport/view mode changes to redraw locked outline
    this._onViewportOrModeChange = () => {
      console.log('PreviewHoverManager: Viewport or view mode changed, redrawing locked outline');
      // Redraw locked outline after viewport/mode change
      if (this.lockedElement && this.lockedElement.type) {
        // Use small timeout to allow positioning to settle
        setTimeout(() => {
          this._redrawLockedOutline();
        }, 500); // Wait for layout to settle
      }
    };
    
    // Listen for window resize to redraw locked outline
    this._onWindowResize = () => {
      console.log('PreviewHoverManager: Window resize detected, queuing locked outline redraw');
      // Clear any pending resize timer
      if (this._resizeTimer) {
        clearTimeout(this._resizeTimer);
      }
      // Queue redraw after resize settles
      this._resizeTimer = setTimeout(() => {
        this._redrawLockedOutline();
        this._resizeTimer = null;
      }, 500); // Wait for resize to complete
    };
    
    document.addEventListener('pageChanged', this._onPageChanged);
    document.addEventListener('propertiesPanelCleared', this._onLayoutEditorDeselect);
    document.addEventListener('viewportChanged', this._onViewportOrModeChange);
    document.addEventListener('cms:editor:mode-change', this._onViewportOrModeChange);
    window.addEventListener('resize', this._onWindowResize);
    
    this._pageChangeListenerActive = true;
    this._resizeTimer = null;
    console.log('PreviewHoverManager: Page change and event listeners installed');
  }
  
  /**
   * Remove listener for page changes
   * @private
   */
  _removePageChangeListener() {
    if (!this._pageChangeListenerActive) {
      return;
    }
    
    if (this._onPageChanged) {
      document.removeEventListener('pageChanged', this._onPageChanged);
      this._onPageChanged = null;
    }
    if (this._onLayoutEditorDeselect) {
      document.removeEventListener('propertiesPanelCleared', this._onLayoutEditorDeselect);
      this._onLayoutEditorDeselect = null;
    }
    if (this._onViewportOrModeChange) {
      document.removeEventListener('viewportChanged', this._onViewportOrModeChange);
      document.removeEventListener('cms:editor:mode-change', this._onViewportOrModeChange);
      this._onViewportOrModeChange = null;
    }
    if (this._onWindowResize) {
      window.removeEventListener('resize', this._onWindowResize);
      this._onWindowResize = null;
    }
    if (this._resizeTimer) {
      clearTimeout(this._resizeTimer);
      this._resizeTimer = null;
    }
    
    this._pageChangeListenerActive = false;
    console.log('PreviewHoverManager: Page change and event listeners removed');
  }
  
  /**
   * Get current hover state (for debugging/testing)
   * @returns {Object} Current hover state
   */
  getHoverState() {
    return { ...this.hoverState };
  }

  /**
   * Programmatically highlight an element in the preview based on editor context
   * @param {Object} context - Context object with type, rowId, columnId, widgetId
   */
  selectElementByContext(context) {
    if (!context || !context.type) {
      return;
    }

    if (!this.isEnabled) {
      return;
    }

    const elementInfo = this._getPreviewElementInfo(context);
    if (!elementInfo || !elementInfo.element) {
      this.clearSelection();
      return;
    }

    const { element, type, id, rowId, columnId } = elementInfo;
    const rect = element.getBoundingClientRect ? element.getBoundingClientRect() : null;
    if (!rect) {
      return;
    }
      let top = rect.top;
      let left = rect.left;
      if (this.isIframeContainer && this.previewContainer && this.previewContainer.getBoundingClientRect) {
        const iframeRect = this.previewContainer.getBoundingClientRect();
        top += iframeRect.top;
        left += iframeRect.left;
      }

      const boundingBox = {
        top,
        left,
        width: rect.width,
        height: rect.height
      };

    // Clear previous locked selection and hover outline
    this.hoverOverlay.hideLockedOutline();
    this.hoverOverlay.hideOutline();
    this.hoverOverlay.removeActionButton();

    // Update state
    this.hoverState.currentElement = {
      type: type,
      element: null,
      id: id,
      rowId: rowId || null,
      columnId: columnId || null,
      boundingBox: boundingBox
    };

    // Lock selection to match layout editor focus
    this.lockedElement = {
      type: type,
      element: null,
      id: id,
      rowId: rowId || null,
      columnId: columnId || null,
      boundingBox: boundingBox
    };

    // Show locked outline (no action button for selection syncing)
    this.hoverOverlay.showLockedOutlineFromBox(boundingBox, type, this.isIframeContainer ? this.previewContainer : null);
    this.hoverState.isOutlineVisible = false;
    this.hoverState.isButtonVisible = false;
  }

  /**
   * Clear any active preview selection/outline
   */
  clearSelection() {
    this.hoverOverlay.hideOutline();
    this.hoverOverlay.removeActionButton();
    this._resetCurrentElement();
  }

  /**
   * Get preview element info from context
   * @private
   * @param {Object} context - Context object with type, rowId, columnId, widgetId
   * @returns {Object|null} element info
   */
  _getPreviewElementInfo(context) {
    const previewDoc = this._getPreviewDocument();
    if (!previewDoc) {
      return null;
    }

    let selector = null;
    let id = null;
    let type = context.type;

    if (type === 'widget' && context.widgetId) {
      id = context.widgetId;
      selector = `[data-widget="${context.widgetId}"]`;
    } else if (type === 'column' && context.columnId) {
      id = context.columnId;
      selector = `[data-col-id="${context.columnId}"]`;
    } else if (type === 'row' && context.rowId) {
      id = context.rowId;
      selector = `[data-row-id="${context.rowId}"]`;
    }

    if (!selector) {
      return null;
    }

    const element = previewDoc.querySelector(selector);
    if (!element) {
      return null;
    }

    return {
      type,
      id,
      rowId: context.rowId || null,
      columnId: context.columnId || null,
      element
    };
  }

  /**
   * Resolve the preview document (iframe-aware)
   * @private
   * @returns {Document|null}
   */
  _getPreviewDocument() {
    if (this.isIframeContainer) {
      return this.previewContainer && (this.previewContainer.contentDocument || this.previewContainer.contentWindow?.document) || null;
    }

    if (this.previewContainer && this.previewContainer.ownerDocument) {
      return this.previewContainer.ownerDocument;
    }

    if (typeof globalThis !== 'undefined' && globalThis.document) {
      return globalThis.document;
    }
    return null;
  }
  
  /**
   * Set up callbacks for component communication
   * @private
   */
  _setupComponentCallbacks() {
    // ElementDetector callbacks
    this.elementDetector.onElementDetected = this._onElementDetected;
    this.elementDetector.onElementLost = this._onElementLost;
    
    // HoverOverlay callbacks
    this.hoverOverlay.onActionButtonClick = this._onActionButtonClick;
    this.hoverOverlay.onActionButtonLeave = this._onActionButtonLeave;
  }
  
  /**
   * Handle element detection events
   * @private
   * @param {Object} elementInfo - Detected element information
   */
  _onElementDetected(elementInfo) {
    if (!this.isEnabled || !elementInfo) {
      return;
    }
    
    try {
      console.debug('PreviewHoverManager: Detected element', elementInfo.type, elementInfo.id);
      
      // Check if this is the locked element - if so, no need to show hover outline
      if (this.lockedElement && this.lockedElement.element && 
          this.lockedElement.type === elementInfo.type &&
          this.lockedElement.id === elementInfo.id) {
        return; // This is the locked element, keep showing its outline
      }
      
      // Check if this is the same element we're already hovering
      if (this.hoverState.currentElement.type === elementInfo.type &&
          this.hoverState.currentElement.id === elementInfo.id) {
        return; // No change needed
      }
      
      // Update current element state
      this.hoverState.currentElement = {
        type: elementInfo.type,
        element: elementInfo.element,
        id: elementInfo.id,
        boundingBox: elementInfo.element.getBoundingClientRect()
      };
      
      // Show outline around the element
      this.hoverOverlay.showOutline(elementInfo.element, elementInfo.type);
      this.hoverState.isOutlineVisible = true;
      
      // Render action button only for widgets; hide for rows/columns
      if (elementInfo.type === 'widget') {
        this.hoverOverlay.renderActionButton(elementInfo.element, elementInfo.type);
        this.hoverState.isButtonVisible = true;
      } else {
        this.hoverOverlay.removeActionButton();
        this.hoverState.isButtonVisible = false;
      }
      
    } catch (error) {
      console.error('PreviewHoverManager: Error handling element detection:', error);
    }
  }
  
  /**
   * Handle element lost events (cursor exit)
   * @private
   */
  _onElementLost() {
    if (!this.isEnabled) {
      return;
    }
    
    try {
      // Hide hover outline and button
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      
      // Reset hover state but keep locked element
      this._resetCurrentElement();
      
      // If there's a locked element, re-show its outline
      if (this.lockedElement && this.lockedElement.element && this.lockedElement.type) {
        console.debug('PreviewHoverManager: Re-showing locked element outline');
        this.hoverOverlay.showOutline(this.lockedElement.element, this.lockedElement.type);
          this.hoverOverlay.showLockedOutline(this.lockedElement.element, this.lockedElement.type);
        if (this.lockedElement.type === 'widget') {
          this.hoverOverlay.renderActionButton(this.lockedElement.element, this.lockedElement.type);
        }
      }
      
    } catch (error) {
      console.error('PreviewHoverManager: Error handling element lost:', error);
    }
  }
  
  /**
   * Handle action button click events
   * @private
   * @param {Event} event - Click event
   */
  _onActionButtonClick(event) {
    if (!this.isEnabled || !this.hoverState.currentElement.type) {
      console.debug('PreviewHoverManager: _onActionButtonClick: not enabled or no element type');
      return;
    }
    
    try {
      const { type, id, rowId, columnId } = this.hoverState.currentElement;
      
      console.debug('PreviewHoverManager: Action button clicked for element:', { type, id, rowId, columnId });
      
      // Call appropriate PropertyEditorBridge method based on element type
      let success = false;
      switch (type) {
        case 'widget':
          // Pass rowId and columnId for widget context when available (from iframe)
          console.debug('PreviewHoverManager: Calling openWidgetProperties');
          success = this.propertyEditorBridge.openWidgetProperties(id, rowId, columnId);
          break;
        case 'column':
          // Pass rowId when available (from iframe) so we don't need to search for it
          console.debug('PreviewHoverManager: Calling openColumnProperties');
          success = this.propertyEditorBridge.openColumnProperties(id, rowId);
          break;
        case 'row':
          console.debug('PreviewHoverManager: Calling openRowProperties');
          success = this.propertyEditorBridge.openRowProperties(id);
          break;
        default:
          console.warn('PreviewHoverManager: Unknown element type:', type);
      }
      
      if (success) {
        console.log(`PreviewHoverManager: Successfully opened ${type} properties for ID:`, id);
        // Lock the selection so it stays visible
        this.lockedElement = {
          type: this.hoverState.currentElement.type,
          element: this.hoverState.currentElement.element,
          id: this.hoverState.currentElement.id,
          boundingBox: this.hoverState.currentElement.boundingBox
        };
        console.debug('PreviewHoverManager: Locked element:', this.lockedElement.type, this.lockedElement.id);

          // Hide any existing locked outline to avoid a brief incorrect position
          this.hoverOverlay.hideLockedOutline();

          // After opening properties panel, recalculate locked outline position after layout settles
          // This fixes cases where opening the properties panel or action menu causes layout shifts
          setTimeout(() => {
            console.debug('PreviewHoverManager: Recalculating locked outline position after action button click');
            this._redrawLockedOutline();
          }, 300);
      } else {
        console.warn(`PreviewHoverManager: Failed to open ${type} properties for ID:`, id);
      }
      
    } catch (error) {
      console.error('PreviewHoverManager: Error handling action button click:', error);
    }
  }
  
  /**
   * Handle action button leave events
   * @private
   */
  _onActionButtonLeave() {
    // This is called when the mouse leaves the action button area
    // and is not moving back to the outlined element
    this._onElementLost();
  }
  
  /**
   * Reset current element state
   * @private
   */
  _resetCurrentElement() {
    this.hoverState.currentElement = {
      type: null,
      element: null,
      id: null,
      boundingBox: null
    };
    this.hoverState.isOutlineVisible = false;
    this.hoverState.isButtonVisible = false;
  }

  /**
   * Clear locked selection - hides locked outline and clears locked element state
   * Called when switching preview modes to clean up UI
   */
  clearLockedSelection() {
    console.log('PreviewHoverManager: Clearing locked selection');
    // Hide locked outline
    this.hoverOverlay.hideLockedOutline();
    // Clear locked element
    this.lockedElement = {
      type: null,
      element: null,
      id: null,
      boundingBox: null
    };
  }

  /**
   * Get the current locked element
   * @returns {Object} The locked element object
   */
  getLockedElement() {
    return this.lockedElement;
  }

  /**
   * Public method to redraw the locked outline
   * Called when viewport or layout changes
   */
  redrawLockedOutline() {
    console.log('PreviewHoverManager: Redrawing locked outline');
    this._redrawLockedOutline();
  }

  /**
   * Redraw locked outline with recalculated bounding box from current DOM position
   * @private
   */
  _redrawLockedOutline() {
    if (!this.lockedElement || !this.lockedElement.type) {
      return;
    }

    try {
      // For iframe mode: requery the element from the iframe DOM to get updated position/size
      if (this.isIframeContainer && (this.lockedElement.id || this.lockedElement.rowId)) {
        const updatedInfo = this._getPreviewElementInfo({
          type: this.lockedElement.type,
          rowId: this.lockedElement.rowId,
          columnId: this.lockedElement.columnId,
          widgetId: this.lockedElement.id
        });

        if (updatedInfo && updatedInfo.element) {
          const rect = updatedInfo.element.getBoundingClientRect();
          if (rect && rect.width > 0 && rect.height > 0) {
            // Calculate iframe offset
            let offsetTop = 0;
            let offsetLeft = 0;
            if (this.previewContainer && this.previewContainer.getBoundingClientRect) {
              const iframeRect = this.previewContainer.getBoundingClientRect();
              offsetTop = iframeRect.top;
              offsetLeft = iframeRect.left;
            }

            // Update bounding box with current position and offset
            this.lockedElement.boundingBox = {
              top: rect.top + offsetTop,
              left: rect.left + offsetLeft,
              width: rect.width,
              height: rect.height
            };
            console.debug('PreviewHoverManager: Updated locked element bounding box from iframe:', this.lockedElement.boundingBox);
            this.hoverOverlay.showLockedOutlineFromBox(this.lockedElement.boundingBox, this.lockedElement.type, this.previewContainer);
            return;
          }
        }
      }

      // For non-iframe mode: requery element from regular DOM
      if (!this.isIframeContainer && (this.lockedElement.id || this.lockedElement.rowId)) {
        const updatedInfo = this._getPreviewElementInfo({
          type: this.lockedElement.type,
          rowId: this.lockedElement.rowId,
          columnId: this.lockedElement.columnId,
          widgetId: this.lockedElement.id
        });

        if (updatedInfo && updatedInfo.element) {
          const rect = updatedInfo.element.getBoundingClientRect();
          if (rect && rect.width > 0 && rect.height > 0) {
            // Update bounding box with current position
            this.lockedElement.boundingBox = {
              top: rect.top,
              left: rect.left,
              width: rect.width,
              height: rect.height
            };
            console.debug('PreviewHoverManager: Updated locked element bounding box from DOM:', this.lockedElement.boundingBox);
            this.hoverOverlay.showOutlineFromBox(this.lockedElement.boundingBox, this.lockedElement.type, null);
            return;
          }
        }
      }

      // Fall back to stored element reference if lookup failed
      if (this.lockedElement.element && typeof this.lockedElement.element.getBoundingClientRect === 'function') {
        const rect = this.lockedElement.element.getBoundingClientRect();
        if (rect && rect.width > 0 && rect.height > 0) {
          this.lockedElement.boundingBox = {
            top: rect.top,
            left: rect.left,
            width: rect.width,
            height: rect.height
          };
          console.debug('PreviewHoverManager: Updated locked element bounding box from element ref:', this.lockedElement.boundingBox);
          this.hoverOverlay.showLockedOutline(this.lockedElement.element, this.lockedElement.type);
          return;
        }
      }

      // Fall back to bounding box if element lookup failed
      if (this.lockedElement.boundingBox && this.lockedElement.type) {
        console.debug('PreviewHoverManager: Using stored bounding box (element not found for requery)');
        this.hoverOverlay.showLockedOutlineFromBox(this.lockedElement.boundingBox, this.lockedElement.type, this.previewContainer);
        return;
      }

      console.warn('PreviewHoverManager: Could not redraw locked outline - no element reference or bounding box');
    } catch (error) {
      console.error('PreviewHoverManager: Error redrawing locked outline:', error);
    }
  }
}

// Export for module systems or make globally available
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PreviewHoverManager;
} else if (typeof globalThis !== 'undefined') {
  globalThis.PreviewHoverManager = PreviewHoverManager;
}