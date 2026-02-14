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
  let contentListManager;
  let pageTreeManager;
  let pageLibraryManager;
  let sitemapExplorer;
  let hubContentManager;
  let contentEditorBridge;
  let previewManager;
  let pageContentBlocksManager;
  let pagesReady = false;
  let contentReady = false;
  let hubReady = false;
  let libraryReady = false;

  /**
   * Initialize the visual content editor
   */
  function initializeEditor() {
    // Create bridge first since others depend on it
    contentEditorBridge = new ContentEditorBridge();

    // Create other managers
    contentListManager = new ContentListManager(contentEditorBridge);
    pageTreeManager = new PageTreeManager(contentEditorBridge);
    pageLibraryManager = new PageLibraryManager(contentEditorBridge);
    sitemapExplorer = new SitemapExplorer(contentEditorBridge);
    hubContentManager = new HubContentManager(contentEditorBridge);
    previewManager = new PreviewManager(contentEditorBridge);
    pageContentBlocksManager = new PageContentBlocksManager(contentEditorBridge);

    // Wire pageContentBlocksManager to the bridge
    contentEditorBridge.setPageContentBlocksManager(pageContentBlocksManager);

    // Initialize core managers
    contentEditorBridge.init();
    previewManager.init();
    pageContentBlocksManager.init();

    // Setup event listeners for tab switching
    setupTabListeners();

    // Setup modal listeners
    setupModalListeners();

    // Setup toolbar listeners
    setupToolbarListeners();

    // Setup dark mode
    setupDarkMode();

    // Setup Pages view toggle buttons
    setupPagesViewToggle();

    // Setup right panel resize
    setupRightPanelResize();

    // Activate the default tab (Pages)
    activateTab('pages-tab');

    console.log('Visual Content Editor initialized');
  }

  /**
   * Set up Pages tab view toggle (Site Navigation vs Page Library)
   */
  function setupPagesViewToggle() {
    const navBtn = document.getElementById('view-site-navigation-btn');
    const libBtn = document.getElementById('view-page-library-btn');

    if (navBtn) {
      navBtn.addEventListener('click', () => {
        navBtn.classList.add('active');
        libBtn.classList.remove('active');
        showPagesView('navigation');
      });
    }

    if (libBtn) {
      libBtn.addEventListener('click', () => {
        libBtn.classList.add('active');
        navBtn.classList.remove('active');
        showPagesView('library');
      });
    }
  }

  /**
   * Show specific Pages view (navigation or library)
   */
  function showPagesView(view) {
    const sitemap = document.getElementById('sitemap-explorer');
    const library = document.getElementById('page-library-explorer');
    const editor = document.getElementById('content-editor');
    const calendar = document.getElementById('calendar-view');
    const zoomControls = document.getElementById('sitemap-zoom-controls');
    const middlePanelIcon = document.getElementById('middle-panel-icon');
    const middlePanelTitleText = document.getElementById('middle-panel-title-text');
    const addMenuTabBtn = document.getElementById('add-menu-tab-btn');
    const refreshSitemapBtn = document.getElementById('refresh-sitemap-btn');
    const addRootPageBtn = document.getElementById('add-root-page-btn');

    if (editor) editor.style.display = 'none';
    if (calendar) calendar.style.display = 'none';

    if (view === 'navigation') {
      if (sitemap) sitemap.style.display = 'block';
      if (library) library.style.display = 'none';
      if (zoomControls) zoomControls.style.display = 'flex';
      if (middlePanelIcon) middlePanelIcon.className = 'far fa-sitemap';
      if (middlePanelTitleText) middlePanelTitleText.textContent = 'Site Navigation';
      if (addMenuTabBtn) addMenuTabBtn.style.display = 'inline-flex';
      if (refreshSitemapBtn) refreshSitemapBtn.style.display = 'inline-flex';
      if (addRootPageBtn) addRootPageBtn.style.display = 'none';
      // Ensure sitemap is initialized and query/refresh the data
      ensurePagesReady();
      if (sitemapExplorer) {
        sitemapExplorer.refresh();
      }
    } else if (view === 'library') {
      if (sitemap) sitemap.style.display = 'none';
      if (library) library.style.display = 'flex';
      if (zoomControls) zoomControls.style.display = 'none';
      if (middlePanelIcon) middlePanelIcon.className = 'far fa-folder-open';
      if (middlePanelTitleText) middlePanelTitleText.textContent = 'Page Library';
      if (addMenuTabBtn) addMenuTabBtn.style.display = 'none';
      if (refreshSitemapBtn) refreshSitemapBtn.style.display = 'none';
      if (addRootPageBtn) addRootPageBtn.style.display = 'inline-flex';
      ensurePageLibraryReady();
    }
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
   * Set up modal listeners
   */
  function setupModalListeners() {
    const newPageBtn = document.getElementById('new-page-btn');
    const newContentBtn = document.getElementById('new-content-btn');
    const newBlogPostBtn = document.getElementById('new-blog-post-btn');

    const newPageModal = document.getElementById('new-page-modal');
    const newContentModal = document.getElementById('new-content-modal');
    const newBlogPostModal = document.getElementById('new-blog-post-modal');

    // New Page Modal
    if (newPageBtn && newPageModal) {
      newPageBtn.addEventListener('click', () => {
        newPageModal.style.display = 'block';
        document.getElementById('page-title').focus();
      });

      document.getElementById('cancel-new-page').addEventListener('click', () => {
        newPageModal.style.display = 'none';
      });

      document.getElementById('new-page-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const title = document.getElementById('page-title').value;
        const link = document.getElementById('page-link').value;

        if (!link.startsWith('/')) {
          document.getElementById('page-link-error').style.display = 'block';
          return;
        }

        showNotification('info', 'Page Creation', 'Create pages in the Visual Page Editor for now.');
        newPageModal.style.display = 'none';
      });
    }

    // New Content Modal
    if (newContentBtn && newContentModal) {
      newContentBtn.addEventListener('click', () => {
        newContentModal.style.display = 'block';
        document.getElementById('content-unique-id').focus();
      });

      document.getElementById('cancel-new-content').addEventListener('click', () => {
        newContentModal.style.display = 'none';
      });

      document.getElementById('new-content-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const uniqueId = document.getElementById('content-unique-id').value;
        const initialText = document.getElementById('content-initial-text').value;

        const params = new FormData();
        params.append('uniqueId', uniqueId);
        params.append('content', initialText || '<p></p>');
        params.append('token', window.getFormToken());

        fetch('/json/content/save-draft', {
          method: 'POST',
          headers: {
            'Accept': 'application/json'
          },
          body: params
        })
          .then(response => response.json())
          .then(data => {
            if (data.status === 'ok' && data.data) {
              showNotification('success', 'Content Created', 'Draft content created successfully.');
              newContentModal.style.display = 'none';
              ensureContentReady();
              contentListManager.refresh();
              contentEditorBridge.loadContent(data.data.id, data.data.unique_id);
              activateTab('content-tab');
            } else {
              showNotification('error', 'Create Failed', data.error || 'Failed to create content');
            }
          })
          .catch(error => {
            showNotification('error', 'Create Failed', 'Error creating content: ' + error.message);
          });
      });
    }

    // New Blog Post Modal
    if (newBlogPostBtn && newBlogPostModal) {
      newBlogPostBtn.addEventListener('click', () => {
        // Populate blog selection
        const blogSelect = document.getElementById('blog-selection');
        if (hubContentManager && hubContentManager.blogs) {
          blogSelect.innerHTML = '<option value="">Select a blog...</option>';
          hubContentManager.blogs.forEach(blog => {
            const option = document.createElement('option');
            option.value = blog.id;
            option.textContent = blog.title;
            blogSelect.appendChild(option);
          });
        }

        newBlogPostModal.style.display = 'block';
        blogSelect.focus();
      });

      document.getElementById('cancel-new-blog-post').addEventListener('click', () => {
        newBlogPostModal.style.display = 'none';
      });

      document.getElementById('new-blog-post-form').addEventListener('submit', (e) => {
        e.preventDefault();
        const blogId = document.getElementById('blog-selection').value;
        const title = document.getElementById('blog-post-title').value;
        const content = document.getElementById('blog-post-content').value;

        showNotification('info', 'Blog Posts', 'Use the Blog Editor to create new posts.');
        newBlogPostModal.style.display = 'none';
      });
    }

    // Close modals on outside click
    document.addEventListener('click', (e) => {
      if (e.target === newPageModal) newPageModal.style.display = 'none';
      if (e.target === newContentModal) newContentModal.style.display = 'none';
      if (e.target === newBlogPostModal) newBlogPostModal.style.display = 'none';
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
    }
  }

  /**
   * Toggle dark mode
   */
  function toggleDarkMode() {
    const html = document.documentElement;
    const isDark = html.getAttribute('data-theme') === 'dark';

    if (isDark) {
      html.removeAttribute('data-theme');
      localStorage.setItem('editor-theme', 'light');
    } else {
      html.setAttribute('data-theme', 'dark');
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
    const contentTools = document.getElementById('content-tools');
    const hubTools = document.getElementById('hub-tools');

    // Hide all tools first
    if (pagesTools) pagesTools.style.display = 'none';
    if (contentTools) contentTools.style.display = 'none';
    if (hubTools) hubTools.style.display = 'none';

    if (tabId === 'pages-tab') {
      if (middlePanelIcon) middlePanelIcon.className = 'far fa-sitemap';
      if (middlePanelTitleText) middlePanelTitleText.textContent = 'Site Navigation';
      if (pagesTools) pagesTools.style.display = 'flex';
      ensurePagesReady();
      // Reset button group to Pages Library
      const navBtn = document.getElementById('view-site-navigation-btn');
      const libBtn = document.getElementById('view-page-library-btn');
      if (navBtn) libBtn.classList.add('active');
      if (libBtn) navBtn.classList.remove('active');
      showPagesView('library');
    } else if (tabId === 'content-tab') {
      if (middlePanelIcon) middlePanelIcon.className = 'far fa-edit';
      if (middlePanelTitleText) middlePanelTitleText.textContent = 'Edit Content';
      if (contentTools) contentTools.style.display = 'flex';
      ensureContentReady();
      setMiddlePanelView('content');
    } else if (tabId === 'hub-tab') {
      if (middlePanelIcon) middlePanelIcon.className = 'far fa-calendar';
      if (middlePanelTitleText) middlePanelTitleText.textContent = 'Hub';
      if (hubTools) hubTools.style.display = 'flex';
      ensureHubReady();
      setMiddlePanelView('calendar');
      // Show calendar if there's an active hubType
      if (hubContentManager && hubContentManager.currentHubType) {
        const calendar = document.getElementById('calendar-view');
        if (calendar) calendar.style.display = 'block';
      }
    }
  }

  function ensurePagesReady() {
    if (!pagesReady) {
      pageTreeManager.init();
      sitemapExplorer.init();
      pagesReady = true;
    }
  }

  function ensurePageLibraryReady() {
    if (!libraryReady) {
      pageLibraryManager.init();
      libraryReady = true;
      return;
    }

    pageLibraryManager.refresh();
  }

  function ensureContentReady() {
    if (!contentReady) {
      contentListManager.init();
      contentReady = true;
    }
  }

  function ensureHubReady() {
    if (!hubReady) {
      hubContentManager.init();
      hubReady = true;
    }
    hubContentManager.refresh();
  }

  function setMiddlePanelView(view) {
    const sitemap = document.getElementById('sitemap-explorer');
    const library = document.getElementById('page-library-explorer');
    const editor = document.getElementById('content-editor');
    const calendar = document.getElementById('calendar-view');
    const zoomControls = document.getElementById('sitemap-zoom-controls');

    if (sitemap) sitemap.style.display = view === 'sitemap' ? 'block' : 'none';
    if (library) library.style.display = 'none'; // Always hide page library when in other tabs
    if (calendar) calendar.style.display = 'none'; // Always hide calendar unless a hub item is selected    
    if (zoomControls) zoomControls.style.display = view === 'sitemap' || view === 'navigation' ? 'flex' : 'none';
    if (editor) {
      if (view === 'content') {
        editor.style.display = 'block';
      } else {
        editor.style.display = 'none';
      }
    }
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
    contentListManager,
    pageTreeManager,
    pageLibraryManager,
    sitemapExplorer,
    hubContentManager,
    contentEditorBridge,
    previewManager,
    pageContentBlocksManager
  };
})();
