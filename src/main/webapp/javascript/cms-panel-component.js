/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 */

(function (window, document) {
  'use strict';

  function togglePanel(panel) {
    if (!panel) return;
    const content = panel.querySelector('.panel-content');
    const isHidden = window.getComputedStyle(content).display === 'none';
    content.style.display = isHidden ? 'block' : 'none';
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.macrosuite-panel').forEach(function (panel) {
      const content = panel.querySelector('.panel-content');
      if (content) {
        content.style.display = 'none';
      }
    });
  });

  document.addEventListener('click', function (e) {
    const panel = e.target.closest('.macrosuite-panel');
    if (!panel) return;
    if (
      e.target.closest('.toggle-btn') ||
      e.target.closest('.panel-header')
    ) {
      const content = panel.querySelector('.panel-content');
      if (content && content.contains(e.target)) return;
      const isButton = e.target.closest('.toggle-btn');
      if (isButton) {
        e.preventDefault();
      }
      togglePanel(panel);
    }
  });

  window.CMSPanelComponent = {
    togglePanel: togglePanel
  };
})(window, document);