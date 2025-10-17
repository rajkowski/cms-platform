/**
 * Widget Registry
 * Manages available widgets and their metadata
 * 
 * @author matt rajkowski
 * @created 10/16/25 12:00 PM
 */

class WidgetRegistry {
  constructor() {
    this.widgets = new Map();
    this.loadWidgetDefinitions();
  }
  
  /**
   * Load widget definitions
   */
  loadWidgetDefinitions() {
    // Content widgets
    this.register('content', {
      name: 'Content',
      category: 'Content',
      icon: 'fa-paragraph',
      description: 'Rich text content block',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });
    
    this.register('contentAccordion', {
      name: 'Content Accordion',
      category: 'Content',
      icon: 'fa-list',
      description: 'Collapsible content sections',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });
    
    this.register('contentCards', {
      name: 'Content Cards',
      category: 'Content',
      icon: 'fa-th',
      description: 'Content card grid',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });
    
    this.register('contentSlider', {
      name: 'Content Slider',
      category: 'Content',
      icon: 'fa-images',
      description: 'Image/content slider',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false },
        showControls: { type: 'checkbox', label: 'Show Controls' },
        showPagination: { type: 'checkbox', label: 'Show Pagination' },
        loop: { type: 'checkbox', label: 'Loop' },
        autoplayDelay: { type: 'number', label: 'Autoplay Delay (ms)' }
      }
    });
    
    // Navigation widgets
    this.register('breadcrumbs', {
      name: 'Breadcrumbs',
      category: 'Navigation',
      icon: 'fa-ellipsis-h',
      description: 'Navigation breadcrumbs',
      properties: {
        links: { type: 'links', label: 'Links' }
      }
    });
    
    this.register('menu', {
      name: 'Menu',
      category: 'Navigation',
      icon: 'fa-bars',
      description: 'Navigation menu',
      properties: {
        class: { type: 'text', label: 'CSS Class' },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty' },
        links: { type: 'links', label: 'Links' }
      }
    });
    
    // UI Elements
    this.register('button', {
      name: 'Button',
      category: 'UI Elements',
      icon: 'fa-hand-pointer',
      description: 'Call-to-action button',
      properties: {
        name: { type: 'text', label: 'Button Text', required: true },
        link: { type: 'text', label: 'Link URL' },
        buttonClass: { type: 'text', label: 'CSS Class' },
        leftIcon: { type: 'text', label: 'Left Icon' },
        icon: { type: 'text', label: 'Right Icon' }
      }
    });
    
    this.register('card', {
      name: 'Card',
      category: 'UI Elements',
      icon: 'fa-square',
      description: 'Information card',
      properties: {
        title: { type: 'text', label: 'Title', required: true },
        icon: { type: 'text', label: 'Icon' },
        link: { type: 'text', label: 'Link URL' },
        linkTitle: { type: 'text', label: 'Link Title' },
        linkIcon: { type: 'text', label: 'Link Icon' },
        class: { type: 'text', label: 'CSS Class' }
      }
    });
    
    this.register('statisticCard', {
      name: 'Statistic Card',
      category: 'UI Elements',
      icon: 'fa-chart-bar',
      description: 'Display statistics',
      properties: {
        label: { type: 'text', label: 'Label', required: true },
        value: { type: 'text', label: 'Value', required: true },
        icon: { type: 'text', label: 'Icon' },
        link: { type: 'text', label: 'Link URL' },
        iconColor: { type: 'text', label: 'Icon Color' },
        view: { type: 'select', label: 'View', options: ['default', 'vertical'] }
      }
    });
    
    console.log('Widget registry loaded:', this.widgets.size, 'widgets');
  }
  
  /**
   * Register a widget
   */
  register(type, definition) {
    this.widgets.set(type, definition);
  }
  
  /**
   * Get widget definition
   */
  get(type) {
    return this.widgets.get(type);
  }
  
  /**
   * Check if widget exists
   */
  has(type) {
    return this.widgets.has(type);
  }
  
  /**
   * Get all widgets
   */
  getAll() {
    return Array.from(this.widgets.values());
  }
  
  /**
   * Get widgets by category
   */
  getByCategory(category) {
    return this.getAll().filter(w => w.category === category);
  }
  
  /**
   * Get all categories
   */
  getCategories() {
    const categories = new Set();
    this.widgets.forEach(widget => {
      if (widget.category) {
        categories.add(widget.category);
      }
    });
    return Array.from(categories);
  }
}
