/**
 * Visual Hierarchy Editor Module
 * Handles hierarchy-focused views: page tree list, page library, and preview/properties integration.
 *
 * @author matt rajkowski
 * @created 06/15/26 04:00 PM
 */

class VisualHierarchyEditor {
  constructor(editorBridge, pageTreeManager, pageLibraryManager) {
    this.editorBridge = editorBridge;
    this.pageTreeManager = pageTreeManager;
    this.pageLibraryManager = pageLibraryManager;
    this.pagesReady = false;
    this.libraryReady = false;
  }

  /**
   * Activate the hierarchy workspace in the middle panel.
   */
  activate() {
    this.ensurePagesReady();
    this.showLibraryView();
    this.ensurePageLibraryReady();
  }

  ensurePagesReady() {
    if (!this.pagesReady) {
      this.pageTreeManager.init();
      this.pagesReady = true;
    }
  }

  ensurePageLibraryReady() {
    if (!this.libraryReady) {
      this.pageLibraryManager.init();
      this.libraryReady = true;
      return;
    }

    this.pageLibraryManager.refresh();
  }

  showLibraryView() {
    const library = document.getElementById('page-library-explorer');
    const editor = document.getElementById('content-editor');
    const calendar = document.getElementById('calendar-view');

    if (editor) editor.style.display = 'none';
    if (calendar) calendar.style.display = 'none';
    if (library) library.style.display = 'flex';
  }
}