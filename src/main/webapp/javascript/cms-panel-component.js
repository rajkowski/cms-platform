/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 */

(function (window, document) {
  'use strict';

  function onDocumentReady(callback) {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', callback);
      return;
    }
    callback();
  }

  function getPanelContent(panel) {
    if (!panel) return null;
    return panel.querySelector('.panel-content, .confluence-toggle-container');
  }

  function shouldToggleFromTarget(target) {
    if (!target) return false;
    return Boolean(
      target.closest('.toggle-btn') ||
      target.closest('.panel-header') ||
      target.closest('.landing-page-gettingstarted-eFbmvT') ||
      target.closest('.landing-page-gettingstarted')
    );
  }

  function togglePanel(panel) {
    if (!panel) return;
    const content = getPanelContent(panel);
    if (!content) return;
    const isHidden = window.getComputedStyle(content).display === 'none';
    content.style.display = isHidden ? 'block' : 'none';
  }

  // Fallback for legacy Confluence inline click handlers used across onboarding pages.
  // Keep the header panel visible and only toggle details content.
  window.toggleGettingStartedVisibility = window.toggleGettingStartedVisibility || function () {
    try {
      var content = document.getElementById('landing-page-gettingstarted-content');
      var regionalContent = document.getElementById('landing-page-gettingstarted-contentregional');

      // The toggle element is used to determine if the toggle is present on the page,
      // as some pages may have had it stripped by the editor.
      var toggle = document.querySelector('.landing-page-gettingstarted-eFbmvT');

      // This is hidden by CSS so toggle it.
      if (toggle) {
        var accentStrip = document.querySelector('.confluence-toggle .landing-page-gettingstarted-accentstrip');
        var toggleContainer = document.querySelector('.confluence-toggle .confluence-toggle-container');
        if (accentStrip && toggleContainer) {
          var isHidden = accentStrip.style.display === 'none' || accentStrip.style.display === '';
          if (isHidden) {
            accentStrip.style.display = 'block';
            toggleContainer.style.display = 'block';
          } else {
            accentStrip.style.display = 'none';
            toggleContainer.style.display = 'none';
          }
        }
      }

      // If original method content is not present on the page, it may have been stripped by the editor.
      if (!content) {
        return false;
      }

      // Toggle original content too.
      var isContentHidden = content.style.display === 'none' || content.style.display === '';
      if (isContentHidden) {
        content.style.display = 'flex';
        if (regionalContent) {
          regionalContent.style.display = 'flex';
        }
      } else {
        content.style.display = 'none';
        if (regionalContent) {
          regionalContent.style.display = 'none';
        }
      }
    } catch (e) {
      // Keep this fallback resilient: never block page rendering due to toggle errors.
    }
    return false;
  };

  onDocumentReady(function () {
    document.querySelectorAll('.macrosuite-panel').forEach(function (panel) {
      const content = getPanelContent(panel);
      if (content) {
        content.style.display = 'none';
      }
    });

    // If legacy toggle does not have an inline onClick, add one.
    document.querySelectorAll('.landing-page-gettingstarted-eFbmvT').forEach(function (toggle) {
      if (!toggle.getAttribute('onclick')) {
        toggle.setAttribute('onclick', 'toggleGettingStartedVisibility()');
      }
    });
  });

  // Capture-phase listener avoids conflicts with handlers that stop bubbling.
  document.addEventListener('click', function (e) {
    if (e.__cmsPanelToggleHandled) return;
    const panel = e.target.closest('.macrosuite-panel');
    if (!panel) return;
    if (shouldToggleFromTarget(e.target)) {
      const content = getPanelContent(panel);
      if (!content || content.contains(e.target)) return;
      const isButton = e.target.closest('.toggle-btn');
      if (isButton) {
        e.preventDefault();
      }
      e.__cmsPanelToggleHandled = true;
      togglePanel(panel);
    }
  }, true);

  window.CMSPanelComponent = {
    togglePanel: togglePanel
  };
})(window, document);
