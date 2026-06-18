/**
 * Visual Content Editor - Main Initialization
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

(function () {
  'use strict';

  /**
   * Get the CSRF token from the hidden form
   */
  window.getFormToken = function () {
    const tokenInput = document.querySelector('#editor-form input[name="token"]');
    return tokenInput ? tokenInput.value : '';
  };

  // Global managers
  let pageTreeManager;
  let pageLibraryManager;
  let hierarchyEditor;
  let contentEditorBridge;
  let previewManager;

  /**
   * Initialize the visual content editor
   */
  function initializeEditor() {
    // Create bridge first since others depend on it
    contentEditorBridge = new ContentEditorBridge();

    // Create other managers
    pageTreeManager = new PageTreeManager(contentEditorBridge);
    pageLibraryManager = new PageLibraryManager(contentEditorBridge, pageTreeManager);
    hierarchyEditor = new VisualHierarchyEditor(contentEditorBridge, pageTreeManager, pageLibraryManager);
    previewManager = new PreviewManager(contentEditorBridge);

    // Initialize core managers
    contentEditorBridge.init();
    previewManager.init();

    // Setup event listeners for tab switching
    setupTabListeners();

    // Setup toolbar listeners
    setupToolbarListeners();

    // Setup dark mode
    setupDarkMode();

    // Setup right panel resize
    setupRightPanelResize();

    // Activate the default tab (Pages)
    activateTab('pages-tab');

    console.log('Visual Hierarchy Editor initialized');
  }

  /**
   * Update preview iframe scale based on right panel width
   */
  function updatePreviewScale() {
    const rightPanel = document.getElementById('right-panel');
    const previewIframe = document.getElementById('preview-iframe');
    
    if (!rightPanel || !previewIframe) return;
    
    const rightPanelWidth = rightPanel.offsetWidth;
    
    // If panel is minimized, don't update scale
    if (rightPanelWidth <= 50) return;
    
    // Calculate available width (subtract padding)
    const padding = 40; // 15px on each side
    const availableWidth = rightPanelWidth - padding;
    
    // Preview iframe min-width is 64em, which is approximately 1024px
    const iframeBaseWidth = 1024;
    
    // Calculate scale to fit the iframe within available width
    const scale = Math.min(1, availableWidth / iframeBaseWidth);
    
    // Apply the scale transform
    previewIframe.style.transform = `scale(${scale})`;
    
    // Don't adjust container height - let CSS handle the 50/50 split
    // The preview-content and page-content-blocks are set to 50% height each
  }

  /**
   * Set up right panel resize functionality
   */
  function setupRightPanelResize() {
    const rightPanel = document.getElementById('right-panel');
    const resizeHandle = document.getElementById('right-panel-resize-handle');

    if (!rightPanel || !resizeHandle) return;

    let isResizing = false;
    let startX = 0;
    let startWidth = 0;
    const minWidth = 0; // Allow minimizing completely
    const maxWidth = 800;

    resizeHandle.addEventListener('mousedown', (e) => {
      isResizing = true;
      startX = e.clientX;
      startWidth = rightPanel.offsetWidth;
      resizeHandle.classList.add('resizing');
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
      e.preventDefault();
    });

    document.addEventListener('mousemove', (e) => {
      if (!isResizing) return;

      const deltaX = startX - e.clientX;
      const newWidth = Math.max(minWidth, Math.min(maxWidth, startWidth + deltaX));

      rightPanel.style.width = newWidth + 'px';

      // If minimized completely, hide it
      if (newWidth <= 50) {
        rightPanel.style.width = '0px';
      }

      // Update preview scale dynamically during resize
      updatePreviewScale();
    });

    document.addEventListener('mouseup', () => {
      if (isResizing) {
        isResizing = false;
        resizeHandle.classList.remove('resizing');
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
      }
    });

    // Set initial scale
    updatePreviewScale();

    // Update scale on window resize
    window.addEventListener('resize', updatePreviewScale);
  }

  /**
   * Set up tab switching listeners
   */
  function setupTabListeners() {
    // Only select left panel tabs to avoid conflicts with right panel tabs
    const tabNav = document.querySelectorAll('.left-panel-tabs-container .tabs-nav a');

    tabNav.forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();

        const tabId = link.getAttribute('href').substring(1);
        const tabContent = document.getElementById(tabId);

        if (!tabContent) return;

        activateTab(tabId);
      });
    });

    // Right panel tab switching
    const rightPanelTabs = document.querySelectorAll('.right-panel-tabs-nav a');
    rightPanelTabs.forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();

        const tabId = link.dataset.tab;
        const tabContent = document.querySelector(`.right-panel-tab-content[data-tab="${tabId}"]`);

        if (!tabContent) return;

        // Hide all tabs
        document.querySelectorAll('.right-panel-tab-content').forEach(tab => {
          tab.classList.remove('active');
        });

        // Remove active from all links
        document.querySelectorAll('.right-panel-tabs-nav a').forEach(a => {
          a.classList.remove('active');
        });

        // Show selected tab and mark link as active
        tabContent.classList.add('active');
        link.classList.add('active');

        // Update preview if switching to preview tab
        if (tabId === 'preview') {
          const mode = contentEditorBridge && typeof contentEditorBridge.getPreviewMode === 'function'
            ? contentEditorBridge.getPreviewMode()
            : 'content';
          if (mode === 'page' && contentEditorBridge && typeof contentEditorBridge.refreshPagePreview === 'function') {
            contentEditorBridge.refreshPagePreview();
          } else {
            previewManager.updatePreviewFromEditor();
          }
        }
      });
    });
  }

  /**
   * Set up toolbar listeners
   */
  function setupToolbarListeners() {
    // Setup Editor App Switcher
    const appSwitcher = document.querySelector('.editor-app-switcher');
    if (appSwitcher) {
      const switcherBtn = appSwitcher.querySelector('.editor-app-switcher-btn');
      const dropdown = appSwitcher.querySelector('.editor-app-switcher-dropdown');
      if (switcherBtn && dropdown) {
        let isOpen = false;

        function closeAppSwitcher() {
          if (!isOpen) return;
          isOpen = false;
          appSwitcher.classList.remove('open');
        }

        switcherBtn.addEventListener('click', (e) => {
          e.preventDefault();
          e.stopPropagation();
          isOpen = !isOpen;
          appSwitcher.classList.toggle('open', isOpen);
        });

        document.addEventListener('click', (e) => {
          if (!appSwitcher.contains(e.target)) {
            closeAppSwitcher();
          }
        });

        dropdown.addEventListener('click', (e) => {
          if (e.target.closest('a')) {
            closeAppSwitcher();
          }
        });
      }
    }

    const appsBtn = document.getElementById('apps-btn');
    const appsMenu = document.querySelector('.apps-menu');
    const appsDropdown = document.querySelector('.apps-dropdown');

    if (appsBtn && appsMenu && appsDropdown) {
      appsBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        appsMenu.classList.toggle('open');
      });

      document.addEventListener('click', (e) => {
        if (!e.target.closest('.apps-menu')) {
          appsMenu.classList.remove('open');
        }
      });
    }

    // Toggle right panel visibility
    const toggleRightPanelBtn = document.getElementById('toggle-right-panel-btn');
    const rightPanel = document.getElementById('right-panel');
    if (toggleRightPanelBtn && rightPanel) {
      toggleRightPanelBtn.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const isHidden = rightPanel.style.display === 'none';
        rightPanel.style.display = isHidden ? 'flex' : 'none';
        toggleRightPanelBtn.classList.toggle('active', isHidden);
      });
    }

    // Dark mode toggle
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
    if (darkModeToggle) {
      darkModeToggle.addEventListener('click', (e) => {
        e.preventDefault();
        toggleDarkMode();
      });
    }

    // Prevent exit without confirmation if content is dirty
    window.addEventListener('beforeunload', (e) => {
      if (contentEditorBridge && contentEditorBridge.isDirty) {
        e.preventDefault();
        e.returnValue = '';
      }
    });
  }

  /**
   * Set up dark mode functionality
   */
  function setupDarkMode() {
    const isDarkMode = localStorage.getItem('editor-theme') === 'dark';
    if (isDarkMode) {
      document.documentElement.setAttribute('data-theme', 'dark');
      const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
      if (darkModeToggle) {
        const iconElement = darkModeToggle.querySelector('i');
        if (iconElement) {
          iconElement.classList.replace('fa-moon', 'fa-sun');
        }
      }
    }
  }

  /**
   * Toggle dark mode
   */
  function toggleDarkMode() {
    const html = document.documentElement;
    const isDark = html.getAttribute('data-theme') === 'dark';
    const darkModeToggle = document.getElementById('dark-mode-toggle-menu');
    const iconElement = darkModeToggle ? darkModeToggle.querySelector('i') : null;

    if (isDark) {
      html.removeAttribute('data-theme');
      if (iconElement) {
        iconElement.classList.replace('fa-sun', 'fa-moon');
      }
      localStorage.setItem('editor-theme', 'light');
    } else {
      html.setAttribute('data-theme', 'dark');
      if (iconElement) {
        iconElement.classList.replace('fa-moon', 'fa-sun');
      }
      localStorage.setItem('editor-theme', 'dark');
    }
  }

  function activateTab(tabId) {
    const tabContent = document.getElementById(tabId);
    if (!tabContent) return;

    // Only manage left panel tab content
    document.querySelectorAll('.left-panel-tabs-container .tab-content').forEach(tab => {
      tab.classList.remove('active');
    });

    // Only manage left panel tab navigation
    document.querySelectorAll('.left-panel-tabs-container .tabs-nav a').forEach(a => {
      a.classList.remove('active');
    });

    tabContent.classList.add('active');
    document.querySelector(`.left-panel-tabs-container .tabs-nav a[href="#${tabId}"]`)?.classList.add('active');

    // Update middle panel header and tools
    const middlePanelIcon = document.getElementById('middle-panel-icon');
    const middlePanelTitleText = document.getElementById('middle-panel-title-text');
    const pagesTools = document.getElementById('pages-tools');

    // Hide all tools first
    if (pagesTools) pagesTools.style.display = 'none';

    if (tabId === 'pages-tab') {
      if (pagesTools) pagesTools.style.display = 'flex';
      if (hierarchyEditor) {
        hierarchyEditor.activate();
      }
    }
  }

  function setMiddlePanelView(view) {
    const library = document.getElementById('page-library-explorer');
    if (library) library.style.display = 'none'; // Always hide page library when in other tabs
  }

  function showNotification(type, title, message) {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
      <button class="notification-close" aria-label="Close">&times;</button>
      <div class="notification-title">${escapeHtml(title)}</div>
      <div class="notification-message">${escapeHtml(message)}</div>
    `;

    notification.querySelector('.notification-close').addEventListener('click', () => {
      notification.remove();
    });

    document.body.appendChild(notification);

    setTimeout(() => {
      if (notification.parentElement) {
        notification.remove();
      }
    }, 5000);
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text || '';
    return div.innerHTML;
  }

  /**
   * Wait for document to be ready and initialize
   */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeEditor);
  } else {
    initializeEditor();
  }

  // Expose for debugging
  window.visualContentEditor = {
    pageTreeManager,
    pageLibraryManager,
    hierarchyEditor,
    previewManager
  };
})();
