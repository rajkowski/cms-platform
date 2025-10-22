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
    
    this.content.innerHTML = `
      <h6>Row Properties</h6>
      <div class="property-group">
        <div class="property-label">CSS Class</div>
        <input type="text" class="property-input" id="row-css-class" value="${row.cssClass || ''}" placeholder="e.g., align-center" />
      </div>
      <button class="button tiny primary expanded" onclick="window.pageEditor.getPropertiesPanel().saveRowProperties('${rowId}')">
        Apply Changes
      </button>
    `;
  }
  
  /**
   * Show column properties
   */
  showColumnProperties(rowId, columnId) {
    const row = this.editor.getLayoutManager().getRow(rowId);
    if (!row) return;
    
    const column = row.columns.find(c => c.id === columnId);
    if (!column) return;
    
    this.content.innerHTML = `
      <h6>Column Properties</h6>
      <div class="property-group">
        <div class="property-label">CSS Class</div>
        <input type="text" class="property-input" id="column-css-class" value="${column.cssClass || ''}" placeholder="e.g., small-12 medium-6" />
      </div>
      <button class="button tiny primary expanded" onclick="window.pageEditor.getPropertiesPanel().saveColumnProperties('${rowId}', '${columnId}')">
        Apply Changes
      </button>
    `;
  }
  
  /**
   * Show widget properties
   */
  showWidgetProperties(rowId, columnId, widgetId) {
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
    
    html += `
      <button class="button tiny primary expanded" onclick="window.pageEditor.getPropertiesPanel().saveWidgetProperties('${rowId}', '${columnId}', '${widgetId}')">
        Apply Changes
      </button>
    `;
    
    this.content.innerHTML = html;

    // Initialize color pickers
    this.initColorPickers();
  }
  
  /**
   * Initialize color pickers for any color properties
   */
  initColorPickers() {
    const colorInputs = this.content.querySelectorAll('input[data-type="color"]');
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
        allowEmpty: true
      });
    });
  }

  /**
   * Render a property field
   */
  renderPropertyField(name, definition, value) {
    let html = '<div class="property-group">';
    html += `<div class="property-label">${definition.label}${definition.required ? ' *' : ''}</div>`;
    
    switch (definition.type) {
      case 'text':
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(value)}" ${definition.required ? 'required' : ''} />`;
        break;
      
      case 'color':
        html += `<input type="text" class="property-input" data-type="color" id="prop-${name}" value="${this.escapeHtml(value)}" />`;
        break;

      case 'textarea':
        html += `<textarea class="property-input" id="prop-${name}" rows="5" ${definition.required ? 'required' : ''}>${this.escapeHtml(value)}</textarea>`;
        break;
        
      case 'number':
        html += `<input type="number" class="property-input" id="prop-${name}" value="${value}" ${definition.required ? 'required' : ''} />`;
        break;
        
      case 'checkbox':
        const checked = value === 'true' || value === true ? 'checked' : '';
        html += `<label><input type="checkbox" id="prop-${name}" ${checked} /> ${definition.label}</label>`;
        break;
        
      case 'select':
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
        html += '<div style="font-size:12px;color:#666;margin-top:5px;">Links management coming soon</div>';
        break;
        
      default:
        html += `<input type="text" class="property-input" id="prop-${name}" value="${this.escapeHtml(value)}" />`;
    }
    
    html += '</div>';
    return html;
  }
  
  /**
   * Save row properties
   */
  saveRowProperties(rowId) {
    const cssClass = document.getElementById('row-css-class').value;
    
    this.editor.getLayoutManager().updateRowClass(rowId, cssClass);
    
    // Re-render the row
    const row = this.editor.getLayoutManager().getRow(rowId);
    this.editor.getCanvasController().renderRow(rowId, row);
    
    // Save to history
    this.editor.saveToHistory();
    
    alert('Row properties saved');
  }
  
  /**
   * Save column properties
   */
  saveColumnProperties(rowId, columnId) {
    const cssClass = document.getElementById('column-css-class').value;
    
    this.editor.getLayoutManager().updateColumnClass(rowId, columnId, cssClass);
    
    // Re-render the row
    const row = this.editor.getLayoutManager().getRow(rowId);
    this.editor.getCanvasController().renderRow(rowId, row);
    
    // Save to history
    this.editor.saveToHistory();
    
    alert('Column properties saved');
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
    
    // Re-render the row
    const row = this.editor.getLayoutManager().getRow(rowId);
    this.editor.getCanvasController().renderRow(rowId, row);
    
    // Save to history
    this.editor.saveToHistory();
    
    alert('Widget properties saved');
  }
  
  /**
   * Clear properties panel
   */
  clear() {
    this.currentContext = null;
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
