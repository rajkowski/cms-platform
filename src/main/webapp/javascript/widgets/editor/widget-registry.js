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
    
    this.register('contentTabs', {
      name: 'Content Tabs',
      category: 'Content',
      icon: 'fa-folder-open',
      description: 'Content tab container',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });

    this.register('contentReveal', {
      name: 'Content Reveal',
      category: 'Content',
      icon: 'fa-plus',
      description: 'Reveals content when clicked',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });

    this.register('contentGallery', {
      name: 'Content Gallery',
      category: 'Content',
      icon: 'fa-th-large',
      description: 'A gallery of content items',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });

    this.register('contentCarousel', {
      name: 'Content Carousel',
      category: 'Content',
      icon: 'fa-images',
      description: 'A carousel of content items',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });

    this.register('blogPostList', {
      name: 'Blog Post List',
      category: 'Content',
      icon: 'fa-newspaper',
      description: 'A list of blog posts',
      properties: {
        view: { type: 'select', label: 'View', options: ['condensed', 'basic', 'summary'] },
        condensed: { type: 'checkbox', label: 'Condensed' },
        showImages: { type: 'checkbox', label: 'Show Images' },
        showAuthor: { type: 'checkbox', label: 'Show Author' },
        showDate: { type: 'checkbox', label: 'Show Date' },
        showComments: { type: 'checkbox', label: 'Show Comments' },
        rows: { type: 'number', label: 'Rows' },
        class: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('form', {
      name: 'Form',
      category: 'Content',
      icon: 'fa-check-square',
      description: 'A form',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        form: { type: 'text', label: 'Form Unique ID' },
        returnPage: { type: 'text', label: 'Return Page' },
        buttonName: { type: 'text', label: 'Button Name' },
        class: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('photoGallery', {
      name: 'Photo Gallery',
      category: 'Content',
      icon: 'fa-camera-retro',
      description: 'A gallery of photos',
      properties: {
        album: { type: 'text', label: 'Album Unique ID' },
        albumId: { type: 'number', label: 'Album ID' },
        records: { type: 'text', label: 'Records' }
      }
    });

    this.register('fileList', {
      name: 'File List',
      category: 'Content',
      icon: 'fa-file-alt',
      description: 'A list of files',
      properties: {
        folder: { type: 'text', label: 'Folder Unique ID' },
        folderId: { type: 'number', label: 'Folder ID' },
        category: { type: 'text', label: 'Category' },
        tags: { type: 'text', label: 'Tags' },
        view: { type: 'select', label: 'View', options: ['list', 'grid'] },
        showSearch: { type: 'checkbox', label: 'Show Search' },
        showPaging: { type: 'checkbox', label: 'Show Paging' },
        allowUpload: { type: 'checkbox', label: 'Allow Upload' },
        allowFolders: { type: 'checkbox', label: 'Allow Folders' },
        allowDelete: { type: 'checkbox', label: 'Allow Delete' },
        allowSubfolders: { type: 'checkbox', label: 'Allow Subfolders' },
        allowFileVersions: { type: 'checkbox', label: 'Allow File Versions' },
        allowSharing: { type: 'checkbox', label: 'Allow Sharing' },
        allowDirectDownload: { type: 'checkbox', label: 'Allow Direct Download' },
        showBreadcrumbs: { type: 'checkbox', label: 'Show Breadcrumbs' },
        showTitle: { type: 'checkbox', label: 'Show Title' },
        showActions: { type: 'checkbox', label: 'Show Actions' },
        showDate: { type: 'checkbox', label: 'Show Date' },
        showSize: { type: 'checkbox', label: 'Show Size' },
        showVersion: { type: 'checkbox', label: 'Show Version' },
        showDescription: { type: 'checkbox', label: 'Show Description' },
        showCategories: { type: 'checkbox', label: 'Show Categories' },
        showTags: { type: 'checkbox', label: 'Show Tags' },
        showUploader: { type: 'checkbox', label: 'Show Uploader' },
        showLastUpdated: { type: 'checkbox', label: 'Show Last Updated' },
        showMimeType: { type: 'checkbox', label: 'Show Mime Type' },
        showExtension: { type: 'checkbox', label: 'Show Extension' },
        showPreview: { type: 'checkbox', label: 'Show Preview' },
        showDownloadCount: { type: 'checkbox', label: 'Show Download Count' },
        showShareLink: { type: 'checkbox', label: 'Show Share Link' },
        showCopyLink: { type: 'checkbox', label: 'Show Copy Link' },
        showEditLink: { type: 'checkbox', label: 'Show Edit Link' },
        showDeleteLink: { type: 'checkbox', label: 'Show Delete Link' },
        showUploadLink: { type: 'checkbox', label: 'Show Upload Link' },
        showFolderLink: { type: 'checkbox', label: 'Show Folder Link' },
        showSubfolderLink: { type: 'checkbox', label: 'Show Subfolder Link' },
        showFileLink: { type: 'checkbox', label: 'Show File Link' },
        showFileVersionLink: { type: 'checkbox', label: 'Show File Version Link' },
        showSharingLink: { type: 'checkbox', label: 'Show Sharing Link' },
        showDirectDownloadLink: { type: 'checkbox', label: 'Show Direct Download Link' }
      }
    });

    this.register('remoteContent', {
      name: 'Remote Content',
      category: 'Content',
      icon: 'fa-rss',
      description: 'Content from a remote source',
      properties: {
        title: { type: 'text', label: 'Title', required: false },
        icon: { type: 'text', label: 'Icon', required: false },
        url: { type: 'text', label: 'URL', required: true },
        cache: { type: 'number', label: 'Cache (minutes)' },
        startTag: { type: 'text', label: 'Start HTML Tag', required: false },
        endTag: { type: 'text', label: 'End HTML Tag', required: false },
        includeTags: { type: 'checkbox', label: 'Include Tags' },
        adjustTable: { type: 'checkbox', label: 'Adjust Table' },
      }
    });

    this.register('socialMediaLinks', {
      name: 'Social Media Links',
      category: 'Content',
      icon: 'fa-share-alt',
      description: 'Links to social media profiles',
      properties: {
        title: { type: 'text', label: 'Title' },
        showTitles: { type: 'checkbox', label: 'Show Titles' },
        useSmallIcons: { type: 'checkbox', label: 'Use Small Icons' },
        cssClass: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('instagram', {
      name: 'Instagram',
      category: 'Content',
      icon: 'fa-instagram',
      description: 'Instagram feed',
      properties: {
        token: { type: 'text', label: 'Access Token' },
        count: { type: 'number', label: 'Count' }
      }
    });

    this.register('leaderboard', {
      name: 'Leaderboard',
      category: 'Content',
      icon: 'fa-trophy',
      description: 'A leaderboard',
      properties: {
        collection: { type: 'text', label: 'Collection Unique ID' },
        collectionId: { type: 'number', label: 'Collection ID' },
        view: { type: 'select', label: 'View', options: ['list', 'grid'] },
        rows: { type: 'number', label: 'Rows' }
      }
    });

    // Dashboard widgets
    this.register('progressCard', {
      name: 'Progress Card',
      category: 'Dashboard',
      icon: 'fa-tasks',
      description: 'A card with a progress bar',
      properties: {
        label: { type: 'text', label: 'Label', required: true },
        value: { type: 'number', label: 'Value', required: true },
        total: { type: 'number', label: 'Total', required: true },
        link: { type: 'text', label: 'Link URL' },
        icon: { type: 'text', label: 'Icon' },
        color: { type: 'text', label: 'Color' }
      }
    });

    this.register('card', {
      name: 'Card',
      category: 'Content',
      icon: 'fa-vcard',
      description: 'A card with content',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        html: { type: 'textarea', label: 'HTML Content', required: false }
      }
    });

    this.register('statisticCard', {
      name: 'Statistic Card',
      category: 'Dashboard',
      icon: 'fa-calculator',
      description: 'A card with a statistic',
      properties: {
        label: { type: 'text', label: 'Label', required: true },
        value: { type: 'text', label: 'Value', required: true },
        link: { type: 'text', label: 'Link URL' },
        icon: { type: 'text', label: 'Icon' },
        color: { type: 'text', label: 'Color' }
      }
    });

    this.register('superset', {
      name: 'Superset',
      category: 'Dashboard',
      icon: 'fa-chart-pie',
      description: 'An Apache Superset chart',
      properties: {
        url: { type: 'text', label: 'URL', required: true },
        height: { type: 'number', label: 'Height' }
      }
    });

    // User Profile widgets
    this.register('emailSubscribe', {
      name: 'Email Subscribe',
      category: 'User Profile',
      icon: 'fa-envelope',
      description: 'A form to subscribe to a mailing list',
      properties: {
        list: { type: 'text', label: 'List Unique ID', required: true },
        buttonName: { type: 'text', label: 'Button Name' },
        returnPage: { type: 'text', label: 'Return Page' }
      }
    });

    // E-Commerce widgets
    this.register('productBrowser', {
      name: 'Product Browser',
      category: 'E-Commerce',
      icon: 'fa-store',
      description: 'A list of products',
      properties: {
        view: { type: 'select', label: 'View', options: ['grid', 'list'] },
        showImages: { type: 'checkbox', label: 'Show Images' },
        showPrice: { type: 'checkbox', label: 'Show Price' },
        showDescription: { type: 'checkbox', label: 'Show Description' },
        showAddToCart: { type: 'checkbox', label: 'Show Add to Cart' },
        rows: { type: 'number', label: 'Rows' },
        class: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('addToCart', {
      name: 'Add to Cart',
      category: 'E-Commerce',
      icon: 'fa-cart-plus',
      description: 'An add to cart button',
      properties: {
        sku: { type: 'text', label: 'SKU', required: true },
        name: { type: 'text', label: 'Button Text' },
        class: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('cart', {
      name: 'Cart',
      category: 'E-Commerce',
      icon: 'fa-shopping-cart',
      description: 'The shopping cart',
      properties: {
        checkoutPage: { type: 'text', label: 'Checkout Page' }
      }
    });

    // Calendar widgets
    this.register('calendar', {
      name: 'Calendar',
      category: 'Calendar',
      icon: 'fa-calendar-alt',
      description: 'A calendar',
      properties: {
        calendar: { type: 'text', label: 'Calendar Unique ID' },
        calendarId: { type: 'number', label: 'Calendar ID' },
        view: { type: 'select', label: 'View', options: ['month', 'week', 'day'] },
        showTitle: { type: 'checkbox', label: 'Show Title' },
        showToday: { type: 'checkbox', label: 'Show Today Button' }
      }
    });

    this.register('upcomingCalendarEvents', {
      name: 'Upcoming Calendar Events',
      category: 'Calendar',
      icon: 'fa-calendar-check',
      description: 'A list of upcoming calendar events',
      properties: {
        calendar: { type: 'text', label: 'Calendar Unique ID' },
        calendarId: { type: 'number', label: 'Calendar ID' },
        rows: { type: 'number', label: 'Rows' }
      }
    });

    // Map widgets
    this.register('map', {
      name: 'Map',
      category: 'Map',
      icon: 'fa-map-marked-alt',
      description: 'A map',
      properties: {
        collection: { type: 'text', label: 'Collection Unique ID' },
        collectionId: { type: 'number', label: 'Collection ID' },
        height: { type: 'number', label: 'Height' },
        latitude: { type: 'text', label: 'Latitude' },
        longitude: { type: 'text', label: 'Longitude' },
        zoom: { type: 'number', label: 'Zoom' }
      }
    });

    // Collection widgets
    this.register('directory', {
      name: 'Directory',
      category: 'Collections',
      icon: 'fa-address-book',
      description: 'A directory of items',
      properties: {
        collection: { type: 'text', label: 'Collection Unique ID', required: true },
        view: { type: 'select', label: 'View', options: ['list', 'grid'] },
        showSearch: { type: 'checkbox', label: 'Show Search' },
        showPaging: { type: 'checkbox', label: 'Show Paging' },
        rows: { type: 'number', label: 'Rows' }
      }
    });

    this.register('itemsList', {
      name: 'Items List',
      category: 'Collections',
      icon: 'fa-list-alt',
      description: 'A list of items',
      properties: {
        collection: { type: 'text', label: 'Collection Unique ID', required: true },
        view: { type: 'select', label: 'View', options: ['list', 'grid', 'table'] },
        showSearch: { type: 'checkbox', label: 'Show Search' },
        showPaging: { type: 'checkbox', label: 'Show Paging' },
        rows: { type: 'number', label: 'Rows' }
      }
    });

    this.register('categoriesList', {
      name: 'Categories List',
      category: 'Collections',
      icon: 'fa-sitemap',
      description: 'A list of categories',
      properties: {
        collection: { type: 'text', label: 'Collection Unique ID', required: true },
        view: { type: 'select', label: 'View', options: ['list', 'cloud'] },
        showCount: { type: 'checkbox', label: 'Show Count' }
      }
    });

    this.register('addItemButton', {
      name: 'Add Item Button',
      category: 'Collections',
      icon: 'fa-plus-circle',
      description: 'A button to add an item',
      properties: {
        collection: { type: 'text', label: 'Collection Unique ID', required: true },
        name: { type: 'text', label: 'Button Text' },
        class: { type: 'text', label: 'CSS Class' }
      }
    });

    // Other widgets
    this.register('logo', {
      name: 'Logo',
      category: 'Other',
      icon: 'fa-gem',
      description: 'The site logo',
      properties: {
        link: { type: 'text', label: 'Link URL' }
      }
    });

    this.register('copyright', {
      name: 'Copyright',
      category: 'Other',
      icon: 'fa-copyright',
      description: 'A copyright notice',
      properties: {}
    });

    this.register('systemAlert', {
      name: 'System Alert',
      category: 'Other',
      icon: 'fa-exclamation-triangle',
      description: 'A system-wide alert message',
      properties: {}
    });

    this.register('toggleMenu', {
      name: 'Toggle Menu',
      category: 'Other',
      icon: 'fa-bars',
      description: 'A button to toggle a menu',
      properties: {
        menu: { type: 'text', label: 'Menu Unique ID', required: true }
      }
    });

    this.register('mainMenu', {
      name: 'Main Menu',
      category: 'Other',
      icon: 'fa-stream',
      description: 'The main navigation menu',
      properties: {}
    });

    this.register('globalMessage', {
      name: 'Global Message',
      category: 'Other',
      icon: 'fa-info-circle',
      description: 'A global message',
      properties: {}
    });

    this.register('searchForm', {
      name: 'Search Form',
      category: 'Other',
      icon: 'fa-search',
      description: 'A search form',
      properties: {
        searchPage: { type: 'text', label: 'Search Page' }
      }
    });

    this.register('link', {
      name: 'Link',
      category: 'Other',
      icon: 'fa-link',
      description: 'A hyperlink',
      properties: {
        name: { type: 'text', label: 'Link Text', required: true },
        link: { type: 'text', label: 'Link URL' },
        cssClass: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('systemMessages', {
      name: 'System Messages',
      category: 'Other',
      icon: 'fa-comment-alt',
      description: 'System messages',
      properties: {}
    });

    this.register('button', {
      name: 'Button',
      category: 'Other',
      icon: 'fa-square',
      description: 'A button',
      properties: {
        name: { type: 'text', label: 'Button Text', required: true },
        link: { type: 'text', label: 'Link URL' },
        cssClass: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('breadcrumbs', {
      name: 'Breadcrumbs',
      category: 'Other',
      icon: 'fa-angle-double-right',
      description: 'Breadcrumb navigation',
      properties: {
        useHomepage: { type: 'checkbox', label: 'Use Homepage' },
        useWebPage: { type: 'checkbox', label: 'Use Web Page' }
      }
    });

    this.register('menu', {
      name: 'Menu',
      category: 'Other',
      icon: 'fa-bars',
      description: 'A menu',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        name: { type: 'text', label: 'Menu Name' }
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
