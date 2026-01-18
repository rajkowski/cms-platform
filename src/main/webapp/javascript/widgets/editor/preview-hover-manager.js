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
    
    this.isEnabled = false;
    
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
    
    const self = this;
    window.addEventListener('message', function(event) {
      if (!event.data || !event.data.type) {
        return;
      }
      
      // Handle iframe hover detection messages
      if (event.data.type === 'previewHover:elementDetected' && event.data.data) {
        const data = event.data.data;
        console.debug('PreviewHoverManager: Received element detected from iframe:', data.type, data.id);
        
        // Create synthetic element info using the bounding box from iframe
        const elementInfo = {
          type: data.type,
          id: data.id,
          element: null, // No element reference from iframe
          boundingBox: data.boundingBox
        };
        
        // Check if same element
        if (self.hoverState.currentElement.type === data.type &&
            self.hoverState.currentElement.id === data.id) {
          return; // No change
        }
        
        self.hoverState.currentElement = {
          type: data.type,
          element: null,
          id: data.id,
          rowId: data.rowId,
          columnId: data.columnId,
          boundingBox: data.boundingBox
        };
        
        // Show outline using bounding box coordinates
        self.hoverOverlay.showOutlineFromBox(data.boundingBox, data.type, self.previewContainer);
        self.hoverState.isOutlineVisible = true;
        
        // Set up button click callback before rendering
        self.hoverOverlay.setActionButtonClickCallback(function() {
          self._onActionButtonClick();
        });
        
        // Render action button at bounding box location
        self.hoverOverlay.renderActionButtonFromBox(data.boundingBox, data.type, self.previewContainer);
        self.hoverState.isButtonVisible = true;
        
      } else if (event.data.type === 'previewHover:elementLost') {
        console.debug('PreviewHoverManager: Received element lost from iframe');
        
        if (self.hoverState.currentElement.id) {
          self.hoverOverlay.hideOutline();
          self.hoverOverlay.removeActionButton();
          self._resetCurrentElement();
        }
      }
    });
  }
  
  /**
   * Enable hover functionality
   * Activates hover detection and visual feedback
   */
  enable() {
    if (this.isEnabled) {
      return;
    }
    
    try {
      if (this.isIframeContainer) {
        // For iframe: send enable message to iframe's hover bridge
        if (this.previewContainer.contentWindow) {
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
    } else {
      this.disable();
    }
  }
  
  /**
   * Get current hover state (for debugging/testing)
   * @returns {Object} Current hover state
   */
  getHoverState() {
    return { ...this.hoverState };
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
      // Check if this is the same element we're already showing
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
      
      // Render action button
      this.hoverOverlay.renderActionButton(elementInfo.element, elementInfo.type);
      this.hoverState.isButtonVisible = true;
      
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
      // Hide outline and button
      this.hoverOverlay.hideOutline();
      this.hoverOverlay.removeActionButton();
      
      // Reset state
      this._resetCurrentElement();
      
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
}

// Export for module systems or make globally available
if (typeof module !== 'undefined' && module.exports) {
  module.exports = PreviewHoverManager;
} else if (typeof window !== 'undefined') {
  window.PreviewHoverManager = PreviewHoverManager;
}