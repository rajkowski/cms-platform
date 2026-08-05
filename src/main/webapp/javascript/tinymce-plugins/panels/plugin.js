/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Allows users to insert panels
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
const panelsPlugin = (editor) => {

    function getSelectedPanel() {
        const node = editor.selection.getNode();
        return editor.dom.getParent(node, '.panel-wrapper, .macrosuite-panel');
    }

    function createPanel() {
        return `
            <div class="panel-wrapper macrosuite-panel landing-page-gettingstarted-panel confluence-toggle">
                <div class="panel-header landing-page-gettingstarted-headersubheader">
                    <div class="landing-page-gettingstarted">
                        <div class="panel-title landing-page-gettingstarted">
                            <h2>Panel Heading</h2>
                        </div>
                        <div class="panel-subtitle landing-page-gettingstarted">
                            <p>Subheading goes here</p>
                        </div>
                        <div>
                            <em>(Click to see additional information)</em>
                        </div>
                    </div>
                </div>
                <div class="panel-divider landing-page-gettingstarted-accentstrip"></div>
                <div class="panel-content confluence-toggle-container">
                    <div class="confluence-toggle-column-size-30">
                        <p>Panel content goes here</p>
                    </div>
                </div>
            </div>
        `
    }

    function insertPanels() {
        editor.insertContent(createPanel());
    }

    function deletePanel() {
        const panel = getSelectedPanel();
        if (!panel) {
            alert('Select a panel first');
            return;
        }
        panel.remove();
    }

    editor.ui.registry.addMenuButton('panelsMenu', {
        text: 'Panels',
        fetch: (callback) => {

            const panel = getSelectedPanel();

            callback([
                {
                    type: 'menuitem',
                    text: 'Insert Panel',
                    onAction: insertPanels
                },
                { type: 'separator' },
                {
                    type: 'menuitem',
                    text: 'Delete Panel',
                    enabled: !!panel,
                    onAction: deletePanel
                }
            ]);
        }
    });

}

// Auto-register the plugin with HugeRTE/TinyMCE when this module loads
if (typeof hugerte !== 'undefined' && hugerte.PluginManager) {
    hugerte.PluginManager.add('panels', panelsPlugin);
} else if (typeof tinymce !== 'undefined' && tinymce.PluginManager) {
    tinymce.PluginManager.add('panels', panelsPlugin);
}
