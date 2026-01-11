/**
 * Right Panel Tabs Manager
 * Manages the tabbed interface in the right panel (Info, Properties, CSS, XML)
 * 
 * @author matt rajkowski
 * @created 1/10/26 12:00 PM
 */

class RightPanelTabs {
  constructor(editor) {
    this.editor = editor;
    this.currentTab = 'info';
    this.previousNonPropertiesTab = 'info';
    this.tabs = ['info', 'properties', 'css', 'xml'];
    this.dirtyState = { info: false, css: false, xml: false };
    this.tabsNav = null;
    this.tabContents = null;
  }
  
  /**
   * Initialize the right panel tabs
   */
  init() {
    this.tabsNav = document.querySelectorAll('.right-panel-tabs-nav a');
    this.tabContents = document.querySelectorAll('.right-panel-tab-content');
    
    if (!this.tabsNav || this.tabsNav.length === 0) {
      console.warn('RightPanelTabs: No tab navigation elements found');
      return;
    }
    
    // Set up click handlers for tabs
    this.tabsNav.forEach(tab => {
      tab.addEventListener('click', (e) => {
        e.preventDefault();
        const tabName = tab.getAttribute('data-tab');
        if (tabName) {
          this.switchTab(tabName);
        }
      });
    });
    
    // Always start with Info tab when editor opens (don't restore from localStorage)
    // The HTML already has Info tab as active, so we just ensure our state matches
    this.currentTab = 'info';
    this.previousNonPropertiesTab = 'info';
    
    // Listen for page changes to preserve tab state and clear dirty states
    // Requirements 7.1: Tab selection preserved across page changes
    document.addEventListener('pageChanged', (e) => {
      this.handlePageChanged(e);
    });
    
    console.log('RightPanelTabs initialized');
  }
  
  /**
   * Handle page changed event
   * Preserves current tab selection and clears dirty states for the new page
   * @param {CustomEvent} e - The pageChanged event
   */
  handlePageChanged(e) {
    const pageLink = e.detail?.pageLink || e.detail?.link;
    console.log('RightPanelTabs: Page changed to', pageLink);
    
    // Clear dirty states since we're loading a new page
    // The individual tab managers will reload their data
    this.clearDirty();
    
    // Preserve the current tab selection (Requirements 7.1)
    // The current tab is already stored in this.currentTab
    // We don't need to switch tabs - just ensure the current tab stays selected
    
    // If we're on the properties tab and no element is selected,
    // switch back to the previous non-properties tab
    if (this.currentTab === 'properties') {
      // After page change, no element will be selected, so restore previous tab
      this.restorePreviousTab();
    }
    
    // Dispatch event to notify that tabs are ready for the new page
    document.dispatchEvent(new CustomEvent('rightPanelTabsPageChanged', {
      detail: { 
        pageLink: pageLink,
        currentTab: this.currentTab 
      }
    }));
  }
  
  /**
   * Switch to a specific tab
   * @param {string} tabName - The name of the tab to switch to
   */
  switchTab(tabName) {
    if (!this.tabs.includes(tabName)) {
      console.warn(`RightPanelTabs: Invalid tab name "${tabName}"`);
      return;
    }
    
    // Store previous non-properties tab for restoration
    if (this.currentTab !== 'properties' && tabName === 'properties') {
      this.previousNonPropertiesTab = this.currentTab;
    }
    
    // Update current tab
    this.currentTab = tabName;
    
    // Update tab navigation active state
    this.tabsNav.forEach(tab => {
      const isActive = tab.getAttribute('data-tab') === tabName;
      tab.classList.toggle('active', isActive);
    });
    
    // Update tab content visibility
    this.tabContents.forEach(content => {
      const isActive = content.getAttribute('data-tab') === tabName;
      content.classList.toggle('active', isActive);
    });
    
    // Note: We don't save tab preference to localStorage anymore
    // The editor always starts with the Info tab when opened
    
    // Dispatch event for other components to listen to
    document.dispatchEvent(new CustomEvent('rightPanelTabChanged', {
      detail: { tab: tabName, previousTab: this.previousNonPropertiesTab }
    }));
  }
  
  /**
   * Get the current active tab name
   * @returns {string} The current tab name
   */
  getCurrentTab() {
    return this.currentTab;
  }
  
  /**
   * Get the previous non-properties tab name
   * @returns {string} The previous non-properties tab name
   */
  getPreviousNonPropertiesTab() {
    return this.previousNonPropertiesTab;
  }
  
  /**
   * Restore to the previous non-properties tab
   * Used when deselecting an element in the canvas
   */
  restorePreviousTab() {
    if (this.currentTab === 'properties') {
      this.switchTab(this.previousNonPropertiesTab);
    }
  }
  
  /**
   * Mark a tab as having unsaved changes
   * @param {string} tabName - The tab to mark as dirty
   */
  markDirty(tabName) {
    if (tabName in this.dirtyState) {
      this.dirtyState[tabName] = true;
      this.updateDirtyIndicator(tabName);
    }
  }
  
  /**
   * Clear dirty state for a specific tab
   * @param {string} tabName - The tab to clear dirty state for
   */
  clearDirtyForTab(tabName) {
    if (tabName in this.dirtyState) {
      this.dirtyState[tabName] = false;
      this.updateDirtyIndicator(tabName);
    }
  }
  
  /**
   * Check if any tab has unsaved changes
   * @returns {boolean} True if any tab is dirty
   */
  isDirty() {
    return Object.values(this.dirtyState).some(dirty => dirty);
  }
  
  /**
   * Get array of dirty tab names
   * @returns {string[]} Array of tab names that have unsaved changes
   */
  getDirtyTabs() {
    return Object.entries(this.dirtyState)
      .filter(([_, dirty]) => dirty)
      .map(([tabName, _]) => tabName);
  }
  
  /**
   * Clear all dirty states
   */
  clearDirty() {
    Object.keys(this.dirtyState).forEach(tabName => {
      this.dirtyState[tabName] = false;
      this.updateDirtyIndicator(tabName);
    });
  }
  
  /**
   * Update the visual dirty indicator for a tab
   * @param {string} tabName - The tab to update indicator for
   */
  updateDirtyIndicator(tabName) {
    const tabLink = document.querySelector(`.right-panel-tabs-nav a[data-tab="${tabName}"]`);
    if (tabLink) {
      const isDirty = this.dirtyState[tabName];
      // Add or remove a visual indicator (asterisk or dot)
      const indicator = tabLink.querySelector('.dirty-indicator');
      if (isDirty && !indicator) {
        const span = document.createElement('span');
        span.className = 'dirty-indicator';
        span.textContent = ' •';
        span.style.color = '#dc3545';
        tabLink.appendChild(span);
      } else if (!isDirty && indicator) {
        indicator.remove();
      }
    }
  }
  
  /**
   * Check if a specific tab is dirty
   * @param {string} tabName - The tab to check
   * @returns {boolean} True if the tab is dirty
   */
  isTabDirty(tabName) {
    return this.dirtyState[tabName] || false;
  }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = RightPanelTabs;
}
