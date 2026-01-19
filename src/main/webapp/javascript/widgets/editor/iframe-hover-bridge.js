/**
 * IframeHoverBridge - postMessage-based communication for iframe hover detection
 * 
 * Runs inside the preview iframe to detect hovers and communicate element info
 * to the parent window. The parent renders outlines and buttons in the iframe.
 * 
 * This avoids cross-document DOM complexity by using messaging.
 */
(function initializeIframeHoverBridge() {
  // Only initialize if we're inside an iframe
  if (window.self === window.top) {
    console.debug('IframeHoverBridge: Not in iframe, skipping');
    return;
  }

  const iframeHoverBridge = {
    isEnabled: false,
    lastElement: null,
    throttleDelay: 16, // 16ms for 60fps
    lastDetectionTime: 0,
    boundMouseMove: null,
    boundMouseLeave: null,
    eventCount: 0,

    init() {
      console.debug('IframeHoverBridge: Initializing');
      
      // Store bound functions for proper cleanup
      if (!this.boundMouseMove) {
        this.boundMouseMove = this.onMouseMove.bind(this);
      }
      if (!this.boundMouseLeave) {
        this.boundMouseLeave = this.onMouseLeave.bind(this);
      }
      
      // Remove any existing listeners before adding new ones
      document.removeEventListener('mousemove', this.boundMouseMove);
      document.removeEventListener('mouseleave', this.boundMouseLeave);
      
      // Add listeners
      document.addEventListener('mousemove', this.boundMouseMove);
      document.addEventListener('mouseleave', this.boundMouseLeave);
      this.isEnabled = true;
      this.eventCount = 0;
      console.debug('IframeHoverBridge: Initialized and listening for mousemove events');
    },

    disable() {
      console.debug('IframeHoverBridge: Disabling');
      if (this.boundMouseMove) {
        document.removeEventListener('mousemove', this.boundMouseMove);
      }
      if (this.boundMouseLeave) {
        document.removeEventListener('mouseleave', this.boundMouseLeave);
      }
      this.isEnabled = false;
      this.lastElement = null;
      // Tell parent to hide outline
      window.parent.postMessage({ type: 'previewHover:elementLost' }, '*');
    },

    onMouseMove(event) {
      this.eventCount++;
      if (this.eventCount <= 3) {
        console.debug('IframeHoverBridge: mousemove event received', this.eventCount, 'enabled:', this.isEnabled);
      }
      
      if (!this.isEnabled) {
        if (this.eventCount === 1) {
          console.warn('IframeHoverBridge: Received mousemove but bridge is disabled');
        }
        return;
      }

      // Throttle
      const now = Date.now();
      if (now - this.lastDetectionTime < this.throttleDelay) {
        return;
      }
      this.lastDetectionTime = now;

      const { clientX, clientY } = event;
      const elementsAtPoint = document.elementsFromPoint(clientX, clientY);

      if (!elementsAtPoint || elementsAtPoint.length === 0) {
        if (this.lastElement) {
          this.lastElement = null;
          window.parent.postMessage({ type: 'previewHover:elementLost' }, '*');
        }
        return;
      }

      // Find widget, column, or row
      let elementInfo = null;
      for (const el of elementsAtPoint) {
        // Widget
        if (el.hasAttribute && el.hasAttribute('data-widget')) {
          const id = el.getAttribute('data-widget');
          if (id) {
            elementInfo = { type: 'widget', id, element: el };
            break;
          }
        }
        // Column
        if (el.hasAttribute && el.hasAttribute('data-col-id')) {
          const id = el.getAttribute('data-col-id');
          if (id) {
            elementInfo = { type: 'column', id, element: el };
            break;
          }
        }
        // Row
        if (el.hasAttribute && el.hasAttribute('data-row-id')) {
          const id = el.getAttribute('data-row-id');
          if (id) {
            elementInfo = { type: 'row', id, element: el };
            break;
          }
        }
      }

      // Check if same element
      if (elementInfo && this.lastElement &&
          elementInfo.type === this.lastElement.type &&
          elementInfo.id === this.lastElement.id) {
        return; // No change
      }

      this.lastElement = elementInfo;

      if (elementInfo) {
        const rect = elementInfo.element.getBoundingClientRect();
        
        // Find parent row/column context for widgets
        let rowId = null;
        let columnId = null;
        if (elementInfo.type === 'widget') {
          // Find parent column
          let parent = elementInfo.element.parentElement;
          while (parent) {
            if (parent.hasAttribute && parent.hasAttribute('data-col-id')) {
              columnId = parent.getAttribute('data-col-id');
              break;
            }
            parent = parent.parentElement;
          }
          // Find parent row
          parent = elementInfo.element.parentElement;
          while (parent) {
            if (parent.hasAttribute && parent.hasAttribute('data-row-id')) {
              rowId = parent.getAttribute('data-row-id');
              break;
            }
            parent = parent.parentElement;
          }
        }
        
        console.debug('IframeHoverBridge: Detected element', elementInfo.type, elementInfo.id, 'sending to parent');
        window.parent.postMessage({
          type: 'previewHover:elementDetected',
          data: {
            type: elementInfo.type,
            id: elementInfo.id,
            rowId: rowId,
            columnId: columnId,
            boundingBox: {
              top: rect.top,
              left: rect.left,
              width: rect.width,
              height: rect.height
            }
          }
        }, '*');
      } else {
        if (this.lastElement) {
          this.lastElement = null;
          console.debug('IframeHoverBridge: No element detected, sending elementLost to parent');
          window.parent.postMessage({ type: 'previewHover:elementLost' }, '*');
        }
      }
    },

    onMouseLeave(event) {
      if (!this.isEnabled) return;
      if (this.lastElement) {
        this.lastElement = null;
        window.parent.postMessage({ type: 'previewHover:elementLost' }, '*');
      }
    }
  };

  // Listen for enable/disable messages from parent
  window.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'previewHover:enable') {
      console.debug('IframeHoverBridge: Received enable message from parent');
      iframeHoverBridge.init();
    } else if (event.data && event.data.type === 'previewHover:disable') {
      console.debug('IframeHoverBridge: Received disable message from parent');
      iframeHoverBridge.disable();
    }
  });

  // Listen for navigation events (popstate/hashchange) and signal parent to reattach
  window.addEventListener('popstate', () => {
    console.debug('IframeHoverBridge: popstate event detected, signaling parent to reattach');
    window.parent.postMessage({ type: 'previewHover:navigationDetected' }, '*');
  });
  
  window.addEventListener('hashchange', () => {
    console.debug('IframeHoverBridge: hashchange event detected, signaling parent to reattach');
    window.parent.postMessage({ type: 'previewHover:navigationDetected' }, '*');
  });

  // Auto-initialize if previewHover is enabled globally
  if (window.parent && window.parent.previewHoverManager && window.parent.previewHoverManager.isHoverEnabled()) {
    console.debug('IframeHoverBridge: Auto-initializing as hover is enabled in parent');
    iframeHoverBridge.init();
  }
})();
