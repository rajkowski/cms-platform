/**
 * ElementDetector - Handles mouse event tracking and element identification
 * 
 * Monitors mouse position within preview container and identifies DOM elements
 * at cursor position with precedence logic (widget > column > row).
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 7.1
 */
class ElementDetector {
  
  /**
   * Initialize the element detector
   * @param {HTMLElement} previewContainer - The preview container to monitor
   */
  constructor(previewContainer, options) {
    // Validate preview container
    if (!previewContainer || !previewContainer.nodeType) {
      console.error('ElementDetector: Invalid preview container provided');
      throw new Error('ElementDetector requires a valid DOM element as preview container');
    }
    
    this.previewContainer = previewContainer;
    const opts = options || {};
    // Context (supports iframe documents)
    this.doc = opts.document || (typeof document !== 'undefined' ? document : null);
    this.win = opts.window || (typeof globalThis !== 'undefined' ? globalThis.window : (typeof window !== 'undefined' ? window : null));
    // Parent/iframe context for cross-document event handling
    this.parentDoc = opts.parentDocument || (typeof document !== 'undefined' ? document : null);
    this.parentWin = opts.parentWindow || (typeof window !== 'undefined' ? window : null);
    // If the preview container itself is an iframe, prefer it
    this.iframeEl = (previewContainer && previewContainer.tagName && previewContainer.tagName.toLowerCase() === 'iframe')
      ? previewContainer
      : (opts.iframeElement || (this.parentDoc && this.parentDoc.getElementById ? this.parentDoc.getElementById('preview-iframe') : null));
    this.iframeDoc = this.iframeEl && this.iframeEl.contentDocument ? this.iframeEl.contentDocument : null;
    this.iframeWin = this.iframeEl && this.iframeEl.contentWindow ? this.iframeEl.contentWindow : null;
    
    this.isListening = false;
    this.throttleDelay = 16; // 16ms for 60fps
    this.lastDetectionTime = 0;
    this.errorCount = 0;
    this.maxErrors = 10; // Maximum errors before disabling
    
    // Event callbacks (set by PreviewHoverManager)
    this.onElementDetected = null;
    this.onElementLost = null;
    
    // Bind event handlers
    this._onMouseMove = this._onMouseMove.bind(this);
    this._onMouseLeave = this._onMouseLeave.bind(this);
  }
  
  /**
   * Attach mouse event listeners to preview container
   */
  attachMouseListeners() {
    if (this.isListening || !this.previewContainer) {
      return;
    }
    
    // Check if container is still in DOM
    const isIframeContainer = !!(this.previewContainer && this.previewContainer.tagName && this.previewContainer.tagName.toLowerCase() === 'iframe');
    let containerInDom = false;
    if (isIframeContainer) {
      // The iframe element should exist in the parent document
      containerInDom = !!(this.parentDoc && this.parentDoc.contains && this.parentDoc.contains(this.previewContainer));
      // Also require that iframe's document exists for hover detection
      if (!this.iframeDoc) {
        console.error('ElementDetector: Iframe document not available yet');
        return;
      }
    } else {
      containerInDom = !!(this.doc && this.doc.contains && this.doc.contains(this.previewContainer));
    }
    if (!containerInDom) {
      console.error('ElementDetector: Preview container is not in DOM');
      return;
    }
    
    try {
      // Refresh iframe document/window references in case the iframe navigated
      if (this.iframeEl) {
        this.iframeDoc = this.iframeEl.contentDocument || this.iframeDoc;
        this.iframeWin = this.iframeEl.contentWindow || this.iframeWin;
      }
      // Listen on container, document, and window to ensure coverage inside iframe
      this.previewContainer.addEventListener('mousemove', this._onMouseMove);
      this.previewContainer.addEventListener('pointermove', this._onMouseMove);
      if (this.doc && this.doc.addEventListener) {
        this.doc.addEventListener('mousemove', this._onMouseMove);
        this.doc.addEventListener('pointermove', this._onMouseMove);
      }
      if (this.win && this.win.addEventListener) {
        this.win.addEventListener('mousemove', this._onMouseMove);
        this.win.addEventListener('pointermove', this._onMouseMove);
      }
      // Critically, attach listeners inside the iframe document/window so events are captured
      if (this.iframeDoc && this.iframeDoc.addEventListener) {
        this.iframeDoc.addEventListener('mousemove', this._onMouseMove);
        this.iframeDoc.addEventListener('pointermove', this._onMouseMove);
        this.iframeDoc.addEventListener('mouseleave', this._onMouseLeave);
      }
      if (this.iframeWin && this.iframeWin.addEventListener) {
        this.iframeWin.addEventListener('mousemove', this._onMouseMove);
        this.iframeWin.addEventListener('pointermove', this._onMouseMove);
      }
      if (this.parentDoc && this.parentDoc.addEventListener) {
        this.parentDoc.addEventListener('mousemove', this._onMouseMove);
        this.parentDoc.addEventListener('pointermove', this._onMouseMove);
      }
      if (this.parentWin && this.parentWin.addEventListener) {
        this.parentWin.addEventListener('mousemove', this._onMouseMove);
        this.parentWin.addEventListener('pointermove', this._onMouseMove);
      }
      this.previewContainer.addEventListener('mouseleave', this._onMouseLeave);
      this.isListening = true;
      const dbgDocInfo = {
        docHasElementsFromPoint: !!(this.doc && this.doc.elementsFromPoint),
        winInner: this.win ? { w: this.win.innerWidth, h: this.win.innerHeight } : null
      };
      console.debug('ElementDetector: Mouse listeners attached successfully', dbgDocInfo);
    } catch (error) {
      console.error('ElementDetector: Failed to attach mouse listeners:', error);
      this._handleError('Failed to attach mouse listeners', error);
    }
  }
  
  /**
   * Detach mouse event listeners from preview container
   */
  detachMouseListeners() {
    if (!this.isListening || !this.previewContainer) {
      return;
    }
    
    try {
      this.previewContainer.removeEventListener('mousemove', this._onMouseMove);
      this.previewContainer.removeEventListener('pointermove', this._onMouseMove);
      this.previewContainer.removeEventListener('mouseleave', this._onMouseLeave);
      try {
        if (this.doc && this.doc.removeEventListener) {
          this.doc.removeEventListener('mousemove', this._onMouseMove);
          this.doc.removeEventListener('pointermove', this._onMouseMove);
        }
        if (this.win && this.win.removeEventListener) {
          this.win.removeEventListener('mousemove', this._onMouseMove);
          this.win.removeEventListener('pointermove', this._onMouseMove);
        }
        if (this.iframeDoc && this.iframeDoc.removeEventListener) {
          this.iframeDoc.removeEventListener('mousemove', this._onMouseMove);
          this.iframeDoc.removeEventListener('pointermove', this._onMouseMove);
          this.iframeDoc.removeEventListener('mouseleave', this._onMouseLeave);
        }
        if (this.iframeWin && this.iframeWin.removeEventListener) {
          this.iframeWin.removeEventListener('mousemove', this._onMouseMove);
          this.iframeWin.removeEventListener('pointermove', this._onMouseMove);
        }
        if (this.parentDoc && this.parentDoc.removeEventListener) {
          this.parentDoc.removeEventListener('mousemove', this._onMouseMove);
          this.parentDoc.removeEventListener('pointermove', this._onMouseMove);
        }
        if (this.parentWin && this.parentWin.removeEventListener) {
          this.parentWin.removeEventListener('mousemove', this._onMouseMove);
          this.parentWin.removeEventListener('pointermove', this._onMouseMove);
        }
      } catch (removalError) {
        console.error('ElementDetector: Error removing document/window listeners:', removalError);
      }
      this.isListening = false;
      console.debug('ElementDetector: Mouse listeners detached successfully');
    } catch (error) {
      console.error('ElementDetector: Failed to detach mouse listeners:', error);
      this._handleError('Failed to detach mouse listeners', error);
      // Force isListening to false even if removal failed
      this.isListening = false;
    }
  }
  
  /**
   * Detect element at specific point coordinates
   * @param {number} x - X coordinate relative to viewport
   * @param {number} y - Y coordinate relative to viewport
   * @returns {Object|null} Element information or null if no valid element found
   */
  detectElementAtPoint(x, y) {
    // Validate coordinates
    if (typeof x !== 'number' || typeof y !== 'number' || Number.isNaN(x) || Number.isNaN(y)) {
      console.warn('ElementDetector: Invalid coordinates provided:', { x, y });
      return null;
    }
    
    
    try {
      // Get all elements at the point
      const docForDetection = (this.iframeDoc && this.iframeDoc.elementsFromPoint) ? this.iframeDoc : (this.doc && this.doc.elementsFromPoint ? this.doc : null);
      const elementsAtPoint = docForDetection ? docForDetection.elementsFromPoint(x, y) : [];
      if (elementsAtPoint && Array.isArray(elementsAtPoint)) {
        console.debug('ElementDetector: elementsFromPoint count', elementsAtPoint.length);
      }
      
      // Handle null or empty results from elementsFromPoint
      if (!elementsAtPoint) {
        console.debug('ElementDetector: document.elementsFromPoint returned null');
        return null;
      }
      
      if (!Array.isArray(elementsAtPoint)) {
        console.warn('ElementDetector: document.elementsFromPoint returned non-array:', elementsAtPoint);
        return null;
      }
      
      if (elementsAtPoint.length === 0) {
        console.debug('ElementDetector: No elements found at point:', { x, y });
        return null;
      }
      
      // Apply precedence rules to find the highest priority element
      return this.applyPrecedenceRules(elementsAtPoint);
    } catch (error) {
      console.error('ElementDetector: Error detecting element at point:', error);
      this._handleError('Error in detectElementAtPoint', error);
      return null;
    }
  }
  
  /**
   * Apply precedence rules to determine which element to highlight
   * Priority order: widget > column > row
   * @param {Element[]} elements - Array of DOM elements at cursor position
   * @returns {Object|null} Highest priority element info or null
   */
  applyPrecedenceRules(elements) {
    // Validate input
    if (!Array.isArray(elements) || elements.length === 0) {
      console.debug('ElementDetector: Invalid or empty elements array provided to applyPrecedenceRules');
      return null;
    }
    
    try {
      // Helper to find nearest ancestor with data attributes
      const findMatch = (el) => {
        if (!el || typeof el.closest !== 'function') return null;
        // Widget
        let match = el.closest('[data-widget]');
        if (match) {
          const id = this.getElementId(match, 'widget');
          if (id) return { type: 'widget', element: match, id };
        }
        // Column
        match = el.closest('[data-col-id]');
        if (match) {
          const id = this.getElementId(match, 'column');
          if (id) return { type: 'column', element: match, id };
        }
        // Row
        match = el.closest('[data-row-id]');
        if (match) {
          const id = this.getElementId(match, 'row');
          if (id) return { type: 'row', element: match, id };
        }
        return null;
      };

      // First pass: prefer widget
      for (const element of elements) {
        const info = findMatch(element);
        if (info && info.type === 'widget') return info;
      }
      // Second pass: column
      for (const element of elements) {
        const info = findMatch(element);
        if (info && info.type === 'column') return info;
      }
      // Third pass: row
      for (const element of elements) {
        const info = findMatch(element);
        if (info && info.type === 'row') return info;
      }
      
      console.debug('ElementDetector: No valid elements with data attributes found');
      return null;
    } catch (error) {
      console.error('ElementDetector: Error in applyPrecedenceRules:', error);
      this._handleError('Error in applyPrecedenceRules', error);
      return null;
    }
  }
  
  /**
   * Check if element is a widget
   * @param {Element} element - DOM element to check
   * @returns {boolean} True if element has widget data attribute
   */
  isWidget(element) {
    try {
      return element && 
             typeof element.hasAttribute === 'function' && 
             element.hasAttribute('data-widget');
    } catch (error) {
      console.debug('ElementDetector: Error checking if element is widget:', error);
      return false;
    }
  }
  
  /**
   * Check if element is a column
   * @param {Element} element - DOM element to check
   * @returns {boolean} True if element has column data attribute
   */
  isColumn(element) {
    try {
      return element && 
             typeof element.hasAttribute === 'function' && 
             element.hasAttribute('data-col-id');
    } catch (error) {
      console.debug('ElementDetector: Error checking if element is column:', error);
      return false;
    }
  }
  
  /**
   * Check if element is a row
   * @param {Element} element - DOM element to check
   * @returns {boolean} True if element has row data attribute
   */
  isRow(element) {
    try {
      return element && 
             typeof element.hasAttribute === 'function' && 
             element.hasAttribute('data-row-id');
    } catch (error) {
      console.debug('ElementDetector: Error checking if element is row:', error);
      return false;
    }
  }
  
  /**
   * Extract element ID from data attributes
   * @param {Element} element - DOM element
   * @param {string} type - Element type ('widget', 'column', 'row')
   * @returns {string|null} Element ID or null if not found
   */
  getElementId(element, type) {
    if (!element || typeof element.getAttribute !== 'function') {
      console.debug('ElementDetector: Invalid element provided to getElementId');
      return null;
    }
    
    if (!type || typeof type !== 'string') {
      console.debug('ElementDetector: Invalid type provided to getElementId:', type);
      return null;
    }
    
    try {
      let attributeName;
      switch (type) {
        case 'widget':
          attributeName = 'data-widget';
          break;
        case 'column':
          attributeName = 'data-col-id';
          break;
        case 'row':
          attributeName = 'data-row-id';
          break;
        default:
          console.warn('ElementDetector: Unknown element type:', type);
          return null;
      }
      
      const id = element.getAttribute(attributeName);
      
      // Validate that ID is not empty or just whitespace
      if (!id || typeof id !== 'string' || id.trim().length === 0) {
        console.debug(`ElementDetector: Empty or invalid ${type} ID found:`, id);
        return null;
      }
      
      return id.trim();
    } catch (error) {
      console.error('ElementDetector: Error getting element ID:', error);
      this._handleError('Error in getElementId', error);
      return null;
    }
  }
  
  /**
   * Handle mouse move events with throttling
   * @private
   * @param {MouseEvent} event - Mouse move event
   */
  _onMouseMove(event) {
    try {
      // Check if we've exceeded error threshold
      if (this.errorCount >= this.maxErrors) {
        console.warn('ElementDetector: Too many errors, disabling mouse move handling');
        this.detachMouseListeners();
        return;
      }
      
      // Validate event object
      if (!event || typeof event.clientX !== 'number' || typeof event.clientY !== 'number') {
        console.debug('ElementDetector: Invalid mouse event received');
        return;
      }
      
      // Throttle events for performance (60fps)
      const now = Date.now();
      if (now - this.lastDetectionTime < this.throttleDelay) {
        return;
      }
      this.lastDetectionTime = now;
      
      // Determine coordinates based on event source
      let x = event.clientX;
      let y = event.clientY;
      
      // If event is from the iframe window, use coordinates directly (already in iframe space)
      const isIframeEvent = !!(this.iframeWin && event.view === this.iframeWin);
      
      // If event is from parent window and we have an iframe, translate coordinates
      const isParentEvent = !!(this.parentWin && event.view === this.parentWin);
      
      if (!isIframeEvent && isParentEvent && this.iframeEl) {
        // Parent event - need to translate to iframe coordinates
        const rect = this.iframeEl.getBoundingClientRect();
        const inside = x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
        if (inside) {
          // Translate parent coordinates to iframe space
          x = x - rect.left;
          y = y - rect.top;
        } else {
          // Mouse is outside iframe in parent - ignore
          if (this.onElementLost) {
            this.onElementLost();
          }
          return;
        }
      }
      
      // Detect element at cursor position within iframe doc
      const elementInfo = this.detectElementAtPoint(x, y);
      
      if (elementInfo && this.onElementDetected) {
        this.onElementDetected(elementInfo);
      } else if (!elementInfo && this.onElementLost) {
        this.onElementLost();
      }
    } catch (error) {
      console.error('ElementDetector: Error in mouse move handler:', error);
      this._handleError('Error in _onMouseMove', error);
    }
  }
  
  /**
   * Handle mouse leave events
   * @private
   * @param {MouseEvent} event - Mouse leave event
   */
  _onMouseLeave(event) {
    try {
      // Validate event object
      if (!event) {
        console.debug('ElementDetector: Invalid mouse leave event received');
        return;
      }
      
      // Check if mouse is moving to the action button
      const relatedTarget = event.relatedTarget;
      if (relatedTarget && (
          relatedTarget.classList.contains('preview-hover-button') ||
          relatedTarget.closest('.preview-hover-button')
      )) {
        // Don't trigger element lost if moving to action button
        return;
      }
      
      if (this.onElementLost) {
        this.onElementLost();
      }
    } catch (error) {
      console.error('ElementDetector: Error in mouse leave handler:', error);
      this._handleError('Error in _onMouseLeave', error);
    }
  }
  
  /**
   * Handle errors and track error count
   * @private
   * @param {string} context - Context where error occurred
   * @param {Error} error - The error object
   */
  _handleError(context, error) {
    this.errorCount++;
    console.error(`ElementDetector: ${context} (Error #${this.errorCount}):`, error);
    
    // If too many errors, disable the detector
    if (this.errorCount >= this.maxErrors) {
      console.error('ElementDetector: Maximum error threshold reached, disabling detector');
      this.detachMouseListeners();
      
      // Notify parent component if callback exists
      if (this.onElementLost) {
        try {
          this.onElementLost();
        } catch (callbackError) {
          console.error('ElementDetector: Error calling onElementLost callback:', callbackError);
        }
      }
    }
  }
  
  /**
   * Reset error count (can be called by parent to re-enable after fixing issues)
   */
  resetErrorCount() {
    this.errorCount = 0;
    console.debug('ElementDetector: Error count reset');
  }
}

// Export for module systems or make globally available
if (typeof module !== 'undefined' && module.exports) {
  module.exports = ElementDetector;
} else if (typeof window !== 'undefined') {
  window.ElementDetector = ElementDetector;
}