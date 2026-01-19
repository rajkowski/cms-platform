/**
 * XML Tab Manager
 * Manages the XML tab content and raw layout data editing in the visual editor
 * Uses ACE editor for XML syntax highlighting and editing
 * Provides bidirectional sync between XML and the visual canvas
 * 
 * @author matt rajkowski
 * @created 1/10/26 12:00 PM
 */

class XMLTabManager {
  constructor(editor, rightPanelTabs) {
    this.editor = editor;
    this.rightPanelTabs = rightPanelTabs;
    this.aceEditor = null;
    this.container = null;
    this.isInitialized = false;
    
    // Bidirectional sync flags to prevent infinite loops
    this.isUpdatingFromCanvas = false;
    this.isUpdatingFromXML = false;
    
    // Error state
    this.hasParseError = false;
    this.lastValidXML = '';
    
    // Debounce timer for XML changes
    this.syncDebounceTimer = null;
    this.syncDebounceDelay = 500; // ms
    
    // Track if we're on the XML tab
    this.isActive = false;
  }
  
  /**
   * Initialize the XML tab manager
   */
  init() {
    this.container = document.getElementById('xml-tab-content');
    if (!this.container) {
      console.warn('XMLTabManager: Container element not found');
      return;
    }
    
    // Create the editor container HTML
    this.container.innerHTML = `
      <div id="xml-editor-wrapper" style="display: flex; flex-direction: column; height: 100%;">
        <div id="xml-editor-container" style="flex: 1; min-height: 200px; border: 1px solid var(--editor-border); border-radius: 4px;"></div>
        <div id="xml-editor-error" style="display: none; padding: 8px; font-size: 12px; color: #dc3545; background: rgba(220, 53, 69, 0.1); border: 1px solid rgba(220, 53, 69, 0.3); border-radius: 4px; margin-top: 8px;">
          <i class="fa fa-exclamation-triangle"></i> <span id="xml-error-text"></span>
        </div>
        <div id="xml-editor-status" style="padding: 8px; font-size: 12px; color: var(--editor-text-muted); border-top: 1px solid var(--editor-border);">
          <span id="xml-status-text">XML view of the current page layout</span>
        </div>
      </div>
    `;
    
    // Initialize ACE editor
    this.initAceEditor();
    
    // Listen for tab changes
    document.addEventListener('rightPanelTabChanged', (e) => {
      const isXmlTab = e.detail?.tab === 'xml';
      this.isActive = isXmlTab;
      
      if (isXmlTab && this.aceEditor) {
        // Resize editor when tab becomes visible
        setTimeout(() => {
          this.resize();
          // Sync from canvas when switching to XML tab
          this.updateFromCanvas();
        }, 100);
      }
    });
    
    // Listen for page changes (Requirements 5.2, 7.1)
    document.addEventListener('pageChanged', (e) => {
      // Clear error state on page change
      this.clearError();
      this.hasParseError = false;
      
      // Clear previous XML data
      this.clearForPageChange();
      
      // Update XML from canvas after a short delay to allow layout to load
      setTimeout(() => {
        this.updateFromCanvas();
      }, 200);
    });
    
    // Listen for theme changes
    const editorWrapper = document.getElementById('visual-page-editor-wrapper');
    if (editorWrapper) {
      const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.attributeName === 'data-theme') {
            this.updateEditorTheme();
          }
        });
      });
      observer.observe(editorWrapper, { attributes: true });
    }
    
    // Listen for layout changes from the canvas
    document.addEventListener('layoutChanged', (e) => {
      // Only update if the change came from the canvas (not from XML tab)
      if (e.detail?.source === 'canvas' && !this.isUpdatingFromXML) {
        // Update XML tab if it's currently active
        if (this.isActive) {
          this.updateFromCanvas();
        }
      }
    });
    
    // Listen for window resize to adjust editor height
    this.resizeHandler = () => {
      if (this.aceEditor) {
        this.resize()
      }
    };
    window.addEventListener('resize', this.resizeHandler);
    
    console.log('XMLTabManager initialized');
  }
  
  /**
   * Initialize the ACE editor with XML mode
   */
  initAceEditor() {
    const editorContainer = document.getElementById('xml-editor-container');
    if (!editorContainer) {
      console.warn('XMLTabManager: Editor container not found');
      return;
    }
    
    // Check if ACE is available
    if (typeof ace === 'undefined') {
      console.warn('XMLTabManager: ACE editor not loaded');
      editorContainer.innerHTML = '<p style="padding: 15px; color: var(--editor-text-muted);">XML editor not available</p>';
      return;
    }
    
    // Create ACE editor instance
    this.aceEditor = ace.edit(editorContainer);
    
    // Configure editor
    this.aceEditor.setOptions({
      mode: 'ace/mode/xml',
      showLineNumbers: true,
      showGutter: true,
      highlightActiveLine: true,
      showPrintMargin: false,
      tabSize: 2,
      useSoftTabs: true,
      wrap: true,
      fontSize: '13px',
      fontFamily: 'Monaco, Menlo, "Ubuntu Mono", Consolas, source-code-pro, monospace'
    });
    
    // Set initial theme based on current mode
    this.updateEditorTheme();
    
    // Set up change listener for bidirectional sync
    this.aceEditor.on('change', () => {
      if (this.isInitialized && !this.isUpdatingFromCanvas) {
        // Debounce the sync to canvas
        this.debouncedApplyToCanvas();
      }
    });
    
    // Set placeholder text
    this.aceEditor.setValue('', -1);
    
    this.isInitialized = true;
  }
  
  /**
   * Update the ACE editor theme based on the current dark/light mode
   */
  updateEditorTheme() {
    if (!this.aceEditor) return;
    
    const editorWrapper = document.getElementById('visual-page-editor-wrapper');
    const isDarkMode = editorWrapper?.getAttribute('data-theme') === 'dark';
    
    if (isDarkMode) {
      this.aceEditor.setTheme('ace/theme/monokai');
    } else {
      this.aceEditor.setTheme('ace/theme/chrome');
    }
  }

  /**
   * Update XML editor content from the canvas/LayoutManager
   * Called when the visual editor changes
   */
  updateFromCanvas() {
    if (!this.aceEditor || !this.editor) {
      return;
    }
    
    // Prevent recursive updates
    if (this.isUpdatingFromXML) {
      return;
    }
    
    this.isUpdatingFromCanvas = true;
    
    try {
      const layoutManager = this.editor.getLayoutManager();
      if (!layoutManager) {
        this.aceEditor.setValue('<!-- No layout manager available -->\n', -1);
        return;
      }
      
      // Get XML from layout manager
      const xml = layoutManager.toXML();
      
      // Store as last valid XML
      this.lastValidXML = xml;
      
      // Update the editor
      const currentValue = this.aceEditor.getValue();
      if (currentValue !== xml) {
        // Preserve cursor position if possible
        const cursorPosition = this.aceEditor.getCursorPosition();
        this.aceEditor.setValue(xml, -1);
        
        // Try to restore cursor position
        try {
          this.aceEditor.moveCursorToPosition(cursorPosition);
        } catch (e) {
          // If position is invalid, move to start
          this.aceEditor.moveCursorTo(0, 0);
        }
      }
      
      // Clear any previous errors
      this.clearError();
      this.hasParseError = false;
      
      // Update status
      this.updateStatusText('Synced from canvas');
      
    } catch (error) {
      console.error('Error updating XML from canvas:', error);
      this.showError('Error generating XML: ' + error.message);
    } finally {
      this.isUpdatingFromCanvas = false;
    }
  }
  
  /**
   * Debounced version of applyToCanvas to prevent excessive updates
   */
  debouncedApplyToCanvas() {
    // Clear any existing timer
    if (this.syncDebounceTimer) {
      clearTimeout(this.syncDebounceTimer);
    }
    
    // Set new timer
    this.syncDebounceTimer = setTimeout(() => {
      this.applyToCanvas();
    }, this.syncDebounceDelay);
  }
  
  /**
   * Apply XML changes to the canvas/LayoutManager
   * Called when the XML editor content changes
   */
  applyToCanvas() {
    if (!this.aceEditor || !this.editor) {
      return;
    }
    
    // Prevent recursive updates
    if (this.isUpdatingFromCanvas) {
      return;
    }
    
    const xml = this.aceEditor.getValue();
    
    // Skip if empty or placeholder
    if (!xml || xml.trim() === '' || xml.includes('<!-- Select a page')) {
      return;
    }
    
    this.isUpdatingFromXML = true;
    
    try {
      const layoutManager = this.editor.getLayoutManager();
      if (!layoutManager) {
        this.showError('Layout manager not available');
        return;
      }
      
      // Validate XML first
      const validationResult = this.validateXML(xml);
      if (!validationResult.valid) {
        this.showError(validationResult.error);
        this.hasParseError = true;
        // Don't update canvas with invalid XML
        return;
      }
      
      // Parse XML and update layout manager
      layoutManager.fromXML(xml);
      
      // Re-render the canvas
      const canvasController = this.editor.getCanvasController();
      if (canvasController) {
        canvasController.renderLayout(layoutManager.getStructure());
      }
      
      // Update properties panel if an element is selected
      this.updatePropertiesPanel();
      
      // Store as last valid XML
      this.lastValidXML = xml;
      
      // Clear error state
      this.clearError();
      this.hasParseError = false;
      
      // Mark as dirty
      this.rightPanelTabs.markDirty('xml');
      
      // Save to history
      if (this.editor.saveToHistory) {
        this.editor.saveToHistory();
      }
      
      // Update status
      this.updateStatusText('Applied to canvas');
      
    } catch (error) {
      console.error('Error applying XML to canvas:', error);
      this.showError('Error parsing XML: ' + error.message);
      this.hasParseError = true;
    } finally {
      this.isUpdatingFromXML = false;
    }
  }
  
  /**
   * Validate XML string
   * @param {string} xml - The XML string to validate
   * @returns {Object} Validation result with valid boolean and error message
   */
  validateXML(xml) {
    try {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(xml, 'text/xml');
      
      // Check for parsing errors
      const parserError = xmlDoc.getElementsByTagName('parsererror');
      if (parserError.length > 0) {
        // Extract error message
        let errorText = parserError[0].textContent || 'Invalid XML syntax';
        // Clean up the error message
        errorText = errorText.replace(/This page contains the following errors:/, '').trim();
        errorText = errorText.replace(/Below is a rendering of the page up to the first error./, '').trim();
        
        return { valid: false, error: errorText };
      }
      
      // Check for required <page> element
      const pageElement = xmlDoc.getElementsByTagName('page')[0];
      if (!pageElement) {
        return { valid: false, error: 'Missing required <page> root element' };
      }
      
      return { valid: true, error: null };
      
    } catch (error) {
      return { valid: false, error: 'XML validation error: ' + error.message };
    }
  }
  
  /**
   * Update the properties panel when XML changes affect the selected element
   */
  updatePropertiesPanel() {
    const propertiesPanel = this.editor.getPropertiesPanel();
    if (!propertiesPanel || !propertiesPanel.currentContext) {
      return;
    }
    
    // Re-show the properties for the currently selected element
    // This will refresh the panel with updated data from the layout manager
    propertiesPanel.show(propertiesPanel.currentContext);
  }
  
  /**
   * Show an error message
   * @param {string} message - The error message to display
   */
  showError(message) {
    const errorContainer = document.getElementById('xml-editor-error');
    const errorText = document.getElementById('xml-error-text');
    
    if (errorContainer && errorText) {
      errorText.textContent = message;
      errorContainer.style.display = 'block';
    }
    
    this.updateStatusText('Error - see above');
    
    // Also highlight the error in ACE editor if possible
    if (this.aceEditor) {
      // Add error annotation
      this.aceEditor.getSession().setAnnotations([{
        row: 0,
        column: 0,
        text: message,
        type: 'error'
      }]);
    }
  }
  
  /**
   * Clear error display
   */
  clearError() {
    const errorContainer = document.getElementById('xml-editor-error');
    if (errorContainer) {
      errorContainer.style.display = 'none';
    }
    
    // Clear ACE editor annotations
    if (this.aceEditor) {
      this.aceEditor.getSession().clearAnnotations();
    }
  }
  
  /**
   * Update the status text
   * @param {string} text - The status text to display
   */
  updateStatusText(text) {
    const statusElement = document.getElementById('xml-status-text');
    if (statusElement) {
      statusElement.textContent = text;
    }
  }
  
  /**
   * Check if there are unsaved changes
   * Note: XML changes are immediately applied to canvas, so dirty state
   * is tracked through the layout changes, not the XML content itself
   * @returns {boolean} True if there are changes
   */
  hasChanges() {
    // XML tab changes are immediately synced to canvas
    // The dirty state is managed through the layout manager
    return false;
  }
  
  /**
   * Get current XML content
   * @returns {string} The current XML content
   */
  getXML() {
    if (!this.aceEditor) {
      return '';
    }
    return this.aceEditor.getValue();
  }
  
  /**
   * Set XML content
   * @param {string} xml - The XML content to set
   */
  setXML(xml) {
    if (!this.aceEditor) {
      return;
    }
    
    this.isUpdatingFromCanvas = true;
    this.aceEditor.setValue(xml, -1);
    this.aceEditor.clearSelection();
    this.aceEditor.moveCursorTo(0, 0);
    this.lastValidXML = xml;
    this.isUpdatingFromCanvas = false;
  }
  
  /**
   * Check if there's a parse error
   * @returns {boolean} True if there's a parse error
   */
  hasError() {
    return this.hasParseError;
  }
  
  /**
   * Get the last valid XML
   * @returns {string} The last valid XML content
   */
  getLastValidXML() {
    return this.lastValidXML;
  }
  
  /**
   * Revert to last valid XML
   */
  revertToLastValid() {
    if (this.lastValidXML && this.aceEditor) {
      this.setXML(this.lastValidXML);
      this.clearError();
      this.hasParseError = false;
      this.updateStatusText('Reverted to last valid XML');
    }
  }
  
  /**
   * Clear the editor
   */
  clear() {
    this.lastValidXML = '';
    this.hasParseError = false;
    
    if (this.aceEditor) {
      this.isUpdatingFromCanvas = true;
      this.aceEditor.setValue('<!-- Select a page to view its XML layout -->\n', -1);
      this.aceEditor.clearSelection();
      this.isUpdatingFromCanvas = false;
    }
    
    this.clearError();
    this.updateStatusText('XML view of the current page layout');
  }
  
  /**
   * Clear data when switching pages (preserves tab selection)
   * Different from clear() which is for complete reset
   */
  clearForPageChange() {
    this.lastValidXML = '';
    this.hasParseError = false;
    this.clearError();
    // Don't clear the editor content - it will be updated by updateFromCanvas()
  }
  
  /**
   * Focus the editor
   */
  focus() {
    if (this.aceEditor) {
      this.aceEditor.focus();
    }
  }
  
  /**
   * Resize the editor (call when container size changes)
   */
  resize() {
    if (this.aceEditor) {
      let tabContainer = document.getElementById('xml-tab');
      let tabRect = tabContainer.getBoundingClientRect();
      let tabTop = window.pageYOffset || document.documentElement.scrollTop;
      let tabAvailableHeight = window.innerHeight - Math.round(tabRect.top + tabTop);

      let statusContainer = document.getElementById('xml-editor-status');
      let statusRect = statusContainer.getBoundingClientRect();

      let container = document.getElementById('xml-tab-content');
      let newHeight = Math.round(tabAvailableHeight - statusRect.height);
      container.style.height = newHeight + 'px';
      this.aceEditor.resize();
    }
  }
  
  /**
   * Check if the editor is ready
   * @returns {boolean} True if the editor is initialized
   */
  isReady() {
    return this.isInitialized && this.aceEditor !== null;
  }
  
  /**
   * Format/beautify the XML content
   */
  formatXML() {
    if (!this.aceEditor) return;
    
    const xml = this.aceEditor.getValue();
    try {
      // Simple XML formatting
      const formatted = this.beautifyXML(xml);
      this.setXML(formatted);
      this.updateStatusText('XML formatted');
    } catch (error) {
      console.error('Error formatting XML:', error);
    }
  }
  
  /**
   * Simple XML beautifier
   * @param {string} xml - The XML string to beautify
   * @returns {string} Formatted XML string
   */
  beautifyXML(xml) {
    let formatted = '';
    let indent = '';
    const tab = '  ';
    
    xml.split(/>\s*</).forEach(function(node) {
      if (node.match(/^\/\w/)) {
        // Closing tag
        indent = indent.substring(tab.length);
      }
      
      formatted += indent + '<' + node + '>\n';
      
      if (node.match(/^<?\w[^>]*[^\/]$/) && !node.startsWith('?') && !node.startsWith('!')) {
        // Opening tag (not self-closing)
        indent += tab;
      }
    });
    
    // Clean up the result
    return formatted.substring(1, formatted.length - 2);
  }

  /**
   * Cleanup method to remove event listeners
   */
  destroy() {
    if (this.resizeHandler) {
      window.removeEventListener('resize', this.resizeHandler);
    }
  }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = XMLTabManager;
}
