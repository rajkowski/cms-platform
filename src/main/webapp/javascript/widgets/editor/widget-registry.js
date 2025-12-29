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
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true, default: 'GENERATE' },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false, default: '<p>Content</p>' }
      }
    });
    
    this.register('contentAccordion', {
      name: 'Content Accordion',
      category: 'Content',
      icon: 'fa-list',
      description: 'Collapsible content sections',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true, default: 'GENERATE' },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false, default: '<h1>Title</h1><p>&gt; Item to expand</p><p>The item content goes here...</p><hr /><p>&gt; Item to expand</p><p>The item content goes here...</p><hr />' }
      }
    });
    
    this.register('contentCards', {
      name: 'Content Cards',
      category: 'Content',
      icon: 'fa-th',
      description: 'Content card grid',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true, default: 'GENERATE' },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false, default: '<p>Multiple card content separated by HR</p>' },
        gridMargin: { type: 'checkbox', label: 'Set Grid Margin CSS', default: false },
        smallCardCount: {
          type: 'number',
          label: 'Small Screen Cards',
          description: 'Number of cards per row on small screens',
          default: 1
        },
        mediumCardCount: {
          type: 'number',
          label: 'Medium Screen Cards',
          description: 'Number of cards per row on medium screens'
        },
        largeCardCount: {
          type: 'number',
          label: 'Large Screen Cards',
          description: 'Number of cards per row on large screens'
        },
        cardClass: { type: 'text', label: 'Card CSS Class', required: false }
      }
    });
    
    this.register('contentSlider', {
      name: 'Content Slider',
      category: 'Content',
      icon: 'fa-images',
      description: 'Image/content slider',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true, default: 'GENERATE' },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false, default: '<p>Multiple slide content separated by HR</p>' },
        showControls: { type: 'checkbox', label: 'Show Controls', default: true },
        showLeftControl: { type: 'checkbox', label: 'Show Left Control', default: true },
        showRightControl: { type: 'checkbox', label: 'Show Right Control', default: true },
        showPagination: { type: 'checkbox', label: 'Show Pagination', default: true },
        loop: { type: 'checkbox', label: 'Loop', default: false },
        autoplayDelay: { type: 'number', label: 'Autoplay Delay (ms)', default: '5000' },
        carouselClass: { type: 'text', label: 'Carousel CSS Class', required: false },
      }
    });
    
    this.register('tableOfContents', {
      name: 'Table of Contents',
      category: 'Content',
      icon: 'fa-book',
      description: 'A table of contents',
      properties: {
        uniqueId: { type: 'text', label: 'Table of Contents Repository ID', required: true },
        link: { type: 'text', label: 'Named Title Link at Top of List (name=/link)' }
      }
    });

    this.register('contentTabs', {
      name: 'Content Tabs',
      category: 'Content',
      icon: 'fa-folder-open',
      description: 'Content tab container from XML',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        tabs: { type: 'xml', label: 'Tab Data', required: true },
        useLinks: { type: 'checkbox', label: 'Use Links Instead of Divs', default: false },
        smudge: { type: 'checkbox', label: 'Set Div Tab Click in URL', default: true },
      }
    });

    this.register('contentReveal', {
      name: 'Content Reveal',
      category: 'Content',
      icon: 'fa-plus',
      description: 'Reveals content when clicked',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false },
        attach: { type: 'select', label: 'Attach Side', options: ['left', ''], default: '' },
        animate: { type: 'select', label: 'Animate Directions', options: ['up', 'down', 'right', 'left', 'fade'], default: 'left' },
        useIcon: { type: 'checkbox', label: 'Use Icon on Button', default: false },
        revealClass: { type: 'text', label: 'Reveal CSS Class', required: false },
        size: { type: 'text', label: 'Reveal Size CSS Class', required: false },
      }
    });

    this.register('contentGallery', {
      name: 'Content Gallery',
      category: 'Content',
      icon: 'fa-th-large',
      description: 'A gallery of content items',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false },
        carouselSize: { type: 'select', label: 'Carousel Size', options: ['tiny', 'small', 'medium', 'large'], default: 'small' },
        carouselTitle: { type: 'text', label: 'Carousel Title', required: false },
        useIcon: { type: 'checkbox', label: 'Use Icon on Button', default: false },
        showControls: { type: 'checkbox', label: 'Show Controls', default: true },
        showLeftControl: { type: 'checkbox', label: 'Show Left Control', default: true },
        showRightControl: { type: 'checkbox', label: 'Show Right Control', default: true },
        showBullets: { type: 'checkbox', label: 'Show Bullets', default: true },
        carouselClass: { type: 'text', label: 'Carousel CSS Class', required: false },
      }
    });

    this.register('contentCarousel', {
      name: 'Content Carousel',
      category: 'Content',
      icon: 'fa-images',
      description: 'A carousel of content items',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        uniqueId: { type: 'text', label: 'Content Repository ID', required: true },
        html: { type: 'textarea', label: 'Fallback HTML Content', required: false }
      }
    });

    this.register('blogPostList', {
      name: 'Blog Post List',
      category: 'Content',
      icon: 'fa-newspaper',
      description: 'A list of blog posts',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        blogUniqueId: { type: 'text', label: 'Blog Repository ID', required: true },
        type: { type: 'select', label: 'Type of List', options: ['default', 'recent'], default: 'default' },
        view: { type: 'select', label: 'View As', options: ['default', 'masonry', 'overview', 'titles', 'cards', 'featured'], default: 'default' },
        limit: { type: 'number', label: 'Items Per Page', default: 10},
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: true },
        showPaging: { type: 'checkbox', label: 'Show Paging', default: true },
        showSort: { type: 'checkbox', label: 'Show Sort', default: false },
        showAuthor: { type: 'checkbox', label: 'Show Author', default: true },
        showDate: { type: 'checkbox', label: 'Show Date', default: true },
        addDateToTitle: { type: 'checkbox', label: 'Add Date to Title', default: false },
        showTags: { type: 'checkbox', label: 'Show Tags', default: false },
        showImage: { type: 'checkbox', label: 'Show Image', default: true },
        showSummary: { type: 'checkbox', label: 'Show Summary', default: true },
        showReadMore: { type: 'checkbox', label: 'Show Read More Link', default: true },
        readMoreText: { type: 'text', label: 'Read More Text Label', default: 'Read more' },
        showBullets: { type: 'checkbox', label: 'Show Bullets in Title View', default: false },
        smallCardCount: {
          type: 'number',
          label: 'Small Screen Cards',
          description: 'Number of cards per row on small screens (default: 3)',
          default: 3
        },
        mediumCardCount: {
          type: 'number',
          label: 'Medium Screen Cards',
          description: 'Number of cards per row on medium screens'
        },
        largeCardCount: {
          type: 'number',
          label: 'Large Screen Cards',
          description: 'Number of cards per row on large screens'
        },
        cardClass: { type: 'text', label: 'Card CSS Class', required: false }
      }
    });

    this.register('form', {
      name: 'Form',
      category: 'Content',
      icon: 'fa-check-square',
      description: 'A form',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        subtitle: { type: 'text', label: 'Widget Subtitle', required: false },
        formUniqueId: { type: 'text', label: 'Form Repository ID', required: true },
        fields: { type: 'text', label: 'Form Input Fields', required: true },
        useCaptcha: { type: 'checkbox', label: 'Display a Captcha', default: true },
        checkForSpam: { type: 'checkbox', label: 'Check Content and Mark for Spam', default: true },
        buttonName: { type: 'text', label: 'Button Name', default: 'Submit' },
        successTitle: { type: 'text', label: 'Success Title to Display' },
        successMessage: { type: 'text', label: 'Success Message to Display', default: 'Your information has been submitted.' }
      }
    });

    this.register('fileDropZone', {
      name: 'File Drop Zone',
      category: 'Content',
      icon: 'fa-upload',
      description: 'A drop zone for uploading files',
      properties: {
        folderUniqueId: { type: 'text', label: 'Folder Unique ID', required: true }
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
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        folderUniqueId: { type: 'text', label: 'Folder Unique ID' },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: true },
        showLinks: { type: 'checkbox', label: 'Show Links', default: true },
        useViewer: { type: 'checkbox', label: 'Enable Viewer', default: false },
        withinLastDays: {
          type: 'number',
          label: 'Sent Within Last Days',
          description: 'Files sent within the last X days (default: -1)',
          default: -1
        },
        rules: { 
          type: 'select', 
          label: 'Access Rules', 
          options: ['role-based', 'user-created']
        },
        orderBy: { 
          type: 'select', 
          label: 'Sort Order', 
          options: ['newest', 'oldest', 'descending', 'ascending'],
          default: 'ascending'
        }
      }
    });

    this.register('socialMediaLinks', {
      name: 'Social Media Links',
      description: 'Displays a list of social media icons linking to site profiles.',
      category: 'Content',
      icon: 'fa-share-alt',
      properties: {
        title: { type: 'text', label: 'Title' },
        showTitles: { type: 'checkbox', label: 'Show Titles' },
        useSmallIcons: { type: 'checkbox', label: 'Use Small Icons' },
        cssClass: { type: 'text', label: 'CSS Class' }
      }
    });

    this.register('instagram', {
      name: 'Instagram Feed',
      description: 'Displays a feed of recent Instagram posts.',
      category: 'Content',
      icon: 'fa-instagram',
      properties: {
        title: {
          type: 'text',
          label: 'Title',
          description: 'An optional title for the widget'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'An optional icon for the title'
        },
        limit: {
          type: 'number',
          label: 'Limit',
          description: 'The number of posts to display (default: 8)'
        },
        cardClass: {
          type: 'text',
          label: 'Card CSS Class',
          description: 'CSS class to apply to each post card'
        },
        smallCardCount: {
          type: 'number',
          label: 'Small Screen Cards',
          description: 'Number of cards per row on small screens (default: 6)',
          default: 6
        },
        mediumCardCount: {
          type: 'number',
          label: 'Medium Screen Cards',
          description: 'Number of cards per row on medium screens'
        },
        largeCardCount: {
          type: 'number',
          label: 'Large Screen Cards',
          description: 'Number of cards per row on large screens'
        }
      }
    });

    this.register('leaderboard', {
      name: 'Leaderboard',
      description: 'Displays a leaderboard from a dataset.',
      category: 'Content',
      icon: 'fa-trophy',
      properties: {
        title: {
          type: 'text',
          label: 'Title',
          description: 'An optional title for the widget'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'An optional icon for the title'
        },
        dataset: {
          type: 'text',
          label: 'Dataset Name',
          description: 'The name of the dataset to use for the leaderboard'
        }
      }
    });

    // Dashboard widgets
    this.register('progressCard', {
      name: 'Progress Card',
      category: 'Dashboard',
      icon: 'fa-tasks',
      description: 'A card with a progress bar',
      properties: {
        label: { type: 'text', label: 'Label', required: false },
        value: { type: 'text', label: 'Value', required: false },
        progress: { type: 'number', label: 'Progress', required: true },
        maxValue: { type: 'number', label: 'Max Value', required: true },
        maxLabel: { type: 'text', label: 'Max Label' },
        link: { type: 'text', label: 'Link' },
        textColor: { type: 'color', label: 'Text Color' },
        subheaderColor: { type: 'color', label: 'Sub Header Color' },
        progressColor: { type: 'color', label: 'Progress Color' },
        remainderColor: { type: 'color', label: 'Remained Color' },
        view: { type: 'text', label: 'View' }
      }
    });

    this.register('card', {
      name: 'Card',
      category: 'Content',
      icon: 'fa-vcard',
      description: 'A card with content',
      properties: {
        classData: { type: 'text', label: 'Class', required: false },
        title: { type: 'text', label: 'Title', required: false, default: 'Card Title' },
        icon: { type: 'text', label: 'Icon', required: false, default: '' },
        link: { type: 'text', label: 'Link', required: false, default: '' },
        linkTitle: { type: 'text', label: 'Link Title', required: false, default: '' },
        linkIcon: { type: 'text', label: 'Link Icon', required: false }
      }
    });

    this.register('statisticCard', {
      name: 'Statistic Card',
      description: 'Displays a card with a key statistic and an icon.',
      category: 'Dashboard',
      icon: 'fa-chart-bar',
      properties: {
        label: {
          type: 'text',
          label: 'Label',
          description: 'The label for the statistic',
          default: 'Statistic'
        },
        value: {
          type: 'number',
          label: 'Value',
          description: 'The numerical value of the statistic',
          default: '0'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'A Font Awesome icon to display',
          default: 'chart-bar'
        },
        link: {
          type: 'text',
          label: 'Link',
          description: 'An optional page URL to link to (/example)',
          default: ''
        },
        iconColor: {
          type: 'color',
          label: 'Icon Color',
          description: 'Color for the icon (e.g., #ffffff or theme.color)',
          default: 'theme.body.text.color'
        },
        view: {
          label: 'View',
          type: 'select',
          options: ['default', 'vertical'],
          description: 'Set to "vertical" for a vertical layout',
          default: 'default'
        }
      }
    });

    this.register('superset', {
      name: 'Superset Dashboard',
      description: 'Renders an embedded Superset dashboard.',
      category: 'Dashboard',
      icon: 'fa-chart-pie',
      properties: {
        dashboardValue: {
          type: 'text',
          label: 'Dashboard Value',
          description: 'The value/ID of the dashboard to display'
        },
        dashboardEmbeddedId: {
          type: 'text',
          label: 'Embedded ID',
          description: 'The embedded ID for the dashboard'
        },
        height: {
          type: 'text',
          label: 'Height',
          description: 'The height of the dashboard container (e.g., 300px)'
        },
        hideChartTitle: {
          type: 'boolean',
          label: 'Hide Chart Title',
          description: 'Whether to hide the chart title (default: true)'
        },
        hideChartControls: {
          type: 'boolean',
          label: 'Hide Chart Controls',
          description: 'Whether to hide the chart controls (default: true)'
        },
        clause: {
          type: 'text',
          label: 'RLS Clause',
          description: 'Row-Level Security (RLS) clause to apply'
        }
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
        calendarUniqueId: { type: 'text', label: 'Calendar Repository ID' },
        defaultView: { type: 'select', label: 'View', options: ['month', 'list', 'day'], default: 'month' },
        view: { type: 'select', label: 'Placement Size', options: ['default', 'small'], default: 'default' },
        height: { type: 'number', label: 'Optional Height Value', default: '' },
        showEvents: { type: 'checkbox', label: 'Show Events', default: true },
        showHolidays: { type: 'checkbox', label: 'Show Holidays', default: true },
        showMoodleEvents: { type: 'checkbox', label: 'Show Moodle Events', default: false },
        moodleTextColor: { type: 'color', label: 'Moodle Text Color', default: '#000000' },
        moodleBackgroundColor: { type: 'color', label: 'Moodle Background Color', default: '#ffffff' }
      }
    });

    this.register('upcomingCalendarEvents', {
      name: 'Upcoming Calendar Events',
      category: 'Calendar',
      icon: 'fa-calendar-check',
      description: 'A list of upcoming calendar events',
      properties: {
        calendarUniqueId: { type: 'text', label: 'Calendar Repository ID' },
        view: { type: 'select', label: 'Display As', options: ['list', 'cards'], default: 'list' },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: true },
        daysToShow: { type: 'number', label: 'Days to Show', default: '-1' },
        monthsToShow: { type: 'number', label: 'Months to Show', default: '1' },
        showMonthName: { type: 'checkbox', label: 'Show Month Name', default: true },
        showEventLink: { type: 'checkbox', label: 'Show Event Link', default: true },
        includeLastEvent: { type: 'checkbox', label: 'Include Last Previous Event', default: false },
        limit: { type: 'number', label: 'Max Events', default: '-1' },
        smallCardCount: {
          type: 'number',
          label: 'Small Screen Cards',
          description: 'Number of cards per row on small screens (default: 6)'
        },
        mediumCardCount: {
          type: 'number',
          label: 'Medium Screen Cards',
          description: 'Number of cards per row on medium screens'
        },
        largeCardCount: {
          type: 'number',
          label: 'Large Screen Cards',
          description: 'Number of cards per row on large screens'
        },
        cardClass: { type: 'text', label: 'Card CSS Class' },
        calendarLink: { type: 'text', label: 'Custom Link to Calendar' },
        titles: { type: 'text', label: 'Card Titles (Past|Current|Upcoming)' }
      }
    });

    this.register('calendarEventDetails', {
      name: 'Calendar Event Details',
      category: 'Calendar',
      icon: 'fa-calendar-alt',
      description: 'A calendar event',
      properties: {
        icon: { type: 'text', label: 'Widget Icon' },
        title: { type: 'text', label: 'Widget Title' },
      }
    });

    // Map widgets
    this.register('map', {
      name: 'Map',
      category: 'Map',
      icon: 'fa-map-marked-alt',
      description: 'A map',
      properties: {
        icon: { type: 'text', label: 'Widget Icon' },
        title: { type: 'text', label: 'Widget Title' },
        coordinates: { type: 'text', label: 'Coordinates (comma-separated)' },
        latitude: { type: 'text', label: 'Latitude' },
        longitude: { type: 'text', label: 'Longitude' },
        mapHeight: { type: 'number', label: 'Height', default: '290' },
        mapZoomLevel: { type: 'number', label: 'Zoom Level', default: '12' },
        showMarker: { type: 'checkbox', label: 'Show a Map Marker', default: true },
        markerTitle: { type: 'text', label: 'Marker Title' },
        markerText: { type: 'text', label: 'Marker Caption Text' },
      }
    });

    // Collection widgets
    this.register('directory', {
      name: 'Directory',
      category: 'Collections',
      icon: 'fa-address-book',
      description: 'A directory of items',
      properties: {
        collection: { type: 'text', label: 'Collection Repository ID', required: true },
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
        collection: { type: 'text', label: 'Collection Repository ID', required: true },
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
        collection: { type: 'text', label: 'Collection Repository ID', required: true },
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
        collection: { type: 'text', label: 'Collection Repository ID', required: true },
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
      category: 'Search',
      icon: 'fa-search',
      description: 'A search form',
      properties: {
        placeholder: { type: 'text', label: 'Placeholder Text', default: 'Search the site...' },
        linkText: { type: 'text', label: 'Button Text', default: 'Search' },
        expand: { type: 'checkbox', label: 'Expand input field when selected', default: false }
      }
    });

    this.register('searchInfo', {
      name: 'Search Info',
      category: 'Search',
      icon: 'fa-info',
      description: 'Displays what the user searched for and a summary',
      properties: {
        icon: { type: 'text', label: 'Widget Icon' },
        title: { type: 'text', label: 'Widget Title' },
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
      category: 'System',
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
        name: { type: 'text', label: 'Button Text', required: true, default: 'Go' },
        link: { type: 'text', label: 'Link URL' },
        cssClass: { type: 'text', label: 'Button CSS Class' }
      }
    });

    this.register('breadcrumbs', {
      name: 'Breadcrumbs',
      category: 'Other',
      icon: 'fa-angle-double-right',
      description: 'Breadcrumb navigation',
      properties: {
        useHomepage: { type: 'checkbox', label: 'Use Homepage' },
        useWebPage: { type: 'checkbox', label: 'Use Web Page' },
        links: { type: 'array', label: 'Links', default: [] }
      }
    });

    this.register('menu', {
      name: 'Menu',
      category: 'Other',
      icon: 'fa-bars',
      description: 'A menu',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true },
        name: { type: 'text', label: 'Menu Name' },
        class: { type: 'text', label: 'Menu Class', default: 'vertical' },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: false },
        links: { type: 'array', label: 'Links', default: [] }
      }
    });

    this.register('webPageSearchResults', {
      name: 'Web Page Search Results',
      category: 'Search',
      icon: 'fa-file-alt',
      description: 'Displays web page search results',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        limit: { type: 'number', label: 'Max results to show', default: 15 },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: true }
      }
    });

    this.register('webPageTitleSearchResults', {
      name: 'Web Page Title Search Results',
      category: 'Search',
      icon: 'fa-file-alt',
      description: 'Displays web page title search results',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        limit: { type: 'number', label: 'Limit', default: 15 },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: false }
      }
    });

    this.register('blogPostSearchResults', {
      name: 'Blog Post Search Results',
      category: 'Search',
      icon: 'fa-file-alt',
      description: 'Displays blog post search results',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        limit: { type: 'number', label: 'Limit', default: 15 },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: false },
        sortBy: { type: 'select', label: 'Sort Results By', options: ['results', 'new'] },
        showPaging: { type: 'checkbox', label: 'Show Paging', default: false },
      }
    });

    this.register('calendarSearchResults', {
      name: 'Calendar Event Search Results',
      category: 'Search',
      icon: 'fa-calendar',
      description: 'Displays calendar event search results',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        limit: { type: 'number', label: 'Limit', default: 3 },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: false }
      }
    });

    this.register('itemsSearchResults', {
      name: 'Directory Items Search Results',
      category: 'Search',
      icon: 'fa-file-alt',
      description: 'Displays collection item search results',
      properties: {
        icon: { type: 'text', label: 'Widget Icon', required: false },
        title: { type: 'text', label: 'Widget Title', required: false },
        limit: { type: 'number', label: 'Limit', default: 15 },
        showWhenEmpty: { type: 'checkbox', label: 'Show When Empty', default: false },
        sortBy: { type: 'select', label: 'Sort Results By', options: ['results', 'new'] },
        showPaging: { type: 'checkbox', label: 'Show Paging', default: false },
        useItemLink: { type: 'checkbox', label: 'Use URL from Item For Link', default: false },
      }
    });

    /*
    this.register('tableOfContentsEditor', {
      name: 'Table of Contents Editor',
      category: 'System',
      icon: 'fa-edit',
      description: 'An editor for a table of contents',
      properties: {
        uniqueId: { type: 'text', label: 'Unique ID', required: true }
      }
    });
    */

    this.register('imageUpload', {
      name: 'Image Upload',
      description: 'Handles image uploads and returns a URL.',
      category: 'Content',
      icon: 'fa-upload',
      properties: {}
    });

    this.register('imageBrowser', {
      name: 'Image Browser',
      description: 'Displays a gallery of uploaded images to select from.',
      category: 'System',
      icon: 'fa-images',
      properties: {
        title: {
          type: 'text',
          label: 'Title',
          description: 'A title to show for the widget'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'A Font Awesome icon to show with the title'
        }
      }
    });

    this.register('videoBrowser', {
      name: 'Video Browser',
      description: 'Displays a gallery of uploaded videos to select from.',
      category: 'System',
      icon: 'fa-video',
      properties: {
        title: {
          type: 'text',
          label: 'Title',
          description: 'A title to show for the widget'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'A Font Awesome icon to show with the title'
        }
      }
    });

    this.register('fileBrowser', {
      name: 'File Browser',
      description: 'Displays a gallery of uploaded files to select from.',
      category: 'System',
      icon: 'fa-file',
      properties: {
        title: {
          type: 'text',
          label: 'Title',
          description: 'A title to show for the widget'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'A Font Awesome icon to show with the title'
        }
      }
    });

    this.register('wiki', {
      name: 'Wiki',
      description: 'Displays a navigable wiki.',
      category: 'Content',
      icon: 'fa-book',
      properties: {
        title: {
          type: 'text',
          label: 'Title',
          description: 'A title to show for the widget'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'A Font Awesome icon to show with the title'
        },
        wikiUniqueId: {
          type: 'text',
          label: 'Wiki Repository ID',
          description: 'The unique ID of the wiki to display'
        }
      }
    });

    this.register('wikiEditor', {
      name: 'Wiki Editor',
      description: 'Provides an editor for a wiki page.',
      category: 'System',
      icon: 'fa-edit',
      properties: {}
    });

    this.register('remoteContent', {
      name: 'Remote Content',
      description: 'Fetches and displays content from a remote URL.',
      category: 'Content',
      icon: 'fa-cloud-download-alt',
      properties: {
        url: {
          type: 'text',
          label: 'URL',
          description: 'The URL of the content to fetch'
        },
        title: {
          type: 'text',
          label: 'Title',
          description: 'An optional title for the content block'
        },
        icon: {
          type: 'text',
          label: 'Icon',
          description: 'An optional icon for the title'
        },
        clean: {
          type: 'boolean',
          label: 'Clean HTML',
          description: 'Whether to clean the fetched HTML content (default: true)'
        },
        includeTags: {
          type: 'boolean',
          label: 'Include Tags',
          description: 'When using start/end tags, whether to include the tags themselves (default: true)'
        },
        startTag: {
          type: 'text',
          label: 'Start Tag',
          description: 'The starting HTML tag to begin capturing content from'
        },
        endTag: {
          type: 'text',
          label: 'End Tag',
          description: 'The ending HTML tag to stop capturing content at'
        },
        adjustTable: {
          type: 'boolean',
          label: 'Adjust Table',
          description: 'Add a scroll class to tables for responsiveness'
        }
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
