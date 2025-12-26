/**
 * Layout Manager
 * Manages the page structure (rows, columns, widgets)
 * 
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */

class LayoutManager {
  constructor(editor) {
    this.editor = editor;
    this.structure = {
      rows: []
    };
    this.nextRowId = 1;
    this.nextColumnId = 1;
    this.nextWidgetId = 1;
  }
  
  /**
   * Add a new row with specified column layout
   * @param {Array} columnClasses Array of CSS classes for each column
   * @param {string|null} targetRowId Optional ID of the row to insert before
   * @returns {string} Row ID
   */
  addRow(columnClasses, targetRowId = null) {
    const rowId = 'row-' + (this.nextRowId++);
    
    const row = {
      id: rowId,
      cssClass: '',
      columns: columnClasses.map(cssClass => ({
        id: 'col-' + (this.nextColumnId++),
        cssClass: cssClass + ' cell',
        widgets: []
      }))
    };
    
    if (targetRowId) {
      const targetIndex = this.structure.rows.findIndex(r => r.id === targetRowId);
      if (targetIndex !== -1) {
        this.structure.rows.splice(targetIndex, 0, row);
      } else {
        this.structure.rows.push(row);
      }
    } else {
      this.structure.rows.push(row);
    }
    
    console.log('Row added:', rowId);
    
    return rowId;
  }
  
  /**
   * Remove a row
   */
  removeRow(rowId) {
    const index = this.structure.rows.findIndex(r => r.id === rowId);
    if (index !== -1) {
      this.structure.rows.splice(index, 1);
      console.log('Row removed:', rowId);
      return true;
    }
    return false;
  }
  
  /**
   * Get a row by ID
   */
  getRow(rowId) {
    return this.structure.rows.find(r => r.id === rowId);
  }
  
  /**
   * Update row CSS class
   */
  updateRowClass(rowId, cssClass) {
    const row = this.getRow(rowId);
    if (row) {
      row.cssClass = cssClass;
      return true;
    }
    return false;
  }

  /**
   * Move a row to a new position
   */
  moveRow(draggedRowId, targetRowId) {
    const draggedIndex = this.structure.rows.findIndex(r => r.id === draggedRowId);
    if (draggedIndex === -1) {
      console.error('Dragged row not found');
      return false;
    }

    const [draggedRow] = this.structure.rows.splice(draggedIndex, 1);

    if (targetRowId) {
      const targetIndex = this.structure.rows.findIndex(r => r.id === targetRowId);
      if (targetIndex !== -1) {
        this.structure.rows.splice(targetIndex, 0, draggedRow);
      } else {
        // If target is not found (e.g., it was the placeholder), append to the end
        this.structure.rows.push(draggedRow);
      }
    } else {
      // If no target, move to the end
      this.structure.rows.push(draggedRow);
    }
    
    console.log(`Row ${draggedRowId} moved`);
    return true;
  }

  /**
   * Move a row to the end of the list
   */
  moveRowToEnd(draggedRowId) {
    const draggedIndex = this.structure.rows.findIndex(r => r.id === draggedRowId);
    if (draggedIndex === -1) return false;

    const draggedRow = this.structure.rows.splice(draggedIndex, 1)[0];
    this.structure.rows.push(draggedRow);
    
    console.log(`Row ${draggedRowId} moved to end`);
    return true;
  }
  
  /**
   * Add a widget to a column
   */
  addWidget(rowId, columnId, widgetType, targetWidgetId = null) {
    const row = this.getRow(rowId);
    if (!row) return null;
    
    const column = row.columns.find(c => c.id === columnId);
    if (!column) return null;
    
    const widgetId = 'widget-' + (this.nextWidgetId++);
    const widget = {
      id: widgetId,
      type: widgetType,
      properties: this.getDefaultProperties(widgetType)
    };
    
    if (targetWidgetId) {
      const targetIndex = column.widgets.findIndex(w => w.id === targetWidgetId);
      if (targetIndex !== -1) {
        column.widgets.splice(targetIndex, 0, widget);
      } else {
        column.widgets.push(widget);
      }
    } else {
      column.widgets.push(widget);
    }
    
    console.log('Widget added:', widgetId, widgetType);
    
    return widgetId;
  }
  
  /**
   * Move a widget from one column to another
   */
  moveWidget(sourceRowId, sourceColumnId, widgetId, targetRowId, targetColumnId, targetWidgetId = null) {
    const sourceRow = this.getRow(sourceRowId);
    if (!sourceRow) return false;

    const sourceColumn = sourceRow.columns.find(c => c.id === sourceColumnId);
    if (!sourceColumn) return false;

    const widgetIndex = sourceColumn.widgets.findIndex(w => w.id === widgetId);
    if (widgetIndex === -1) return false;

    // Remove from source
    const [widget] = sourceColumn.widgets.splice(widgetIndex, 1);

    // Add to target
    const targetRow = this.getRow(targetRowId);
    if (!targetRow) return false;

    const targetColumn = targetRow.columns.find(c => c.id === targetColumnId);
    if (!targetColumn) return false;

    if (targetWidgetId) {
      const targetIndex = targetColumn.widgets.findIndex(w => w.id === targetWidgetId);
      if (targetIndex !== -1) {
        targetColumn.widgets.splice(targetIndex, 0, widget);
      } else {
        targetColumn.widgets.push(widget);
      }
    } else {
      targetColumn.widgets.push(widget);
    }

    console.log(`Widget ${widgetId} moved from ${sourceColumnId} to ${targetColumnId}`);
    return true;
  }

  /**
   * Remove a widget
   */
  removeWidget(rowId, columnId, widgetId) {
    const row = this.getRow(rowId);
    if (!row) return false;
    
    const column = row.columns.find(c => c.id === columnId);
    if (!column) return false;
    
    const index = column.widgets.findIndex(w => w.id === widgetId);
    if (index !== -1) {
      column.widgets.splice(index, 1);
      console.log('Widget removed:', widgetId);
      return true;
    }
    
    return false;
  }
  
  /**
   * Get a widget
   */
  getWidget(rowId, columnId, widgetId) {
    const row = this.getRow(rowId);
    if (!row) return null;
    
    const column = row.columns.find(c => c.id === columnId);
    if (!column) return null;
    
    return column.widgets.find(w => w.id === widgetId);
  }
  
  /**
   * Update widget properties
   */
  updateWidgetProperties(rowId, columnId, widgetId, properties) {
    const widget = this.getWidget(rowId, columnId, widgetId);
    if (widget) {
      widget.properties = { ...widget.properties, ...properties };
      return true;
    }
    return false;
  }
  
  /**
   * Update column CSS class
   */
  updateColumnClass(rowId, columnId, cssClass) {
    const row = this.getRow(rowId);
    if (!row) return false;
    
    const column = row.columns.find(c => c.id === columnId);
    if (column) {
      column.cssClass = cssClass;
      return true;
    }
    return false;
  }
  
  /**
   * Get default properties for a widget type
   */
  getDefaultProperties(widgetType) {
    const defaults = {
      content: {
        uniqueId: this.generateUniqueId('content'),
        html: '<p>Content</p>'
      },
      contentAccordion: {
        uniqueId: this.generateUniqueId('accordion'),
        html: '<h1>Title</h1><p>&gt; Item to expand</p><p>The item content goes here...</p><hr /><p>&gt; Item to expand</p><p>The item content goes here...</p><hr />'
      },
      contentCards: {
        uniqueId: this.generateUniqueId('cards'),
        html: '<p>Multiple card content separated by HR</p>'
      },
      contentSlider: {
        uniqueId: this.generateUniqueId('slider'),
        html: '<p>Multiple slide content separated by HR</p>',
        showControls: 'true',
        showPagination: 'true',
        loop: 'false',
        autoplayDelay: '5000'
      },
      breadcrumbs: {
        links: []
      },
      menu: {
        class: 'vertical',
        showWhenEmpty: 'false',
        links: []
      },
      button: {
        name: 'Click Me',
        link: '#',
        buttonClass: ''
      },
      calendar: {
        defaultView: 'month',
        view: 'default',
        height: '',
        showEvents: 'true',
        showHolidays: 'true',
        showMoodleEvents: 'false',
        moodleTextColor: '#000000',
        moodleBackgroundColor: '#ffffff'
      },
      upcomingCalendarEvents: {
        view: 'list',
        showWhenEmpty: 'true',
        daysToShow: '-1',
        monthsToShow: '1',
        showMonthName: 'true',
        showEventLink: 'true',
        includeLastEvent: 'false',
        limit: '-1'
      },
      card: {
        title: 'Card Title',
        icon: '',
        link: '',
        linkTitle: '',
        class: ''
      },
      statisticCard: {
        label: 'Statistic',
        value: '0',
        icon: 'chart-bar',
        link: '',
        iconColor: 'theme.body.text.color',
        view: 'default'
      }
    };
    
    return defaults[widgetType] || {};
  }
  
  /**
   * Generate unique ID for content
   */
  generateUniqueId(prefix) {
    return prefix + '-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
  }
  
  /**
   * Get the entire structure
   */
  getStructure() {
    return this.structure;
  }
  
  /**
   * Set the entire structure
   */
  setStructure(structure) {
    this.structure = structure;
  }
  
  /**
   * Convert structure to XML
   */
  toXML() {
    let xml = '<page>\n';
    
    for (const row of this.structure.rows) {
      xml += '  <section';
      if (row.cssClass) {
        xml += ` class="${this.escapeXml(row.cssClass)}"`;
      }
      if (row.hr) {
        xml += ' hr="true"';
      }
      xml += '>\n';
      
      for (const column of row.columns) {
        xml += '    <column';
        if (column.cssClass) {
          xml += ` class="${this.escapeXml(column.cssClass)}"`;
        }
        if (column.hr) {
          xml += ' hr="true"';
        }
        xml += '>\n';
        
        for (const widget of column.widgets) {
          xml += this.widgetToXml(widget, '      ');
        }
        
        xml += '    </column>\n';
      }
      
      xml += '  </section>\n';
    }
    
    xml += '</page>';
    
    return xml;
  }
  
  /**
   * Convert widget to XML
   */
  widgetToXml(widget, indent) {
    let xml = indent + `<widget name="${this.escapeXml(widget.type)}"`;
    if (widget.cssClass && widget.cssClass.trim()) {
      xml += ` class="${this.escapeXml(widget.cssClass)}"`;
    }
    if (widget.hr) {
      xml += ' hr="true"';
    }
    xml += '>\n';
    
    for (const [key, value] of Object.entries(widget.properties)) {
      if (Array.isArray(value)) {
      // Handle array properties (like links)
      xml += indent + `  <${key}>\n`;
      for (const item of value) {
        xml += indent + `    <link`;
        for (const [attr, attrValue] of Object.entries(item)) {
        xml += ` ${attr}="${this.escapeXml(attrValue)}"`;
        }
        xml += ' />\n';
      }
      xml += indent + `  </${key}>\n`;
      } else if (key === 'html') {
      // Handle CDATA content
      xml += indent + `  <${key}><![CDATA[${value}]]></${key}>\n`;
      } else {
      // Handle simple properties
      xml += indent + `  <${key}>${this.escapeXml(value)}</${key}>\n`;
      }
    }
    
    xml += indent + '</widget>\n';
    
    return xml;
  }
  
  /**
   * Load structure from XML
   */
  fromXML(xmlString) {
    console.log('Parsing XML:', xmlString);
    
    // Parse XML string
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml');
    
    // Check for parsing errors
    const parserError = xmlDoc.getElementsByTagName('parsererror');
    if (parserError.length > 0) {
      throw new Error('XML parsing error: ' + parserError[0].textContent);
    }
    
    // Reset structure
    this.structure = { rows: [] };
    
    // Parse page element
    const pageElement = xmlDoc.getElementsByTagName('page')[0];
    if (!pageElement) {
      throw new Error('No <page> element found');
    }
    
    // Parse sections (rows)
    const sections = pageElement.getElementsByTagName('section');
    for (const section of sections) {
      const rowId = 'row-' + (this.nextRowId++);
      const row = {
        id: rowId,
        cssClass: section.getAttribute('class') || '',
        hr: section.getAttribute('hr') === 'true',
        columns: []
      };
      
      // Parse columns
      const columns = section.getElementsByTagName('column');
      for (const column of columns) {
        const columnId = 'col-' + (this.nextColumnId++);
        const col = {
          id: columnId,
          cssClass: column.getAttribute('class') || '',
          hr: column.getAttribute('hr') === 'true',
          widgets: []
        };
        
        // Parse widgets
        const widgets = column.getElementsByTagName('widget');
        for (const widget of widgets) {
          const widgetId = 'widget-' + (this.nextWidgetId++);
          const w = {
            id: widgetId,
            type: widget.getAttribute('name') || '',
            cssClass: widget.getAttribute('class') || '',
            hr: widget.getAttribute('hr') === 'true',
            properties: this.parseWidgetProperties(widget)
          };
          
          col.widgets.push(w);
        }
        
        row.columns.push(col);
      }
      
      this.structure.rows.push(row);
    }
    
    console.log('XML parsed, structure:', this.structure);
  }
  
  /**
   * Parse widget properties from XML element
   */
  parseWidgetProperties(widgetElement) {
    const properties = {};
    
    for (const child of widgetElement.children) {
      const tagName = child.tagName;
      
      if (child.children.length > 0) {
        // Complex property
        if (tagName === 'links') {
          properties.links = [];
          const links = child.getElementsByTagName('link');
          for (const link of links) {
            const linkObj = {};
            for (const attr of link.attributes) {
              linkObj[attr.name] = attr.value;
            }
            properties.links.push(linkObj);
          }
        }
      } else {
        // Simple property
        properties[tagName] = child.textContent;
      }
    }
    
    return properties;
  }
  
  /**
   * Escape XML special characters
   */
  escapeXml(str) {
    if (typeof str !== 'string') return str;
    
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&apos;');
  }
}
