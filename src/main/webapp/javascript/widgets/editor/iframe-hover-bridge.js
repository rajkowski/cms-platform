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

    init() {
      console.debug('IframeHoverBridge: Initializing');
      document.addEventListener('mousemove', this.onMouseMove.bind(this));
      document.addEventListener('mouseleave', this.onMouseLeave.bind(this));
      this.isEnabled = true;
      console.debug('IframeHoverBridge: Initialized');
    },

    disable() {
      console.debug('IframeHoverBridge: Disabling');
      document.removeEventListener('mousemove', this.onMouseMove.bind(this));
      document.removeEventListener('mouseleave', this.onMouseLeave.bind(this));
      this.isEnabled = false;
      this.lastElement = null;
      // Tell parent to hide outline
      window.parent.postMessage({ type: 'previewHover:elementLost' }, '*');
    },

    onMouseMove(event) {
      if (!this.isEnabled) return;

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
      iframeHoverBridge.init();
    } else if (event.data && event.data.type === 'previewHover:disable') {
      iframeHoverBridge.disable();
    }
  });

  // Auto-initialize if previewHover is enabled globally
  if (window.parent && window.parent.previewHoverManager && window.parent.previewHoverManager.isHoverEnabled()) {
    iframeHoverBridge.init();
  }
})();
