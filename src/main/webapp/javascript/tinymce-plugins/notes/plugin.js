/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Allows users to insert notes (info, warning, tip, note)
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
tinymce.PluginManager.add('notes', function (editor) {

    function getSelectedNote() {
        const node = editor.selection.getNode();
        return editor.dom.getParent(
            node,
            '.confluence-macro-info, .confluence-macro-warning, .confluence-macro-tip, .confluence-macro-note'
        );
    }

    function createNote(type = 'note') {

        let config = {
            info: {
                title: 'Info',
                className: 'confluence-macro-info'
            },
            warning: {
                title: 'Warning',
                className: 'confluence-macro-warning'
            },
            tip: {
                title: 'Tip',
                className: 'confluence-macro-tip'
            },
            note: {
                title: 'Note',
                className: 'confluence-macro-note'
            }
        };

        const { title, className } = config[type] || config.note;

        return `
            <div class="${className}">
                <div class="macro-note-title">${title}</div>
                <p>Type your ${title.toLowerCase()} here...</p>
            </div>
        `;
    }

    function getTypeFromNode(node) {
        if (node.classList.contains('confluence-macro-warning')) return 'warning';
        if (node.classList.contains('confluence-macro-tip')) return 'tip';
        if (node.classList.contains('confluence-macro-info')) return 'info';
        return 'note';
    }

    function insertNote(type) {
        const temp = document.createElement('div');
        temp.innerHTML = createNote(type);
        editor.selection.setNode(temp.firstElementChild);
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
                    type: 'nestedmenuitem',
                    text: 'Insert Note',
                    getSubmenuItems: () => getTypeMenu(insertNote)
                },

                { type: 'separator' },

                // ADD ABOVE
                {
                    type: 'nestedmenuitem',
                    text: 'Add Above',
                    enabled: !!note,
                    getSubmenuItems: () => getTypeMenu(addAbove)
                },

                // ADD BELOW
                {
                    type: 'nestedmenuitem',
                    text: 'Add Below',
                    enabled: !!note,
                    getSubmenuItems: () => getTypeMenu(addBelow)
                },

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
            { type: 'menuitem', text: 'Warning', onAction: () => actionFn('warning') },
            { type: 'menuitem', text: 'Tip', onAction: () => actionFn('tip') },
            { type: 'menuitem', text: 'Note', onAction: () => actionFn('note') }
        ];
    }

    editor.ui.registry.addContextMenu('notesContext', {
        update: (element) => {
            const match = editor.dom.getParent(
                element,
                '.confluence-macro-info, .confluence-macro-warning, .confluence-macro-tip, .confluence-macro-note'
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

});