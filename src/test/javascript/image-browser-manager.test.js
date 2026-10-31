/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const managerScript = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/javascript/image-browser-manager.js'), 'utf8');
const browserJsp = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/WEB-INF/jsp/cms/image-browser.jsp'), 'utf8');
const sitePropertiesJsp = fs.readFileSync(path.resolve(__dirname, '../../main/webapp/WEB-INF/jsp/admin/site-properties-editor.jsp'), 'utf8');
const initializationScript = browserJsp.match(/\/\/ Initialize the image browser manager([\s\S]*?)<\/script>/)[1].replaceAll('${ctx}', '/cms');

let context;

function selectionScript(inputId, previewId) {
  return browserJsp.match(/function mySubmit\(itemUrl\) \{[\s\S]*?\n      \}/)[0]
    .replace('<c:out value="${inputId}" />', inputId)
    .replace(/<c:out value="\$\{param\.previewId\}" default="\$\{inputId\}Preview" \/>/, previewId || `${inputId}Preview`);
}

function renderBrowser() {
  document.body.innerHTML = '<input id="image-search"><button id="clear-search-btn">Clear</button><div id="image-count">Loading...</div><div id="image-grid-container"></div>';
}

beforeEach(() => {
  renderBrowser();
  const sandbox = {
    document,
    URLSearchParams,
    console: { log: jest.fn(), error: jest.fn() },
    fetch: jest.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        images: [{ url: '/images/photo.jpg', filename: 'photo.jpg', width: 100, height: 80, fileLength: 1024 }],
        total: 1
      })
    }),
    IntersectionObserver: jest.fn().mockImplementation(() => ({ observe: jest.fn() })),
    mySubmit: jest.fn()
  };
  sandbox.window = sandbox;
  context = vm.createContext(sandbox);
  vm.runInContext(managerScript, context);
  jest.spyOn(context.ImageBrowserManager.prototype, 'loadImages');
});

afterEach(() => {
  jest.restoreAllMocks();
  document.body.innerHTML = '';
});

test('can execute the manager script repeatedly in the same global scope', () => {
  const constructor = context.ImageBrowserManager;
  expect(typeof constructor).toBe('function');
  expect(() => vm.runInContext(managerScript, context)).not.toThrow();
  expect(context.ImageBrowserManager).toBe(constructor);
});

test.each(['interactive', 'complete'])('initializes AJAX content immediately when the document is %s', async (readyState) => {
  jest.spyOn(document, 'readyState', 'get').mockReturnValue(readyState);

  vm.runInContext(initializationScript, context);

  expect(context.fetch).toHaveBeenCalledTimes(1);
  expect(context.fetch.mock.calls[0][0]).toBe('/cms/json/imageLibrary?page=1&limit=20');
  await context.ImageBrowserManager.prototype.loadImages.mock.results[0].value;
  expect(document.getElementById('image-count').textContent).toBe('1 image');
  expect(document.querySelector('#image-grid-container img').getAttribute('src')).toBe('/cms/images/photo.jpg');
  document.querySelector('#image-grid-container img').click();
  expect(context.mySubmit).toHaveBeenCalledWith('/cms/images/photo.jpg');
});

test('waits for DOMContentLoaded on a standalone page and initializes only once', () => {
  jest.spyOn(document, 'readyState', 'get').mockReturnValue('loading');
  const init = jest.spyOn(context.ImageBrowserManager.prototype, 'init').mockImplementation(() => {});

  vm.runInContext(initializationScript, context);
  expect(init).not.toHaveBeenCalled();
  document.dispatchEvent(new Event('DOMContentLoaded'));
  document.dispatchEvent(new Event('DOMContentLoaded'));
  expect(init).toHaveBeenCalledTimes(1);
});

test('reopening the reveal reloads and renders images with a fresh manager', async () => {
  jest.spyOn(document, 'readyState', 'get').mockReturnValue('complete');
  const init = jest.spyOn(context.ImageBrowserManager.prototype, 'init');

  vm.runInContext(initializationScript, context);
  await context.ImageBrowserManager.prototype.loadImages.mock.results[0].value;
  expect(document.querySelectorAll('#image-grid-container img')).toHaveLength(1);

  renderBrowser();
  vm.runInContext(managerScript, context);
  vm.runInContext(initializationScript, context);
  await context.ImageBrowserManager.prototype.loadImages.mock.results[1].value;

  expect(context.fetch).toHaveBeenCalledTimes(2);
  expect(init.mock.instances[0]).not.toBe(init.mock.instances[1]);
  expect(document.getElementById('image-count').textContent).toBe('1 image');
  expect(document.querySelectorAll('#image-grid-container img')).toHaveLength(1);
  document.querySelector('#image-grid-container img').click();
  expect(context.mySubmit).toHaveBeenCalledWith('/cms/images/photo.jpg');
});

test.each([
  ['imageUrl37', 'imageUrlPreview37'],
  ['imageUrl', undefined]
])('selects into the current admin frame and updates the preview for %s', async (inputId, previewId) => {
  jest.spyOn(document, 'readyState', 'get').mockReturnValue('complete');
  document.body.insertAdjacentHTML('beforeend', `<input id="${inputId}"><img id="${previewId || `${inputId}Preview`}">`);
  const topLookup = jest.fn(() => null);
  context.top = { document: { getElementById: topLookup } };
  const foundation = jest.fn();
  context.$ = jest.fn(() => ({ foundation }));
  vm.runInContext(selectionScript(inputId, previewId), context);
  vm.runInContext(initializationScript, context);
  await context.ImageBrowserManager.prototype.loadImages.mock.results[0].value;

  document.querySelector('#image-grid-container img').click();

  expect(document.getElementById(inputId).value).toBe('/cms/images/photo.jpg');
  expect(document.getElementById(previewId || `${inputId}Preview`).getAttribute('src')).toBe('/cms/images/photo.jpg');
  expect(topLookup).not.toHaveBeenCalled();
  expect(context.$).toHaveBeenCalledWith('#imageBrowserReveal');
  expect(foundation).toHaveBeenCalledWith('close');
});

test('site properties passes matching input and preview IDs when opening the reveal', () => {
  const revealScript = sitePropertiesJsp.match(/<script>\s*(\$\('#imageBrowserReveal'\)[\s\S]*?)<\/script>/)[1].replaceAll('${ctx}', '/cms');
  let onOpen;
  context.$ = jest.fn(() => ({
    on: (event, callback) => { onOpen = callback; },
    html: jest.fn()
  }));
  context.$.ajax = jest.fn(() => ({ done: jest.fn() }));
  context.currentPhotoId = 37;
  vm.runInContext(revealScript, context);

  onOpen();

  const url = new URL(context.$.ajax.mock.calls[0][0].url, 'http://localhost');
  expect(url.searchParams.get('inputId')).toBe('imageUrl37');
  expect(url.searchParams.get('previewId')).toBe('imageUrlPreview37');
  expect(sitePropertiesJsp).toContain('id="imageUrl${siteProperty.id}"');
  expect(sitePropertiesJsp).toContain('id="imageUrlPreview${siteProperty.id}"');
});
