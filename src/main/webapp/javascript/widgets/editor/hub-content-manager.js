/**
 * Hub Content Manager for Visual Content Editor
 * Handles blogs, calendars, and other hub content types with calendar visualization
 * 
 * @author matt rajkowski
 * @created 02/07/26 02:00 PM
 */

class HubContentManager {
  constructor(editorBridge) {
    this.editorBridge = editorBridge;
    this.blogs = [];
    this.calendars = [];
    this.pages = [];
    this.content = [];
    this.datasets = [];
    this.wikis = [];
    this.selectedBlogIds = new Set();
    this.selectedCalendarIds = new Set();
    this.selectedPageIds = new Set();
    this.selectedContentIds = new Set();
    this.selectedDatasetIds = new Set();
    this.selectedWikiIds = new Set();
    this.calendar = null;
    this.currentHubType = null;
    this.showUSHolidays = false;
  }

  /**
   * Initialize the hub content manager
   */
  init() {
    this.setupEventListeners();
    this.setupUSHolidaysToggle();
    this.initializeCalendar();
    this.loadBlogs();
    this.loadCalendars();
  }

  /**
   * Setup US Holidays toggle
   */
  setupUSHolidaysToggle() {
    const toggle = document.getElementById('toggle-us-holidays');
    if (toggle) {
      toggle.addEventListener('change', (e) => {
        this.showUSHolidays = e.target.checked;
        this.updateCalendarEventSources();
      });
    }
  }

  /**
   * Initialize calendar immediately (even if empty)
   */
  initializeCalendar() {
    const container = document.getElementById('calendar-container');
    if (!container) return;

    if (this.calendar) {
      this.calendar.destroy();
    }

    this.calendar = new FullCalendar.Calendar(container, {
      initialView: 'dayGridMonth',
      headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
      },
      editable: false,
      eventSources: [],
      eventClick: (info) => {
        this.handleEventClick(info.event);
      },
      eventDidMount: (info) => {
        if (info.event.extendedProps.type) {
          info.el.classList.add(`event-${info.event.extendedProps.type}`);
        }
      }
    });

    this.calendar.render();
  }

  /**
   * Set up event listeners for hub tabs
   */
  setupEventListeners() {
    const hubList = document.getElementById('hub-list-container');
    if (hubList) {
      hubList.addEventListener('click', (e) => {
        const item = e.target.closest('.hub-item[data-hub-type]');
        if (item) {
          const hubType = item.dataset.hubType;
          this.switchHubType(hubType);
        }
      });
    }
  }

  /**
   * Load blogs from server
   */
  loadBlogs() {
    fetch('/json/blogs/list', {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          this.blogs = Array.isArray(data.data) ? data.data : [];
        } else {
          console.error('Failed to load blogs:', data.error);
        }
      })
      .catch(error => console.error('Error loading blogs:', error));
  }

  /**
   * Load calendars from server
   */
  loadCalendars() {
    fetch('/json/calendars/list', {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          this.calendars = Array.isArray(data.data) ? data.data : [];
        } else {
          console.error('Failed to load calendars:', data.error);
        }
      })
      .catch(error => console.error('Error loading calendars:', error));
  }

  /**
   * Switch hub type (blogs/calendars)
   */
  switchHubType(hubType) {
    this.currentHubType = hubType;

    // Update active state
    document.querySelectorAll('#hub-list-container .hub-item').forEach(item => item.classList.remove('selected'));
    document.querySelector(`#hub-list-container .hub-item[data-hub-type="${hubType}"]`)?.classList.add('selected');

    // Show calendar-view when any hub-item is selected
    const calendarView = document.getElementById('calendar-view');
    if (calendarView) {
      calendarView.classList.add('active');
      calendarView.style.display = 'block';
    }

    const calendarSelector = document.getElementById('calendar-selector');
    if (calendarSelector) {
      calendarSelector.innerHTML = '';
    }

    if (hubType === 'blogs') {
      this.renderBlogSelector();
    } else if (hubType === 'calendars') {
      this.renderCalendarSelector();
    } else if (hubType === 'pages') {
      this.renderPagesSelector();
    } else if (hubType === 'content') {
      this.renderContentSelector();
    } else if (hubType === 'datasets') {
      this.renderDatasetsSelector();
    } else if (hubType === 'wikis') {
      this.renderWikisSelector();
    }

    this.updateCalendarEventSources();
  }

  /**
   * Render blog selector with checkboxes
   */
  renderBlogSelector() {
    const selector = document.getElementById('calendar-selector');
    if (!selector || this.blogs.length === 0) {
      selector.innerHTML = '<p>No blogs available</p>';
      return;
    }

    let html = '<div class="selector-container"><h4>Select Blogs</h4><div class="checkbox-list">';

    this.blogs.forEach(blog => {
      const checked = this.selectedBlogIds.has(blog.id) ? 'checked' : '';
      html += `
        <label class="selector-checkbox">
          <input type="checkbox" data-blog-id="${blog.id}" ${checked} />
          <span>${this.escapeHtml(blog.title)}</span>
        </label>
      `;
    });

    html += '</div></div>';
    selector.innerHTML = html;

    selector.querySelectorAll('input[data-blog-id]').forEach(checkbox => {
      checkbox.addEventListener('change', (e) => {
        const blogId = Number.parseInt(e.target.dataset.blogId, 10);
        if (e.target.checked) {
          this.selectedBlogIds.add(blogId);
        } else {
          this.selectedBlogIds.delete(blogId);
        }
        this.updateCalendarEventSources();
      });
    });

    if (this.selectedBlogIds.size > 0) {
      this.updateCalendarEventSources();
    }
  }

  /**
   * Render calendar selector with checkboxes
   */
  renderCalendarSelector() {
    const selector = document.getElementById('calendar-selector');
    if (!selector || this.calendars.length === 0) {
      selector.innerHTML = '<p>No calendars available</p>';
      return;
    }

    let html = '<div class="selector-container"><h4>Select Calendars</h4><div class="checkbox-list">';

    this.calendars.forEach(calendar => {
      const checked = this.selectedCalendarIds.has(calendar.id) ? 'checked' : '';
      html += `
        <label class="selector-checkbox">
          <input type="checkbox" data-calendar-id="${calendar.id}" ${checked} />
          <span>${this.escapeHtml(calendar.title)}</span>
        </label>
      `;
    });

    html += '</div></div>';
    selector.innerHTML = html;

    // Set up event listeners for calendar checkboxes
    selector.querySelectorAll('input[data-calendar-id]').forEach(checkbox => {
      checkbox.addEventListener('change', (e) => {
        const calendarId = Number.parseInt(e.target.dataset.calendarId, 10);
        if (e.target.checked) {
          this.selectedCalendarIds.add(calendarId);
        } else {
          this.selectedCalendarIds.delete(calendarId);
        }
        this.updateCalendarEventSources();
      });
    });

    // Load initial events if any calendars are selected
    if (this.selectedCalendarIds.size > 0) {
      this.updateCalendarEventSources();
    }
  }

  /**
   * Load blog posts for selected blogs
   */
  loadBlogPosts() {
    if (this.selectedBlogIds.size === 0) {
      this.initializeFullCalendar([]);
      return;
    }

    const blogIds = Array.from(this.selectedBlogIds).join(',');

    fetch(`/json/blogs/posts?blogIds=${blogIds}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          const events = (Array.isArray(data.data) ? data.data : []).map(post => ({
            id: `post-${post.id}`,
            title: post.title,
            start: post.publishDate || post.created,
            extendedProps: {
              type: 'blog-post',
              content: post.content,
              blogId: post.blogId
            }
          }));
          this.initializeFullCalendar(events);
        }
      })
      .catch(error => console.error('Error loading blog posts:', error));
  }

  /**
   * Load calendar events for selected calendars
   */
  loadCalendarEvents() {
    if (this.selectedCalendarIds.size === 0) {
      this.initializeFullCalendar([]);
      return;
    }

    const calendarIds = Array.from(this.selectedCalendarIds).join(',');

    fetch(`/json/calendars/events?calendarIds=${calendarIds}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json'
      }
    })
      .then(response => response.json())
      .then(data => {
        if (data.status === 'ok' && data.data) {
          const events = (Array.isArray(data.data) ? data.data : []).map(event => ({
            id: `event-${event.id}`,
            title: event.title,
            start: event.eventDate || event.startDate,
            end: event.endDate,
            extendedProps: {
              type: 'calendar-event',
              description: event.description,
              calendarId: event.calendarId
            }
          }));
          this.initializeFullCalendar(events);
        }
      })
      .catch(error => console.error('Error loading calendar events:', error));
  }

  /**
   * Initialize FullCalendar with events
   */
  initializeFullCalendar(events) {
    const calendarContainer = document.getElementById('calendar-container');
    if (!calendarContainer) return;

    // Clear existing calendar
    calendarContainer.innerHTML = '';

    // Only show calendar if there are events or a hub type is selected
    if (events.length === 0 && !this.currentHubType) {
      return;
    }

    // Create new calendar if FullCalendar is available
    if (typeof FullCalendar !== 'undefined') {
      this.calendar = new FullCalendar.Calendar(calendarContainer, {
        initialView: 'dayGridMonth',
        headerToolbar: {
          left: 'prev,next today',
          center: 'title',
          right: 'dayGridMonth,listMonth'
        },
        events: events,
        eventClick: (info) => this.handleEventClick(info.event),
        dateClick: (info) => this.handleDateClick(info.date),
        height: 'auto'
      });

      this.calendar.render();
    }
  }

  /**
   * Handle calendar event click
   */
  handleEventClick(event) {
    const props = event.extendedProps;

    if (props.type === 'blog-post') {
      this.showBlogPostModal(event);
    } else if (props.type === 'calendar-event') {
      this.showCalendarEventModal(event);
    }
  }

  /**
   * Handle calendar date click
   */
  handleDateClick(date) {
    if (this.currentHubType === 'blogs') {
      this.showNewBlogPostModal(date);
    } else if (this.currentHubType === 'calendars') {
      this.showNewCalendarEventModal(date);
    }
  }

  /**
   * Show blog post modal
   */
  showBlogPostModal(post) {
    alert(`Blog Post: ${post.title}\n\nDate: ${post.start}`);
    // Can be enhanced to show full modal with edit options
  }

  /**
   * Show calendar event modal
   */
  showCalendarEventModal(event) {
    alert(`Event: ${event.title}\n\nDate: ${event.start}`);
    // Can be enhanced to show full modal with edit options
  }

  /**
   * Show new blog post modal for selected date
   */
  showNewBlogPostModal(date) {
    const modal = document.getElementById('new-blog-post-modal');
    if (modal) {
      modal.style.display = 'block';
    }
  }

  /**
   * Show new calendar event modal for selected date
   */
  showNewCalendarEventModal(date) {
    // Can be enhanced to show modal for creating new calendar event
    alert(`Create new event on ${date.toDateString()}`);
  }

  /**
   * Escape HTML special characters
   */
  escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Refresh hub content
   */
  refresh() {
    this.loadBlogs();
    this.loadCalendars();
  }

  /**
   * Update calendar event sources dynamically
   */
  updateCalendarEventSources() {
    if (!this.calendar) return;

    this.calendar.getEventSources().forEach(source => source.remove());

    if (this.showUSHolidays) {
      this.calendar.addEventSource({
        url: '/json/calendar?showHolidays=true',
        color: '#dc3545',
        textColor: 'white'
      });
    }

    if (this.currentHubType === 'blogs' && this.selectedBlogIds.size > 0) {
      const blogIds = Array.from(this.selectedBlogIds).join(',');
      this.calendar.addEventSource({
        url: `/json/blogs/posts?blogIds=${blogIds}`,
        color: '#007bff'
      });
    } else if (this.currentHubType === 'calendars' && this.selectedCalendarIds.size > 0) {
      const calendarIds = Array.from(this.selectedCalendarIds).join(',');
      this.calendar.addEventSource({
        url: `/json/calendars/events?calendarIds=${calendarIds}`,
        color: '#28a745'
      });
    } else if (this.currentHubType === 'pages') {
      const pageIds = Array.from(this.selectedPageIds).join(',');
      const query = pageIds ? `?pageIds=${pageIds}` : '';
      this.calendar.addEventSource({
        url: `/json/pages/publish-dates${query}`,
        color: '#6c757d'
      });
    } else if (this.currentHubType === 'content') {
      const contentIds = Array.from(this.selectedContentIds).join(',');
      const query = contentIds ? `?contentIds=${contentIds}` : '';
      this.calendar.addEventSource({
        url: `/json/content/update-dates${query}`,
        color: '#17a2b8'
      });
    } else if (this.currentHubType === 'datasets') {
      const datasetIds = Array.from(this.selectedDatasetIds).join(',');
      const query = datasetIds ? `?datasetIds=${datasetIds}` : '';
      this.calendar.addEventSource({
        url: `/json/datasets/update-dates${query}`,
        color: '#ffc107'
      });
    } else if (this.currentHubType === 'wikis') {
      const wikiIds = Array.from(this.selectedWikiIds).join(',');
      const query = wikiIds ? `?wikiIds=${wikiIds}` : '';
      this.calendar.addEventSource({
        url: `/json/wikis/update-dates${query}`,
        color: '#fd7e14'
      });
    }

    this.calendar.refetchEvents();
  }

  /**
   * Render pages selector (placeholder for now)
   */
  renderPagesSelector() {
    const selector = document.getElementById('calendar-selector');
    if (!selector) return;
    selector.innerHTML = '<p style="padding: 15px; color: var(--editor-text-muted);">Page publish dates will appear on the calendar when pages are published.</p>';
  }

  /**
   * Render content selector (placeholder for now)
   */
  renderContentSelector() {
    const selector = document.getElementById('calendar-selector');
    if (!selector) return;
    selector.innerHTML = '<p style="padding: 15px; color: var(--editor-text-muted);">Content update dates will appear on the calendar.</p>';
  }

  /**
   * Render datasets selector (placeholder for now)
   */
  renderDatasetsSelector() {
    const selector = document.getElementById('calendar-selector');
    if (!selector) return;
    selector.innerHTML = '<p style="padding: 15px; color: var(--editor-text-muted);">Dataset update dates will appear on the calendar.</p>';
  }

  /**
   * Render wikis selector (placeholder for now)
   */
  renderWikisSelector() {
    const selector = document.getElementById('calendar-selector');
    if (!selector) return;
    selector.innerHTML = '<p style="padding: 15px; color: var(--editor-text-muted);">Wiki update dates will appear on the calendar.</p>';
  }
}
