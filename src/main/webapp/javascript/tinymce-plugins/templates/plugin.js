/**
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 *
 * Allows users to insert predefined templates
 * 
 * @author matt rajkowski
 * @created 7/24/26 8:00 AM
 */
tinymce.PluginManager.add('templates', function (editor) {

    // Template files will be fetched dynamically
    let templates = [];
    let filteredTemplates = [];

    // Fetch template list from backend
    function loadTemplateList() {
        return fetch('/json/templateList')
            .then(res => {
                if (!res.ok) throw new Error("Failed to load template list");
                return res.json();
            })
            .then(data => {
                if (data && data.templates) {
                    templates = data.templates;
                    filteredTemplates = [...templates];
                    return templates;
                }
                throw new Error("Invalid template list response");
            })
            .catch(err => {
                console.error("Template list error:", err);
                alert("Failed to load template list");
                return [];
            });
    }

    // Load template content
    function loadTemplate(fileName) {
        fetch('/json/templateContent?file=' + encodeURIComponent(fileName))
            .then(res => {
                if (!res.ok) throw new Error("Failed to load template");
                return res.json();
            })
            .then(data => {
                if (data && data.content) {
                    // Insert template at cursor position instead of replacing entire content
                    editor.insertContent(data.content);
                } else {
                    throw new Error("Invalid template response");
                }
            })
            .catch(err => {
                console.error(err);
                alert("Template load failed");
            });
    }

    // Filter templates based on search input
    function filterTemplates(searchTerm) {
        const lowerSearch = searchTerm.toLowerCase();
        filteredTemplates = templates.filter(t => 
            t.displayName.toLowerCase().includes(lowerSearch) ||
            t.fileName.toLowerCase().includes(lowerSearch)
        );
        return filteredTemplates;
    }

    // Create template list HTML
    function createTemplateListHtml(templateList) {
        if (!templateList || templateList.length === 0) {
            return '<div style="padding:12px;color:#666;text-align:center;">No templates found</div>';
        }

        return templateList.map(t => `
            <div class="template-item" 
                 data-filename="${t.fileName}"
                 style="padding:12px;border-bottom:1px solid #e0e0e0;cursor:pointer;transition:background-color 0.2s;"
                 onmouseover="this.style.backgroundColor='#f5f5f5'"
                 onmouseout="this.style.backgroundColor='transparent'">
                <div style="font-weight:500;color:#333;">${t.displayName}</div>
                <div style="font-size:11px;color:#888;margin-top:2px;">${t.fileName}</div>
            </div>
        `).join('');
    }

    // Open modal dialog
    function openTemplateDialog() {
        // Load templates before opening dialog
        loadTemplateList().then(() => {
            const dialogApi = editor.windowManager.open({
                title: 'Select Template',
                size: 'normal',
                body: {
                    type: 'panel',
                    items: [
                        {
                            type: 'htmlpanel',
                            html: `
                                <div style="margin-bottom:10px;">
                                    <input type="text" 
                                           id="template-search-input" 
                                           placeholder="Search templates..." 
                                           style="width:100%;padding:8px;border:1px solid #ccc;border-radius:4px;box-sizing:border-box;" />
                                </div>
                                <div id="template-list-container" 
                                     style="max-height:400px;overflow-y:auto;border:1px solid #e0e0e0;border-radius:4px;background:#fff;">
                                    ${createTemplateListHtml(filteredTemplates)}
                                </div>
                            `
                        }
                    ]
                },
                buttons: [
                    { type: 'cancel', text: 'Close' }
                ],
                onAction: function (api, details) {
                    // Not used
                }
            });

            // Attach event listeners after dialog is rendered
            setTimeout(() => {
                const searchInput = document.getElementById('template-search-input');
                const listContainer = document.getElementById('template-list-container');

                if (searchInput) {
                    searchInput.addEventListener('input', function (e) {
                        const searchTerm = e.target.value;
                        const filtered = filterTemplates(searchTerm);
                        listContainer.innerHTML = createTemplateListHtml(filtered);
                        attachTemplateClickHandlers();
                    });

                    // Focus the search input
                    searchInput.focus();
                }

                // Attach click handlers to template items
                function attachTemplateClickHandlers() {
                    const templateItems = document.querySelectorAll('.template-item');
                    templateItems.forEach(item => {
                        item.addEventListener('click', function () {
                            const fileName = this.getAttribute('data-filename');
                            loadTemplate(fileName);
                            dialogApi.close();
                        });
                    });
                }

                attachTemplateClickHandlers();
            }, 100);
        });
    }

    // Toolbar button
    editor.ui.registry.addButton('templatesMenu', {
        text: 'Templates',
        tooltip: 'Insert predefined template',
        onAction: openTemplateDialog
    });

});