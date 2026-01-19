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
    
    // Iframe context for proper event handling
    this.iframeElement = opts.iframeElement || null;
    this.iframeDoc = null;
    this.iframeWin = null;
    if (this.iframeElement && this.iframeElement.contentDocument) {
      this.iframeDoc = this.iframeElement.contentDocument;
      this.iframeWin = this.iframeElement.contentWindow;
    }
    
    // Track current element for dynamic repositioning
    this.currentElement = null;
    this.currentElementType = null;
    this.currentOutlinePosition = null;
    
    // Click handler for clickable outlines (row/column)
    this._outlineClickHandler = null;
    
    // MutationObserver for tracking DOM changes
    this._mutationObserver = null;
    this._isObservingMutations = false;
    
    // Event callbacks (set by PreviewHoverManager)
    this.onActionButtonClick = null;
    this.onActionButtonLeave = null;
    
    // Bind event handlers
    this._onButtonClick = this._onButtonClick.bind(this);
    this._onScrollOrResize = this._onScrollOrResize.bind(this);
    this._onMutation = this._onMutation.bind(this);
    
    // Throttle scroll/resize updates
    this._scrollResizeThrottle = null;
    this._isListeningForScrollResize = false;
    this._mutationThrottle = null;
  }
  
  /**
   * Set up scroll and resize listeners for dynamic outline adjustment
   * Handles both parent window and iframe contexts
   * @private
   */
  _setupScrollResizeListeners() {
    if (this._isListeningForScrollResize) {
      return;
    }
    
    // Listen on parent window
    if (this.win) {
      this.win.addEventListener('scroll', this._onScrollOrResize, true);
      this.win.addEventListener('resize', this._onScrollOrResize, true);
    }
    
    // Also listen on iframe window if present
    if (this.iframeWin && this.iframeWin !== this.win) {
      this.iframeWin.addEventListener('scroll', this._onScrollOrResize, true);
      this.iframeWin.addEventListener('resize', this._onScrollOrResize, true);
    }
    
    // Listen on iframe document for scroll events
    if (this.iframeDoc && this.iframeDoc !== this.doc) {
      this.iframeDoc.addEventListener('scroll', this._onScrollOrResize, true);
    }
    
    this._isListeningForScrollResize = true;
  }
  
  /**
   * Remove scroll and resize listeners
   * @private
   */
  _removeScrollResizeListeners() {
    if (!this._isListeningForScrollResize) {
      return;
    }
    
    // Remove from parent window
    if (this.win) {
      this.win.removeEventListener('scroll', this._onScrollOrResize, true);
      this.win.removeEventListener('resize', this._onScrollOrResize, true);
    }
    
    // Remove from iframe window
    if (this.iframeWin && this.iframeWin !== this.win) {
      this.iframeWin.removeEventListener('scroll', this._onScrollOrResize, true);
      this.iframeWin.removeEventListener('resize', this._onScrollOrResize, true);
    }
    
    // Remove from iframe document
    if (this.iframeDoc && this.iframeDoc !== this.doc) {
      this.iframeDoc.removeEventListener('scroll', this._onScrollOrResize, true);
    }
    
    this._isListeningForScrollResize = false;
  }
  
  /**
   * Handle scroll or resize events - update outline position
   * @private
   */
  _onScrollOrResize() {
    // Throttle updates to avoid excessive redraws
    if (this._scrollResizeThrottle) {
      clearTimeout(this._scrollResizeThrottle);
    }
    
    this._scrollResizeThrottle = setTimeout(() => {
      if (this.currentElement && this.isOutlineVisible) {
        try {
          const newPosition = this.calculateOutlinePosition(this.currentElement);
          if (newPosition && this._isValidPosition(newPosition)) {
            this.currentOutlinePosition = newPosition;
            if (this.outlineElement) {
              this._positionOutline(this.outlineElement, newPosition);
            }
            
            // Update button position if visible
            if (this.isButtonVisible && this.actionButtonElement) {
              const buttonPos = this.calculateButtonPosition(newPosition, this.currentElementType);
              if (buttonPos && this._isValidPosition(buttonPos)) {
                this._positionButton(this.actionButtonElement, buttonPos);
              }
            }
          }
        } catch (error) {
          console.error('HoverOverlay: Error updating outline on scroll/resize:', error);
        }
      }
    }, 16); // 16ms for 60fps
  }
  
  /**
   * Set up MutationObserver to detect DOM changes that affect element size/position
   * @private
   */
  _setupMutationObserver() {
    if (this._isObservingMutations || !this.currentElement) {
      return;
    }
    
    // Check if MutationObserver is available
    const MutationObserverConstructor = (this.win && this.win.MutationObserver) || 
                                        (this.iframeWin && this.iframeWin.MutationObserver) ||
                                        (typeof MutationObserver !== 'undefined' ? MutationObserver : null);
    
    if (!MutationObserverConstructor) {
      console.debug('HoverOverlay: MutationObserver not available');
      return;
    }
    
    try {
      this._mutationObserver = new MutationObserverConstructor(this._onMutation);
      
      // Observe the current element and its ancestors for changes
      const observeTarget = this.currentElement.ownerDocument || this.iframeDoc || this.doc;
      if (observeTarget && observeTarget.body) {
        this._mutationObserver.observe(observeTarget.body, {
          attributes: true,
          childList: true,
          subtree: true,
          attributeFilter: ['style', 'class']
        });
        this._isObservingMutations = true;
        console.debug('HoverOverlay: MutationObserver started');
      }
    } catch (error) {
      console.error('HoverOverlay: Error setting up MutationObserver:', error);
    }
  }
  
  /**
   * Remove MutationObserver
   * @private
   */
  _removeMutationObserver() {
    if (this._mutationObserver) {
      try {
        this._mutationObserver.disconnect();
        this._mutationObserver = null;
        this._isObservingMutations = false;
        console.debug('HoverOverlay: MutationObserver stopped');
      } catch (error) {
        console.error('HoverOverlay: Error removing MutationObserver:', error);
      }
    }
  }
  
  /**
   * Handle DOM mutations - update outline when element changes
   * @private
   * @param {MutationRecord[]} mutations - Array of mutation records
   */
  _onMutation(mutations) {
    // Throttle mutation updates to avoid excessive processing
    if (this._mutationThrottle) {
      return; // Already scheduled
    }
    
    this._mutationThrottle = setTimeout(() => {
      this._mutationThrottle = null;
      
      if (!this.currentElement || !this.isOutlineVisible) {
        return;
      }
      
      // Check if mutations affect the current element or its ancestors
      let shouldUpdate = false;
      for (const mutation of mutations) {
        const target = mutation.target;
        
        // Check if the mutation affects our tracked element
        if (target === this.currentElement || 
            this.currentElement.contains(target) ||
            (target.contains && target.contains(this.currentElement))) {
          shouldUpdate = true;
          break;
        }
      }
      
      if (shouldUpdate) {
        try {
          const newPosition = this.calculateOutlinePosition(this.currentElement);
          if (newPosition && this._isValidPosition(newPosition)) {
            this.currentOutlinePosition = newPosition;
            if (this.outlineElement) {
              this._positionOutline(this.outlineElement, newPosition);
            }
            
            // Update button position if visible
            if (this.isButtonVisible && this.actionButtonElement && this.currentElementType === 'widget') {
              const buttonPos = this.calculateButtonPosition(newPosition, this.currentElementType);
              if (buttonPos && this._isValidPosition(buttonPos)) {
                this._positionButton(this.actionButtonElement, buttonPos);
              }
            }
            
            console.debug('HoverOverlay: Outline updated due to DOM mutation');
          }
        } catch (error) {
          console.error('HoverOverlay: Error updating outline on mutation:', error);
        }
      }
    }, 50); // 50ms throttle for mutations (less frequent than scroll)
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
      // Store current element for dynamic repositioning
      this.currentElement = element;
      this.currentElementType = type;
      
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
      
      // Store position for scroll/resize updates
      this.currentOutlinePosition = position;
      
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
      
      // Set up click handler for row/column outlines (they're clickable anywhere)
      this._setupOutlineClickHandler(type);
      
      // Add to DOM if not already present
      if (!this.outlineElement.parentNode) {
        const parentBody = this.doc && this.doc.body ? this.doc.body : null;
        const success = this._insertElementIntoDOM(this.outlineElement, parentBody);
        if (!success) {
          console.error('HoverOverlay: Failed to insert outline into DOM');
          return;
        }
      }
      
      // Set up scroll/resize listeners for dynamic positioning
      this._setupScrollResizeListeners();
      
      // Set up mutation observer to detect DOM changes
      this._setupMutationObserver();
      
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
      
      // Set up click handler for row/column outlines (they're clickable anywhere)
      this._setupOutlineClickHandler(type);

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
   * Only widgets get action buttons - rows and columns are clicked directly
   * @param {Object} boundingBox - Bounding box with top, left, width, height
   * @param {string} type - Element type ('widget', 'column', 'row')
   * @param {HTMLElement} iframeElement - The iframe element (for offset calculation)
   */
  renderActionButtonFromBox(boundingBox, type, iframeElement) {
    // Render buttons for all types to avoid relying on outline click areas
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
      this.actionButtonElement = this._createActionButtonElement(type);
      if (!this.actionButtonElement) {
        console.error('HoverOverlay: Failed to create action button');
        return;
      }

      // Calculate button position (top-right of bounding box)
      const buttonSize = 36; // Match updated button size
      const offset = 6; // Match updated offset
      const buttonTop = boundingBox.top + offsetTop - offset - (buttonSize / 2);
      const buttonLeft = boundingBox.left + offsetLeft + boundingBox.width - (buttonSize / 2) - offset;

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
    // Remove click handler if present
    if (this._outlineClickHandler && this.outlineElement) {
      this.outlineElement.removeEventListener('click', this._outlineClickHandler);
      this._outlineClickHandler = null;
    }
    
    // Clean up scroll/resize listeners
    this._removeScrollResizeListeners();
    
    // Clear throttled scroll/resize timer
    if (this._scrollResizeThrottle) {
      clearTimeout(this._scrollResizeThrottle);
      this._scrollResizeThrottle = null;
    }
    
    // Clean up mutation observer
    this._removeMutationObserver();
    
    // Clear throttled mutation timer
    if (this._mutationThrottle) {
      clearTimeout(this._mutationThrottle);
      this._mutationThrottle = null;
    }
    
    // Clear current element reference
    this.currentElement = null;
    this.currentElementType = null;
    this.currentOutlinePosition = null;
    
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
   * Only widgets get action buttons - rows and columns are clicked directly
   * @param {Element} element - DOM element the button is associated with
   * @param {string} type - Element type ('widget', 'column', 'row')
   */
  renderActionButton(element, type) {
    // Only render buttons for widgets - rows and columns are clicked directly
    if (type === 'column' || type === 'row') {
      console.debug(`HoverOverlay: Skipping button for ${type} - click outline instead`);
      return;
    }
    
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
        this.actionButtonElement = this._createActionButtonElement(type);
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
      // Use larger button size and better offset for improved visibility
      const buttonSize = 36; // Increased button size (was 32px)
      const offsetFromEdge = 6; // Offset from outline edge
      
      // Get viewport dimensions
      const viewportWidth = (this.win && this.win.innerWidth) || (this.doc && this.doc.documentElement && this.doc.documentElement.clientWidth) || 0;
      const viewportHeight = (this.win && this.win.innerHeight) || (this.doc && this.doc.documentElement && this.doc.documentElement.clientHeight) || 0;
      
      // Calculate available space in each direction
      const spaceLeft = outlinePosition.left;
      const spaceBottom = viewportHeight - (outlinePosition.top + outlinePosition.height);
      
      // Default position: top-right corner with smart fallback
      let position = {
        top: Math.max(0, outlinePosition.top - offsetFromEdge - (buttonSize / 2)),
        left: Math.max(0, outlinePosition.left + outlinePosition.width - (buttonSize / 2) - offsetFromEdge),
        placement: 'top-right'
      };
      
      // If button would go off the right edge and there's space on left, move it
      if (position.left + buttonSize > viewportWidth && spaceLeft > buttonSize + 10) {
        position.left = outlinePosition.left - buttonSize + (buttonSize / 2) + offsetFromEdge;
        position.placement = 'top-left';
      }
      
      // If button would go off the top and there's space at bottom, move it
      if (position.top < 0 && spaceBottom > buttonSize + 10) {
        position.top = outlinePosition.top + outlinePosition.height + offsetFromEdge - (buttonSize / 2);
        position.placement = position.placement.replace('top', 'bottom');
      }
      
      // Clamp to viewport bounds as last resort
      position.top = Math.max(0, Math.min(position.top, viewportHeight - buttonSize));
      position.left = Math.max(0, Math.min(position.left, viewportWidth - buttonSize));
      
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
   * Set up click handler on outline for row/column types
   * Rows and columns are clickable anywhere in their outline
   * Widgets are not clickable (only their cog button is)
   * @private
   * @param {string} type - Element type ('widget', 'column', 'row')
   */
  _setupOutlineClickHandler(type) {
    if (!this.outlineElement) {
      return;
    }
    
    // Remove any existing click handler
    if (this._outlineClickHandler) {
      this.outlineElement.removeEventListener('click', this._outlineClickHandler);
      this._outlineClickHandler = null;
    }
    
    // Only rows and columns are clickable (widgets use the cog button)
    if (type === 'column' || type === 'row') {
      this._outlineClickHandler = (event) => {
        // Prevent click from propagating to underlying elements
        event.stopPropagation();
        event.preventDefault();
        
        // Trigger the action button click callback
        if (this.onActionButtonClick) {
          this.onActionButtonClick(event);
        }
        
        console.debug(`HoverOverlay: ${type} outline clicked`);
      };
      
      this.outlineElement.addEventListener('click', this._outlineClickHandler);
    }
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
   * @param {string} type - Element type ('widget', 'column', 'row') for styling
   * @returns {Element} Button element
   */
  _createActionButtonElement(type = 'widget') {
    const button = document.createElement('button');
    button.className = 'preview-hover-button';
    
    // Add type-specific styling class
    button.classList.add(`preview-hover-button--${type}`);
    
    button.type = 'button';
    button.setAttribute('aria-label', `Edit ${type} properties`);
    button.title = `Edit ${type} properties`;
    
    // Store element type for later reference
    button.dataset.elementType = type;
    
    // Add cog icon (using Font Awesome or similar)
    const icon = document.createElement('i');
    icon.className = 'fas fa-cog';
    icon.setAttribute('aria-hidden', 'true');
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