// Pre-designed page templates for the visual page editor
// This file is auto-included by visual-page-editor.jsp

window.preDesignedTemplateLabels = {
  "landing": "Simple Landing Page",
  "modern-long": "Modern Landing Page",
  "app-landing": "App Landing Page",
  "agency": "Creative Agency",
  "services": "Services Overview",
  "portfolio": "Portfolio",
  "dashboard": "Dashboard",
  "sales": "Sales Promo",
  "about": "About Us",
  "contact": "Contact Form",
  "faq": "FAQ Page",
  "blog": "Blog"
};

window.preDesignedTemplates = {
  "modern-long": [
    {
      layout: ['small-12'],
      widgets: [
        { type: 'content', description: 'hero', properties: { html: '<h2>Welcome to Modern Solutions</h2>' } }
      ]
    },
    {
      layout: ['small-6', 'small-6'],
      widgets: [
        { type: 'content', format: 'feature', properties: { html: '<h2>Cutting-Edge Tech</h2>' } },
        { type: 'content', format: 'feature', properties: { html: '<h2>Award-Winning Team</h2>' } }
      ]
    },
    {
      layout: ['small-12'],
      widgets: [
        { type: 'content', format: 'banner-image', properties: { html: '<p>Full-width Banner</p>' } },
      ]
    },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'service', content: 'Consulting' },
        { type: 'service', content: 'Development' },
        { type: 'service', content: 'Support' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'testimonial', content: '“They transformed our business!”' }] },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Contact Us Today' }] },
    { layout: ['small-12'], widgets: [{ type: 'footer', content: '© 2025 Modern Solutions' }] }
  ],
  "agency": [
    { layout: ['small-12'], widgets: [{ type: 'hero', content: 'Creative Agency' }] },
    {
      layout: ['small-6', 'small-6'], widgets: [
        { type: 'about', content: 'Who We Are' },
        { type: 'stats', content: '10+ Years Experience' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'gallery', content: 'Our Portfolio' }] },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'service', content: 'Branding' },
        { type: 'service', content: 'Web Design' },
        { type: 'service', content: 'Marketing' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'team', content: 'Meet the Team' }] },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Let’s Work Together' }] },
    { layout: ['small-12'], widgets: [{ type: 'footer', content: '© 2025 Creative Agency' }] }
  ],
  "app-landing": [
    { layout: ['small-12'], widgets: [{ type: 'hero', content: 'Download Our App' }] },
    {
      layout: ['small-6', 'small-6'], widgets: [
        { type: 'screenshot', content: 'App Screenshot 1' },
        { type: 'screenshot', content: 'App Screenshot 2' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'features', content: 'Why Choose Us?' }] },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'feature', content: 'Secure' },
        { type: 'feature', content: 'Fast' },
        { type: 'feature', content: 'Easy to Use' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Get the App' }] },
    { layout: ['small-12'], widgets: [{ type: 'footer', content: '© 2025 AppName' }] }
  ],
  "services": [
    { layout: ['small-12'], widgets: [{ type: 'hero', content: 'Our Services' }] },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'service', content: 'Consulting' },
        { type: 'service', content: 'Implementation' },
        { type: 'service', content: 'Training' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'testimonial', content: '“Outstanding support!”' }] },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Request a Quote' }] },
    { layout: ['small-12'], widgets: [{ type: 'footer', content: '© 2025 Services Inc.' }] }
  ],
  "faq": [
    { layout: ['small-12'], widgets: [{ type: 'hero', content: 'Frequently Asked Questions' }] },
    {
      layout: ['small-6', 'small-6'], widgets: [
        { type: 'faq-list', content: 'General Questions' },
        { type: 'faq-list', content: 'Technical Questions' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Still have questions? Contact us!' }] },
    { layout: ['small-12'], widgets: [{ type: 'footer', content: '© 2025 FAQ Page' }] }
  ],
  landing: [
    { layout: ['small-12'], widgets: [{ type: 'hero', content: 'Welcome to Our Site!' }] },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'feature', content: 'Fast & Reliable' },
        { type: 'feature', content: 'Modern Design' },
        { type: 'feature', content: '24/7 Support' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Get Started Now' }] }
  ],
  sales: [
    { layout: ['small-12'], widgets: [{ type: 'banner', content: 'Big Sale!' }] },
    {
      layout: ['small-6', 'small-6'], widgets: [
        { type: 'product-list', content: 'Featured Products' },
        { type: 'testimonial', content: 'What Our Customers Say' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'cta', content: 'Shop Now' }] }
  ],
  dashboard: [
    { layout: ['small-12'], widgets: [{ type: 'stats', content: 'Overview' }] },
    {
      layout: ['small-6', 'small-6'], widgets: [
        { type: 'chart', content: 'Sales Chart' },
        { type: 'table', content: 'Recent Orders' }
      ]
    },
    { layout: ['small-12'], widgets: [{ type: 'activity-feed', content: 'Latest Activity' }] }
  ],
  about: [
    { layout: ['small-12'], widgets: [{ type: 'text', content: 'About Us' }] },
    {
      layout: ['small-6', 'small-6'], widgets: [
        { type: 'image', content: '' },
        { type: 'text', content: 'Our Story' }
      ]
    }
  ],
  contact: [
    { layout: ['small-12'], widgets: [{ type: 'contact-form', content: '' }] }
  ],
  portfolio: [
    { layout: ['small-12'], widgets: [{ type: 'gallery', content: 'Our Work' }] },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'project', content: 'Project 1' },
        { type: 'project', content: 'Project 2' },
        { type: 'project', content: 'Project 3' }
      ]
    }
  ],
  blog: [
    { layout: ['small-12'], widgets: [{ type: 'blog-list', content: 'Latest Posts' }] },
    {
      layout: ['small-4', 'small-4', 'small-4'], widgets: [
        { type: 'category', content: 'News' },
        { type: 'category', content: 'Tips' },
        { type: 'category', content: 'Events' }
      ]
    }
  ]
};
