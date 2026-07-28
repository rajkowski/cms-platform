/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 * 
 * Allows users to insert diagrams from the diagram browser
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
(function () {
  'use strict';

  var global = tinymce.util.Tools.resolve('tinymce.PluginManager');

  /**
   * Register the plugin
   */
  var Plugin = function (editor) {
    var selectedDiagramNode = null;
    
    /**
     * Open diagram browser dialog
     */
    var openDiagramBrowser = function (diagramNode) {
      selectedDiagramNode = diagramNode || null;
      // Use an absolute root-relative endpoint to avoid resolving under /content-editor.
      var cmsURL = '/diagram-browser';
      
      // Open URL dialog
      var instanceApi = editor.windowManager.openUrl({
        title: 'Select a Diagram',
        url: cmsURL,
        width: 850,
        height: 650,
        onMessage: function (dialogApi, details) {
          if (details.mceAction === 'DiagramSelected') {
            insertDiagram(selectedDiagramNode, details.webPath, details.label);
            selectedDiagramNode = null;
            instanceApi.close();
          }
        }
      });
    };

    /**
     * Insert a diagram reference into the editor
     * @param {HTMLElement|null} diagramNode - The selected diagram node being replaced
     * @param {string} webPath - The web path of the diagram file
     * @param {string} label - Optional label/title for the diagram
     */
    var insertDiagram = function (diagramNode, webPath, label) {
      // Create the span element with the diagram reference
      var displayText = label && label.trim() !== '' ? label : webPath;
      var spanHtml = '<span class="drawio-diagram-ref" contenteditable="false" ' +
                     'data-webpath="' + escapeHtml(webPath) + '" ' +
                     'data-label="' + escapeHtml(label || '') + '" ' +
                     'style="background-color: #fff3cd; padding: 2px 6px; border-radius: 3px; ' +
                     'border: 1px solid #ffecb5; display: inline-block; font-family: monospace; font-size: 0.9em;">' +
                     'diagram: ' + escapeHtml(displayText) + '</span>';

      if (diagramNode) {
        editor.dom.setOuterHTML(diagramNode, spanHtml);
        return;
      }

      editor.insertContent(spanHtml + '&nbsp;');
    };

    /**
     * Escape HTML special characters
     */
    var escapeHtml = function (text) {
      if (!text) return '';
      return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    };

    /**
     * Handle deletion/editing of diagram spans
     */
    var setupEditor = function () {
      // Add double-click handler to replace diagram
      editor.on('dblclick', function (e) {
        var target = e.target;
        if (target.classList && target.classList.contains('drawio-diagram-ref')) {
          e.preventDefault();
          openDiagramBrowser(target);
        }
      });

      // Handle backspace/delete on diagram spans
      editor.on('keydown', function (e) {
        if (e.keyCode === 8 || e.keyCode === 46) { // Backspace or Delete
          var selectedNode = editor.selection.getNode();
          if (selectedNode && selectedNode.classList && selectedNode.classList.contains('drawio-diagram-ref')) {
            e.preventDefault();
            selectedNode.remove();
            return false;
          }
        }
      });
    };

    /**
     * Register toolbar button
     */
    editor.ui.registry.addButton('diagram', {
      icon: 'gamma',
      tooltip: 'Insert Diagram',
      onAction: function () {
        openDiagramBrowser(null);
      }
    });

    /**
     * Register menu item
     */
    editor.ui.registry.addMenuItem('diagram', {
      icon: 'gamma',
      text: 'Insert/edit diagram',
      onAction: function () {
        openDiagramBrowser(null);
      }
    });

    /**
     * Initialize editor setup
     */
    editor.on('init', function () {
      setupEditor();
    });

    /**
     * Register command
     */
    editor.addCommand('mceDiagram', function () {
      openDiagramBrowser(null);
    });

    return {
      getMetadata: function () {
        return {
          name: 'Diagram Plugin',
          url: 'https://www.github.com/rajkowski/cms-platform'
        };
      }
    };
  };

  // Register the plugin
  global.add('diagram', Plugin);

})();
