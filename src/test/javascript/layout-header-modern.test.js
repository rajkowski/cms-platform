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
  const textarea = drawer.querySelector('textarea');
  const messages = document.createElement('div');
  messages.id = 'askAIMessages';
  Object.defineProperty(messages, 'scrollHeight', { value: 500 });
  drawer.appendChild(messages);
  expect(drawer.parentElement).toBe(document.body);
  trigger.click();
  expect(drawer.classList.contains('open')).toBe(true);
  expect(drawer.getAttribute('aria-hidden')).toBe('false');
  expect(trigger.getAttribute('aria-expanded')).toBe('true');
  expect(document.activeElement).toBe(textarea);
  expect(messages.scrollTop).toBe(500);
  textarea.click();
  expect(drawer.classList.contains('open')).toBe(true);
  document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
  expect(drawer.classList.contains('open')).toBe(false);
  expect(drawer.getAttribute('aria-hidden')).toBe('true');
  expect(trigger.getAttribute('aria-expanded')).toBe('false');
  expect(document.activeElement).toBe(trigger);
  trigger.click();
  document.body.click();
  expect(drawer.classList.contains('open')).toBe(false);
  expect(trigger.getAttribute('aria-expanded')).toBe('false');
});

test('Ask AI submits a question and renders answers and safe sources as text', async () => {
  initialize('<button data-header-control="askAI" aria-expanded="false">Ask AI</button><div id="askAIDrawer" aria-hidden="true"><button data-header-control="closeAskAI">Close</button><div id="askAIMessages"></div><form data-ask-ai-form data-context-path="/cms" action="/cms/json/askAI"><input name="token" value="test-token"><textarea name="question"></textarea><button type="submit">Send</button></form></div>');
  const request = deferred();
  fetch.mockReturnValue(request.promise);
  const trigger = document.querySelector('[data-header-control="askAI"]');
  const drawer = document.getElementById('askAIDrawer');
  const page = document.createElement('div');
  page.className = 'web-content';
  page.innerHTML = '<p>Visible page content</p><p style="display:none">Hidden content</p><form>Private form text<input value="secret"></form><script>ignored</script>';
  document.body.appendChild(page);
  const outside = document.createElement('article');
  outside.textContent = 'Outside page content';
  document.body.appendChild(outside);
  trigger.click();
  drawer.querySelector('[data-header-control="closeAskAI"]').click();
  expect(drawer.classList.contains('open')).toBe(false);
  const textarea = drawer.querySelector('textarea');
  textarea.value = 'Question';
  const submit = new Event('submit', { bubbles: true, cancelable: true });
  drawer.querySelector('form').dispatchEvent(submit);
  expect(submit.defaultPrevented).toBe(true);
  expect(textarea.value).toBe('');
  expect(textarea.disabled).toBe(true);
  expect(fetch.mock.calls[0][0]).toBe('http://localhost/cms/json/askAI');
  expect(fetch.mock.calls[0][1].body.get('token')).toBe('test-token');
  expect(fetch.mock.calls[0][1].body.get('question')).toBe('Question');
  expect(fetch.mock.calls[0][1].body.get('pageContext')).toBe('Visible page content');
  expect(drawer.querySelector('.ask-ai-pending').textContent).toContain('Searching');
  expect(drawer.querySelector('.ask-ai-pending .fa-spinner').className).toBe('fa-solid fa-spinner fa-spin');
  request.resolve({ ok: true, json: async () => ({ status: 'ok', data: { answer: '<img src=x>', sources: [{ title: 'Source', link: '/page' }, { title: 'Bad', link: '/\\evil.test' }] } }) });
  await settle();
  expect(textarea.disabled).toBe(false);
  expect(drawer.querySelector('.ask-ai-pending')).toBeNull();
  expect(drawer.querySelector('.ask-ai-message-assistant').textContent).toContain('<img src=x>');
  expect(drawer.querySelector('img')).toBeNull();
  expect(drawer.querySelectorAll('.ask-ai-sources a')).toHaveLength(1);
  expect(drawer.querySelector('.ask-ai-sources a').getAttribute('href')).toBe('/cms/page');
});

test('Ask AI submits on Enter but preserves Shift+Enter and composition', () => {
  initialize('<div id="askAIDrawer"><div id="askAIMessages"></div><form data-ask-ai-form action="/json/askAI"><textarea name="question" required></textarea><button type="submit">Send</button></form></div>');
  const textarea = document.querySelector('#askAIDrawer textarea');
  const shiftEnter = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true, bubbles: true, cancelable: true });
  const composingEnter = new KeyboardEvent('keydown', { key: 'Enter', isComposing: true, bubbles: true, cancelable: true });
  textarea.value = 'First line\nSecond line';
  textarea.dispatchEvent(shiftEnter);
  textarea.dispatchEvent(composingEnter);
  expect(shiftEnter.defaultPrevented).toBe(false);
  expect(composingEnter.defaultPrevented).toBe(false);
  expect(fetch).not.toHaveBeenCalled();
  const enter = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
  textarea.dispatchEvent(enter);
  expect(enter.defaultPrevented).toBe(true);
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(fetch.mock.calls[0][1].body.get('question')).toBe('First line\nSecond line');
  expect(fetch.mock.calls[0][1].body.has('pageContext')).toBe(false);
});

test('Ask AI limits page context and excludes hidden or interactive content', () => {
  initialize('<div id="askAIDrawer"><div id="askAIMessages"></div><form data-ask-ai-form action="/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>');
  const page = document.createElement('div');
  page.className = 'web-content';
  page.innerHTML = '<div hidden>Hidden</div><div style="visibility:hidden">Invisible</div><nav>Navigation</nav><form>Form</form><p></p>';
  page.querySelector('p').textContent = 'x'.repeat(13000);
  document.body.appendChild(page);
  document.querySelector('#askAIDrawer textarea').value = 'Question';
  document.querySelector('#askAIDrawer form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  expect(fetch.mock.calls[0][1].body.get('pageContext')).toBe('x'.repeat(12000));
});

test('Ask AI restores a failed question for retry', async () => {
  initialize('<div id="askAIDrawer"><div id="askAIMessages"></div><form data-ask-ai-form action="/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>');
  fetch.mockResolvedValue({ ok: false, json: async () => ({ status: 'error', error: 'AI unavailable' }) });
  const drawer = document.getElementById('askAIDrawer');
  const textarea = drawer.querySelector('textarea');
  textarea.value = 'Question';
  drawer.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  await settle();
  expect(textarea.value).toBe('Question');
  expect(textarea.disabled).toBe(false);
  expect(drawer.querySelector('#askAIMessages').textContent).toContain('AI unavailable');
});

test('Ask AI restores saved messages and safe sources after navigation', async () => {
  const markup = '<div id="askAIDrawer"><button data-ask-ai-clear>Clear</button><div id="askAIMessages"></div><form data-ask-ai-form data-context-path="/cms" data-user-id="42" action="/cms/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>';
  initialize(markup);
  fetch.mockResolvedValue({ ok: true, json: async () => ({ status: 'ok', data: { answer: '<b>Answer</b>', sources: [{ title: 'Source', link: '/page' }, { title: 'Unsafe', link: '//evil.test' }, { title: 'Unsafe', link: '/\n/evil.test' }] } }) });
  document.querySelector('#askAIDrawer textarea').value = 'Question';
  document.querySelector('#askAIDrawer form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  await settle();
  initialize(markup);
  const messages = document.querySelector('#askAIMessages');
  expect(messages.querySelector('.ask-ai-message-user').textContent).toBe('Question');
  expect(messages.querySelector('.ask-ai-message-assistant').textContent).toContain('<b>Answer</b>');
  expect(messages.querySelector('b')).toBeNull();
  expect(messages.querySelectorAll('.ask-ai-sources a')).toHaveLength(1);
  expect(messages.querySelector('.ask-ai-sources a').getAttribute('href')).toBe('/cms/page');
  initialize(markup.replace('data-user-id="42"', 'data-user-id="43"'));
  expect(document.querySelector('#askAIMessages').textContent).toBe('');
  initialize(markup);
  document.querySelector('[data-ask-ai-clear]').click();
  initialize(markup);
  expect(document.querySelector('#askAIMessages').textContent).toBe('');
});

test('Ask AI separates exchanges in restored, collapsed, and new history', async () => {
  localStorage.setItem('askAI::42', JSON.stringify([
    { type: 'user', text: 'First question' },
    { type: 'assistant', text: 'First answer' },
    { type: 'user', text: 'Second question' },
    { type: 'assistant', text: 'Second answer' }
  ]));
  initialize('<button data-header-control="askAI">Ask AI</button><div id="askAIDrawer"><div id="askAIMessages"><button data-ask-ai-clear>Clear</button></div><form data-ask-ai-form data-user-id="42"><textarea></textarea><button type="submit">Send</button></form></div>');
  const messages = document.querySelector('#askAIMessages');
  const questions = messages.querySelectorAll('.ask-ai-message-user');
  expect(questions[0].querySelector('hr')).toBeNull();
  expect(questions[1].firstElementChild.matches('hr.ask-ai-separator')).toBe(true);
  document.querySelector('[data-header-control="askAI"]').click();
  expect(messages.querySelector('.ask-ai-previous hr.ask-ai-separator')).not.toBeNull();
  fetch.mockResolvedValue({ ok: true, json: async () => ({ status: 'ok', data: { answer: 'Third answer' } }) });
  document.querySelector('#askAIDrawer textarea').value = 'Third question';
  document.querySelector('#askAIDrawer form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  await settle();
  expect(messages.querySelectorAll('hr.ask-ai-separator')).toHaveLength(2);
  expect(messages.querySelector(':scope > .ask-ai-message-user > hr.ask-ai-separator')).not.toBeNull();
  document.querySelector('[data-ask-ai-clear]').click();
  expect(messages.querySelector('hr')).toBeNull();
});

test('Ask AI positions a new answer at its beginning without scrolling the page', async () => {
  initialize('<button data-header-control="askAI">Ask AI</button><div id="askAIDrawer"><div id="askAIMessages"></div><form data-ask-ai-form action="/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>');
  document.querySelector('[data-header-control="askAI"]').click();
  const messages = document.querySelector('#askAIMessages');
  const input = document.querySelector('#askAIDrawer textarea');
  const focus = jest.spyOn(input, 'focus');
  Object.defineProperty(messages, 'scrollHeight', { value: 1800 });
  jest.spyOn(Element.prototype, 'getBoundingClientRect').mockImplementation(function () {
    if (this === messages) return { top: 100 };
    if (this.classList.contains('ask-ai-message-assistant')) return { top: 600 - messages.scrollTop };
    return { top: 200 - messages.scrollTop };
  });
  fetch.mockResolvedValue({ ok: true, json: async () => ({ status: 'ok', data: { answer: 'Long answer' } }) });
  input.value = 'Question';
  document.querySelector('#askAIDrawer form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  await settle();
  expect(messages.scrollTop).toBe(484);
  expect(focus).toHaveBeenLastCalledWith({ preventScroll: true });
});

test('Ask AI collapses previous discussion on opening and keeps new responses visible', async () => {
  localStorage.setItem('askAI::42', JSON.stringify([{ type: 'user', text: 'Earlier question' }, { type: 'assistant', text: 'Earlier answer', sources: [{ title: 'Source', link: '/page' }] }]));
  initialize('<button data-header-control="askAI">Ask AI</button><div id="askAIDrawer"><button data-header-control="closeAskAI">Close</button><div id="askAIMessages"><button type="button" data-ask-ai-clear>Clear</button></div><form data-ask-ai-form data-user-id="42" action="/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>');
  const trigger = document.querySelector('[data-header-control="askAI"]');
  const drawer = document.querySelector('#askAIDrawer');
  trigger.click();
  const previous = drawer.querySelector('details.ask-ai-previous');
  expect(previous.open).toBe(false);
  expect(previous.querySelector('summary').firstChild.textContent).toBe('Previous discussion');
  expect(previous.querySelector('summary [data-ask-ai-clear]')).not.toBeNull();
  expect(previous.querySelectorAll('.ask-ai-message')).toHaveLength(2);
  expect(previous.querySelector('a').getAttribute('href')).toBe('/page');
  previous.open = true;
  const request = deferred();
  fetch.mockReturnValue(request.promise);
  drawer.querySelector('textarea').value = 'New question';
  drawer.querySelector('form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  drawer.querySelector('[data-header-control="closeAskAI"]').click();
  trigger.click();
  expect(previous.open).toBe(false);
  expect(previous.textContent).toContain('New question');
  expect(drawer.querySelector('.ask-ai-pending').closest('details')).toBeNull();
  request.resolve({ ok: true, json: async () => ({ status: 'ok', data: { answer: 'New answer' } }) });
  await settle();
  expect(drawer.querySelector('.ask-ai-message-assistant:last-child').closest('details')).toBeNull();
  drawer.querySelector('[data-ask-ai-clear]').click();
  expect(drawer.querySelector('.ask-ai-previous')).toBeNull();
  expect(drawer.querySelector('#askAIMessages > [data-ask-ai-clear]')).not.toBeNull();
  drawer.querySelector('[data-header-control="closeAskAI"]').click();
  trigger.click();
  expect(drawer.querySelector('.ask-ai-previous')).toBeNull();
});

test('Ask AI clear removes saved chat and ignores an in-flight answer', async () => {
  const markup = '<div id="askAIDrawer"><div id="askAIMessages"><button type="button" data-ask-ai-clear>Clear</button></div><form data-ask-ai-form action="/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>';
  initialize(markup);
  expect(document.querySelector('#askAIMessages').contains(document.querySelector('[data-ask-ai-clear]'))).toBe(true);
  expect(document.querySelector('[data-ask-ai-form]').contains(document.querySelector('[data-ask-ai-clear]'))).toBe(false);
  const request = deferred();
  fetch.mockReturnValue(request.promise);
  const textarea = document.querySelector('#askAIDrawer textarea');
  textarea.value = 'Question';
  document.querySelector('#askAIDrawer form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  textarea.value = 'Draft to keep';
  document.querySelector('[data-ask-ai-clear]').click();
  expect(textarea.disabled).toBe(false);
  expect(textarea.value).toBe('Draft to keep');
  expect(document.querySelectorAll('#askAIMessages .ask-ai-message')).toHaveLength(0);
  expect(document.querySelector('#askAIMessages [data-ask-ai-clear]')).not.toBeNull();
  request.resolve({ ok: true, json: async () => ({ status: 'ok', data: { answer: 'Late answer' } }) });
  await settle();
  initialize(markup);
  expect(document.querySelectorAll('#askAIMessages .ask-ai-message')).toHaveLength(0);
  expect(document.querySelector('#askAIMessages [data-ask-ai-clear]')).not.toBeNull();
});

test('Ask AI works when browser storage is unavailable', async () => {
  jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Denied'); });
  jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Denied'); });
  jest.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => { throw new Error('Denied'); });
  initialize('<div id="askAIDrawer"><button data-ask-ai-clear>Clear</button><div id="askAIMessages"></div><form data-ask-ai-form action="/json/askAI"><textarea name="question"></textarea><button type="submit">Send</button></form></div>');
  fetch.mockResolvedValue({ ok: true, json: async () => ({ status: 'ok', data: { answer: 'Answer' } }) });
  document.querySelector('#askAIDrawer textarea').value = 'Question';
  document.querySelector('#askAIDrawer form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  await settle();
  expect(document.querySelector('#askAIMessages').textContent).toContain('Answer');
  document.querySelector('[data-ask-ai-clear]').click();
  expect(document.querySelector('#askAIMessages').textContent).toBe('');
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