/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Allows users to insert cards
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
tinymce.PluginManager.add('cards', function (editor) {

    function getSelectedCard() {
        const node = editor.selection.getNode();
        return editor.dom.getParent(node, '.macrosuite-card');
    }

    function getContainer(card) {
        return editor.dom.getParent(card, '.macrosuite-cards-container');
    }

    function createCard() {
        return `
            <div class="macrosuite-card">
                <h3>New Card</h3>
                <p>Type text here</p>
                <p><a href="#">View</a></p>
            </div>
        `;
    }

    function insertCards() {
        const containerHtml = `
            <div class="macrosuite-cards-container">
                <div class="macrosuite-card">
                    <h3>Card 1</h3>
                    <p>Type text here</p>
                    <p><a href="#">View</a></p>
                </div>
                <div class="macrosuite-card">
                    <h3>Card 2</h3>
                    <p>Type text here</p>
                    <p><a href="#">View</a></p>
                </div>
            </div>
        `;

        const temp = document.createElement('div');
        temp.innerHTML = containerHtml;

        editor.selection.setNode(temp.firstElementChild);
    }

    function addLeft() {
        const card = getSelectedCard();
        if (!card) {
            alert('Select a card first');
            return;
        }

        const container = getContainer(card);
        const temp = document.createElement('div');
        temp.innerHTML = createCard();

        container.insertBefore(temp.firstElementChild, card);
    }

    function addRight() {
        const card = getSelectedCard();
        if (!card) {
            alert('Select a card first');
            return;
        }

        const container = getContainer(card);
        const temp = document.createElement('div');
        temp.innerHTML = createCard();

        container.insertBefore(temp.firstElementChild, card.nextSibling);
    }

    function deleteCard() {
        const card = getSelectedCard();
        if (card) card.remove();
    }

    editor.ui.registry.addButton('cardsCreate', {
        text: 'Insert Cards',
        onAction: insertCards
    });

    editor.ui.registry.addMenuButton('cardsMenu', {
        text: 'Cards',
        fetch: (callback) => {

            const card = getSelectedCard();

            callback([
                {
                    type: 'menuitem',
                    text: 'Insert Cards',
                    onAction: insertCards
                },
                { type: 'separator' },
                {
                    type: 'menuitem',
                    text: 'Add Left',
                    enabled: !!card,
                    onAction: addLeft
                },
                {
                    type: 'menuitem',
                    text: 'Add Right',
                    enabled: !!card,
                    onAction: addRight
                },
                {
                    type: 'menuitem',
                    text: 'Delete Card',
                    enabled: !!card,
                    onAction: deleteCard
                }
            ]);
        }
    });

    editor.ui.registry.addMenuItem('cardsAddLeft', {
        text: 'Add Card Left',
        onAction: addLeft
    });

    editor.ui.registry.addMenuItem('cardsAddRight', {
        text: 'Add Card Right',
        onAction: addRight
    });

    editor.ui.registry.addMenuItem('cardsDelete', {
        text: 'Delete Card',
        onAction: deleteCard
    });

    editor.ui.registry.addMenuItem('cardsCreateMenu', {
        text: 'Insert Cards',
        onAction: insertCards
    });

    // Context menu if we right click on card if we wanted to show the menu dropdown options enable this
    // editor.ui.registry.addContextMenu('cardsContext', {
    //     update: (element) => {
    //         if (editor.dom.getParent(element, '.macrosuite-card')) {
    //             return 'cardsAddLeft cardsAddRight cardsDelete';
    //         }
    //         return '';
    //     }
    // });

    // Context menu if we right click on card if we wanted to show the menu dropdown options disable this
    editor.on('cardsContext', (e) => {
        const isCard = editor.dom.getParent(e.target, '.macrosuite-card');

        if (isCard) {
            e.preventDefault();
        }
    });

});