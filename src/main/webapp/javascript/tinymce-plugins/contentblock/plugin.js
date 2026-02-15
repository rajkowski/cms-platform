/**
 * TinyMCE Content Block Plugin
 * Allows users to insert content block references as ${uniqueId:value}
 * 
 * @author matt rajkowski
 * @created 2/14/26 10:00 PM
 */
(function () {
  'use strict';

  var global = tinymce.util.Tools.resolve('tinymce.PluginManager');

  /**
   * Register the plugin
   */
  var Plugin = function (editor) {
    
    /**
     * Open content browser dialog
     */
    var openContentBrowser = function () {
      var ctx = window.location.pathname.split('/')[1] || '';
      if (ctx) {
        ctx = '/' + ctx;
      }
      // 
      // var cmsURL = ctx + '/content-browser';
      var cmsURL = '/content-browser';
      
      // Open URL dialog
      var instanceApi = editor.windowManager.openUrl({
        title: 'Content Browser',
        url: cmsURL,
        width: 850,
        height: 650,
        onMessage: function (dialogApi, details) {
          if (details.mceAction === 'ContentBlockSelected') {
            insertContentBlock(details.content);
            instanceApi.close();
          }
        }
      });
    };

    /**
     * Insert a content block reference into the editor
     * @param {string} uniqueId - The unique ID of the content block
     */
    var insertContentBlock = function (uniqueId) {
      // Create the span element with the content block reference
      var spanHtml = '<span class="content-block-ref" contenteditable="false" data-uniqueid="' + 
                     uniqueId + '" style="background-color: #e3f2fd; padding: 2px 6px; border-radius: 3px; ' +
                     'border: 1px solid #90caf9; display: inline-block; font-family: monospace; font-size: 0.9em;">' +
                     '${uniqueId:' + uniqueId + '}' +
                     '</span>&nbsp;';
      
      editor.insertContent(spanHtml);
    };

    /**
     * Handle deletion/editing of content blocks
     */
    var setupEditor = function () {
      // Add double-click handler to replace content block
      editor.on('dblclick', function (e) {
        var target = e.target;
        if (target.classList && target.classList.contains('content-block-ref')) {
          e.preventDefault();
          openContentBrowser();
        }
      });

      // Handle backspace/delete on content block spans
      editor.on('keydown', function (e) {
        if (e.keyCode === 8 || e.keyCode === 46) { // Backspace or Delete
          var selectedNode = editor.selection.getNode();
          if (selectedNode && selectedNode.classList && selectedNode.classList.contains('content-block-ref')) {
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
    editor.ui.registry.addButton('contentblock', {
      icon: 'browse',
      tooltip: 'Insert Content Block',
      onAction: function () {
        openContentBrowser();
      }
    });

    /**
     * Register menu item
     */
    editor.ui.registry.addMenuItem('contentblock', {
      icon: 'browse',
      text: 'Insert Content Block',
      onAction: function () {
        openContentBrowser();
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
    editor.addCommand('mceContentBlock', function () {
      openContentBrowser();
    });

    return {
      getMetadata: function () {
        return {
          name: 'Content Block Plugin',
          url: 'https://www.simiscms.com'
        };
      }
    };
  };

  // Register the plugin
  global.add('contentblock', Plugin);

})();
