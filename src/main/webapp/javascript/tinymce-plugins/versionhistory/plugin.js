/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Allows users to view and restore content version history
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
(function() {
  'use strict';

  tinymce.PluginManager.add('versionhistory', function(editor, url) {
    
    // Register the toolbar button
    editor.ui.registry.addButton('versionhistory', {
      text: 'Versions',
      icon: 'restore-draft',
      tooltip: 'View content version history',
      onAction: function() {
        openVersionHistoryDialog();
      }
    });

    /**
     * Open the version history dialog
     */
    function openVersionHistoryDialog() {
      // Get content ID from the editor element or data attribute
      var contentElement = editor.getElement();
      var contentId = contentElement.getAttribute('data-content-id');
      var uniqueId = contentElement.getAttribute('data-unique-id');

      if (!contentId || contentId === '-1' || contentId === '') {
        editor.notificationManager.open({
          text: 'Please save the content first before viewing version history',
          type: 'warning',
          timeout: 3000
        });
        return;
      }

      // Show loading state
      editor.setProgressState(true);

      // Fetch version history
      fetchVersionHistory(contentId);
    }

    /**
     * Fetch version history from the server
     */
    function fetchVersionHistory(contentId) {
      fetch('/json/content/versions?contentId=' + contentId, {
        method: 'GET',
        headers: {
          'Accept': 'application/json'
        }
      })
      .then(function(response) {
        editor.setProgressState(false);
        if (!response.ok) {
          // Try to get error details from response
          return response.text().then(function(text) {
            var errorMsg = 'HTTP ' + response.status + ': ';
            try {
              var json = JSON.parse(text);
              errorMsg += json.error && json.error.title ? json.error.title : response.statusText;
            } catch (e) {
              errorMsg += text || response.statusText;
            }
            throw new Error(errorMsg);
          });
        }
        return response.json();
      })
      .then(function(data) {
        // Handle both direct array and wrapped response formats
        var versions = data;
        if (data && data.data) {
          versions = typeof data.data === 'string' ? JSON.parse(data.data) : data.data;
        }
        
        // Show dialog with actual version data (or empty if no versions)
        showVersionHistoryDialog(versions || []);
      })
      .catch(function(error) {
        editor.setProgressState(false);
        console.error('Error loading version history:', error);
        editor.notificationManager.open({
          text: 'Error loading version history: ' + error.message,
          type: 'error',
          timeout: 5000
        });
      });
    }

    /**
     * Show the version history dialog with the list of versions
     */
    function showVersionHistoryDialog(versions) {
      var versionListHtml = '';
      
      // Check if there are no versions
      if (!versions || versions.length === 0) {
        versionListHtml = '<div style="padding: 40px; text-align: center; color: #666;">';
        versionListHtml += '<p style="font-size: 16px; margin: 0;">No content version history available</p>';
        versionListHtml += '<p style="font-size: 14px; margin-top: 10px; color: #999;">Changes will be tracked once you save this content.</p>';
        versionListHtml += '</div>';
      } else {
        // Build HTML for version list
        versionListHtml = '<div style="max-height: 500px; overflow-y: auto;">';
        versionListHtml += '<table style="width: 100%; border-collapse: collapse; font-size: 14px;">';
        versionListHtml += '<thead><tr style="background: #f5f5f5; position: sticky; top: 0;">';
        versionListHtml += '<th style="padding: 10px; text-align: left; border-bottom: 2px solid #ddd; width: 80px;">Version #</th>';
        versionListHtml += '<th style="padding: 10px; text-align: left; border-bottom: 2px solid #ddd; width: 150px;">Created</th>';
        versionListHtml += '<th style="padding: 10px; text-align: left; border-bottom: 2px solid #ddd; width: 150px;">Created By</th>';
        versionListHtml += '<th style="padding: 10px; text-align: left; border-bottom: 2px solid #ddd;">Content Preview</th>';
        versionListHtml += '<th style="padding: 10px; text-align: left; border-bottom: 2px solid #ddd; width: 150px;">Notes</th>';
        versionListHtml += '<th style="padding: 10px; text-align: center; border-bottom: 2px solid #ddd; width: 200px;">Actions</th>';
        versionListHtml += '</tr></thead><tbody>';

        versions.forEach(function(version, index) {
          var rowStyle = index % 2 === 0 ? 'background: #fafafa;' : 'background: white;';
          var contentPreview = getContentPreview(version.content);
          versionListHtml += '<tr style="' + rowStyle + '" data-version-id="' + version.versionId + '">';
          versionListHtml += '<td style="padding: 10px; border-bottom: 1px solid #eee;">#' + version.versionNumber + '</td>';
          versionListHtml += '<td style="padding: 10px; border-bottom: 1px solid #eee;">' + formatDate(version.created) + '</td>';
          versionListHtml += '<td style="padding: 10px; border-bottom: 1px solid #eee;">' + escapeHtml(version.createdByName || 'Unknown') + '</td>';
          versionListHtml += '<td style="padding: 10px; border-bottom: 1px solid #eee; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="' + escapeHtml(contentPreview) + '">' + escapeHtml(contentPreview) + '</td>';
          versionListHtml += '<td style="padding: 10px; border-bottom: 1px solid #eee;">' + escapeHtml(version.notes || '-') + '</td>';
          versionListHtml += '<td style="padding: 10px; border-bottom: 1px solid #eee; text-align: center;">';
          versionListHtml += '<button type="button" class="tox-button tox-button--secondary" style="margin: 0 5px; padding: 4px 12px;" onclick="window.previewVersion(' + version.versionId + ')">Preview</button>';
          versionListHtml += '<button type="button" class="tox-button" style="margin: 0 5px; padding: 4px 12px;" onclick="window.restoreVersion(' + version.versionId + ')">Restore</button>';
          versionListHtml += '</td>';
          versionListHtml += '</tr>';
        });

        versionListHtml += '</tbody></table></div>';
      }

      // Store versions in window for access by button handlers (only if there are versions)
      if (versions && versions.length > 0) {
        window.currentVersions = versions;
        window.currentEditor = editor;
      }

      // Open dialog with version history
      var dialog = editor.windowManager.open({
        title: 'Content Version History',
        size: 'large',
        body: {
          type: 'panel',
          items: [{
            type: 'htmlpanel',
            html: versionListHtml
          }]
        },
        buttons: [
          {
            type: 'cancel',
            text: 'Close'
          }
        ],
        onClose: function() {
          // Clean up
          delete window.currentVersions;
          delete window.currentEditor;
          delete window.previewVersion;
          delete window.restoreVersion;
        }
      });

      // Define global functions for preview and restore (only if there are versions)
      if (versions && versions.length > 0) {
        window.previewVersion = function(versionId) {
        var version = window.currentVersions.find(function(v) { return v.versionId === versionId; });
        if (!version) return;

        var previewHtml = '<div style="max-height: 600px; overflow-y: auto; padding: 15px; border: 1px solid #ddd; background: white;">' + version.content + '</div>';
        
        window.currentEditor.windowManager.open({
          title: 'Preview Content Version #' + version.versionNumber + ' (' + formatDate(version.created) + ')',
          size: 'large',
          body: {
            type: 'panel',
            items: [{
              type: 'htmlpanel',
              html: previewHtml
            }]
          },
          buttons: [
            {
              type: 'custom',
              text: 'Restore This Version',
              name: 'restore',
              primary: true
            },
            {
              type: 'cancel',
              text: 'Close'
            }
          ],
          onAction: function(dialogApi, details) {
            if (details.name === 'restore') {
              dialogApi.close();
              window.restoreVersion(versionId);
            }
          }
        });
      };

      window.restoreVersion = function(versionId) {
        var version = window.currentVersions.find(function(v) { return v.versionId === versionId; });
        if (!version) return;

        // Show confirmation modal instead of alert
        window.currentEditor.windowManager.open({
          title: 'Confirm Restore',
          body: {
            type: 'panel',
            items: [
              {
                type: 'htmlpanel',
                html: '<div style="padding: 20px; text-align: center;">' +
                      '<p style="font-size: 16px; margin-bottom: 15px;">Are you sure you want to restore <strong>Version #' + version.versionNumber + '</strong>?</p>' +
                      '<p style="color: #d9534f; margin-bottom: 10px;">⚠️ This will replace the current content in the editor.</p>' +
                      '<p style="color: #666; font-size: 14px;">You will need to save to make the change permanent.</p>' +
                      '</div>'
              }
            ]
          },
          buttons: [
            {
              type: 'custom',
              text: 'Confirm',
              name: 'confirm',
              primary: true
            },
            {
              type: 'cancel',
              text: 'Cancel'
            }
          ],
          onAction: function(dialogApi, details) {
            if (details.name === 'confirm') {
              dialogApi.close();
              // Restore the version
              window.currentEditor.setContent(version.content);
              window.currentEditor.notificationManager.open({
                text: 'Version #' + version.versionNumber + ' has been restored. Remember to save your changes.',
                type: 'success',
                timeout: 5000
              });
              // Close the version history dialog if it's open
              window.currentEditor.windowManager.close();
            }
          }
        });
      };
      } // End of if (versions && versions.length > 0)
    }

    /**
     * Format date for display
     */
    function formatDate(dateString) {
      if (!dateString) return '-';
      var date = new Date(dateString);
      var options = { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric', 
        hour: '2-digit', 
        minute: '2-digit' 
      };
      return date.toLocaleDateString('en-US', options);
    }

    /**
     * Get a text preview of HTML content
     */
    function getContentPreview(htmlContent) {
      if (!htmlContent) return '-';
      // Strip HTML tags and get plain text
      var div = document.createElement('div');
      div.innerHTML = htmlContent;
      var text = div.textContent || div.innerText || '';
      // Remove extra whitespace and newlines
      text = text.replace(/\s+/g, ' ').trim();
      // Return first 100 characters with ellipsis if longer
      if (text.length > 100) {
        return text.substring(0, 100) + '...';
      }
      return text || '(empty content)';
    }

    /**
     * Escape HTML to prevent XSS
     */
    function escapeHtml(text) {
      if (!text) return '';
      var div = document.createElement('div');
      div.textContent = text;
      return div.innerHTML;
    }

    /**
     * Escape content for JavaScript string
     * Note: Consider using a well-tested sanitization library for production use.
     * Backslashes must be escaped FIRST to prevent breaking subsequent escape sequences.
     */
    function escapeForJs(str) {
      if (!str) return '';
      // Escape backslashes first, then other special characters (order matters!)
      return str.replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/"/g, '\\"').replace(/\n/g, '\\n').replace(/\r/g, '\\r').replace(/\t/g, '\\t');
    }

    return {
      getMetadata: function() {
        return {
          name: 'Version History Plugin',
          url: 'https://www.github.com/rajkowski/cms-platform'
        };
      }
    };
  });
})();
