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
   * Show properties for selected element
   */
  show(context) {
    this.currentContext = context;
    
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
    html += `<div style="border-bottom:1px solid #ddd;padding-bottom:10px;margin-bottom:10px;">
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
      <div style="font-size:12px;color:#666;margin-top:5px;">Any additional custom classes (space-separated)</div>
    </div>`;
    
    // CSS preview
    html += `<div class="property-group" style="background:#f5f5f5;padding:10px;border-radius:4px;margin:10px 0;">
      <div style="font-weight:bold;font-size:12px;margin-bottom:5px;">Applied Classes</div>
      <div id="row-css-preview" style="font-family:monospace;font-size:11px;word-break:break-all;color:#333;max-height:80px;overflow-y:auto;"></div>
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
      }
    };
    
    const self = this;
    if (marginTopSelect) marginTopSelect.addEventListener('input', function() { updatePreview.call(self); });
    if (marginBottomSelect) marginBottomSelect.addEventListener('input', function() { updatePreview.call(self); });
    if (additionalClassesInput) additionalClassesInput.addEventListener('input', function() { updatePreview.call(self); });
    
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
    html += `<div style="border-bottom:1px solid #ddd;padding-bottom:10px;margin-bottom:10px;">
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
    html += `<div style="border-bottom:1px solid #ddd;padding-bottom:10px;margin-bottom:10px;">
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
    html += `<div style="border-bottom:1px solid #ddd;padding-bottom:10px;margin-bottom:10px;">
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
    html += `<div style="border-bottom:1px solid #ddd;padding-bottom:10px;margin-bottom:10px;">
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
    html += `<div style="border-bottom:1px solid #ddd;padding-bottom:10px;margin-bottom:10px;">
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
      <div style="font-size:12px;color:#666;margin-top:5px;">Any additional custom classes (space-separated)</div>
    </div>`;
    
    // CSS preview
    html += `<div class="property-group" style="background:#f5f5f5;padding:10px;border-radius:4px;margin:10px 0;">
      <div style="font-weight:bold;font-size:12px;margin-bottom:5px;">Applied Classes</div>
      <div id="column-css-preview" style="font-family:monospace;font-size:11px;word-break:break-all;color:#333;max-height:100px;overflow-y:auto;"></div>
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
      }
    };
    
    ['column-size-small', 'column-size-medium', 'column-size-large', 'column-padding-top', 'column-padding-bottom', 'column-callout', 'column-alignment', 'column-text', 'column-css-custom'].forEach(id => {
      const el = document.getElementById(id);
      if (el) {
        el.addEventListener('change', function() { updatePreview(); });
        el.addEventListener('input', function() { updatePreview(); });
      }
    });
    
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
    // Clear any previously highlighted row or column
    this.clearHighlight();
    
    const widget = this.editor.getLayoutManager().getWidget(rowId, columnId, widgetId);
    if (!widget) return;
    
    const definition = this.editor.widgetRegistry.get(widget.type);
    if (!definition) {
      this.content.innerHTML = '<p>Widget definition not found</p>';
      return;
    }
    
    let html = `<h6>${definition.name} Properties</h6>`;
    
    // Render property fields
    for (const [propName, propDef] of Object.entries(definition.properties)) {
      html += this.renderPropertyField(propName, propDef, widget.properties[propName] || '');
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
        }
      });
    }

    // Initialize color pickers
    this.initColorPickers(rowId, columnId, widgetId, definition);
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
          }
        }
      });
    });
  }

  /**
   * Render a property field
   */
  renderPropertyField(name, definition, value) {
    let html = '<div class="property-group">';
    
    switch (definition.type) {
      case 'text':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(value)}" ${definition.required ? 'required' : ''} />`;
        break;
      
      case 'color':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="text" class="property-input" data-type="color" id="prop-${name}" value="${this.escapeHtml(value)}" />`;
        break;

      case 'textarea':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<textarea class="property-input" id="prop-${name}" rows="5" ${definition.required ? 'required' : ''}>${this.escapeHtml(value)}</textarea>`;
        break;
        
      case 'number':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="number" class="property-input" id="prop-${name}" value="${value}" ${definition.required ? 'required' : ''} />`;
        break;
        
      case 'checkbox':
        const checked = value === 'true' || value === true ? 'checked' : '';
        html += `<label><input type="checkbox" id="prop-${name}" ${checked} /> ${definition.label}${definition.required ? ' *' : ''}</label>`;
        break;
        
      case 'select':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<select class="property-input" id="prop-${name}">`;
        if (definition.options) {
          for (const option of definition.options) {
            const selected = value === option ? 'selected' : '';
            html += `<option value="${option}" ${selected}>${option}</option>`;
          }
        }
        html += '</select>';
        break;
        
      case 'links':
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += '<div style="font-size:12px;color:#666;margin-top:5px;">Links management coming soon</div>';
        break;
        
      default:
        html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(value)}" />`;
    }
    
    html += '</div>';
    return html;
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
      
      if (propDef.type === 'checkbox') {
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
   * Clear highlighting from all elements
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
    this.currentContext = null;
    this.clearHighlight();
    this.content.innerHTML = '<p style="color: #6c757d; font-size: 14px;">Select an element to edit its properties</p>';
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
}
