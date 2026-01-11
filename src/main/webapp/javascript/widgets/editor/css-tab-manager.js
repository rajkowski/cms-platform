/**
 * CSS Tab Manager
 * Manages the CSS tab content and page-specific stylesheet editing in the visual editor
 * Uses ACE editor for CSS syntax highlighting and editing
 * 
 * @author Matt Rajkowski
 * @created 01/10/26 12:00 PM
 */

class CSSTabManager {
  constructor(editor, rightPanelTabs) {
    this.editor = editor;
    this.rightPanelTabs = rightPanelTabs;
    this.aceEditor = null;
    this.originalCSS = '';
    this.stylesheetId = -1;
    this.webPageId = -1;
    this.container = null;
    this.isLoading = false;
    this.currentPageLink = null;
    this.isInitialized = false;
  }
  
  /**
   * Initialize the CSS tab manager
   */
  init() {
    this.container = document.getElementById('css-tab-content');
    if (!this.container) {
      console.warn('CSSTabManager: Container element not found');
      return;
    }
    
    // Create the editor container HTML
    this.container.innerHTML = `
      <div id="css-editor-wrapper" style="display: flex; flex-direction: column; height: 100%;">
        <div id="css-editor-container" style="flex: 1; min-height: 200px; border: 1px solid var(--editor-border); border-radius: 4px;"></div>
        <div id="css-editor-status" style="padding: 8px; font-size: 12px; color: var(--editor-text-muted); border-top: 1px solid var(--editor-border);">
          <span id="css-status-text">Select a page to edit its CSS</span>
        </div>
      </div>
    `;
    
    // Initialize ACE editor
    this.initAceEditor();
    
    // Listen for page changes (Requirements 4.5, 7.1)
    document.addEventListener('pageChanged', (e) => {
      const pageLink = e.detail?.pageLink || e.detail?.link;
      if (pageLink) {
        // Clear previous data before loading new page
        this.clearForPageChange();
        this.loadStylesheet(pageLink);
      }
    });
    
    // Listen for tab changes to resize editor when CSS tab becomes visible
    document.addEventListener('rightPanelTabChanged', (e) => {
      if (e.detail?.tab === 'css' && this.aceEditor) {
        // Resize editor when tab becomes visible
        setTimeout(() => {
          this.resize();
        }, 100);
      }
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
    
    // Listen for window resize to adjust editor height
    this.resizeHandler = () => {
      if (this.aceEditor) {
        this.resize();
      }
    };
    window.addEventListener('resize', this.resizeHandler);
    
    console.log('CSSTabManager initialized');
  }
  
  /**
   * Clear data when switching pages (preserves tab selection)
   * Different from clear() which is for complete reset
   */
  clearForPageChange() {
    this.originalCSS = '';
    this.stylesheetId = -1;
    this.webPageId = -1;
    // Don't clear currentPageLink yet - it will be set by loadStylesheet
    // Don't clear dirty state here - RightPanelTabs handles that
  }
  
  /**
   * Initialize the ACE editor with CSS mode
   */
  initAceEditor() {
    const editorContainer = document.getElementById('css-editor-container');
    if (!editorContainer) {
      console.warn('CSSTabManager: Editor container not found');
      return;
    }
    
    // Check if ACE is available
    if (typeof ace === 'undefined') {
      console.warn('CSSTabManager: ACE editor not loaded');
      editorContainer.innerHTML = '<p style="padding: 15px; color: var(--editor-text-muted);">CSS editor not available</p>';
      return;
    }
    
    // Create ACE editor instance
    this.aceEditor = ace.edit(editorContainer);
    
    // Configure editor
    this.aceEditor.setOptions({
      mode: 'ace/mode/css',
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
    
    // Set up change listener for dirty state tracking
    this.aceEditor.on('change', () => {
      if (this.isInitialized && this.hasChanges()) {
        this.rightPanelTabs.markDirty('css');
        this.updateStatusText('Modified');
      }
    });
    
    // Set placeholder text
    this.aceEditor.setValue('/* Select a page to edit its CSS */\n', -1);
    this.aceEditor.setReadOnly(true);
    
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
   * Load stylesheet from the API
   * @param {string} webPageLink - The page link to load stylesheet for
   */
  async loadStylesheet(webPageLink) {
    if (!webPageLink || this.isLoading) {
      return;
    }
    
    this.isLoading = true;
    this.currentPageLink = webPageLink;
    
    // Check if this is a new page (not saved yet)
    const isNewPage = this.editor.pagesTabManager?.selectedPageId === 'new';
    
    if (isNewPage) {
      // For new pages, start with empty CSS
      console.log('Loading CSS for new page:', webPageLink);
      
      this.originalCSS = '';
      this.stylesheetId = -1;
      this.webPageId = -1;
      
      if (this.aceEditor) {
        this.aceEditor.setValue('', -1);
        this.aceEditor.setReadOnly(false);
        this.aceEditor.clearSelection();
        this.aceEditor.moveCursorTo(0, 0);
      }
      
      this.updateStatusText('New page - add CSS below');
      this.rightPanelTabs.clearDirtyForTab('css');
      this.isLoading = false;
      return;
    }
    
    // Show loading state for existing pages
    this.updateStatusText('Loading...');
    if (this.aceEditor) {
      this.aceEditor.setReadOnly(true);
      this.aceEditor.setValue('/* Loading stylesheet... */\n', -1);
    }
    
    try {
      const response = await fetch(`/json/stylesheet?link=${encodeURIComponent(webPageLink)}`);
      const data = await response.json();
      
      if (data.error) {
        this.showError(data.error);
        return;
      }
      
      // Store original data for change detection
      this.originalCSS = data.css || '';
      this.stylesheetId = data.id || -1;
      this.webPageId = data.webPageId || -1;
      
      // Update the editor
      if (this.aceEditor) {
        this.aceEditor.setValue(this.originalCSS, -1);
        this.aceEditor.setReadOnly(false);
        this.aceEditor.clearSelection();
        this.aceEditor.moveCursorTo(0, 0);
      }
      
      // Update status
      if (data.hasStylesheet) {
        this.updateStatusText('Loaded');
      } else {
        this.updateStatusText('No stylesheet - add CSS below');
      }
      
      // Clear dirty state since we just loaded
      this.rightPanelTabs.clearDirtyForTab('css');
      
    } catch (error) {
      console.error('Error loading stylesheet:', error);
      this.showError('Failed to load stylesheet');
    } finally {
      this.isLoading = false;
    }
  }
  
  /**
   * Show an error message
   * @param {string} message - The error message to display
   */
  showError(message) {
    this.updateStatusText('Error: ' + message);
    if (this.aceEditor) {
      this.aceEditor.setValue('/* Error: ' + message + ' */\n', -1);
      this.aceEditor.setReadOnly(true);
    }
  }
  
  /**
   * Update the status text
   * @param {string} text - The status text to display
   */
  updateStatusText(text) {
    const statusElement = document.getElementById('css-status-text');
    if (statusElement) {
      statusElement.textContent = text;
    }
  }
  
  /**
   * Check if there are unsaved changes
   * @returns {boolean} True if there are changes
   */
  hasChanges() {
    if (!this.aceEditor || !this.currentPageLink) {
      return false;
    }
    
    const currentCSS = this.aceEditor.getValue();
    return currentCSS !== this.originalCSS;
  }
  
  /**
   * Get current CSS content
   * @returns {string} The current CSS content
   */
  getCSS() {
    if (!this.aceEditor) {
      return '';
    }
    return this.aceEditor.getValue();
  }
  
  /**
   * Save stylesheet to the API
   * @returns {Promise<Object>} The save result
   */
  async save() {
    if (!this.currentPageLink) {
      return { success: false, message: 'No page selected' };
    }
    
    const css = this.getCSS();
    
    try {
      const formData = new FormData();
      formData.append('token', document.querySelector('input[name="token"]')?.value || '');
      formData.append('link', this.currentPageLink);
      formData.append('css', css);
      
      const response = await fetch('/json/saveStylesheet', {
        method: 'POST',
        body: formData
      });
      
      const result = await response.json();
      
      if (result.success) {
        // Update original CSS to match current (changes saved)
        this.originalCSS = css;
        this.stylesheetId = result.id || this.stylesheetId;
        this.rightPanelTabs.clearDirtyForTab('css');
        this.updateStatusText('Saved');
      }
      
      return result;
      
    } catch (error) {
      console.error('Error saving stylesheet:', error);
      return { success: false, message: 'Failed to save stylesheet: ' + error.message };
    }
  }
  
  /**
   * Reset the editor to original CSS
   */
  reset() {
    if (this.aceEditor && this.originalCSS !== undefined) {
      this.aceEditor.setValue(this.originalCSS, -1);
      this.aceEditor.clearSelection();
      this.rightPanelTabs.clearDirtyForTab('css');
      this.updateStatusText('Reset to saved');
    }
  }
  
  /**
   * Clear the editor
   */
  clear() {
    this.originalCSS = '';
    this.stylesheetId = -1;
    this.webPageId = -1;
    this.currentPageLink = null;
    
    if (this.aceEditor) {
      this.aceEditor.setValue('/* Select a page to edit its CSS */\n', -1);
      this.aceEditor.setReadOnly(true);
      this.aceEditor.clearSelection();
    }
    
    this.updateStatusText('Select a page to edit its CSS');
    this.rightPanelTabs.clearDirtyForTab('css');
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
      let tabContainer = document.getElementById('css-tab');
      let tabRect = tabContainer.getBoundingClientRect();
      let tabTop = window.pageYOffset || document.documentElement.scrollTop;
      let tabAvailableHeight = window.innerHeight - Math.round(tabRect.top + tabTop);

      let statusContainer = document.getElementById('css-editor-status');
      let statusRect = statusContainer.getBoundingClientRect();

      let container = document.getElementById('css-tab-content');
      let newHeight = Math.round(tabAvailableHeight - statusRect.height);
      container.style.height = newHeight + 'px';
      this.aceEditor.resize();
    }
  }
  
  /**
   * Get the current page link
   * @returns {string|null} The current page link
   */
  getCurrentPageLink() {
    return this.currentPageLink;
  }
  
  /**
   * Check if the editor is ready
   * @returns {boolean} True if the editor is initialized
   */
  isReady() {
    return this.isInitialized && this.aceEditor !== null;
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
  module.exports = CSSTabManager;
}
