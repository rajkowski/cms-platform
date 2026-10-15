const fs = require('node:fs');
const path = require('node:path');

const script = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/javascript/layout-header-modern.js'), 'utf8');
const regionScript = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/javascript/site-region-selector.js'), 'utf8');

function initialize(markup = '') {
  document.body.innerHTML = `<header data-header-modern data-theme="light" data-scroll-behavior="none" data-scroll-threshold="100">${markup}</header>`;
  window.eval(script);
  document.dispatchEvent(new Event('DOMContentLoaded'));
  return document.querySelector('[data-header-modern]');
}

function searchMarkup() {
  return '<form action="http://localhost/search" data-header-search data-min-chars="3" data-debounce-ms="300"><input name="query"/><div data-header-search-panel><div data-header-search-results></div></div></form><div data-header-search-backdrop></div>';
}

function changeQuery(value) {
  const input = document.querySelector('input[name="query"]');
  input.value = value;
  input.dispatchEvent(new Event('input', { bubbles: true }));
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((fulfill, fail) => { resolve = fulfill; reject = fail; });
  return { promise, resolve, reject };
}

async function settle() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}

beforeEach(() => {
  jest.useFakeTimers();
  Object.defineProperty(window, 'scrollY', { configurable: true, value: 0 });
  localStorage.clear();
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.clearAllTimers();
  jest.useRealTimers();
  jest.restoreAllMocks();
  document.body.innerHTML = '';
});

test('initializes missing optional controls once without errors', () => {
  const root = initialize();
  expect(root.dataset.headerInitialized).toBe('true');
  expect(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))).not.toThrow();
  window.eval(script);
  expect(root.dataset.headerInitialized).toBe('true');
});

test('debounces valid queries and never submits short input', async () => {
  initialize(searchMarkup());
  changeQuery('ab');
  jest.advanceTimersByTime(1000);
  expect(fetch).not.toHaveBeenCalled();
  fetch.mockResolvedValue({ ok: true, text: async () => '<div class="search-main-content"><a href="/result">Result</a></div>' });
  changeQuery('abc');
  jest.advanceTimersByTime(299);
  expect(fetch).not.toHaveBeenCalled();
  jest.advanceTimersByTime(1);
  await settle();
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(fetch.mock.calls[0][0]).toContain('query=abc');
  expect(document.querySelector('[data-header-search-results]').textContent).toBe('Result');
});

test('shortening a pending query prevents stale success', async () => {
  initialize(searchMarkup());
  const request = deferred();
  fetch.mockReturnValue(request.promise);
  changeQuery('long query');
  jest.advanceTimersByTime(300);
  changeQuery('a');
  request.resolve({ ok: true, text: async () => '<div class="search-main-content"><a href="/old">Old</a></div>' });
  await settle();
  expect(document.querySelector('[data-header-search-panel]').classList.contains('is-open')).toBe(false);
  expect(document.querySelector('[data-header-search-results]').textContent).not.toBe('Old');
});

test('newer input wins over out-of-order success and failure', async () => {
  initialize(searchMarkup());
  const first = deferred();
  const second = deferred();
  fetch.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise);
  changeQuery('first');
  jest.advanceTimersByTime(300);
  changeQuery('second');
  jest.advanceTimersByTime(300);
  second.resolve({ ok: true, text: async () => '<div class="search-main-content"><a href="/new">New</a></div>' });
  await settle();
  first.reject(new Error('Stale error'));
  await settle();
  expect(document.querySelector('[data-header-search-results]').textContent).toBe('New');
});

test('Escape prevents a pending response from reopening results', async () => {
  initialize(searchMarkup());
  const request = deferred();
  fetch.mockReturnValue(request.promise);
  changeQuery('pending');
  jest.advanceTimersByTime(300);
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  request.resolve({ ok: true, text: async () => '<div class="search-main-content"><a href="/old">Old</a></div>' });
  await settle();
  expect(document.querySelector('[data-header-search-panel]').getAttribute('aria-hidden')).toBe('true');
});

test('shows explicit empty and HTTP-error states', async () => {
  initialize(searchMarkup());
  fetch.mockResolvedValueOnce({ ok: true, text: async () => '<div class="search-main-content">Empty</div>' });
  changeQuery('empty');
  jest.advanceTimersByTime(300);
  await settle();
  expect(document.querySelector('[data-header-search-results]').textContent).toBe('No results found');
  fetch.mockResolvedValueOnce({ ok: false });
  changeQuery('error');
  jest.advanceTimersByTime(300);
  await settle();
  expect(document.querySelector('[data-header-search-results]').textContent).toBe('Error loading results');
});

test('strips executable content from returned result markup', async () => {
  initialize(searchMarkup());
  fetch.mockResolvedValue({ ok: true, text: async () => '<div class="search-main-content"><script>alert(1)</script><a href="/safe" onclick="alert(1)">Safe</a><a href="javascript:alert(1)">Unsafe</a></div>' });
  changeQuery('safe');
  jest.advanceTimersByTime(300);
  await settle();
  const results = document.querySelector('[data-header-search-results]');
  expect(results.querySelector('script')).toBeNull();
  expect(results.querySelector('[onclick]')).toBeNull();
  expect(results.querySelector('a[href^="javascript:"]')).toBeNull();
});

test('mobile and account controls expose synchronized state and Escape focus', () => {
  initialize('<nav data-header-nav></nav><button data-header-control="mobileMenu"><i class="fa-bars"></i></button><div data-header-account><button data-header-account-trigger>Account</button></div>');
  const mobile = document.querySelector('[data-header-control="mobileMenu"]');
  mobile.click();
  expect(mobile.getAttribute('aria-expanded')).toBe('true');
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  expect(mobile.getAttribute('aria-expanded')).toBe('false');
  const account = document.querySelector('[data-header-account-trigger]');
  account.click();
  expect(account.getAttribute('aria-expanded')).toBe('true');
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  expect(account.getAttribute('aria-expanded')).toBe('false');
  expect(document.activeElement).toBe(account);
});

test('Ask AI control opens and closes its drawer with keyboard focus management', () => {
  initialize('<button data-header-control="askAI" aria-controls="askAIDrawer" aria-expanded="false">Ask AI</button><div id="askAIDrawer" role="dialog" aria-hidden="true" tabindex="-1"><button data-header-control="closeAskAI">Close</button><form data-ask-ai-form><textarea></textarea><button type="submit">Send</button></form></div>');
  const trigger = document.querySelector('[data-header-control="askAI"]');
  const drawer = document.getElementById('askAIDrawer');
  const close = drawer.querySelector('[data-header-control="closeAskAI"]');
  expect(drawer.parentElement).toBe(document.body);
  trigger.click();
  expect(drawer.classList.contains('open')).toBe(true);
  expect(drawer.getAttribute('aria-hidden')).toBe('false');
  expect(trigger.getAttribute('aria-expanded')).toBe('true');
  expect(document.activeElement).toBe(close);
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  expect(drawer.classList.contains('open')).toBe(false);
  expect(drawer.getAttribute('aria-hidden')).toBe('true');
  expect(trigger.getAttribute('aria-expanded')).toBe('false');
  expect(document.activeElement).toBe(trigger);
});

test('Ask AI drawer close button and form submit are handled without inline handlers', () => {
  initialize('<button data-header-control="askAI" aria-expanded="false">Ask AI</button><div id="askAIDrawer" aria-hidden="true"><button data-header-control="closeAskAI">Close</button><form data-ask-ai-form><textarea></textarea><button type="submit">Send</button></form></div>');
  const trigger = document.querySelector('[data-header-control="askAI"]');
  const drawer = document.getElementById('askAIDrawer');
  trigger.click();
  drawer.querySelector('[data-header-control="closeAskAI"]').click();
  expect(drawer.classList.contains('open')).toBe(false);
  const textarea = drawer.querySelector('textarea');
  textarea.value = 'Question';
  const submit = new Event('submit', { bubbles: true, cancelable: true });
  drawer.querySelector('form').dispatchEvent(submit);
  expect(submit.defaultPrevented).toBe(true);
  expect(textarea.value).toBe('');
});

test('theme controls work when browser storage access is denied', () => {
  jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Denied'); });
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Denied'); });
  const root = initialize('<button data-header-control="appearance"><i class="fa-moon" data-header-theme-icon></i></button>');
  expect(() => root.querySelector('button').click()).not.toThrow();
  expect(root.dataset.theme).toBe('dark');
  expect(document.documentElement.dataset.theme).toBe('dark');
});

test('none scrolling mode never adds a hide transform', () => {
  const root = initialize();
  Object.defineProperty(window, 'scrollY', { configurable: true, value: 500 });
  window.dispatchEvent(new Event('scroll'));
  expect(root.style.transform).toBe('');
});

test('detached panels ignore late successful responses', async () => {
  const root = initialize(searchMarkup());
  const results = root.querySelector('[data-header-search-results]');
  const request = deferred();
  fetch.mockReturnValue(request.promise);
  changeQuery('detached');
  jest.advanceTimersByTime(300);
  root.remove();
  request.resolve({ ok: true, text: async () => '<div class="search-main-content"><a href="/old">Detached update</a></div>' });
  await settle();
  expect(results.textContent).not.toBe('Detached update');
});

test('outside click and full-page submission close results', () => {
  initialize(searchMarkup());
  changeQuery('visible');
  document.body.click();
  expect(document.querySelector('[data-header-search-panel]').getAttribute('aria-hidden')).toBe('true');
  changeQuery('visible');
  const submit = new Event('submit', { bubbles: true, cancelable: true });
  document.querySelector('form').dispatchEvent(submit);
  expect(submit.defaultPrevented).toBe(false);
  expect(document.querySelector('[data-header-search-panel]').getAttribute('aria-hidden')).toBe('true');
});

test('focus only reopens the current cached query', async () => {
  initialize(searchMarkup());
  fetch.mockResolvedValue({ ok: true, text: async () => '<div class="search-main-content"><a href="/result">Current</a></div>' });
  changeQuery('current');
  jest.advanceTimersByTime(300);
  await settle();
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  const input = document.querySelector('input');
  input.dispatchEvent(new Event('focus'));
  expect(document.querySelector('[data-header-search-panel]').getAttribute('aria-hidden')).toBe('false');
  expect(fetch).toHaveBeenCalledTimes(1);
  input.value = 'different';
  input.dispatchEvent(new Event('focus'));
  expect(document.querySelector('[data-header-search-results]').textContent).toBe('Loading...');
});

test('resizing constrains the result panel to the viewport', () => {
  initialize(searchMarkup());
  changeQuery('visible');
  Object.defineProperty(window, 'innerWidth', { configurable: true, value: 375 });
  window.dispatchEvent(new Event('resize'));
  expect(parseFloat(document.querySelector('[data-header-search-panel]').style.width)).toBeLessThanOrEqual(343);
});

test('Escape dismisses hover navigation until the pointer leaves', () => {
  const original = window.matchMedia;
  window.matchMedia = () => ({ matches: true });
  try {
    const root = initialize('<nav data-header-nav><div class="nav-item has-dropdown"><button data-header-dropdown-toggle>Open</button><div class="dropdown-menu"></div></div></nav>');
    const item = root.querySelector('.nav-item');
    const toggle = root.querySelector('button');
    item.dispatchEvent(new MouseEvent('mouseenter'));
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(toggle);
    item.dispatchEvent(new MouseEvent('mouseenter'));
    expect(toggle.getAttribute('aria-expanded')).toBe('false');
    item.dispatchEvent(new MouseEvent('mouseleave'));
    item.dispatchEvent(new MouseEvent('mouseenter'));
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
  } finally {
    window.matchMedia = original;
  }
});

test('open navigation is not hidden by downward scrolling', () => {
  const root = initialize('<nav data-header-nav class="show"></nav>');
  root.dataset.scrollBehavior = 'hide-on-down';
  Object.defineProperty(window, 'scrollY', { configurable: true, value: 500 });
  window.dispatchEvent(new Event('scroll'));
  expect(root.style.transform).toBe('translateY(0)');
});

test('all optional-control combinations initialize without exceptions', () => {
  const controls = [searchMarkup(), '<nav data-header-nav></nav><button data-header-control="mobileMenu"><i></i></button>', '<div data-header-account><button data-header-account-trigger>Account</button></div>', '<div data-header-cart><button data-header-cart-trigger>Cart</button></div>', '<button data-header-control="appearance"><i data-header-theme-icon></i></button>', '<button data-header-control="askAI">Ask AI</button>'];
  for (let mask = 0; mask < 64; mask++) {
    expect(() => initialize(controls.filter((control, index) => mask & (1 << index)).join(''))).not.toThrow();
    expect(() => document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))).not.toThrow();
  }
});

test('the existing region owner preserves scroll, focus, and cookie identity', () => {
  window.REGION_CONFIG = [{ code: 'EU', name: 'Europe', values: ['EU'] }, { code: 'US', name: 'United States', values: ['US'] }];
  window.REGION_CODES = ['EU', 'US'];
  window.USER_REGION_CODE = 'EU';
  window.REGION_COOKIE_CODE = 'region-code';
  window.REGION_COOKIE_VALUES = 'region-values';
  window.REGION_COOKIE_EXPIRY_DAYS = 365;
  window.CAN_UNSET_REGION = false;
  window.Cookies = { get: jest.fn(() => 'EU'), set: jest.fn() };
  initialize('<button class="region-filter-trigger" data-header-control="region"><span class="btn-text">Region</span></button><div id="regionModal" style="display:none" tabindex="-1"><button data-header-control="closeRegion">Close</button><form id="regionFilterForm"><div id="regionOptionsContainer"></div></form></div>');
  window.eval(regionScript);
  document.body.style.overflow = 'scroll';
  const trigger = document.querySelector('[data-header-control="region"]');
  trigger.focus();
  trigger.click();
  const modal = document.getElementById('regionModal');
  expect(modal.style.display).toBe('block');
  expect(document.activeElement).toBe(modal);
  expect(document.body.style.overflow).toBe('hidden');
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  expect(modal.style.display).toBe('none');
  expect(document.body.style.overflow).toBe('scroll');
  expect(document.activeElement).toBe(trigger);
  expect(window.REGION_COOKIE_CODE).toBe('region-code');
  expect(window.Cookies.set).not.toHaveBeenCalled();
});

test('renders SearchJsonService JSON results for webpage without type tag', async () => {
  initialize(searchMarkup());
  const jsonResponse = {
    status: 'ok',
    meta: { type: 'search', count: 1 },
    data: [
      {
        type: 'webpage',
        title: 'About Us',
        link: '/about-us',
        lastModified: 'Oct 13, 2026',
        modifiedBy: 'Admin',
        content: 'This is the <strong>about us</strong> page.'
      }
    ]
  };
  fetch.mockResolvedValue({ ok: true, text: async () => JSON.stringify(jsonResponse) });
  changeQuery('about');
  jest.advanceTimersByTime(300);
  await settle();

  const results = document.querySelector('[data-header-search-results]');
  const item = results.querySelector('.platform-content-search-result');
  expect(item).not.toBeNull();

  const link = item.querySelector('h5 a');
  expect(link).not.toBeNull();
  expect(link.getAttribute('href')).toBe('/about-us');
  expect(link.textContent).toBe('About Us');

  // Should NOT have a type tag since type is 'webpage'
  const tag = item.querySelector('.search-result-tag');
  expect(tag).toBeNull();

  const p = item.querySelector('p');
  expect(p).not.toBeNull();
  expect(p.innerHTML).toContain('<strong>about us</strong>');

  const meta = item.querySelector('.search-result-meta');
  expect(meta).not.toBeNull();
  expect(meta.textContent).toBe('Modified by: Admin | Last modified: Oct 13, 2026');
});

test('renders search content highlight markers as bold text', async () => {
  initialize(searchMarkup());
  const jsonResponse = {
    status: 'ok',
    meta: { type: 'search', count: 1 },
    data: [
      {
        type: 'webpage',
        title: 'CBD',
        link: '/cbd',
        content: 'Search result: ${b}CBD${/b}'
      }
    ]
  };
  fetch.mockResolvedValue({ ok: true, text: async () => JSON.stringify(jsonResponse) });
  changeQuery('cbd');
  jest.advanceTimersByTime(300);
  await settle();

  const content = document.querySelector('.platform-content-search-result p');
  expect(content.textContent).toBe('Search result: CBD');
  expect(content.querySelector('strong').textContent).toBe('CBD');
});

test('renders SearchJsonService JSON results for non-webpage types with type tag', async () => {
  initialize(searchMarkup());
  const jsonResponse = {
    status: 'ok',
    meta: { type: 'search', count: 2 },
    data: [
      {
        type: 'blog',
        title: 'Latest Post',
        link: '/blog/latest-post',
        lastModified: 'Oct 10, 2026',
        modifiedBy: 'Editor',
        content: 'Blog summary',
        tags: ['news', 'updates']
      },
      {
        type: 'item',
        title: 'Product XYZ',
        link: '/products/xyz',
        content: 'Product description'
      }
    ]
  };
  fetch.mockResolvedValue({ ok: true, text: async () => JSON.stringify(jsonResponse) });
  changeQuery('latest');
  jest.advanceTimersByTime(300);
  await settle();

  const results = document.querySelector('[data-header-search-results]');
  const items = results.querySelectorAll('.platform-content-search-result');
  expect(items.length).toBe(2);

  // First item: blog with tag
  const tag1 = items[0].querySelector('h5 .search-result-tag');
  expect(tag1).not.toBeNull();
  expect(tag1.textContent).toBe('blog');
  expect(tag1.classList.contains('badge')).toBe(true);

  // Tags in first item
  const tags = items[0].querySelectorAll('.search-result-tags .badge');
  expect(tags.length).toBe(2);
  expect(tags[0].textContent).toBe('news');
  expect(tags[1].textContent).toBe('updates');

  // Second item: item with tag
  const tag2 = items[1].querySelector('h5 .search-result-tag');
  expect(tag2).not.toBeNull();
  expect(tag2.textContent).toBe('item');
});

test('renders SearchJsonService empty data array as No results found', async () => {
  initialize(searchMarkup());
  const jsonResponse = {
    status: 'ok',
    meta: { type: 'search', count: 0 },
    data: []
  };
  fetch.mockResolvedValue({ ok: true, text: async () => JSON.stringify(jsonResponse) });
  changeQuery('nothing');
  jest.advanceTimersByTime(300);
  await settle();

  const results = document.querySelector('[data-header-search-results]');
  expect(results.textContent).toBe('No results found');
});