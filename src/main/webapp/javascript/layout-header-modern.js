/*
 * Copyright 2026 Matt Rajkowski (https://www.github.com/rajkowski)
 *
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
(function () {
  'use strict';

  function initializeHeader(root) {
    if (root.dataset.headerInitialized === 'true') return;
    root.dataset.headerInitialized = 'true';
    const nav = root.querySelector('[data-header-nav]');
    const mobileToggle = root.querySelector('[data-header-control="mobileMenu"]');
    const askAITrigger = root.querySelector('[data-header-control="askAI"]');
    const askAIDrawer = root.querySelector('#askAIDrawer');
    const account = root.querySelector('[data-header-account]');
    const cart = root.querySelector('[data-header-cart]');
    const regionTrigger = root.querySelector('[data-header-control="region"]');
    const workspaces = root.querySelector('[data-header-workspaces]');
    const regionModal = root.querySelector('#regionModal');
    const form = root.querySelector('[data-header-search]');
    const input = form && form.querySelector('input[name="query"]');
    const panel = form && form.querySelector('[data-header-search-panel]');
    const results = form && form.querySelector('[data-header-search-results]');
    const backdrop = root.querySelector('[data-header-search-backdrop]');
    if (regionModal) document.body.appendChild(regionModal);
    if (askAIDrawer) document.body.appendChild(askAIDrawer);
    if (backdrop) document.body.appendChild(backdrop);

    function setAskAIDrawer(open, restoreFocus) {
      if (!askAIDrawer) return;
      askAIDrawer.classList.toggle('open', open);
      askAIDrawer.setAttribute('aria-hidden', String(!open));
      if (askAITrigger) askAITrigger.setAttribute('aria-expanded', String(open));
      if (open) {
        const closeButton = askAIDrawer.querySelector('[data-header-control="closeAskAI"]');
        if (closeButton) closeButton.focus();
      } else if (restoreFocus && askAITrigger) {
        askAITrigger.focus();
      }
    }

    if (askAIDrawer) {
      askAIDrawer.addEventListener('click', event => {
        if (event.target.closest('[data-header-control="closeAskAI"]')) setAskAIDrawer(false, true);
      });
      const askAIForm = askAIDrawer.querySelector('[data-ask-ai-form]');
      if (askAIForm) {
        askAIForm.addEventListener('submit', event => {
          event.preventDefault();
          const askAIInput = askAIForm.querySelector('textarea');
          if (askAIInput && askAIInput.value.trim()) askAIInput.value = '';
        });
      }
    }

    function applyTheme(theme) {
      const normalized = theme === 'dark' ? 'dark' : 'light';
      root.dataset.theme = normalized;
      document.documentElement.dataset.theme = normalized;
      root.querySelectorAll('[data-header-theme-icon]').forEach(icon => {
        icon.classList.toggle('fa-moon', normalized !== 'dark');
        icon.classList.toggle('fa-sun', normalized === 'dark');
      });
    }

    function toggleTheme() {
      const theme = root.dataset.theme === 'dark' ? 'light' : 'dark';
      applyTheme(theme);
      try { localStorage.setItem('theme', theme); } catch (error) { }
    }

    let initialTheme = root.dataset.theme;
    try {
      const saved = localStorage.getItem('theme');
      if (saved === 'light' || saved === 'dark') initialTheme = saved;
    } catch (error) { }
    applyTheme(initialTheme);

    function setMobile(open) {
      if (!nav || !mobileToggle) return;
      nav.classList.toggle('show', open);
      mobileToggle.setAttribute('aria-expanded', String(open));
      const icon = mobileToggle.querySelector('i');
      if (icon) {
        icon.classList.toggle('fa-bars', !open);
        icon.classList.toggle('fa-times', open);
      }
    }

    function setMenu(menu, open) {
      if (!menu) return;
      menu.classList.toggle('open', open);
      const trigger = menu.querySelector('[data-header-account-trigger], [data-header-cart-trigger], [data-header-control="workspaces"]');
      if (trigger) trigger.setAttribute('aria-expanded', String(open));
    }

    function setDropdown(item, open) {
      if (open) {
        root.querySelectorAll('.nav-item.open').forEach(other => {
          if (other !== item) setDropdown(other, false);
        });
      }
      item.classList.toggle('open', open);
      const toggle = item.querySelector('[data-header-dropdown-toggle]');
      if (toggle) toggle.setAttribute('aria-expanded', String(open));
    }

    function supportsHover() {
      return typeof window.matchMedia !== 'function' || window.matchMedia('(hover: hover)').matches;
    }

    root.querySelectorAll('.nav-item.has-dropdown').forEach(item => {
      item.addEventListener('mouseenter', () => {
        if (supportsHover() && item.dataset.headerHoverDismissed !== 'true') setDropdown(item, true);
      });
      item.addEventListener('mouseleave', () => {
        delete item.dataset.headerHoverDismissed;
        if (!item.contains(document.activeElement)) setDropdown(item, false);
      });
    });
    if (cart) {
      cart.addEventListener('mouseenter', () => { if (supportsHover() && cart.dataset.headerHoverDismissed !== 'true') setMenu(cart, true); });
      cart.addEventListener('mouseleave', () => {
        delete cart.dataset.headerHoverDismissed;
        if (!cart.contains(document.activeElement)) setMenu(cart, false);
      });
    }

    let generation = 0;
    let debounceTimer;
    let requestController;
    let renderedQuery = '';

    function invalidateSearch() {
      generation++;
      clearTimeout(debounceTimer);
      if (requestController) requestController.abort();
      requestController = null;
    }

    function positionSearch() {
      if (!form || !input || !panel) return;
      const formBounds = form.getBoundingClientRect();
      const inputBounds = input.getBoundingClientRect();
      panel.style.width = Math.min(Math.max(320, window.innerWidth * 0.6), window.innerWidth - 32) + 'px';
      panel.style.top = Math.max(inputBounds.bottom - formBounds.top + 6, 8) + 'px';
      panel.style.left = (window.innerWidth / 2 - formBounds.left) + 'px';
      panel.style.transform = 'translateX(-50%)';
    }

    function openSearch() {
      if (!panel || !input || !form) return;
      positionSearch();
      panel.classList.add('is-open');
      panel.setAttribute('aria-hidden', 'false');
      input.setAttribute('aria-expanded', 'true');
      form.classList.add('search-open');
      if (backdrop) {
        backdrop.classList.add('is-open');
        backdrop.setAttribute('aria-hidden', 'false');
      }
      if (root.dataset.scrollBehavior === 'hide-on-down') root.style.transform = 'translateY(0)';
    }

    function closeSearch() {
      invalidateSearch();
      if (!panel || !input || !form) return;
      panel.classList.remove('is-open');
      panel.setAttribute('aria-hidden', 'true');
      input.setAttribute('aria-expanded', 'false');
      form.classList.remove('search-open');
      if (backdrop) {
        backdrop.classList.remove('is-open');
        backdrop.setAttribute('aria-hidden', 'true');
      }
    }

    if (form && input && panel && results) {
      const minimum = Number(form.dataset.minChars) || 3;
      const delay = Number(form.dataset.debounceMs);
      function message(text) {
        results.textContent = text;
      }
      function sanitizeElement(element) {
        if (!element) return;
        element.querySelectorAll('script, style, iframe, object, embed').forEach(el => el.remove());
        element.querySelectorAll('*').forEach(el => {
          Array.from(el.attributes).forEach(attr => {
            if (attr.name.startsWith('on')) {
              el.removeAttribute(attr.name);
            }
            if (attr.name === 'href' && attr.value.trim().toLowerCase().startsWith('javascript:')) {
              el.removeAttribute('href');
            }
          });
        });
      }
      function renderHighlights(element) {
        Array.from(element.childNodes).forEach(child => {
          if (child.nodeType === Node.TEXT_NODE) {
            const text = child.nodeValue || '';
            const markerPattern = /\$\{b\}([\s\S]*?)\$\{\/b\}/g;
            const fragment = document.createDocumentFragment();
            let lastIndex = 0;
            let match;
            while ((match = markerPattern.exec(text)) !== null) {
              fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
              const strong = document.createElement('strong');
              strong.textContent = match[1];
              fragment.appendChild(strong);
              lastIndex = markerPattern.lastIndex;
            }
            if (lastIndex > 0) {
              fragment.appendChild(document.createTextNode(text.slice(lastIndex)));
              child.replaceWith(fragment);
            }
          } else {
            renderHighlights(child);
          }
        });
      }
      async function search(query, requestGeneration) {
        const controller = typeof AbortController === 'function' ? new AbortController() : null;
        requestController = controller;
        try {
          const url = new URL('/json/search', window.location.href);
          url.searchParams.set('query', query);
          const response = await fetch(url.toString(), { credentials: 'same-origin', signal: controller ? controller.signal : undefined });
          if (!response.ok) throw new Error('Search request failed');
          const json = await response.text();
          if (requestGeneration !== generation || !root.isConnected || !panel.isConnected || !panel.classList.contains('is-open') || input.value.trim() !== query) return;

          let parsed = null;
          let isJson = false;
          try {
            parsed = JSON.parse(json);
            isJson = typeof parsed === 'object' && parsed !== null;
          } catch (e) {
            isJson = false;
          }

          const fragment = document.createDocumentFragment();

          if (isJson) {
            if (parsed.status === 'error') {
              message(parsed.error && parsed.error.title ? parsed.error.title : 'Error loading results');
              renderedQuery = '';
              return;
            }
            const items = Array.isArray(parsed.data) ? parsed.data : (Array.isArray(parsed) ? parsed : null);
            if (!items || items.length === 0) {
              message('No results found');
              renderedQuery = query;
              return;
            }

            items.forEach(page => {
              if (!page) return;
              const item = document.createElement('div');
              item.className = 'platform-content-search-result';

              const heading = document.createElement('h5');
              const link = document.createElement('a');
              const href = (page.link && !page.link.trim().toLowerCase().startsWith('javascript:')) ? page.link : '#';
              link.setAttribute('href', href);
              link.textContent = page.title || page.link || 'Untitled';
              heading.appendChild(link);

              if (page.type && page.type.trim().toLowerCase() !== 'webpage') {
                const typeTag = document.createElement('span');
                typeTag.className = 'badge secondary search-result-tag';
                typeTag.textContent = page.type;
                heading.appendChild(document.createTextNode(' '));
                heading.appendChild(typeTag);
              }
              item.appendChild(heading);

              if (page.actionLink && page.actionLabel) {
                const actionP = document.createElement('p');
                actionP.className = 'margin-bottom-5';
                const actionA = document.createElement('a');
                actionA.setAttribute('href', page.actionLink);
                actionA.className = 'button tiny no-gap';
                actionA.textContent = page.actionLabel;
                actionP.appendChild(actionA);
                item.appendChild(actionP);
              }

              if (page.description && page.description !== page.content) {
                const descP = document.createElement('p');
                descP.textContent = page.description;
                item.appendChild(descP);
              }

              if (page.content) {
                const contentP = document.createElement('p');
                if (/<[a-z][\s\S]*>/i.test(page.content)) {
                  const doc = new DOMParser().parseFromString(page.content, 'text/html');
                  sanitizeElement(doc.body);
                  contentP.innerHTML = doc.body.innerHTML;
                } else {
                  contentP.textContent = page.content;
                }
                renderHighlights(contentP);
                item.appendChild(contentP);
              }

              const metaParts = [];
              if (page.modifiedBy) {
                metaParts.push('Modified by: ' + page.modifiedBy);
              }
              if (page.lastModified) {
                metaParts.push('Last modified: ' + page.lastModified);
              }
              if (metaParts.length > 0) {
                const meta = document.createElement('div');
                meta.className = 'search-result-meta';
                meta.textContent = metaParts.join(' | ');
                item.appendChild(meta);
              }

              if (Array.isArray(page.tags) && page.tags.length > 0) {
                const tagsContainer = document.createElement('div');
                tagsContainer.className = 'search-result-tags';
                page.tags.forEach(tag => {
                  if (!tag) return;
                  const tagBadge = document.createElement('span');
                  tagBadge.className = 'badge';
                  const tagA = document.createElement('a');
                  tagA.href = '?label=' + encodeURIComponent(tag);
                  tagA.textContent = tag;
                  tagBadge.appendChild(tagA);
                  tagsContainer.appendChild(tagBadge);
                });
                item.appendChild(tagsContainer);
              }

              fragment.appendChild(item);
            });
          } else {
            const doc = new DOMParser().parseFromString(json, 'text/html');
            const main = doc.querySelector('.search-main-content') || doc.body;
            sanitizeElement(main);
            const trimmed = main.textContent.trim();
            if (!trimmed || trimmed.toLowerCase() === 'empty' || trimmed.toLowerCase() === 'no results found') {
              message('No results found');
              renderedQuery = query;
              return;
            }
            while (main.firstChild) {
              fragment.appendChild(main.firstChild);
            }
          }

          results.replaceChildren(fragment);
          if (results.childNodes.length === 0) {
            message('No results found');
          }
          renderedQuery = query;
        } catch (error) {
          if (requestGeneration === generation && root.isConnected && panel.isConnected && error.name !== 'AbortError' && panel.classList.contains('is-open')) {
            message('Error loading results');
            renderedQuery = '';
          }
        } finally {
          if (requestGeneration === generation) requestController = null;
        }
      }

      function handleInput() {
        invalidateSearch();
        const query = input.value.trim();
        if (query.length < minimum) {
          renderedQuery = '';
          closeSearch();
          return;
        }
        message('Loading...');
        renderedQuery = '';
        openSearch();
        const requestGeneration = generation;
        debounceTimer = setTimeout(() => search(query, requestGeneration), Number.isFinite(delay) ? delay : 300);
      }
      input.addEventListener('input', handleInput);
      input.addEventListener('focus', () => {
        if (input.value.trim().length < minimum) return;
        if (renderedQuery === input.value.trim()) openSearch(); else handleInput();
      });
      form.addEventListener('submit', closeSearch);
      results.addEventListener('click', event => { if (event.target.closest('a')) closeSearch(); });
      window.addEventListener('resize', () => { if (panel.classList.contains('is-open')) positionSearch(); });
    }

    function regionOpen() {
      return regionModal && getComputedStyle(regionModal).display !== 'none';
    }

    function closeRegion() {
      if (typeof window.closeRegionModal === 'function') window.closeRegionModal();
      if (regionTrigger) regionTrigger.focus();
    }

    root.addEventListener('click', event => {
      const control = event.target.closest('[data-header-control]');
      if (control && root.contains(control)) {
        if (control.dataset.headerControl === 'appearance') {
          toggleTheme();
          setMenu(account, false);
        } else if (control.dataset.headerControl === 'mobileMenu') {
          setMobile(nav && !nav.classList.contains('show'));
        } else if (control.dataset.headerControl === 'region' && typeof window.openRegionModal === 'function') {
          window.openRegionModal();
          if (regionModal) regionModal.focus();
        } else if (control.dataset.headerControl === 'askAI' && askAIDrawer) {
          setAskAIDrawer(!askAIDrawer.classList.contains('open'), true);
        } else if (control.dataset.headerControl === 'workspaces' && workspaces) {
          setMenu(workspaces, !workspaces.classList.contains('open'));
        }
      }
      const accountTrigger = event.target.closest('[data-header-account-trigger]');
      const cartTrigger = event.target.closest('[data-header-cart-trigger]');
      if (accountTrigger && account) setMenu(account, !account.classList.contains('open'));
      if (cartTrigger && cart) setMenu(cart, !cart.classList.contains('open'));
      const dropdownToggle = event.target.closest('[data-header-dropdown-toggle]');
      if (dropdownToggle) {
        const item = dropdownToggle.closest('.nav-item');
        setDropdown(item, !item.classList.contains('open'));
      }
      if (event.target.closest('a')) {
        setMenu(account, false);
        setMenu(cart, false);
        setMenu(workspaces, false);
        setMobile(false);
      }
    });

    if (regionModal) {
      regionModal.addEventListener('click', event => {
        if (event.target === regionModal || event.target.closest('[data-header-control="closeRegion"]')) closeRegion();
      });
    }
    document.addEventListener('click', event => {
      if (form && !form.contains(event.target)) closeSearch();
      if (account && !account.contains(event.target)) setMenu(account, false);
      if (cart && !cart.contains(event.target)) setMenu(cart, false);
      if (workspaces && !workspaces.contains(event.target)) setMenu(workspaces, false);
      if (nav && mobileToggle && !nav.contains(event.target) && !mobileToggle.contains(event.target)) setMobile(false);
      root.querySelectorAll('.nav-item.open').forEach(item => {
        if (!item.contains(event.target)) {
          item.dataset.headerHoverDismissed = 'true';
          setDropdown(item, false);
        }
      });
    });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        closeSearch();
        if (askAIDrawer && askAIDrawer.classList.contains('open')) setAskAIDrawer(false, true);
        if (regionOpen()) closeRegion();
        [account, cart, workspaces].forEach(menu => {
          if (menu && menu.classList.contains('open')) {
            menu.dataset.headerHoverDismissed = 'true';
            setMenu(menu, false);
            menu.querySelector('button').focus();
          }
        });
        if (nav && nav.classList.contains('show')) {
          setMobile(false);
          if (mobileToggle) mobileToggle.focus();
        }
        root.querySelectorAll('.nav-item.open').forEach(item => {
          item.dataset.headerHoverDismissed = 'true';
          setDropdown(item, false);
          const toggle = item.querySelector('[data-header-dropdown-toggle]');
          if (toggle) toggle.focus();
        });
      }
      if (event.key === 'Tab' && askAIDrawer && askAIDrawer.classList.contains('open')) {
        const controls = Array.from(askAIDrawer.querySelectorAll('button, input, a[href], select, textarea, [tabindex="0"]')).filter(element => !element.disabled && getComputedStyle(element).display !== 'none');
        const first = controls[0];
        const last = controls[controls.length - 1];
        if (!first) { event.preventDefault(); askAIDrawer.focus(); }
        else if (event.shiftKey && (document.activeElement === first || document.activeElement === askAIDrawer)) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && (document.activeElement === last || document.activeElement === askAIDrawer)) { event.preventDefault(); first.focus(); }
      }
      if (event.key === 'Tab' && regionOpen()) {
        const controls = Array.from(regionModal.querySelectorAll('button, input, a[href], select, textarea, [tabindex="0"]')).filter(element => !element.disabled && getComputedStyle(element).display !== 'none');
        const first = controls[0];
        const last = controls[controls.length - 1];
        if (!first) { event.preventDefault(); regionModal.focus(); }
        else if (event.shiftKey && (document.activeElement === first || document.activeElement === regionModal)) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && (document.activeElement === last || document.activeElement === regionModal)) { event.preventDefault(); first.focus(); }
      }
    });

    let lastScroll = window.scrollY;
    window.addEventListener('scroll', () => {
      const current = window.scrollY;
      if (root.dataset.scrollBehavior !== 'hide-on-down') {
        lastScroll = current;
        return;
      }
      const threshold = Number(root.dataset.scrollThreshold) || 0;
      const interacting = root.contains(document.activeElement) || regionOpen() || (askAIDrawer && askAIDrawer.classList.contains('open')) || (nav && nav.classList.contains('show')) || root.querySelector('.nav-item.open') || (panel && panel.classList.contains('is-open')) || (account && account.classList.contains('open')) || (cart && cart.classList.contains('open'));
      root.style.transform = root.dataset.scrollBehavior === 'hide-on-down' && !interacting && current > lastScroll && current > threshold ? 'translateY(-100%)' : 'translateY(0)';
      lastScroll = current;
    });

    window.toggleDarkMode = toggleTheme;
    window.applyTheme = applyTheme;
    window.toggleMobileMenu = () => setMobile(nav && !nav.classList.contains('show'));
    window.closeSearchModal = closeSearch;
  }

  function initialize() {
    document.querySelectorAll('[data-header-modern]').forEach(initializeHeader);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', initialize, { once: true });
  else initialize();
}());