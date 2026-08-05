/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Allows users to insert notes (info, warning, tip, note)
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
const notesPlugin = (editor) => {

    function getSelectedNote() {
        const node = editor.selection.getNode();
        return editor.dom.getParent(
            node,
            '.confluence-macro-info, .confluence-macro-note, .confluence-macro-tip, .confluence-macro-warning'
        );
    }

    function createNote(type = 'note') {

        let config = {
            info: {
                title: 'Info',
                className: 'confluence-macro-info'
            },
            note: {
                title: 'Note',
                className: 'confluence-macro-note'
            },
            tip: {
                title: 'Tip',
                className: 'confluence-macro-tip'
            },
            warning: {
                title: 'Warning',
                className: 'confluence-macro-warning'
            }
        };

        const { title, className } = config[type] || config.note;

        return `
            <div class="${className}">
                <div class="macro-note-title">${title}</div>
                <p>Type your ${title.toLowerCase()} here</p>
            </div>
        `;
    }

    function getTypeFromNode(node) {
        if (node.classList.contains('confluence-macro-warning')) return 'warning';
        if (node.classList.contains('confluence-macro-tip')) return 'tip';
        if (node.classList.contains('confluence-macro-info')) return 'info';
        return 'note';
    }

    function insertNote() {
        editor.insertContent(createNote('note'));
    }

    function insertInfo() {
        editor.insertContent(createNote('info'));
    }

    function insertTip() {
        editor.insertContent(createNote('tip'));
    }

    function insertWarning() {
        editor.insertContent(createNote('warning'));
    }

    function addAbove(type) {
        const note = getSelectedNote();
        if (!note) return;

        const temp = document.createElement('div');
        temp.innerHTML = createNote(type || getTypeFromNode(note));

        note.parentNode.insertBefore(temp.firstElementChild, note);
    }

    function addBelow(type) {
        const note = getSelectedNote();
        if (!note) return;

        const temp = document.createElement('div');
        temp.innerHTML = createNote(type || getTypeFromNode(note));

        note.parentNode.insertBefore(temp.firstElementChild, note.nextSibling);
    }

    function deleteNote() {
        const note = getSelectedNote();
        if (note) note.remove();
    }

    editor.ui.registry.addMenuButton('notesMenu', {
        text: 'Notes',
        fetch: (callback) => {

            const note = getSelectedNote();

            callback([

                // INSERT
                {
                    type: 'menuitem',
                    text: 'Insert Info',
                    enabled: !note,
                    onAction: insertInfo
                },
                {
                    type: 'menuitem',
                    text: 'Insert Note',
                    enabled: !note,
                    onAction: insertNote
                },
                {
                    type: 'menuitem',
                    text: 'Insert Tip',
                    enabled: !note,
                    onAction: insertTip
                },
                {
                    type: 'menuitem',
                    text: 'Insert Warning',
                    enabled: !note,
                    onAction: insertWarning
                },

                // { type: 'separator' },

                // // ADD ABOVE
                // {
                //     type: 'nestedmenuitem',
                //     text: 'Add Above',
                //     enabled: !!note,
                //     getSubmenuItems: () => getTypeMenu(addAbove)
                // },

                // // ADD BELOW
                // {
                //     type: 'nestedmenuitem',
                //     text: 'Add Below',
                //     enabled: !!note,
                //     getSubmenuItems: () => getTypeMenu(addBelow)
                // },

                { type: 'separator' },

                // DELETE
                {
                    type: 'menuitem',
                    text: 'Delete Note',
                    enabled: !!note,
                    onAction: deleteNote
                }
            ]);
        }
    });

    function getTypeMenu(actionFn) {
        return [
            { type: 'menuitem', text: 'Info', onAction: () => actionFn('info') },
            { type: 'menuitem', text: 'Note', onAction: () => actionFn('note') },
            { type: 'menuitem', text: 'Tip', onAction: () => actionFn('tip') },
            { type: 'menuitem', text: 'Warning', onAction: () => actionFn('warning') }
        ];
    }

    editor.ui.registry.addContextMenu('notesContext', {
        update: (element) => {
            const match = editor.dom.getParent(
                element,
                '.confluence-macro-info, .confluence-macro-note, .confluence-macro-tip, .confluence-macro-warning'
            );
            return match ? 'noteAddAbove noteAddBelow noteDelete' : '';
        }
    });

    editor.ui.registry.addMenuItem('noteAddAbove', {
        text: 'Add Above',
        onAction: () => addAbove()
    });

    editor.ui.registry.addMenuItem('noteAddBelow', {
        text: 'Add Below',
        onAction: () => addBelow()
    });

    editor.ui.registry.addMenuItem('noteDelete', {
        text: 'Delete Note',
        onAction: deleteNote
    });

};

// Auto-register the plugin with HugeRTE/TinyMCE when this module loads
if (typeof hugerte !== 'undefined' && hugerte.PluginManager) {
    hugerte.PluginManager.add('notes', notesPlugin);
} else if (typeof tinymce !== 'undefined' && tinymce.PluginManager) {
    tinymce.PluginManager.add('notes', notesPlugin);
}
