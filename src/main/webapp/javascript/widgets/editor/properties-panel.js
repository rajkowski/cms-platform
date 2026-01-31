/**
 * Properties Panel
 * Manages the properties editing panel
 * 
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */

class PropertiesPanel {
  constructor(editor) {
    this.editor = editor;
    this.panel = null;
    this.content = null;
    this.currentContext = null;
    this.rightPanelTabs = null;
  }
  
  /**
   * Initialize properties panel
   */
  init() {
    this.panel = document.getElementById('properties-panel');
    this.content = document.getElementById('properties-content');
    console.log('Properties Panel initialized');
  }
  
  /**
   * Set the RightPanelTabs reference for tab switching
   * @param {RightPanelTabs} rightPanelTabs - The right panel tabs manager
   */
  setRightPanelTabs(rightPanelTabs) {
    this.rightPanelTabs = rightPanelTabs;
  }
  
  /**
   * Show properties for selected element
   * @param {Object} context - Context object with type, rowId, columnId, widgetId
   * @param {boolean} skipPreviewRefresh - If true, don't auto-refresh preview when showing (useful for preview mode selections)
   */
  show(context, skipPreviewRefresh) {
    // Switch to Properties tab when showing element properties
    if (this.rightPanelTabs) {
      this.rightPanelTabs.switchTab('properties');
    }
    
    this.currentContext = context;
    this.skipPreviewRefresh = skipPreviewRefresh || false;
    
    switch (context.type) {
      case 'row':
        this.showRowProperties(context.rowId);
        break;
      case 'column':
        this.showColumnProperties(context.rowId, context.columnId);
        break;
      case 'widget':
        this.showWidgetProperties(context.rowId, context.columnId, context.widgetId);
        break;
      default:
        this.clear();
    }
  }
  
  /**
   * Show row properties
   */
  showRowProperties(rowId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;

    // Highlight the row in the preview if preview is enabled (for preview-layout mode)
    if (typeof globalThis !== 'undefined' && globalThis.previewHoverManager &&
        typeof globalThis.previewHoverManager.selectElementByContext === 'function') {
      const previewStateGroup = document.getElementById('preview-state-group');
      const currentPreviewState = previewStateGroup ? previewStateGroup.dataset.previewState : 'preview';
      if (currentPreviewState !== 'layout') {
        console.debug('PropertiesPanel: Highlighting row in preview');
        globalThis.previewHoverManager.selectElementByContext({
          type: 'row',
          rowId
        });
      }
    }
    
    // Parse current CSS classes
    const cssClasses = (row.cssClass || '').split(' ').filter(c => c.trim());
    
    let html = `<h6>Row Properties</h6>`;
    
    // Margin class to value mappings
    const marginTopValues = {
      '': -1, 'margin-top-0': 0, 'margin-top-1': 1, 'margin-top-2': 2, 'margin-top-3': 3,
      'margin-top-4': 4, 'margin-top-5': 5, 'margin-top-10': 10, 'margin-top-15': 15,
      'margin-top-20': 20, 'margin-top-25': 25, 'margin-top-30': 30, 'margin-top-35': 35,
      'margin-top-40': 40, 'margin-top-50': 50, 'margin-top-75': 75, 'margin-top-100': 100,
      'margin-top-150': 150, 'margin-top-200': 200, 'margin-top-240': 240, 'margin-top-250': 250
    };
    
    const marginBottomValues = {
      '': -1, 'margin-bottom-0': 0, 'margin-bottom-5': 5, 'margin-bottom-10': 10, 'margin-bottom-15': 15,
      'margin-bottom-20': 20, 'margin-bottom-25': 25, 'margin-bottom-30': 30, 'margin-bottom-35': 35,
      'margin-bottom-40': 40, 'margin-bottom-50': 50, 'margin-bottom-75': 75, 'margin-bottom-100': 100,
      'margin-bottom-150': 150, 'margin-bottom-200': 200, 'margin-bottom-250': 250, 'margin-bottom-300': 300,
      'margin-bottom-400': 400
    };
    
    // Valid margin values for sliders
    const topMarginOptions = [-1, 0, 1, 2, 3, 4, 5, 10, 15, 20, 25, 30, 35, 40, 50, 75, 100, 150, 200, 240, 250];
    const bottomMarginOptions = [-1, 0, 5, 10, 15, 20, 25, 30, 35, 40, 50, 75, 100, 150, 200, 250, 300, 400];
    
    const getClassValue = (prefix) => cssClasses.find(c => c.startsWith(prefix)) || '';
    const marginTopClass = getClassValue('margin-top-');
    const marginBottomClass = getClassValue('margin-bottom-');
    const marginTopValue = marginTopValues[marginTopClass] || -1;
    const marginBottomValue = marginBottomValues[marginBottomClass] || -1;
    
    // Find initial indices for sliders
    const topMarginIndex = topMarginOptions.indexOf(marginTopValue);
    const bottomMarginIndex = bottomMarginOptions.indexOf(marginBottomValue);
    
    // Extract additional classes (non-margin classes)
    const additionalClasses = cssClasses
      .filter(c => !c.startsWith('margin-top-') && !c.startsWith('margin-bottom-'))
      .join(' ');
    
    // Margin section with snap sliders
    html += `<div style="border-bottom:1px solid var(--editor-border);padding-bottom:10px;margin-bottom:10px;">
      <div style="font-weight:bold;font-size:13px;margin-bottom:8px;">Margins</div>
      
      <div class="property-group">
      <div class="property-label">Top Margin: <span id="row-margin-top-value">${marginTopValue === -1 ? 'default' : marginTopValue + 'px'}</span></div>
      <input type="range" class="property-input" id="row-margin-top" min="0" max="${topMarginOptions.length - 1}" value="${topMarginIndex >= 0 ? topMarginIndex : 0}" style="width:100%;cursor:pointer;" data-values='${JSON.stringify(topMarginOptions)}' />
      </div>
      
      <div class="property-group">
      <div class="property-label">Bottom Margin: <span id="row-margin-bottom-value">${marginBottomValue}px</span></div>
      <input type="range" class="property-input" id="row-margin-bottom" min="0" max="${bottomMarginOptions.length - 1}" value="${bottomMarginIndex >= 0 ? bottomMarginIndex : 0}" style="width:100%;cursor:pointer;" data-values='${JSON.stringify(bottomMarginOptions)}' />
      </div>
    </div>`;
    
    // CSS Class manual override
    html += `<div class="property-group">
      <label style="margin-bottom:10px;"><input type="checkbox" id="row-hr" ${row.hr ? 'checked' : ''} /> Horizontal Line</label>
    </div>`;
    
    html += `<div class="property-group">
      <div class="property-label">Additional CSS Classes</div>
      <input type="text" class="property-input" id="row-css-class" value="${this.escapeHtml(additionalClasses)}" placeholder="e.g., align-center" />
      <div style="font-size:12px;color:var(--editor-text-muted);margin-top:5px;">Any additional custom classes (space-separated)</div>
    </div>`;
    
    // CSS preview
    html += `<div class="property-group" style="background:var(--editor-panel-bg);padding:10px;border-radius:4px;margin:10px 0;border:1px solid var(--editor-border);">
      <div style="font-weight:bold;font-size:12px;margin-bottom:5px;">Applied Classes</div>
      <div id="row-css-preview" style="font-family:monospace;font-size:11px;word-break:break-all;max-height:80px;overflow-y:auto;"></div>
    </div>`;
    
    this.content.innerHTML = html;
    
    // Set up event listener for live preview
    const marginTopSelect = document.getElementById('row-margin-top');
    const marginBottomSelect = document.getElementById('row-margin-bottom');
    const additionalClassesInput = document.getElementById('row-css-class');
    const preview = document.getElementById('row-css-preview');
    
    // Helper to convert value to class
    const valueToTopMarginClass = (val) => {
      for (const [cls, v] of Object.entries(marginTopValues)) {
        if (v === val) return cls;
      }
      return '';
    };
    
    const valueToBottomMarginClass = (val) => {
      for (const [cls, v] of Object.entries(marginBottomValues)) {
        if (v === val) return cls;
      }
      return '';
    };
    
    const updatePreview = () => {
      // Get slider indices and convert to actual values
      const topIndex = parseInt(marginTopSelect?.value || 0);
      const bottomIndex = parseInt(marginBottomSelect?.value || 0);
      const topMarginValues_list = JSON.parse(marginTopSelect?.getAttribute('data-values') || '[]');
      const bottomMarginValues_list = JSON.parse(marginBottomSelect?.getAttribute('data-values') || '[]');
      
      const marginTopVal = topMarginValues_list[topIndex] || 0;
      const marginBottomVal = bottomMarginValues_list[bottomIndex] || 0;
      
      const marginTop = valueToTopMarginClass(marginTopVal);
      const marginBottom = valueToBottomMarginClass(marginBottomVal);
      const additionalClasses = additionalClassesInput?.value.trim() || '';
      const finalClasses = [marginTop, marginBottom, additionalClasses].filter(c => c).join(' ');
      if (preview) {
        preview.textContent = finalClasses || '(no classes applied)';
      }
      
      // Update slider value displays
      const topValueDisplay = document.getElementById('row-margin-top-value');
      const bottomValueDisplay = document.getElementById('row-margin-bottom-value');
      if (topValueDisplay) topValueDisplay.textContent = marginTopVal === -1 ? 'default' : marginTopVal + 'px';
      if (bottomValueDisplay) bottomValueDisplay.textContent = marginBottomVal === -1 ? 'default' : marginBottomVal + 'px';
      
      // Save immediately to the data model
      const finalClass = finalClasses || '';
      if (this.editor && this.editor.getLayoutManager) {
        this.editor.getLayoutManager().updateRowClass(rowId, finalClass);
        const row = this.editor.getLayoutManager().getRow(rowId);
        if (row && this.editor.getCanvasController) {
          this.editor.getCanvasController().renderRow(rowId, row);
          // Re-highlight the row after re-render
          setTimeout(() => {
            const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
            if (rowElement) {
              rowElement.classList.add('selected');
            }
          }, 0);
        }
        if (this.editor.saveToHistory) {
          this.editor.saveToHistory();
        }
        
        // Enhancement: Auto-refresh preview when property is edited and preview is active
        this.refreshPreviewIfActive();
      }
    };
    
    const self = this;
    if (marginTopSelect) marginTopSelect.addEventListener('input', function() { updatePreview.call(self); });
    if (marginBottomSelect) marginBottomSelect.addEventListener('input', function() { updatePreview.call(self); });
    if (additionalClassesInput) additionalClassesInput.addEventListener('input', function() { updatePreview.call(self); });
    
    // Initial preview update to show current state
    updatePreview();
    
    // Add hr checkbox listener
    const hrCheckbox = document.getElementById('row-hr');
    if (hrCheckbox) {
      hrCheckbox.addEventListener('change', function() {
        const row = self.editor.getLayoutManager().getRow(rowId);
        if (row) {
          row.hr = this.checked;
          if (self.editor.getCanvasController) {
            self.editor.getCanvasController().renderRow(rowId, row);
            // Re-highlight the row after re-render
            setTimeout(() => {
              const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
              if (rowElement) {
                rowElement.classList.add('selected');
              }
            }, 0);
          }
          if (self.editor.saveToHistory) {
            self.editor.saveToHistory();
          }
          
          // Enhancement: Auto-refresh preview when property is edited and preview is active
          self.refreshPreviewIfActive();
        }
      });
    }
    
    // Highlight the row in the canvas
    this.highlightRow(rowId);
  }
  
  /**
   * Show column properties
   */
  showColumnProperties(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    const column = row.columns.find(c => c.id === columnId);
    if (!column) return;

    // Highlight the column in the preview if preview is enabled (for preview-layout mode)
    if (typeof globalThis !== 'undefined' && globalThis.previewHoverManager &&
        typeof globalThis.previewHoverManager.selectElementByContext === 'function') {
      const previewStateGroup = document.getElementById('preview-state-group');
      const currentPreviewState = previewStateGroup ? previewStateGroup.dataset.previewState : 'preview';
      if (currentPreviewState !== 'layout') {
        console.debug('PropertiesPanel: Highlighting column in preview');
        globalThis.previewHoverManager.selectElementByContext({
          type: 'column',
          rowId,
          columnId
        });
      }
    }
    
    // Parse current CSS classes
    const cssClasses = (column.cssClass || '').split(' ').filter(c => c.trim());
    
    // Helper to get class
    const getClassValue = (prefix) => cssClasses.find(c => c.startsWith(prefix)) || '';
    
    // Size options (small, medium, large)
    const sizeOptions = [
      '', 'small-12', 'small-11', 'small-10', 'small-9', 'small-8', 'small-7', 'small-6', 
      'small-5', 'small-4', 'small-3', 'small-2', 'small-1'
    ];
    
    const mediumOptions = [
      '', 'medium-12', 'medium-11', 'medium-10', 'medium-9', 'medium-8', 'medium-7', 
      'medium-6', 'medium-5', 'medium-4', 'medium-3', 'medium-2', 'medium-1'
    ];
    
    const largeOptions = [
      '', 'large-12', 'large-11', 'large-10', 'large-9', 'large-8', 'large-7',
      'large-6', 'large-5', 'large-4', 'large-3', 'large-2', 'large-1'
    ];
    
    const paddingOptions = [
      { value: '', label: 'None' },
      { value: 'padding-top-5', label: '5px' },
      { value: 'padding-top-10', label: '10px' },
      { value: 'padding-top-15', label: '15px' },
      { value: 'padding-top-20', label: '20px' },
      { value: 'padding-top-30', label: '30px' },
      { value: 'padding-top-40', label: '40px' },
      { value: 'padding-top-50', label: '50px' },
      { value: 'padding-top-60', label: '60px' }
    ];
    
    const paddingBottomOptions = [
      { value: '', label: 'None' },
      { value: 'padding-bottom-0', label: '0px' },
      { value: 'padding-bottom-5', label: '5px' },
      { value: 'padding-bottom-10', label: '10px' },
      { value: 'padding-bottom-15', label: '15px' },
      { value: 'padding-bottom-20', label: '20px' },
      { value: 'padding-bottom-30', label: '30px' },
      { value: 'padding-bottom-40', label: '40px' },
      { value: 'padding-bottom-50', label: '50px' },
      { value: 'padding-bottom-60', label: '60px' },
      { value: 'padding-bottom-75', label: '75px' },
      { value: 'padding-bottom-100', label: '100px' }
    ];
    
    const calloutOptions = [
      { value: '', label: 'None' },
      { value: 'callout', label: 'Callout (default)' },
      { value: 'callout box', label: 'Callout with box' },
      { value: 'callout no-box', label: 'Callout no-box' },
      { value: 'callout no-border', label: 'Callout no-border' },
      { value: 'callout border-none', label: 'Callout border-none' },
      { value: 'callout radius', label: 'Callout radius' },
      { value: 'callout round', label: 'Callout round' }
    ];
    
    const alignmentOptions = [
      { value: '', label: 'Default' },
      { value: 'align-center', label: 'Center' },
      { value: 'align-left', label: 'Left' },
      { value: 'align-right', label: 'Right' },
      { value: 'align-justify', label: 'Justify' }
    ];
    
    const textOptions = [
      { value: '', label: 'None' },
      { value: 'text-bold', label: 'Bold' },
      { value: 'text-underline', label: 'Underline' },
      { value: 'text-strike', label: 'Strikethrough' },
      { value: 'text-no-wrap', label: 'No Wrap' },
      { value: 'text-middle', label: 'Middle' }
    ];
    
    // Get current values
    const smallClass = getClassValue('small-') || '';
    const mediumClass = getClassValue('medium-');
    const largeClass = getClassValue('large-');
    const paddingTopClass = getClassValue('padding-top-');
    const paddingBottomClass = getClassValue('padding-bottom-');
    const calloutClass = getClassValue('callout');
    const alignmentClass = getClassValue('align-');
    const textClass = getClassValue('text-');
    
    // Calculate unhandled classes to add to custom CSS
    const handledClasses = [smallClass, mediumClass, largeClass, paddingTopClass, paddingBottomClass, calloutClass, alignmentClass, textClass].filter(c => c);
    const unhandledClasses = cssClasses.filter(c => !handledClasses.includes(c)).join(' ');
    
    let html = `<h6>Column Properties</h6>`;
    
    // Size section
    html += `<div style="border-bottom:1px solid var(--editor-border);padding-bottom:10px;margin-bottom:10px;">
      <div style="font-weight:bold;font-size:13px;margin-bottom:8px;">Responsive Sizes</div>
      
      <div class="property-group">
        <div class="property-label">Small (Phone)</div>
        <select class="property-input" id="column-size-small">`;
    for (const size of sizeOptions) {
      const selected = smallClass === size ? 'selected' : '';
      const label = size || '(none)';
      html += `<option value="${size}" ${selected}>${label}</option>`;
    }
    html += `</select></div>
      
      <div class="property-group">
        <div class="property-label">Medium (Tablet)</div>
        <select class="property-input" id="column-size-medium">`;
    for (const size of mediumOptions) {
      const selected = mediumClass === size ? 'selected' : '';
      html += `<option value="${size}" ${selected}>${size || '(same as small)'}</option>`;
    }
    html += `</select></div>
      
      <div class="property-group">
        <div class="property-label">Large (Desktop)</div>
        <select class="property-input" id="column-size-large">`;
    for (const size of largeOptions) {
      const selected = largeClass === size ? 'selected' : '';
      html += `<option value="${size}" ${selected}>${size || '(same as medium)'}</option>`;
    }
    html += `</select></div>
    </div>`;
    
    // Spacing section
    html += `<div style="border-bottom:1px solid var(--editor-border);padding-bottom:10px;margin-bottom:10px;">
      <div style="font-weight:bold;font-size:13px;margin-bottom:8px;">Spacing (Padding)</div>
      
      <div class="property-group">
        <div class="property-label">Top Padding</div>
        <select class="property-input" id="column-padding-top">`;
    for (const option of paddingOptions) {
      const selected = paddingTopClass === option.value ? 'selected' : '';
      html += `<option value="${option.value}" ${selected}>${option.label}</option>`;
    }
    html += `</select></div>
      
      <div class="property-group">
        <div class="property-label">Bottom Padding</div>
        <select class="property-input" id="column-padding-bottom">`;
    for (const option of paddingBottomOptions) {
      const selected = paddingBottomClass === option.value ? 'selected' : '';
      html += `<option value="${option.value}" ${selected}>${option.label}</option>`;
    }
    html += `</select></div>
    </div>`;
    
    // Callout/Box section
    html += `<div style="border-bottom:1px solid var(--editor-border);padding-bottom:10px;margin-bottom:10px;">
      <div class="property-group">
        <div class="property-label">Box Style</div>
        <select class="property-input" id="column-callout">`;
    for (const option of calloutOptions) {
      // Default to 'none' if no callout class exists
      const selected = (calloutClass === option.value || (!calloutClass && option.value === '')) ? 'selected' : '';
      html += `<option value="${option.value}" ${selected}>${option.label}</option>`;
    }
    html += `</select></div>
    </div>`;
    
    // Alignment section
    html += `<div style="border-bottom:1px solid var(--editor-border);padding-bottom:10px;margin-bottom:10px;">
      <div class="property-group">
        <div class="property-label">Alignment</div>
        <select class="property-input" id="column-alignment">`;
    for (const option of alignmentOptions) {
      const selected = alignmentClass === option.value ? 'selected' : '';
      html += `<option value="${option.value}" ${selected}>${option.label}</option>`;
    }
    html += `</select></div>
    </div>`;
    
    // Text section
    html += `<div style="border-bottom:1px solid var(--editor-border);padding-bottom:10px;margin-bottom:10px;">
      <div class="property-group">
        <div class="property-label">Text Style</div>
        <select class="property-input" id="column-text">`;
    for (const option of textOptions) {
      const selected = textClass === option.value ? 'selected' : '';
      html += `<option value="${option.value}" ${selected}>${option.label}</option>`;
    }
    html += `</select></div>
    </div>`;
    
    // Custom CSS
    html += `<div class="property-group">
      <label style="margin-bottom:10px;"><input type="checkbox" id="column-hr" ${column.hr ? 'checked' : ''} /> Horizontal Line</label>
    </div>`;
    
    html += `<div class="property-group">
      <div class="property-label">Additional CSS Classes</div>
      <input type="text" class="property-input" id="column-css-custom" value="${this.escapeHtml(unhandledClasses)}" placeholder="Custom classes" />
      <div style="font-size:12px;color:var(--editor-text-muted);margin-top:5px;">Any additional custom classes (space-separated)</div>
    </div>`;
    
    // CSS preview
    html += `<div class="property-group" style="background:var(--editor-panel-bg);padding:10px;border-radius:4px;margin:10px 0;border:1px solid var(--editor-border);">
      <div style="font-weight:bold;font-size:12px;margin-bottom:5px;">Applied Classes</div>
      <div id="column-css-preview" style="font-family:monospace;font-size:11px;word-break:break-all;max-height:100px;overflow-y:auto;"></div>
    </div>`;
    
    this.content.innerHTML = html;
    
    // Set up event listener for live preview
    const self = this;
    const updatePreview = () => {
      const small = document.getElementById('column-size-small')?.value || '';
      const medium = document.getElementById('column-size-medium')?.value || '';
      const large = document.getElementById('column-size-large')?.value || '';
      const paddingTop = document.getElementById('column-padding-top')?.value || '';
      const paddingBottom = document.getElementById('column-padding-bottom')?.value || '';
      const callout = document.getElementById('column-callout')?.value || '';
      const alignment = document.getElementById('column-alignment')?.value || '';
      const text = document.getElementById('column-text')?.value || '';
      const custom = document.getElementById('column-css-custom')?.value.trim() || '';
      
      const classes = [small, medium, large, paddingTop, paddingBottom, callout, alignment, text, custom].filter(c => c);
      const preview = document.getElementById('column-css-preview');
      if (preview) {
        preview.textContent = classes.length > 0 ? classes.join(' ') : '(no classes applied)';
      }
      
      // Save to data model
      if (self.editor && self.editor.getLayoutManager) {
        self.editor.getLayoutManager().updateColumnClass(rowId, columnId, classes.join(' '));
        const row = self.editor.getLayoutManager().getRow(rowId);
        if (row && self.editor.getCanvasController) {
          self.editor.getCanvasController().renderRow(rowId, row);
          // Re-highlight the column after re-render
          setTimeout(() => {
            const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
            if (rowElement) {
              const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
              if (columnElement) {
                columnElement.classList.add('selected');
              }
            }
          }, 0);
        }
        if (self.editor.saveToHistory) {
          self.editor.saveToHistory();
        }
        
        // Enhancement: Auto-refresh preview when property is edited and preview is active
        self.refreshPreviewIfActive();
      }
    };
    
    ['column-size-small', 'column-size-medium', 'column-size-large', 'column-padding-top', 'column-padding-bottom', 'column-callout', 'column-alignment', 'column-text', 'column-css-custom'].forEach(id => {
      const el = document.getElementById(id);
      if (el) {
        el.addEventListener('change', function() { updatePreview(); });
        el.addEventListener('input', function() { updatePreview(); });
      }
    });
    
    // Initial preview update to show current state
    updatePreview();
    
    // Add hr checkbox listener
    const hrCheckbox = document.getElementById('column-hr');
    if (hrCheckbox) {
      hrCheckbox.addEventListener('change', function() {
        const row = self.editor.getLayoutManager().getRow(rowId);
        if (row) {
          const column = row.columns.find(c => c.id === columnId);
          if (column) {
            column.hr = this.checked;
            if (self.editor.getCanvasController) {
              self.editor.getCanvasController().renderRow(rowId, row);
              // Re-highlight the column after re-render
              setTimeout(() => {
                const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
                if (rowElement) {
                  const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                  if (columnElement) {
                    columnElement.classList.add('selected');
                  }
                }
              }, 0);
            }
            if (self.editor.saveToHistory) {
              self.editor.saveToHistory();
            }
            
            // Enhancement: Auto-refresh preview when property is edited and preview is active
            self.refreshPreviewIfActive();
          }
        }
      });
    }
    
    // Highlight the column in the canvas
    this.highlightColumn(rowId, columnId);
  }
  
  /**
   * Show widget properties
   */
  showWidgetProperties(rowId, columnId, widgetId) {
    // Clean up any existing TinyMCE editors
    this.cleanupHtmlEditors();
    
    // Clear any previously highlighted row or column
    this.clearHighlight();
    
    console.debug('PropertiesPanel: showWidgetProperties called with:', { rowId, columnId, widgetId });
    const widget = this.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
    if (!widget) {
      console.warn('PropertiesPanel: Widget not found in layout manager:', { rowId, columnId, widgetId });
      return;
    }

    // Highlight the widget in the preview if preview is enabled (for preview-layout mode)
    if (globalThis.previewHoverManager && typeof globalThis.previewHoverManager.selectElementByContext === 'function') {
      const previewStateGroup = document.getElementById('preview-state-group');
      const currentPreviewState = previewStateGroup ? previewStateGroup.dataset.previewState : 'preview';
      // Only highlight in preview if preview is enabled (preview or preview-layout mode)
      if (currentPreviewState !== 'layout') {
        console.debug('PropertiesPanel: Highlighting widget in preview');
        globalThis.previewHoverManager.selectElementByContext({
          type: 'widget',
          rowId: rowId,
          columnId: columnId,
          widgetId: widgetId
        });
      }
    }
    
    console.debug('PropertiesPanel: Found widget:', widget);
    const definition = this.editor.widgetRegistry.get(widget.type);
    if (!definition) {
      console.warn('PropertiesPanel: Widget definition not found for type:', widget.type);
      this.content.innerHTML = '<p>Widget definition not found</p>';
      return;
    }
    
    let html = `<h6>${definition.name} Properties</h6>`;
    
    // Get the page link for generating unique IDs
    let pageLink = 'page';
    if (this.editor && this.editor.pagesTabManager && this.editor.pagesTabManager.getSelectedPageLink) {
      pageLink = this.editor.pagesTabManager.getSelectedPageLink();
      // Remove leading "/" if present
      if (pageLink && pageLink.startsWith('/')) {
        pageLink = pageLink.substring(1);
      }
    }
    
    // Render property fields
    for (const [propName, propDef] of Object.entries(definition.properties)) {
      // Pass the value if it exists in widget.properties, otherwise pass undefined
      const propValue = widget.properties.hasOwnProperty(propName) ? widget.properties[propName] : undefined;
      html += this.renderPropertyField(propName, propDef, propValue, pageLink);
    }
    
    // Add Additional CSS Classes field
    html += `<div style="border-top:1px solid #ddd;padding-top:10px;margin-top:10px;">`;
    html += `<div class="property-group">`;
    html += `<label style="margin-bottom:10px;"><input type="checkbox" id="widget-hr" ${widget.hr ? 'checked' : ''} /> Horizontal Line</label>`;
    html += `</div>`;
    html += `<div class="property-group">`;
    html += `<div class="property-label">Additional CSS Classes</div>`;
    html += `<input type="text" class="property-input" id="widget-css-class" value="${this.escapeHtml(widget.cssClass || '')}" placeholder="e.g., margin-50" />`;
    html += `<div style="font-size:12px;color:#666;margin-top:5px;">Any additional custom classes (space-separated)</div>`;
    html += `</div>`;
    html += `</div>`;
    
    this.content.innerHTML = html;

    // Set up event listeners for immediate save for all widget properties
    const self = this;
    
    // Add listeners to all property inputs
    if (definition && definition.properties) {
      for (const propName of Object.keys(definition.properties)) {
        const element = document.getElementById(`prop-${propName}`);
        if (element) {
          const updatePropertyOnChange = () => {
            const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
            if (widgetData) {
              const propDef = definition.properties[propName];
              if (propDef.type === 'checkbox') {
                widgetData.properties[propName] = element.checked ? 'true' : 'false';
              } else {
                widgetData.properties[propName] = element.value;
              }
              if (self.editor.getCanvasController) {
                const row = self.editor.getLayoutManager().getRow(rowId);
                self.editor.getCanvasController().renderRow(rowId, row);
                // Re-highlight the widget after re-render
                setTimeout(() => {
                  const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
                  if (rowElement) {
                    const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                    if (columnElement) {
                      const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                      if (widgetElement) {
                        widgetElement.classList.add('selected');
                      }
                    }
                  }
                }, 0);
              }
              if (self.editor.saveToHistory) {
                self.editor.saveToHistory();
              }
              
              // Enhancement: Auto-refresh preview when property is edited and preview is active
              self.refreshPreviewIfActive();
            }
          };
          
          element.addEventListener('change', updatePropertyOnChange);
          element.addEventListener('input', updatePropertyOnChange);
        }
      }
    }
    
    // Update widget CSS class and set up listeners for CSS class and HR checkbox
    
    const hrCheckbox = document.getElementById('widget-hr');
    if (hrCheckbox) {
      hrCheckbox.addEventListener('change', function() {
        const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
        if (widgetData) {
          widgetData.hr = this.checked;
          if (self.editor.getCanvasController) {
            const row = self.editor.getLayoutManager().getRow(rowId);
            self.editor.getCanvasController().renderRow(rowId, row);
            // Re-highlight the widget after re-render
            setTimeout(() => {
              const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
              if (rowElement) {
                const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                if (columnElement) {
                  const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                  if (widgetElement) {
                    widgetElement.classList.add('selected');
                  }
                }
              }
            }, 0);
          }
          if (self.editor.saveToHistory) {
            self.editor.saveToHistory();
          }
          
          // Enhancement: Auto-refresh preview when property is edited and preview is active
          self.refreshPreviewIfActive();
        }
      });
    }
    
    // Add listener for CSS class input
    const cssClassInput = document.getElementById('widget-css-class');
    if (cssClassInput) {
      cssClassInput.addEventListener('input', function() {
        const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
        if (widgetData) {
          widgetData.cssClass = this.value.trim();
          if (self.editor.getCanvasController) {
            const row = self.editor.getLayoutManager().getRow(rowId);
            self.editor.getCanvasController().renderRow(rowId, row);
            // Re-highlight the widget after re-render
            setTimeout(() => {
              const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
              if (rowElement) {
                const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                if (columnElement) {
                  const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                  if (widgetElement) {
                    widgetElement.classList.add('selected');
                  }
                }
              }
            }, 0);
          }
          if (self.editor.saveToHistory) {
            self.editor.saveToHistory();
          }
          
          // Enhancement: Auto-refresh preview when property is edited and preview is active
          self.refreshPreviewIfActive();
        }
      });
    }

    // Initialize color pickers
    this.initColorPickers(rowId, columnId, widgetId, definition);
    
    // Initialize HTML editors (TinyMCE)
    this.initHtmlEditors(rowId, columnId, widgetId, definition);
    
    // Initialize XML properties
    this.initXmlProperties(rowId, columnId, widgetId, definition);
    
    // Initialize contentUniqueId properties
    this.initContentUniqueIdProperties(rowId, columnId, widgetId, definition);
    
    // Initialize icon pickers
    this.initIconPickers(rowId, columnId, widgetId, definition);
  }
  
  /**
   * Initialize XML array properties with add/edit/delete functionality
   */
  initXmlProperties(rowId, columnId, widgetId, widgetDef) {
    // Find all XML property edit buttons
    const editButtons = this.content.querySelectorAll('button[id^="edit-"]');
    
    editButtons.forEach((button) => {
      const propName = button.id.replace('edit-', '');
      const propDef = widgetDef.properties[propName];
      
      if (!propDef || propDef.type !== 'xml') return;
      
      button.addEventListener('click', (e) => {
        e.preventDefault();
        this.openXmlPropertyModal(propName, propDef, rowId, columnId, widgetId);
      });
    });
  }
  
  /**
   * Open modal dialog for editing XML properties with table-like spreadsheet view
   */
  openXmlPropertyModal(propName, propDef, rowId, columnId, widgetId) {
    const schema = propDef.schema || {};
    const itemSchema = schema.items || {};
    const itemName = itemSchema.name || 'item';
    const attributes = itemSchema.attributes || {};
    
    // Get current widget data to fetch existing items
    const widgetData = this.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
    const currentValue = widgetData?.properties[propName] || '';
    
    // Parse existing items
    let items = [];
    if (currentValue && typeof currentValue === 'string') {
      items = this.parseXmlArrayFromString(currentValue, schema);
    } else if (Array.isArray(currentValue)) {
      items = currentValue;
    }
    
    // Create and show modal
    this.showXmlPropertyModal(propName, propDef.label, itemName, attributes, items, (updatedItems) => {
      // Save the updated items
      this.saveXmlProperty(propName, propDef, updatedItems, rowId, columnId, widgetId);
      
      // Update the summary in the panel
      const summary = document.getElementById(`xml-summary-${propName}`);
      if (summary) {
        if (updatedItems.length === 0) {
          summary.innerHTML = `<div style="color:var(--editor-text-muted);font-style:italic;flex:1;">No ${itemName} entries yet</div><button type="button" class="button small radius no-gap" style="margin-left:10px;">Edit</button>`;
        } else {
          summary.innerHTML = `<div style="color:var(--editor-text);flex:1;"><strong>${updatedItems.length}</strong> ${itemName}${updatedItems.length === 1 ? '' : 's'} configured</div><button type="button" class="button small radius no-gap" style="margin-left:10px;">Edit</button>`;
        }
        
        // Re-attach edit button listener
        const editBtn = summary.querySelector('button');
        if (editBtn) {
          editBtn.addEventListener('click', (e) => {
            e.preventDefault();
            this.openXmlPropertyModal(propName, propDef, rowId, columnId, widgetId);
          });
        }
      }
    });
  }
  
  /**
   * Show modal dialog with spreadsheet-like view for XML property items
   */
  showXmlPropertyModal(propName, propLabel, itemName, attributes, items, onSave) {
    // Create modal HTML
    let modalHtml = `
      <div id="xml-property-modal-${propName}" class="xml-property-modal" style="
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
      ">
        <div class="xml-property-modal-content" style="
          background: var(--editor-panel-bg);
          border-radius: 8px;
          padding: 30px;
          max-width: 90%;
          max-height: 90vh;
          overflow: auto;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
          min-width: 600px;
        ">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h4 style="margin: 0; color: var(--editor-text);">Edit ${propLabel}</h4>
            <button type="button" class="close-modal" style="
              background: none;
              border: none;
              font-size: 24px;
              cursor: pointer;
              color: var(--editor-text);
              padding: 0;
              width: 30px;
              height: 30px;
              display: flex;
              align-items: center;
              justify-content: center;
            ">×</button>
          </div>
          
          <!-- Spreadsheet table -->
          <div style="overflow-x: auto; margin-bottom: 20px; border: 1px solid var(--editor-border); border-radius: 4px;">
            <table id="xml-items-table-${propName}" class="xml-items-table" style="
              width: 100%;
              border-collapse: collapse;
              background: var(--editor-bg);
            ">
              <thead style="background: var(--editor-hover-bg); border-bottom: 2px solid var(--editor-border);">
                <tr>
                  <th style="padding: 10px; text-align: center; width: 30px; color: var(--editor-text); font-weight: bold;" title="Drag to reorder"><i class="fa fa-bars"></i></th>
                  <th style="padding: 10px; text-align: center; width: 40px; color: var(--editor-text); font-weight: bold;">№</th>
    `;
    
    // Add column headers for each attribute
    for (const [attrName, attrDef] of Object.entries(attributes)) {
      const columnLabel = attrDef.label || attrName;
      const requiredMark = attrDef.required ? ' <span style="color: #dc3545;">*</span>' : '';
      modalHtml += `<th style="padding: 10px; text-align: left; color: var(--editor-text); font-weight: bold; white-space: nowrap;">${columnLabel}${requiredMark}</th>`;
    }
    
    modalHtml += `
                  <th style="padding: 10px; text-align: center; width: 50px; color: var(--editor-text); font-weight: bold;">Actions</th>
                </tr>
              </thead>
              <tbody id="xml-modal-tbody-${propName}">
    `;
    
    // Add rows for each item
    items.forEach((item, index) => {
      modalHtml += this.renderXmlModalTableRow(propName, index, item, itemName, attributes);
    });
    
    modalHtml += `
              </tbody>
            </table>
          </div>
          
          <!-- Empty state -->
          <div id="xml-modal-empty-${propName}" style="
            display: ${items.length === 0 ? 'block' : 'none'};
            text-align: center;
            color: var(--editor-text-muted);
            font-style: italic;
            padding: 40px 20px;
          ">
            No ${itemName} entries yet
          </div>
          
          <!-- Action buttons -->
          <div style="display: flex; gap: 10px; margin-bottom: 20px;">
            <button type="button" id="add-item-${propName}" class="button radius" style="flex: 0 0 auto;">
              Add ${itemName}
            </button>
            <button type="button" id="duplicate-row-${propName}" class="button radius secondary" style="flex: 0 0 auto; display: none;">
              Duplicate Selected
            </button>
          </div>
          
          <!-- Modal footer -->
          <div style="display: flex; gap: 10px; justify-content: flex-end; border-top: 1px solid var(--editor-border); padding-top: 20px;">
            <button type="button" class="button radius secondary cancel-modal">Cancel</button>
            <button type="button" class="button radius save-modal">Apply Changes</button>
          </div>
        </div>
      </div>
    `;
    
    // Insert modal into DOM
    const modalContainer = document.createElement('div');
    modalContainer.innerHTML = modalHtml;
    document.body.appendChild(modalContainer);
    
    const modal = document.getElementById(`xml-property-modal-${propName}`);
    const tbody = document.getElementById(`xml-modal-tbody-${propName}`);
    const emptyState = document.getElementById(`xml-modal-empty-${propName}`);
    const table = document.getElementById(`xml-items-table-${propName}`);
    const addBtn = document.getElementById(`add-item-${propName}`);
    const saveBtn = modal.querySelector('.save-modal');
    const cancelBtn = modal.querySelector('.cancel-modal');
    const closeBtn = modal.querySelector('.close-modal');
    
    // Track current items in modal
    let currentItems = [...items];
    // Initialize table, listeners, and Dragula on open
    this.updateXmlModalTable(propName, itemName, attributes, currentItems, tbody, emptyState, table);
    
    // Add button handler
    addBtn.addEventListener('click', (e) => {
      e.preventDefault();
      const newItem = {};
      for (const [attrName, attrDef] of Object.entries(attributes)) {
        newItem[attrName] = attrDef.default || '';
      }
      currentItems.push(newItem);
      this.updateXmlModalTable(propName, itemName, attributes, currentItems, tbody, emptyState, table);
    });
    
    // Save button handler
    saveBtn.addEventListener('click', (e) => {
      e.preventDefault();
      const savedItems = this.getXmlItemsFromModal(propName, attributes);
      onSave(savedItems);
      modal.remove();
    });
    
    // Cancel button handler
    const closeModal = () => {
      modal.remove();
    };
    
    cancelBtn.addEventListener('click', closeModal);
    closeBtn.addEventListener('click', closeModal);
    
    // Close modal when clicking outside
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        closeModal();
      }
    });
  }
  
  /**
   * Render a single row in the XML items table
   */
  renderXmlModalTableRow(propName, index, item, itemName, attributes) {
    let html = `<tr class="xml-modal-row" data-item-index="${index}" data-original-index="${index}" style="border-bottom: 1px solid var(--editor-border); background: var(--editor-bg); transition: background-color 0.2s ease;">`;
    
    // Drag handle - only this cell is draggable
    html += `<td class="drag-handle" draggable="true" style="padding: 10px; text-align: center; color: var(--editor-text-muted); font-size: 16px; cursor: grab; background: var(--editor-panel-bg); user-select: none;"><i class="fa fa-grip-vertical"></i></td>`;
    
    // Row number
    html += `<td style="padding: 10px; text-align: center; color: var(--editor-text-muted); font-size: 12px; background: var(--editor-panel-bg);">${index + 1}</td>`;
    
    // Attribute fields
    for (const [attrName, attrDef] of Object.entries(attributes)) {
      const attrValue = (item && item[attrName]) || attrDef.default || '';
      const fieldId = `xml-modal-field-${propName}-${index}-${attrName}`;
      
      html += `<td style="padding: 8px; color: var(--editor-text);">`;
      
      // Render based on attribute type
      if (attrDef.type === 'select' && attrDef.options) {
        html += `<select id="${fieldId}" class="xml-modal-field property-input no-gap" data-attr="${attrName}" data-index="${index}" style="width: 100%; padding: 5px; border: 1px solid var(--editor-border); border-radius: 3px;">`;
        for (const option of attrDef.options) {
          const optionValue = typeof option === 'object' ? option.value : option;
          const optionLabel = typeof option === 'object' ? option.label : option;
          const selected = attrValue === optionValue ? 'selected' : '';
          html += `<option value="${optionValue}" ${selected}>${optionLabel}</option>`;
        }
        html += `</select>`;
      } else if (attrDef.type === 'checkbox') {
        const checked = attrValue === 'true' || attrValue === true ? 'checked' : '';
        html += `<input type="checkbox" id="${fieldId}" class="xml-modal-field no-gap" data-attr="${attrName}" data-index="${index}" ${checked} style="cursor: pointer;" />`;
      } else {
        html += `<input type="text" id="${fieldId}" class="xml-modal-field property-input no-gap" data-attr="${attrName}" data-index="${index}" value="${this.escapeHtml(attrValue)}" style="width: 100%; padding: 5px; border: 1px solid var(--editor-border); border-radius: 3px;" />`;
      }
      
      html += `</td>`;
    }
    
    // Actions column
    html += `<td style="padding: 8px; text-align: center;">`;
    html += `<button type="button" class="delete-row-btn" data-index="${index}" style="
      padding: 4px 8px;
      font-size: 11px;
      background: var(--editor-hover-bg);
      border: 1px solid var(--editor-border);
      border-radius: 3px;
      cursor: pointer;
      color: #dc3545;
    ">Delete</button>`;
    html += `</td>`;
    
    html += `</tr>`;
    
    return html;
  }
  
  /**
   * Update the modal table with current items
   */
  updateXmlModalTable(propName, itemName, attributes, currentItems, tbody, emptyState, table) {
    console.debug('XML Modal: updateXmlModalTable()', { propName, itemCount: currentItems.length });
    
    // Destroy any existing Dragula instance BEFORE clearing DOM
    if (tbody._dragula) {
      console.debug('XML Modal: destroying dragula instance before DOM clear');
      tbody._dragula.destroy();
      tbody._dragula = null;
    }
    
    // Remove old event listeners if they exist
    if (tbody._dragHandlers) {
      tbody.removeEventListener('dragstart', tbody._dragHandlers.dragstart);
      tbody.removeEventListener('dragend', tbody._dragHandlers.dragend);
      tbody.removeEventListener('dragover', tbody._dragHandlers.dragover);
      tbody.removeEventListener('dragleave', tbody._dragHandlers.dragleave);
      tbody.removeEventListener('drop', tbody._dragHandlers.drop);
      tbody._dragHandlers = null;
    }
    
    // Clear and rebuild table body
    tbody.innerHTML = '';
    
    currentItems.forEach((item, index) => {
      const rowHtml = this.renderXmlModalTableRow(propName, index, item, itemName, attributes);
      tbody.insertAdjacentHTML('beforeend', rowHtml);
    });
    
    // Update empty state
    if (currentItems.length === 0) {
      emptyState.style.display = 'block';
      table.style.display = 'none';
    } else {
      emptyState.style.display = 'none';
      table.style.display = 'table';
    }
    
    // Attach event listeners to delete buttons and input fields
    this.attachXmlModalListeners(propName, itemName, attributes, currentItems, tbody, emptyState, table);
    console.debug('XML Modal: attached input/delete listeners', { propName });
    
    // Attach drag and drop listeners
    this.attachXmlModalDragListeners(propName, itemName, attributes, currentItems, tbody, emptyState, table);
    console.debug('XML Modal: attached drag listeners', { propName });
  }
  
  /**
   * Attach drag and drop event listeners for row reordering
   */
  attachXmlModalDragListeners(propName, itemName, attributes, currentItems, tbody, emptyState, table) {
    console.debug('Dragula: attachXmlModalDragListeners()', { propName, itemCount: currentItems.length });
    this.ensureDragulaLoaded(() => {
      console.debug('Dragula: ensure loaded callback start', { propName });
      // Note: destroy is now done in updateXmlModalTable BEFORE DOM rebuild
      const drake = dragula([tbody], {
        // Scope mirror container to the modal/table to reduce global side-effects
        mirrorContainer: (table && table.closest && table.closest('.modal-content')) || (table && table.parentElement) || document.body,
        direction: 'vertical',
        copy: false,
        revertOnSpill: true,
        accepts: (el, target, source, sibling) => target === source,
        moves: (el, source, handle, sibling) => {
          if (!handle) return false;
          const isHandle = handle.classList.contains('drag-handle') || !!handle.closest('.drag-handle');
          console.debug('Dragula: moves()', { allow: isHandle, handleClass: handle.className, hasClosest: !!handle.closest });
          return isHandle;
        },
        invalid: (el, handle) => {
          if (!handle) return true;
          const isDelete = handle.classList.contains('delete-row-btn') || !!handle.closest('.delete-row-btn');
          const isInput = ['INPUT', 'SELECT', 'TEXTAREA', 'BUTTON'].includes(handle.tagName);
          const invalid = isDelete || isInput;
          console.debug('Dragula: invalid()', { isDelete, isInput, invalid });
          return invalid;
        }
      });
      console.debug('Dragula: initialized', { rows: tbody.querySelectorAll('tr.xml-modal-row').length });
      // Safety cleanup: remove any lingering dragula classes/mirror nodes after drag ends
      drake.on('dragend', () => {
        try {
          if (document && document.body && document.body.classList) {
            document.body.classList.remove('gu-unselectable');
          }
          Array.prototype.slice.call(document.querySelectorAll('.gu-mirror')).forEach(node => {
            if (node && node.parentNode) node.parentNode.removeChild(node);
          });
        } catch (e) {
          console.debug('Dragula cleanup guard error:', e);
        }
      });
      drake.on('drop', () => {
        const rows = Array.from(tbody.querySelectorAll('tr.xml-modal-row'));
        const order = rows.map(r => Number.parseInt(r.dataset.originalIndex, 10));
        console.debug('Dragula: drop()', { order });
        const newItems = order.map(i => currentItems[i]);
        currentItems.splice(0, currentItems.length, ...newItems);
        this.updateXmlModalTable(propName, itemName, attributes, currentItems, tbody, emptyState, table);
      });
      tbody._dragula = drake;
      console.debug('Dragula: instance stored on tbody');
    });
  }

  /**
   * Ensure Dragula JS/CSS are loaded before usage
   */
  ensureDragulaLoaded(callback) {
    if (typeof globalThis !== 'undefined' && globalThis.dragula) {
      console.debug('Dragula: already loaded');
      callback && callback();
      return;
    }
    if (this._dragulaLoading) {
      console.debug('Dragula: load in progress; queueing callback');
      this._dragulaCallbacks = this._dragulaCallbacks || [];
      this._dragulaCallbacks.push(callback);
      return;
    }
    this._dragulaLoading = true;
    this._dragulaCallbacks = this._dragulaCallbacks || [];
    if (callback) this._dragulaCallbacks.push(callback);
    const head = document.head || document.getElementsByTagName('head')[0];
    const cssHref = '/javascript/dragula-3.7.3/dragula.min.css';
    if (!document.querySelector(`link[href="${cssHref}"]`)) {
      console.debug('Dragula: injecting CSS', { href: cssHref });
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = cssHref;
      head.appendChild(link);
    }
    
    setTimeout(() => {
      this._dragulaLoading = false;
      const callbacks = this._dragulaCallbacks || [];
      this._dragulaCallbacks = [];
      console.debug('Dragula: script already present; executing callbacks', { count: callbacks.length });
      callbacks.forEach(cb => { if (cb) cb(); });
    }, 0);
  
  }
  
  /**
   * Attach event listeners to modal table elements
   */
  attachXmlModalListeners(propName, itemName, attributes, currentItems, tbody, emptyState, table) {
    // Delete button listeners - attach directly to buttons for reliability
    const deleteButtons = tbody.querySelectorAll('.delete-row-btn');
    deleteButtons.forEach((btn) => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        e.stopImmediatePropagation();
        console.debug('XML Modal: delete clicked', { propName, index: Number.parseInt(btn.dataset.index, 10) });
        const indexToDelete = Number.parseInt(btn.dataset.index, 10);
        currentItems.splice(indexToDelete, 1);
        this.updateXmlModalTable(propName, itemName, attributes, currentItems, tbody, emptyState, table);
      }, true); // Use capture phase to ensure it fires first
    });
    
    // Input field change listeners
    const fields = tbody.querySelectorAll('.xml-modal-field');
    fields.forEach((field) => {
      field.addEventListener('change', () => {
        this.updateXmlModalItemsFromTable(propName, attributes, currentItems, tbody);
      });
      field.addEventListener('input', () => {
        this.updateXmlModalItemsFromTable(propName, attributes, currentItems, tbody);
      });
    });
  }
  
  /**
   * Update currentItems array from modal table values
   */
  updateXmlModalItemsFromTable(propName, attributes, currentItems, tbody) {
    const rows = tbody.querySelectorAll('tr[data-item-index]');
    rows.forEach((row, index) => {
      const item = {};
      for (const [attrName] of Object.entries(attributes)) {
        const fieldId = `xml-modal-field-${propName}-${index}-${attrName}`;
        const field = document.getElementById(fieldId);
        if (field) {
          const attrDef = attributes[attrName];
          if (attrDef.type === 'checkbox') {
            item[attrName] = field.checked ? 'true' : 'false';
          } else {
            item[attrName] = field.value;
          }
        }
      }
      currentItems[index] = item;
    });
  }
  
  /**
   * Get XML items from modal table
   */
  getXmlItemsFromModal(propName, attributes) {
    const items = [];
    const tbody = document.getElementById(`xml-modal-tbody-${propName}`);
    if (!tbody) return items;
    
    const rows = tbody.querySelectorAll('tr[data-item-index]');
    rows.forEach((row, index) => {
      const item = {};
      for (const [attrName] of Object.entries(attributes)) {
        const fieldId = `xml-modal-field-${propName}-${index}-${attrName}`;
        const field = document.getElementById(fieldId);
        if (field) {
          const attrDef = attributes[attrName];
          if (attrDef.type === 'checkbox') {
            item[attrName] = field.checked ? 'true' : 'false';
          } else {
            item[attrName] = field.value;
          }
        }
      }
      items.push(item);
    });
    
    return items;
  }

  /**
   * Add a new XML item to the array
   */
  addXmlItem(propName, propDef, rowId, columnId, widgetId) {
    const schema = propDef.schema || {};
    const itemSchema = schema.items || {};
    const itemName = itemSchema.name || 'item';
    const attributes = itemSchema.attributes || {};
    
    // Get current items
    const itemsContainer = document.getElementById(`xml-items-${propName}`);
    
    const currentItems = this.getXmlItemsFromUI(propName, itemName, attributes);
    
    // Create new item with defaults
    const newItem = {};
    for (const [attrName, attrDef] of Object.entries(attributes)) {
      if (attrDef.default === undefined) {
        newItem[attrName] = '';
      } else {
        newItem[attrName] = attrDef.default;
      }
    }
    
    currentItems.push(newItem);
    
    // Re-render items container
    const newItemIndex = currentItems.length - 1;
    const itemHtml = this.renderXmlItem(propName, newItemIndex, newItem, itemName, attributes);
    
    // Remove empty state message if present
    const emptyMessage = itemsContainer.querySelector('div[style*="font-style:italic"]');
    if (emptyMessage) {
      emptyMessage.remove();
    }
    
    // Add new item HTML
    itemsContainer.insertAdjacentHTML('beforeend', itemHtml);
    
    // Attach listeners to new item
    this.attachXmlItemListeners(propDef, propName, rowId, columnId, widgetId);
    
    // Save to widget
    this.saveXmlProperty(propName, propDef, currentItems, rowId, columnId, widgetId);
  }
  
  /**
   * Get XML items from the current UI state
   */
  getXmlItemsFromUI(propName, itemName, attributes) {
    const items = [];
    const container = document.getElementById(`xml-items-${propName}`);
    if (!container) return items;
    
    // Get all items within this specific container
    const itemElements = container.querySelectorAll(`div.xml-item[data-item-index]`);
    
    itemElements.forEach((element) => {
      const item = {};
      for (const attrName of Object.keys(attributes)) {
        const fieldId = `xml-field-${propName}-${element.dataset.itemIndex}-${attrName}`;
        const field = document.getElementById(fieldId);
        if (field) {
          const attrDef = attributes[attrName];
          if (attrDef.type === 'checkbox') {
            item[attrName] = field.checked ? 'true' : 'false';
          } else {
            item[attrName] = field.value;
          }
        }
      }
      items.push(item);
    });
    
    return items;
  }
  
  /**
   * Attach event listeners to XML item controls
   */
  attachXmlItemListeners(propDef, propName, rowId, columnId, widgetId) {
    const schema = propDef.schema || {};
    const itemSchema = schema.items || {};
    const itemName = itemSchema.name || 'item';
    const attributes = itemSchema.attributes || {};
    
    // Attach delete button listeners
    const deleteButtons = this.content.querySelectorAll(`button.delete-xml-item[data-prop="${propName}"]`);
    deleteButtons.forEach((btn) => {
      // Remove previous listeners by cloning and replacing
      const newBtn = btn.cloneNode(true);
      btn.parentNode.replaceChild(newBtn, btn);
      
      newBtn.addEventListener('click', (e) => {
        e.preventDefault();
        const indexToDelete = Number.parseInt(newBtn.dataset.index, 10);
        this.deleteXmlItem(propName, indexToDelete, propDef, rowId, columnId, widgetId);
      });
    });
    
    // Attach input field listeners
    const fields = this.content.querySelectorAll(`input[id^="xml-field-${propName}"], select[id^="xml-field-${propName}"]`);
    fields.forEach((field) => {
      // Remove previous listeners by cloning and replacing
      const newField = field.cloneNode(true);
      field.parentNode.replaceChild(newField, field);
      
      newField.addEventListener('change', () => {
        const items = this.getXmlItemsFromUI(propName, itemName, attributes);
        this.saveXmlProperty(propName, propDef, items, rowId, columnId, widgetId);
      });
      
      newField.addEventListener('input', () => {
        const items = this.getXmlItemsFromUI(propName, itemName, attributes);
        this.saveXmlProperty(propName, propDef, items, rowId, columnId, widgetId);
      });
    });
  }
  
  /**
   * Delete an XML item from the array
   */
  deleteXmlItem(propName, indexToDelete, propDef, rowId, columnId, widgetId) {
    const schema = propDef.schema || {};
    const itemSchema = schema.items || {};
    const itemName = itemSchema.name || 'item';
    const attributes = itemSchema.attributes || {};
    
    // Get current items and remove the one at the index
    const currentItems = this.getXmlItemsFromUI(propName, itemName, attributes);
    currentItems.splice(indexToDelete, 1);
    
    // Re-render the container
    const itemsContainer = document.getElementById(`xml-items-${propName}`);
    itemsContainer.innerHTML = '';
    
    if (currentItems.length === 0) {
      itemsContainer.innerHTML = `<div style="color:var(--editor-text-muted);font-style:italic;text-align:center;padding:20px;">No ${itemName} entries yet</div>`;
    } else {
      currentItems.forEach((item, index) => {
        const itemHtml = this.renderXmlItem(propName, index, item, itemName, attributes);
        itemsContainer.insertAdjacentHTML('beforeend', itemHtml);
      });
    }
    
    // Reattach listeners
    this.attachXmlItemListeners(propDef, propName, rowId, columnId, widgetId);
    
    // Save to widget
    this.saveXmlProperty(propName, propDef, currentItems, rowId, columnId, widgetId);
  }
  
  /**
   * Save XML property to widget data
   */
  saveXmlProperty(propName, propDef, items, rowId, columnId, widgetId) {
    const schema = propDef.schema || {};
    const itemSchema = schema.items || {};
    const itemName = itemSchema.name || 'undefined';
    
    // Convert items array back to XML string
    const xmlString = this.convertXmlItemsToString(items, itemName);
    console.log('Save XML property:', propName, xmlString);
    
    // Save to widget
    const widgetData = this.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
    if (widgetData) {
      widgetData.properties[propName] = xmlString;
      
      // Re-render and save to history
      if (this.editor.getCanvasController) {
        const row = this.editor.getLayoutManager().getRow(rowId);
        this.editor.getCanvasController().renderRow(rowId, row);
        
        // Re-highlight the widget after re-render
        setTimeout(() => {
          const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
          if (rowElement) {
            const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
            if (columnElement) {
              const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
              if (widgetElement) {
                widgetElement.classList.add('selected');
              }
            }
          }
        }, 0);
      }
      
      if (this.editor.saveToHistory) {
        this.editor.saveToHistory();
      }
      
      // Enhancement: Auto-refresh preview when property is edited and preview is active
      // Use a small delay to ensure DOM updates have settled
      setTimeout(() => {
        this.refreshPreviewIfActive();
        this.refreshPreviewIfActive();
      }, 100);
    }
  }
  
  /**
   * Initialize color pickers for any color properties
   */
  initColorPickers(rowId, columnId, widgetId, widgetDef) {
    const colorInputs = this.content.querySelectorAll('input[data-type="color"]');
    const self = this;
    
    colorInputs.forEach(input => {
      $(input).spectrum({
        // color: input.value,
        flat: false,
        preferredFormat: "hex",
        chooseText: "Choose",
        cancelText: "Cancel",
        showPalette: true,
        palette: [
          ["#000","#444","#666","#999","#ccc","#eee","#f3f3f3","#fff"],
          ["#f00","#f90","#ff0","#0f0","#0ff","#00f","#90f","#f0f"],
          ["#f4cccc","#fce5cd","#fff2cc","#d9ead3","#d0e0e3","#cfe2f3","#d9d2e9","#ead1dc"],
          ["#ea9999","#f9cb9c","#ffe599","#b6d7a8","#a2c4c9","#9fc5e8","#b4a7d6","#d5a6bd"],
          ["#e06666","#f6b26b","#ffd966","#93c47d","#76a5af","#6fa8dc","#8e7cc3","#c27ba0"],
          ["#c00","#e69138","#f1c232","#6aa84f","#45818e","#3d85c6","#674ea7","#a64d79"],
          ["#900","#b45f06","#bf9000","#38761d","#134f5c","#0b5394","#351c75","#741b47"],
          ["#600","#783f04","#7f6000","#274e13","#0c343d","#073763","#20124d","#4c1130"]
        ],
        showSelectionPalette: true,
        showInput: true,
        showInitial: true,
        showAlpha: false,
        allowEmpty: true,
        change: function(color) {
          // Handle color change
          const propName = input.id.replace('prop-', '');
          const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
          if (widgetData && widgetDef && widgetDef.properties && widgetDef.properties[propName]) {
            widgetData.properties[propName] = color.toHexString();
            if (self.editor.getCanvasController) {
              const row = self.editor.getLayoutManager().getRow(rowId);
              self.editor.getCanvasController().renderRow(rowId, row);
              // Re-highlight the widget after re-render
              setTimeout(() => {
                const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
                if (rowElement) {
                  const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                  if (columnElement) {
                    const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                    if (widgetElement) {
                      widgetElement.classList.add('selected');
                    }
                  }
                }
              }, 0);
            }
            if (self.editor.saveToHistory) {
              self.editor.saveToHistory();
            }
            
            // Enhancement: Auto-refresh preview when property is edited and preview is active
            self.refreshPreviewIfActive();
          }
        }
      });
    });
  }

  /**
   * Initialize HTML editors (TinyMCE) for any html type properties
   */
  initHtmlEditors(rowId, columnId, widgetId, widgetDef) {
    const htmlFields = this.content.querySelectorAll('textarea.html-editor-field');
    const self = this;
    
    if (!htmlFields || htmlFields.length === 0) return;
    
    // Check if TinyMCE is available
    if (typeof tinymce === 'undefined') {
      console.warn('TinyMCE is not loaded');
      return;
    }
    
    htmlFields.forEach(textarea => {
      const propName = textarea.id.replace('prop-', '');
      
      // Initialize TinyMCE on this textarea
      tinymce.init({
        target: textarea,
        inline: false,
        menubar: false,
        branding: false,
        height: 200,
        plugins: 'fullscreen link lists code',
        toolbar: 'fullscreen | blocks | bold italic underline | alignleft aligncenter alignright | bullist numlist hr | link | code',
        // skin: window.matchMedia("(prefers-color-scheme: dark)").matches
        //   ? "oxide-dark"
        //   : "oxide",
        // content_css: window.matchMedia("(prefers-color-scheme: dark)").matches
        //   ? "dark"
        //   : "default",
        content_style: 'body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen-Sans, Ubuntu, Cantarell, "Helvetica Neue", sans-serif; font-size: 14px; }',
        setup: function(editor) {
          editor.on('change', function() {
            // Update the textarea value
            editor.save();
            
            // Save to widget data
            const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
            if (widgetData && widgetDef && widgetDef.properties && widgetDef.properties[propName]) {
              widgetData.properties[propName] = textarea.value;
              
              if (self.editor.getCanvasController) {
                const row = self.editor.getLayoutManager().getRow(rowId);
                self.editor.getCanvasController().renderRow(rowId, row);
                
                // Re-highlight the widget after re-render
                setTimeout(() => {
                  const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
                  if (rowElement) {
                    const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                    if (columnElement) {
                      const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                      if (widgetElement) {
                        widgetElement.classList.add('selected');
                      }
                    }
                  }
                }, 0);
              }
              
              if (self.editor.saveToHistory) {
                self.editor.saveToHistory();
              }
              
              // Enhancement: Auto-refresh preview when property is edited and preview is active
              self.refreshPreviewIfActive();
            }
          });
          
          // Also handle blur event for when user clicks away
          editor.on('blur', function() {
            editor.save();
          });
        }
      });
    });
  }

  /**
   * Render a property field
   */
  renderPropertyField(name, definition, value, pageLink = 'page') {
    let html = '<div class="property-group">';
    
    // Handle GENERATE special case - generate a unique ID
    let displayValue = value;
    
    // If value is undefined, use the default from definition
    if (value === undefined) {
      if (definition.default === 'GENERATE') {
        displayValue = this.generateUniqueId(pageLink);
      } else if (definition.default !== undefined) {
        displayValue = definition.default;
      } else {
        displayValue = '';
      }
    } else if (value === 'GENERATE') {
      // Generate a unique ID using the page link
      displayValue = this.generateUniqueId(pageLink);
    }
    
    switch (definition.type) {
      case 'text':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(displayValue)}" ${definition.required ? 'required' : ''} />`;
        break;
      
      case 'textarea':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<textarea class="property-input" id="prop-${name}" rows="5" ${definition.required ? 'required' : ''}>${this.escapeHtml(displayValue)}</textarea>`;
        break;
        
      case 'html':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<div id="tinymce-container-${name}" class="tinymce-editor-container"></div>`;
        html += `<textarea class="property-input html-editor-field" id="prop-${name}" rows="5" ${definition.required ? 'required' : ''}>${this.escapeHtml(displayValue)}</textarea>`;
        break;
        
      case 'number':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="number" class="property-input" id="prop-${name}" value="${displayValue}" ${definition.required ? 'required' : ''} />`;
        break;
        
      case 'checkbox': {
        const checked = displayValue === 'true' || displayValue === true ? 'checked' : '';
        html += `<label><input type="checkbox" id="prop-${name}" ${checked} /> ${definition.label}${definition.required ? ' *' : ''}</label>`;
        break;
      }

      case 'color':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="text" class="property-input" data-type="color" id="prop-${name}" value="${this.escapeHtml(displayValue)}" />`;
        break;
        
      case 'select':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<select class="property-input" id="prop-${name}">`;
        if (definition.options) {
          for (const option of definition.options) {
            // Support both simple array format: ['value1', 'value2']
            // and labeled format: [{value: 'value1', label: 'Display Label 1'}, {value: 'value2', label: 'Display Label 2'}]
            const optionValue = typeof option === 'object' ? option.value : option;
            const optionLabel = typeof option === 'object' ? option.label : option;
            const selected = displayValue === optionValue ? 'selected' : '';
            html += `<option value="${optionValue}" ${selected}>${optionLabel}</option>`;
          }
        }
        html += '</select>';
        break;
        
      case 'icon':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<div style="display: flex; gap: 8px; align-items: center;">`;
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(displayValue)}" placeholder="fa-icon" style="flex: 1;" />`;
        html += `<button type="button" class="button tiny radius no-gap" id="pick-icon-${name}" style="white-space: nowrap;">
          <i class="fa fa-icons"></i> Pick
        </button>`;
        if (displayValue) {
          html += `<div id="icon-preview-${name}" style="font-size: 24px; color: #6f6f6f; min-width: 30px; text-align: center;">
            <i class="fa ${this.escapeHtml(displayValue)}"></i>
          </div>`;
        } else {
          html += `<div id="icon-preview-${name}" style="font-size: 24px; color: #6f6f6f; min-width: 30px; text-align: center;"></div>`;
        }
        html += `</div>`;
        break;
      
      case 'contentUniqueId':
        html += this.renderContentUniqueIdProperty(name, definition, displayValue, pageLink);
        break;

      case 'xml':
        html += this.renderXmlProperty(name, definition, value);
        break;
                
      default:
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(displayValue)}" />`;
    }
    
    html += '</div>';
    return html;
  }

  /**
   * Render an XML property with modal-based array management interface
   */
  renderXmlProperty(name, definition, value) {
    let html = '<div class="property-group">';
    html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
    
    // Parse existing items from value
    let items = [];
    if (value && typeof value === 'string') {
      // Try to parse XML string
      items = this.parseXmlArrayFromString(value, definition.schema);
    } else if (Array.isArray(value)) {
      items = value;
    }
    
    // Get schema info
    const schema = definition.schema || {};
    const itemSchema = schema.items || {};
    const itemName = itemSchema.name || 'item';
    
    // Display summary of current values in the panel
    html += `<div id="xml-summary-${name}" style="border:1px solid var(--editor-border);border-radius:4px;padding:10px;margin:10px 0;background:var(--editor-panel-bg);min-height:40px;display:flex;align-items:center;justify-content:space-between;">`;
    
    if (items.length === 0) {
      html += `<div style="color:var(--editor-text-muted);font-style:italic;flex:1;">No ${itemName} entries yet</div>`;
    } else {
      html += `<div style="color:var(--editor-text);flex:1;">`;
      html += `<strong>${items.length}</strong> ${itemName}${items.length === 1 ? '' : 's'} configured`;
      html += `</div>`;
    }
    
    // Edit button to open modal
    html += `<button type="button" id="edit-${name}" class="button small radius no-gap" style="margin-left:10px;">Edit</button>`;
    
    html += `</div>`;
    
    // Hidden storage for array data
    html += `<input type="hidden" id="prop-${name}" data-items-count="${items.length}" />`;
    
    html += '</div>';
    
    return html;
  }

  /**
   * Render a single XML item in the array
   */
  renderXmlItem(propName, index, item, itemName, attributes) {
    let html = `<div class="xml-item" data-item-index="${index}" style="border:1px solid var(--editor-border);border-radius:3px;padding:10px;margin-bottom:10px;background:var(--editor-panel-bg);">`;
    
    // Item header with index and delete button
    html += `<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">`;
    html += `<div style="font-weight:bold;font-size:12px;color:var(--editor-text-muted);">${itemName} #${index + 1}</div>`;
    html += `<button type="button" class="delete-xml-item" data-prop="${propName}" data-index="${index}" style="padding:3px 8px;font-size:11px;background:var(--editor-hover-bg);border:1px solid var(--editor-border);border-radius:3px;cursor:pointer;color:var(--editor-text);">Delete</button>`;
    html += `</div>`;
    
    // Render attribute fields
    html += `<div style="display:grid;gap:10px;">`;
    for (const [attrName, attrDef] of Object.entries(attributes)) {
      const attrValue = (item && item[attrName]) || attrDef.default || '';
      const fieldId = `xml-field-${propName}-${index}-${attrName}`;
      
      html += `<div>`;
      html += `<label style="display:block;font-size:12px;font-weight:bold;margin-bottom:3px;color:var(--editor-text);">
        ${attrDef.label || attrName}${attrDef.required ? ' <span style="color:#dc3545;">*</span>' : ''}
      </label>`;
      
      // Render based on attribute type
      if (attrDef.type === 'select' && attrDef.options) {
        html += `<select id="${fieldId}" class="property-input" data-attr="${attrName}" style="width:100%;">`;
        for (const option of attrDef.options) {
          // Support both simple array format: ['value1', 'value2']
          // and labeled format: [{value: 'value1', label: 'Display Label 1'}, {value: 'value2', label: 'Display Label 2'}]
          const optionValue = typeof option === 'object' ? option.value : option;
          const optionLabel = typeof option === 'object' ? option.label : option;
          const selected = attrValue === optionValue ? 'selected' : '';
          html += `<option value="${optionValue}" ${selected}>${optionLabel}</option>`;
        }
        html += `</select>`;
      } else if (attrDef.type === 'checkbox') {
        const checked = attrValue === 'true' || attrValue === true ? 'checked' : '';
        html += `<label style="display:flex;align-items:center;"><input type="checkbox" id="${fieldId}" class="property-input" data-attr="${attrName}" ${checked} /> <span style="margin-left:5px;font-size:12px;color:var(--editor-text);">${attrDef.checkboxLabel || 'Yes'}</span></label>`;
      } else {
        html += `<input type="text" id="${fieldId}" class="property-input" data-attr="${attrName}" value="${this.escapeHtml(attrValue)}" style="width:100%;" />`;
      }
      html += `</div>`;
    }
    html += `</div>`;
    
    html += `</div>`;
    
    return html;
  }

  /**
   * Parse XML string into array of items
   */
  parseXmlArrayFromString(xmlString, schema) {
    try {
      console.log(`Parsing XML string for schema:`, schema);
      if (!xmlString) return [];
      
      const itemSchema = schema?.items || {};
      const itemName = itemSchema.name || 'item';
      
      // Parse XML
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(`<root>${xmlString}</root>`, 'text/xml');
      
      if (xmlDoc.getElementsByTagName('parsererror').length > 0) {
        console.error('XML Parse error');
        return [];
      }
      
      // Extract items
      const items = [];
      const elements = xmlDoc.getElementsByTagName(itemName);
      
      for (let i = 0; i < elements.length; i++) {
        const element = elements[i];
        const item = {};
        
        // Get all attributes
        for (const attr of element.attributes) {
          item[attr.name] = attr.value;
        }
        
        items.push(item);
      }
      
      return items;
    } catch (e) {
      console.error('Error parsing XML:', e);
      return [];
    }
  }

  /**
   * Convert XML items array back to XML string
   */
  convertXmlItemsToString(items, itemName) {
    if (!items || items.length === 0) return '';
    
    let xml = '';
    for (const item of items) {
      xml += `<${itemName}`;
      for (const [key, value] of Object.entries(item)) {
        // Skip empty attributes
        if (value.length === 0) continue;
        // Append the attribute
        console.log('Converting item attribute:', key, value);
        const escapedValue = String(value).replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        xml += ` ${key}="${escapedValue}"`;
      }
      xml += ` />`;
    }
    
    console.log('Converted XML string:', xml);
    return xml;
  }
  
  /**
   * Generate a unique ID for properties (e.g., content repository IDs)
   */
  generateUniqueId(baseName) {
    const pad = (n) => String(n).padStart(2, '0');
    const d = new Date();
    const ts = d.getFullYear().toString() +
      pad(d.getMonth() + 1) +
      pad(d.getDate()) +
      pad(d.getHours()) +
      pad(d.getMinutes()) +
      pad(d.getSeconds());
    return `${baseName ?? 'id'}-${ts}`;
  }
  
  /**
   * Save row properties
   */
  saveRowProperties(rowId) {
    const marginTopSelect = document.getElementById('row-margin-top');
    const marginBottomSelect = document.getElementById('row-margin-bottom');
    const additionalClasses = document.getElementById('row-css-class')?.value.trim() || '';
    const hrCheckbox = document.getElementById('row-hr');
    
    const marginTop = marginTopSelect?.value || '';
    const marginBottom = marginBottomSelect?.value || '';
    const finalClasses = [marginTop, marginBottom, additionalClasses].filter(c => c).join(' ');
    
    this.editor.getLayoutManager().updateRowClass(rowId, finalClasses);
    
    // Update hr attribute
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (row) {
      row.hr = hrCheckbox && hrCheckbox.checked;
    }
    
    // Re-render the row
    this.editor.getCanvasController().renderRow(rowId, row);
    
    // Save to history and mark as dirty
    this.editor.saveToHistory();
    this.editor.updateSaveIndicator();
  }
  
  /**
   * Save column properties
   */
  saveColumnProperties(rowId, columnId) {
    const small = document.getElementById('column-size-small')?.value || '';
    const medium = document.getElementById('column-size-medium')?.value || '';
    const large = document.getElementById('column-size-large')?.value || '';
    const paddingTop = document.getElementById('column-padding-top')?.value || '';
    const paddingBottom = document.getElementById('column-padding-bottom')?.value || '';
    const callout = document.getElementById('column-callout')?.value || '';
    const alignment = document.getElementById('column-alignment')?.value || '';
    const text = document.getElementById('column-text')?.value || '';
    const custom = document.getElementById('column-css-custom')?.value.trim() || '';
    const hrCheckbox = document.getElementById('column-hr');
    
    const classes = [small, medium, large, paddingTop, paddingBottom, callout, alignment, text, custom].filter(c => c);
    const finalCssClass = classes.join(' ');
    
    this.editor.getLayoutManager().updateColumnClass(rowId, columnId, finalCssClass);
    
    // Update hr attribute
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (row) {
      const column = row.columns.find(c => c.id === columnId);
      if (column) {
        column.hr = hrCheckbox && hrCheckbox.checked;
      }
    }
    
    // Re-render the row
    this.editor.getCanvasController().renderRow(rowId, row);
    
    // Save to history and mark as dirty
    this.editor.saveToHistory();
    this.editor.updateSaveIndicator();
  }
  
  /**
   * Save widget properties
   */
  saveWidgetProperties(rowId, columnId, widgetId) {
    const widget = this.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
    if (!widget) return;
    
    const definition = this.editor.widgetRegistry.get(widget.type);
    if (!definition) return;
    
    // Collect property values
    const properties = {};
    
    for (const propName of Object.keys(definition.properties)) {
      const propDef = definition.properties[propName];
      const element = document.getElementById(`prop-${propName}`);
      
      if (!element) continue;
      
      if (propDef.type === 'xml') {
        // For XML properties, extract items from UI and convert to XML string
        const schema = propDef.schema || {};
        const itemSchema = schema.items || {};
        const itemName = itemSchema.name || 'item';
        const attributes = itemSchema.attributes || {};
        
        const items = this.getXmlItemsFromUI(propName, itemName, attributes);
        properties[propName] = this.convertXmlItemsToString(items, itemName);
        console.log('Save XML widget property', propName, properties[propName]);
      } else if (propDef.type === 'checkbox') {
        properties[propName] = element.checked ? 'true' : 'false';
      } else {
        properties[propName] = element.value;
      }
    }
    
    // Update widget properties
    this.editor.getLayoutManager().updateWidgetProperties(rowId, columnId, widgetId, properties);
    
    // Update widget CSS class and hr attribute
    const cssClassInput = document.getElementById('widget-css-class');
    const hrCheckbox = document.getElementById('widget-hr');
    const widgetData = this.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
    if (widgetData) {
      if (cssClassInput) {
        widgetData.cssClass = cssClassInput.value.trim();
      }
      if (hrCheckbox) {
        widgetData.hr = hrCheckbox.checked;
      }
    }
    
    // Re-render the row
    const row = this.editor.getLayoutManager().getRow(rowId);
    this.editor.getCanvasController().renderRow(rowId, row);
    
    // Save to history and mark as dirty
    this.editor.saveToHistory();
    this.editor.updateSaveIndicator();
  }
  
  /**
   * Highlight a row in the canvas
   */
  highlightRow(rowId) {
    // Remove any previous highlighting
    this.clearHighlight();
    
    const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
    if (rowElement) {
      rowElement.classList.add('selected');
      // Scroll into view
      rowElement.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }
  
  /**
   * Highlight a column in the canvas
   */
  highlightColumn(rowId, columnId) {
    // Remove any previous highlighting
    this.clearHighlight();
    
    const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
    const columnElement = rowElement ? rowElement.querySelector(`[data-column-id="${columnId}"]`) : null;
    if (columnElement) {
      columnElement.classList.add('selected');
      // Scroll into view
      columnElement.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }
  
  /**
   * Clear highlighting from row and columns elements
   */
  clearHighlight() {
    document.querySelectorAll('.canvas-row.selected, .canvas-column.selected').forEach(el => {
      el.classList.remove('selected');
    });
  }
  
  /**
   * Clear properties panel
   */
  clear() {
    // Clean up TinyMCE editors before clearing content
    this.cleanupHtmlEditors();
    
    this.currentContext = null;
    this.clearHighlight();
    this.content.innerHTML = '<p style="color: var(--editor-text-muted); font-size: 14px;">Select an element to edit its properties</p>';

    if (typeof globalThis !== 'undefined' && globalThis.previewHoverManager && typeof globalThis.previewHoverManager.clearSelection === 'function') {
      globalThis.previewHoverManager.clearSelection();
    }
    
    // Restore to previous non-properties tab when clearing
    if (this.rightPanelTabs) {
      this.rightPanelTabs.restorePreviousTab();
    }
  }
  
  /**
   * Cleanup TinyMCE editors
   */
  cleanupHtmlEditors() {
    if (typeof tinymce !== 'undefined') {
      const htmlFields = this.content.querySelectorAll('textarea.html-editor-field');
      htmlFields.forEach(textarea => {
        const editor = tinymce.get(textarea.id);
        if (editor) {
          editor.remove();
        }
      });
    }
  }
  
  /**
   * Render contentUniqueId property with content repository integration
   */
  renderContentUniqueIdProperty(name, definition, value, pageLink) {
    let html = '<div class="property-group">';
    html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
    html += `<div style="display:flex;gap:5px;margin-bottom:5px;">`;
    html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(value)}" style="flex:1;" />`;
    html += `<button type="button" class="button small radius" id="browse-content-${name}" style="white-space:nowrap;">Browse</button>`;
    html += `<button type="button" class="button small radius" id="edit-content-${name}" style="white-space:nowrap;display:none;">Edit</button>`;
    html += `</div>`;
    html += `</div>`;
    
    return html;
  }
  
  /**
   * Initialize contentUniqueId property handlers
   */
  initContentUniqueIdProperties(rowId, columnId, widgetId, widgetDef) {
    const self = this;
    Object.entries(widgetDef.properties).forEach(([propName, propDef]) => {
      if (propDef.type === 'contentUniqueId') {
        const input = document.getElementById(`prop-${propName}`);
        const browseBtn = document.getElementById(`browse-content-${propName}`);
        const editBtn = document.getElementById(`edit-content-${propName}`);
        
        if (!input || !browseBtn) {
          console.log('Missing required elements for property:', propName);
          return;
        }
        
        // Browse button - show content list modal
        browseBtn.addEventListener('click', () => {
          self.showContentBrowserModal(propName, propDef, rowId, columnId, widgetId);
        });
        
        // Edit button - open content editor
        if (editBtn) {
          editBtn.addEventListener('click', () => {
            const uniqueId = input.value;
            if (uniqueId) {
              self.openContentEditor(uniqueId);
            }
          });
        } else {
          console.log('Edit button not found for property:', propName);
        }
        
        // Show edit button if value exists
        input.addEventListener('input', () => {
          if (input.value && input.value.trim()) {
            if (editBtn) editBtn.style.display = 'inline-block';
          } else {
            if (editBtn) editBtn.style.display = 'none';
          }
        });
        
        // Initial state
        if (input.value && input.value.trim()) {
          if (editBtn) editBtn.style.display = 'inline-block';
        }
      }
    });
  }
  
  /**
   * Show content browser modal
   */
  showContentBrowserModal(propName, propDef, rowId, columnId, widgetId) {
    // Create modal if it doesn't exist
    let modal = document.getElementById('content-browser-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'content-browser-modal';
      modal.className = 'modal-overlay';
      modal.innerHTML = `
        <div class="modal-content" style="max-width:800px;">
          <h4 style="color:var(--editor-text);margin: 0 0 15px 0; flex-shrink: 0;">Select a Content Item</h4>
          <div id="content-browser-list" style="flex: 1; overflow-y: auto; border: 1px solid #ddd; border-radius: 4px; padding: 15px; background: #f9f9f9; min-height: 200px;">
            <div style="text-align:center;padding:40px;color:var(--editor-text-muted);">
              <i class="fa fa-spinner fa-spin"></i> Loading content...
            </div>
          </div>
          <div style="text-align: right; margin-top: 15px; display: flex; gap: 10px; justify-content: flex-end; flex-shrink: 0;">
            <button type="button" class="button tiny secondary radius" id="close-content-browser-modal">Cancel</button>
          </div>
        </div>
      `;
      document.body.appendChild(modal);
      
      // Close button
      document.getElementById('close-content-browser-modal').addEventListener('click', () => {
        modal.classList.remove('active');
        // Remove escape key listener when modal is closed
        if (modal.escapeKeyHandler) {
          document.removeEventListener('keydown', modal.escapeKeyHandler, true);
          console.log('Content browser - Escape key listener removed');
        }
      });
      
      // Close on overlay click
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          modal.classList.remove('active');
          // Remove escape key listener when modal is closed
          if (modal.escapeKeyHandler) {
            document.removeEventListener('keydown', modal.escapeKeyHandler, true);
            console.log('Content browser - Escape key listener removed');
          }
        }
      });
    }
    
    // Load content list
    this.loadContentList(propName, propDef, rowId, columnId, widgetId);
    
    // Show modal
    modal.classList.add('active');
    
    // Add escape key listener when modal is shown
    modal.escapeKeyHandler = (e) => {
      console.log('Content browser - Key pressed:', e.key, 'Modal visible:', modal.classList.contains('active'));
      if (e.key === 'Escape' || e.keyCode === 27) {
        e.preventDefault();
        e.stopPropagation();
        console.log('Content browser - Escape key detected, hiding modal');
        modal.classList.remove('active');
        // Remove escape key listener
        if (modal.escapeKeyHandler) {
          document.removeEventListener('keydown', modal.escapeKeyHandler, true);
          console.log('Content browser - Escape key listener removed');
        }
      }
    };
    document.addEventListener('keydown', modal.escapeKeyHandler, true);
  }
  
  /**
   * Load content list from server
   */
  loadContentList(propName, propDef, rowId, columnId, widgetId) {
    const listContainer = document.getElementById('content-browser-list');
    listContainer.innerHTML = '<div style="text-align:center;padding:40px;color:var(--editor-text-muted);"><i class="fa fa-spinner fa-spin"></i> Loading content...</div>';
    
    // Fetch content list from server
    fetch('/json/contentList', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    .then(response => response.json())
    .then(data => {
      if (data && data.contentList && Array.isArray(data.contentList)) {
        this.renderContentList(data.contentList, propName, propDef, rowId, columnId, widgetId);
      } else {
        listContainer.innerHTML = '<div style="padding:20px;color:var(--editor-text-muted);">No content available</div>';
      }
    })
    .catch(error => {
      console.error('Error loading content list:', error);
      listContainer.innerHTML = '<div style="padding:20px;color:#dc3545;">Error loading content list</div>';
    });
  }
  
  /**
   * Render content list in modal
   */
  renderContentList(contentList, propName, propDef, rowId, columnId, widgetId) {
    const listContainer = document.getElementById('content-browser-list');
    const self = this;
    
    if (contentList.length === 0) {
      listContainer.innerHTML = '<div style="padding:20px;color:var(--editor-text-muted);">No content available</div>';
      return;
    }
    
    let html = '<div style="display:grid;gap:10px;">';
    contentList.forEach(content => {
      const uniqueId = content.uniqueId || content.contentUniqueId || '';
      const contentSnippet = content.content || uniqueId || 'No content';
      html += `
        <div class="content-item" data-unique-id="${this.escapeHtml(uniqueId)}" style="
          padding:12px;
          border:1px solid var(--editor-border);
          border-radius:4px;
          cursor:pointer;
          background:var(--editor-bg);
          transition:all 0.2s;
        ">
          <div style="font-weight:600;color:var(--editor-text);margin-bottom:5px;">${this.escapeHtml(uniqueId)}</div>
          <div style="font-size:12px;color:var(--editor-text-muted);">${this.escapeHtml(contentSnippet)}</div>
        </div>
      `;
    });
    html += '</div>';
    
    listContainer.innerHTML = html;
    
    // Add click handlers
    listContainer.querySelectorAll('.content-item').forEach(item => {
      item.addEventListener('click', () => {
        const uniqueId = item.getAttribute('data-unique-id');
        const input = document.getElementById(`prop-${propName}`);
        const editBtn = document.getElementById(`edit-content-${propName}`);
        
        if (input) {
          input.value = uniqueId;
          if (editBtn) {
            editBtn.style.display = 'inline-block';
          }
          
          // Update widget property
          const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
          if (widgetData) {
            widgetData.properties[propName] = uniqueId;
            if (self.editor.getCanvasController) {
              const row = self.editor.getLayoutManager().getRow(rowId);
              self.editor.getCanvasController().renderRow(rowId, row);
              setTimeout(() => {
                const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
                if (rowElement) {
                  const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                  if (columnElement) {
                    const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                    if (widgetElement) {
                      widgetElement.classList.add('selected');
                    }
                  }
                }
              }, 0);
            }
            if (self.editor.saveToHistory) {
              self.editor.saveToHistory();
            }
            
            // Enhancement: Auto-refresh preview when property is edited and preview is active
            self.refreshPreviewIfActive();
          }
        }
        
        // Close modal
        const contentModal = document.getElementById('content-browser-modal');
        contentModal.classList.remove('active');
        // Remove escape key listener when modal is closed
        if (contentModal.escapeKeyHandler) {
          document.removeEventListener('keydown', contentModal.escapeKeyHandler, true);
          console.log('Content browser - Escape key listener removed');
        }
      });
      
      item.addEventListener('mouseenter', () => {
        item.style.borderColor = 'var(--editor-selected-border)';
        item.style.background = 'var(--editor-hover-bg)';
      });
      
      item.addEventListener('mouseleave', () => {
        item.style.borderColor = 'var(--editor-border)';
        item.style.background = 'var(--editor-bg)';
      });
    });
  }
  
  /**
   * Open content editor for a uniqueId
   */
  openContentEditor(uniqueId) {
    // Get the currently selected web page
    const webPageLink = window.pageEditor && window.pageEditor.pagesTabManager 
      ? window.pageEditor.pagesTabManager.getSelectedPageLink() 
      : '/';
    
    // Open content editor in the preview iframe
    const url = `/content-editor?uniqueId=${encodeURIComponent(uniqueId)}&returnPage=${encodeURIComponent(webPageLink)}`;
    
    // Use the global openPageInIframe function if available
    if (typeof window.openPageInIframe === 'function') {
      window.openPageInIframe(url, 'Loading content editor...');
    } else {
      console.log('window.openPageInIframe function not available, falling back to window.open');
      // Fallback to opening in new window if function not available
      window.open(url, '_blank');
    }
  }
  
  /**
   * Initialize icon pickers for any icon properties
   */
  initIconPickers(rowId, columnId, widgetId, widgetDef) {
    const self = this;
    
    // Find all icon property pick buttons
    Object.entries(widgetDef.properties).forEach(([propName, propDef]) => {
      if (propDef.type === 'icon') {
        const pickBtn = document.getElementById(`pick-icon-${propName}`);
        const input = document.getElementById(`prop-${propName}`);
        const preview = document.getElementById(`icon-preview-${propName}`);
        
        if (pickBtn && input) {
          pickBtn.addEventListener('click', (e) => {
            e.preventDefault();
            
            // Show icon picker modal
            if (window.iconPickerModal) {
              window.iconPickerModal.show((selectedIcon) => {
                // Update input value
                input.value = selectedIcon;
                
                // Update preview
                if (preview) {
                  if (selectedIcon) {
                    preview.innerHTML = `<i class="fa ${selectedIcon}"></i>`;
                    preview.style.color = '#6f6f6f';
                  } else {
                    preview.innerHTML = '';
                    preview.style.color = '#6f6f6f';
                  }
                }
                
                // Save to widget
                const widgetData = self.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
                if (widgetData) {
                  widgetData.properties[propName] = selectedIcon;
                  
                  if (self.editor.getCanvasController) {
                    const row = self.editor.getLayoutManager().getRow(rowId);
                    self.editor.getCanvasController().renderRow(rowId, row);
                    
                    // Re-highlight the widget after re-render
                    setTimeout(() => {
                      const rowElement = document.querySelector(`[data-row-id="${rowId}"]`);
                      if (rowElement) {
                        const columnElement = rowElement.querySelector(`[data-column-id="${columnId}"]`);
                        if (columnElement) {
                          const widgetElement = columnElement.querySelector(`[data-widget-id="${widgetId}"]`);
                          if (widgetElement) {
                            widgetElement.classList.add('selected');
                          }
                        }
                      }
                    }, 0);
                  }
                  
                  if (self.editor.saveToHistory) {
                    self.editor.saveToHistory();
                  }
                  
                  // Auto-refresh preview when property is edited and preview is active
                  self.refreshPreviewIfActive();
                }
              }, input.value);
            }
          });
          
          // Also update preview when user types manually
          input.addEventListener('input', () => {
            if (preview) {
              const iconClass = input.value.trim();
              if (iconClass) {
                preview.innerHTML = `<i class="fa ${iconClass}"></i>`;
                preview.style.color = '#333';
              } else {
                preview.innerHTML = '';
                preview.style.color = '#ccc';
              }
            }
          });
        }
      }
    });
  }
  
  /**
   * Escape HTML special characters
   */
  escapeHtml(str) {
    if (typeof str !== 'string') return str;
    
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
  
  /**
   * Enhancement: Auto-refresh preview when property is edited and preview is active
   * Respects the skipPreviewRefresh flag set during show() to avoid refreshing on initial selection
   */
  refreshPreviewIfActive() {
    // If skipPreviewRefresh was set during show(), don't auto-refresh
    if (this.skipPreviewRefresh) {
      console.debug('PropertiesPanel: Skipping preview refresh (skipPreviewRefresh flag set)');
      this.skipPreviewRefresh = false; // Reset flag for next time
      return;
    }
    
    // Check if preview mode is active by looking at the toggle button state
    const previewStateGroup = document.getElementById('preview-state-group');
    const previewState = previewStateGroup && previewStateGroup.dataset ? previewStateGroup.dataset.previewState : null;
    const isPreviewEnabled = previewState ? previewState !== 'layout' : false;
    if (isPreviewEnabled) {
      // Call the global refreshPreview function if it exists
      if (typeof window.refreshPreview === 'function') {
        window.refreshPreview();
      }
    }
  }
}
