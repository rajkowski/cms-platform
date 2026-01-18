/**
 * HoverOverlay - Manages visual rendering of hover outlines and action buttons
 * 
 * Creates and positions hover outline elements and action buttons without
 * affecting page layout. Uses absolute positioning to avoid layout reflow.
 * 
 * Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.4, 4.1, 4.2, 4.4, 4.5, 4.6
 */
class HoverOverlay {
  
  /**
   * Initialize the hover overlay manager
   */
  constructor(options) {
    const opts = options || {};
    this.outlineElement = null;
    this.actionButtonElement = null;
    this.isOutlineVisible = false;
    this.isButtonVisible = false;
    this.errorCount = 0;
    this.maxErrors = 10; // Maximum errors before disabling

    // Context (supports iframe documents)
    this.doc = opts.document || (typeof document !== 'undefined' ? document : null);
    this.win = opts.window || (typeof globalThis !== 'undefined' ? globalThis.window : (typeof window !== 'undefined' ? window : null));
    
    // Event callbacks (set by PreviewHoverManager)
    this.onActionButtonClick = null;
    this.onActionButtonLeave = null;
    
    // Bind event handlers
    this._onButtonClick = this._onButtonClick.bind(this);
  }
  
  /**
   * Show outline around the specified element
   * @param {Element} element - DOM element to outline
   * @param {string} type - Element type ('widget', 'column', 'row')
   */
  showOutline(element, type) {
    if (!element) {
      console.debug('HoverOverlay: No element provided to showOutline');
      return;
    }
    
    // Check if we've exceeded error threshold
    if (this.errorCount >= this.maxErrors) {
      console.warn('HoverOverlay: Too many errors, outline functionality disabled');
      return;
    }
    
    // Validate element is still in DOM
    if (!this.doc || !this.doc.contains || !this.doc.contains(element)) {
      console.debug('HoverOverlay: Element is no longer in DOM');
      return;
    }
    
    try {
      // Calculate outline position
      const position = this.calculateOutlinePosition(element);
      if (!position) {
        console.debug('HoverOverlay: Could not calculate position for element');
        return;
      }
      
      // Validate position values
      if (!this._isValidPosition(position)) {
        console.warn('HoverOverlay: Invalid position calculated:', position);
        return;
      }
      
      // Implement singleton pattern - ensure only one outline exists
      this._ensureSingleOutline();
      
      // Create or update outline element
      if (!this.outlineElement) {
        this.outlineElement = this._createOutlineElement();
        if (!this.outlineElement) {
          console.error('HoverOverlay: Failed to create outline element');
          return;
        }
      }
      
      // Position and style the outline
      this._positionOutline(this.outlineElement, position);
      this.applyOutlineStyle(this.outlineElement, type);
      
      // Add to DOM if not already present
      if (!this.outlineElement.parentNode) {
        const parentBody = this.doc && this.doc.body ? this.doc.body : null;
        const success = this._insertElementIntoDOM(this.outlineElement, parentBody);
        if (!success) {
          console.error('HoverOverlay: Failed to insert outline into DOM');
          return;
        }
      }
      
      this.isOutlineVisible = true;
      console.debug('HoverOverlay: Outline shown successfully');
    } catch (error) {
      console.error('HoverOverlay: Error showing outline:', error);
      this._handleError('Error in showOutline', error);
    }
  }
  
  /**
   * Show outline using bounding box coordinates (for iframe communication)
   * @param {Object} boundingBox - Bounding box with top, left, width, height
   * @param {string} type - Element type ('widget', 'column', 'row')
   * @param {HTMLElement} iframeElement - The iframe element (for offset calculation)
   */
  showOutlineFromBox(boundingBox, type, iframeElement) {
    if (!boundingBox) {
      console.debug('HoverOverlay: No bounding box provided');
      return;
    }

    if (this.errorCount >= this.maxErrors) {
      console.warn('HoverOverlay: Too many errors, outline functionality disabled');
      return;
    }

    try {
      // Calculate offset if iframe is provided
      let offsetTop = 0;
      let offsetLeft = 0;
      if (iframeElement && iframeElement.getBoundingClientRect) {
        const rect = iframeElement.getBoundingClientRect();
        offsetTop = rect.top;
        offsetLeft = rect.left;
      }

      // Ensure only one outline exists
      this._ensureSingleOutline();

      // Create outline element if needed
      if (!this.outlineElement) {
        this.outlineElement = this._createOutlineElement();
        if (!this.outlineElement) {
          console.error('HoverOverlay: Failed to create outline element');
          return;
        }
      }

      // Position outline using bounding box
      this.outlineElement.style.position = 'fixed';
      this.outlineElement.style.top = (boundingBox.top + offsetTop) + 'px';
      this.outlineElement.style.left = (boundingBox.left + offsetLeft) + 'px';
      this.outlineElement.style.width = boundingBox.width + 'px';
      this.outlineElement.style.height = boundingBox.height + 'px';

      // Apply type-specific styling
      this.applyOutlineStyle(this.outlineElement, type);

      // Add to DOM if not already present
      if (!this.outlineElement.parentNode) {
        const parentBody = this.doc && this.doc.body ? this.doc.body : null;
        const success = this._insertElementIntoDOM(this.outlineElement, parentBody);
        if (!success) {
          console.error('HoverOverlay: Failed to insert outline into DOM');
          return;
        }
      }

      this.isOutlineVisible = true;
      console.debug('HoverOverlay: Outline shown from bounding box');
    } catch (error) {
      console.error('HoverOverlay: Error showing outline from box:', error);
      this._handleError('Error in showOutlineFromBox', error);
    }
  }

  /**
   * Render action button using bounding box coordinates (for iframe communication)
   * @param {Object} boundingBox - Bounding box with top, left, width, height
   * @param {string} type - Element type ('widget', 'column', 'row')
   * @param {HTMLElement} iframeElement - The iframe element (for offset calculation)
   */
  renderActionButtonFromBox(boundingBox, type, iframeElement) {
    if (!boundingBox || !this.outlineElement) {
      console.debug('HoverOverlay: Missing bounding box or outline for button');
      return;
    }

    if (this.errorCount >= this.maxErrors) {
      console.warn('HoverOverlay: Too many errors, button functionality disabled');
      return;
    }

    try {
      // Calculate offset if iframe is provided
      let offsetTop = 0;
      let offsetLeft = 0;
      if (iframeElement && iframeElement.getBoundingClientRect) {
        const rect = iframeElement.getBoundingClientRect();
        offsetTop = rect.top;
        offsetLeft = rect.left;
      }

      // Remove existing button
      this.removeActionButton();

      // Create button element
      this.actionButtonElement = this._createActionButtonElement();
      if (!this.actionButtonElement) {
        console.error('HoverOverlay: Failed to create action button');
        return;
      }

      // Calculate button position (top-right of bounding box)
      const buttonSize = 32;
      const offset = 5;
      const buttonTop = boundingBox.top + offsetTop - offset;
      const buttonLeft = boundingBox.left + offsetLeft + boundingBox.width - buttonSize + offset;

      this.actionButtonElement.style.position = 'fixed';
      this.actionButtonElement.style.top = buttonTop + 'px';
      this.actionButtonElement.style.left = buttonLeft + 'px';

      // Add to DOM
      const parentBody = this.doc && this.doc.body ? this.doc.body : null;
      const success = this._insertElementIntoDOM(this.actionButtonElement, parentBody);
      if (!success) {
        console.error('HoverOverlay: Failed to insert action button into DOM');
        return;
      }

      this.isButtonVisible = true;
      console.debug('HoverOverlay: Action button rendered from bounding box');
    } catch (error) {
      console.error('HoverOverlay: Error rendering action button from box:', error);
      this._handleError('Error in renderActionButtonFromBox', error);
    }
  }

  /**
   * Set up action button click callback
   * @param {Function} callback - Callback function to call on button click
   */
  setActionButtonClickCallback(callback) {
    this.onActionButtonClick = callback;
  }

  /**
   * Hide the current outline
   */
  hideOutline() {
    if (this.outlineElement && this.outlineElement.parentNode) {
      try {
        // Verify element is still in DOM before attempting removal
        if (this.doc && this.doc.contains && this.doc.contains(this.outlineElement)) {
          this.outlineElement.parentNode.removeChild(this.outlineElement);
          console.debug('HoverOverlay: Outline hidden successfully');
        } else {
          console.debug('HoverOverlay: Outline element was already removed from DOM');
        }
      } catch (error) {
        console.error('HoverOverlay: Error hiding outline:', error);
        this._handleError('Error in hideOutline', error);
        
        // Try alternative removal method
        try {
          if (this.outlineElement.remove) {
            this.outlineElement.remove();
          }
        } catch (removeError) {
          console.error('HoverOverlay: Alternative removal method also failed:', removeError);
        }
      }
    }
    this.isOutlineVisible = false;
  }
  
  /**
   * Render action button for the current outline
   * @param {Element} element - DOM element the button is associated with
   * @param {string} type - Element type ('widget', 'column', 'row')
   */
  renderActionButton(element, type) {
    if (!element || !this.outlineElement) {
      console.debug('HoverOverlay: Missing element or outline for button rendering');
      return;
    }
    
    // Check if we've exceeded error threshold
    if (this.errorCount >= this.maxErrors) {
      console.warn('HoverOverlay: Too many errors, button functionality disabled');
      return;
    }
    
    try {
      // Calculate button position
      const outlinePosition = this.calculateOutlinePosition(element);
      if (!outlinePosition) {
        console.debug('HoverOverlay: Could not calculate outline position for button');
        return;
      }
      
      const buttonPosition = this.calculateButtonPosition(outlinePosition, type);
      if (!buttonPosition || !this._isValidPosition(buttonPosition)) {
        console.warn('HoverOverlay: Invalid button position calculated:', buttonPosition);
        return;
      }
      
      // Implement singleton pattern - ensure only one button exists
      this._ensureSingleButton();
      
      // Create or update button element
      if (!this.actionButtonElement) {
        this.actionButtonElement = this._createActionButtonElement();
        if (!this.actionButtonElement) {
          console.error('HoverOverlay: Failed to create action button element');
          return;
        }
      }
      
      // Position the button
      this._positionButton(this.actionButtonElement, buttonPosition);
      
      // Add to DOM if not already present
      if (!this.actionButtonElement.parentNode) {
        const parentBody = this.doc && this.doc.body ? this.doc.body : null;
        const success = this._insertElementIntoDOM(this.actionButtonElement, parentBody);
        if (!success) {
          console.error('HoverOverlay: Failed to insert button into DOM');
          return;
        }
      }
      
      this.isButtonVisible = true;
      console.debug('HoverOverlay: Action button rendered successfully');
    } catch (error) {
      console.error('HoverOverlay: Error rendering action button:', error);
      this._handleError('Error in renderActionButton', error);
    }
  }
  
  /**
   * Remove the action button
   */
  removeActionButton() {
    if (this.actionButtonElement && this.actionButtonElement.parentNode) {
      try {
        // Verify element is still in DOM before attempting removal
        if (this.doc && this.doc.contains && this.doc.contains(this.actionButtonElement)) {
          this.actionButtonElement.parentNode.removeChild(this.actionButtonElement);
          console.debug('HoverOverlay: Action button removed successfully');
        } else {
          console.debug('HoverOverlay: Action button was already removed from DOM');
        }
      } catch (error) {
        console.error('HoverOverlay: Error removing action button:', error);
        this._handleError('Error in removeActionButton', error);
        
        // Try alternative removal method
        try {
          if (this.actionButtonElement.remove) {
            this.actionButtonElement.remove();
          }
        } catch (removeError) {
          console.error('HoverOverlay: Alternative button removal method also failed:', removeError);
        }
      }
    }
    this.isButtonVisible = false;
  }
  
  /**
   * Calculate outline position based on element bounding box
   * @param {Element} element - Target element
   * @returns {Object|null} Position object with top, left, width, height
   */
  calculateOutlinePosition(element) {
    if (!element || typeof element.getBoundingClientRect !== 'function') {
      console.debug('HoverOverlay: Invalid element provided to calculateOutlinePosition');
      return null;
    }
    
    try {
      const rect = element.getBoundingClientRect();
      
      // Validate bounding rect
      if (!rect || typeof rect.top !== 'number' || typeof rect.left !== 'number' ||
          typeof rect.width !== 'number' || typeof rect.height !== 'number') {
        console.warn('HoverOverlay: Invalid bounding rect returned:', rect);
        return null;
      }
      
      // Check for zero or negative dimensions
      if (rect.width <= 0 || rect.height <= 0) {
        console.debug('HoverOverlay: Element has zero or negative dimensions:', rect);
        return null;
      }
      
      // Get scroll offsets safely
      const scrollX = (this.win && this.win.pageXOffset) || (this.doc && this.doc.documentElement && this.doc.documentElement.scrollLeft) || 0;
      const scrollY = (this.win && this.win.pageYOffset) || (this.doc && this.doc.documentElement && this.doc.documentElement.scrollTop) || 0;
      
      const position = {
        top: rect.top + scrollY,
        left: rect.left + scrollX,
        width: rect.width,
        height: rect.height
      };
      
      // Validate final position
      if (!this._isValidPosition(position)) {
        console.warn('HoverOverlay: Calculated invalid position:', position);
        return null;
      }
      
      return position;
    } catch (error) {
      console.error('HoverOverlay: Error calculating outline position:', error);
      this._handleError('Error in calculateOutlinePosition', error);
      return null;
    }
  }
  
  /**
   * Calculate button position relative to outline
   * @param {Object} outlinePosition - Outline position object
   * @param {string} type - Element type for consistent positioning
   * @returns {Object} Button position with top, left, placement
   */
  calculateButtonPosition(outlinePosition, type) {
    if (!outlinePosition || !this._isValidPosition(outlinePosition)) {
      console.debug('HoverOverlay: Invalid outline position provided to calculateButtonPosition');
      return null;
    }
    
    try {
      // Consistent positioning: top-right corner of outline
      const buttonSize = 32; // Button width/height in pixels
      const offset = 8; // Offset from outline edge
      
      const position = {
        top: outlinePosition.top - offset,
        left: outlinePosition.left + outlinePosition.width - buttonSize + offset,
        placement: 'top-right'
      };
      
      // Ensure button stays within viewport
      const viewportWidth = (this.win && this.win.innerWidth) || (this.doc && this.doc.documentElement && this.doc.documentElement.clientWidth) || 0;
      const viewportHeight = (this.win && this.win.innerHeight) || (this.doc && this.doc.documentElement && this.doc.documentElement.clientHeight) || 0;
      
      // Adjust if button would be outside viewport
      if (position.left + buttonSize > viewportWidth) {
        position.left = outlinePosition.left - buttonSize - offset;
        position.placement = 'top-left';
      }
      
      if (position.top < 0) {
        position.top = outlinePosition.top + outlinePosition.height + offset;
        position.placement = position.placement.replace('top', 'bottom');
      }
      
      return position;
    } catch (error) {
      console.error('HoverOverlay: Error calculating button position:', error);
      this._handleError('Error in calculateButtonPosition', error);
      return null;
    }
  }
  
  /**
   * Apply styling to outline element based on element type
   * @param {Element} outlineElement - Outline DOM element
   * @param {string} type - Element type ('widget', 'column', 'row')
   */
  applyOutlineStyle(outlineElement, type) {
    if (!outlineElement) {
      return;
    }
    
    // Base outline class
    outlineElement.className = 'preview-hover-outline';
    
    // Add type-specific class for potential customization
    outlineElement.classList.add(`preview-hover-outline--${type}`);
  }
  
  /**
   * Create outline DOM element
   * @private
   * @returns {Element} Outline element
   */
  _createOutlineElement() {
    const outline = document.createElement('div');
    outline.className = 'preview-hover-outline';
    return outline;
  }
  
  /**
   * Create action button DOM element
   * @private
   * @returns {Element} Button element
   */
  _createActionButtonElement() {
    const button = document.createElement('button');
    button.className = 'preview-hover-button';
    button.type = 'button';
    button.setAttribute('aria-label', 'Edit element properties');
    button.title = 'Edit properties';
    
    // Add cog icon (using Font Awesome or similar)
    const icon = document.createElement('i');
    icon.className = 'fas fa-cog';
    button.appendChild(icon);
    
    // Attach click handler
    button.addEventListener('click', this._onButtonClick);
    
    // Add hover handlers to prevent flickering
    button.addEventListener('mouseenter', (e) => {
      e.stopPropagation();
      // Keep button visible when hovering over it
    });
    
    button.addEventListener('mouseleave', (e) => {
      // Only hide if not moving back to the outlined element
      const relatedTarget = e.relatedTarget;
      if (!relatedTarget || (!relatedTarget.hasAttribute('data-widget') && 
          !relatedTarget.hasAttribute('data-col-id') && 
          !relatedTarget.hasAttribute('data-row-id') &&
          !relatedTarget.closest('[data-widget], [data-col-id], [data-row-id]'))) {
        // Mouse left both button and element area, trigger cleanup
        setTimeout(() => {
          if (this.onActionButtonLeave) {
            this.onActionButtonLeave();
          }
        }, 50); // Small delay to allow for mouse movement
      }
    });
    
    return button;
  }
  
  /**
   * Position outline element
   * @private
   * @param {Element} outline - Outline element
   * @param {Object} position - Position object
   */
  _positionOutline(outline, position) {
    outline.style.position = 'absolute';
    outline.style.top = position.top + 'px';
    outline.style.left = position.left + 'px';
    outline.style.width = position.width + 'px';
    outline.style.height = position.height + 'px';
  }
  
  /**
   * Position button element
   * @private
   * @param {Element} button - Button element
   * @param {Object} position - Position object
   */
  _positionButton(button, position) {
    button.style.position = 'absolute';
    button.style.top = position.top + 'px';
    button.style.left = position.left + 'px';
  }
  
  /**
   * Handle button click events
   * @private
   * @param {Event} event - Click event
   */
  _onButtonClick(event) {
    if (this.onActionButtonClick) {
      this.onActionButtonClick(event);
    }
  }
  
  /**
   * Validate position object has valid numeric values
   * @private
   * @param {Object} position - Position object to validate
   * @returns {boolean} True if position is valid
   */
  _isValidPosition(position) {
    if (!position || typeof position !== 'object') {
      return false;
    }
    
    const requiredProps = ['top', 'left'];
    for (const prop of requiredProps) {
      if (typeof position[prop] !== 'number' || isNaN(position[prop]) || !isFinite(position[prop])) {
        return false;
      }
    }
    
    // Check width and height if they exist
    if (position.width !== undefined && (typeof position.width !== 'number' || position.width < 0)) {
      return false;
    }
    if (position.height !== undefined && (typeof position.height !== 'number' || position.height < 0)) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Safely insert element into DOM with error handling
   * @private
   * @param {Element} element - Element to insert
   * @param {Element} parent - Parent element to insert into
   * @returns {boolean} True if insertion was successful
   */
  _insertElementIntoDOM(element, parent) {
    if (!element || !parent) {
      console.debug('HoverOverlay: Invalid element or parent for DOM insertion');
      return false;
    }
    
    try {
      parent.appendChild(element);
      
      // Verify insertion was successful
      if (element.parentNode === parent) {
        return true;
      } else {
        console.warn('HoverOverlay: Element insertion appeared to succeed but parent check failed');
        return false;
      }
    } catch (error) {
      console.error('HoverOverlay: Failed to insert element into DOM:', error);
      this._handleError('Error in _insertElementIntoDOM', error);
      return false;
    }
  }
  
  /**
   * Ensure only one outline element exists (singleton pattern)
   * @private
   */
  _ensureSingleOutline() {
    try {
      // Remove any existing outline elements that aren't ours
      const existingOutlines = (this.doc && this.doc.querySelectorAll) ? this.doc.querySelectorAll('.preview-hover-outline') : [];
      for (const outline of existingOutlines) {
        if (outline !== this.outlineElement) {
          console.debug('HoverOverlay: Removing duplicate outline element');
          if (outline.parentNode) {
            outline.parentNode.removeChild(outline);
          }
        }
      }
    } catch (error) {
      console.error('HoverOverlay: Error ensuring single outline:', error);
    }
  }
  
  /**
   * Ensure only one button element exists (singleton pattern)
   * @private
   */
  _ensureSingleButton() {
    try {
      // Remove any existing button elements that aren't ours
      const existingButtons = (this.doc && this.doc.querySelectorAll) ? this.doc.querySelectorAll('.preview-hover-button') : [];
      for (const button of existingButtons) {
        if (button !== this.actionButtonElement) {
          console.debug('HoverOverlay: Removing duplicate button element');
          if (button.parentNode) {
            button.parentNode.removeChild(button);
          }
        }
      }
    } catch (error) {
      console.error('HoverOverlay: Error ensuring single button:', error);
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
    console.error(`HoverOverlay: ${context} (Error #${this.errorCount}):`, error);
    
    // If too many errors, disable the overlay
    if (this.errorCount >= this.maxErrors) {
      console.error('HoverOverlay: Maximum error threshold reached, disabling overlay');
      this.hideOutline();
      this.removeActionButton();
    }
  }
  
  /**
   * Reset error count (can be called by parent to re-enable after fixing issues)
   */
  resetErrorCount() {
    this.errorCount = 0;
    console.debug('HoverOverlay: Error count reset');
  }
}

// Export for module systems or make globally available
if (typeof module !== 'undefined' && module.exports) {
  module.exports = HoverOverlay;
} else if (typeof window !== 'undefined') {
  window.HoverOverlay = HoverOverlay;
}