/**
 * Apps Menu Controller
 * Manages the click-to-toggle behavior for the apps menu in visual editors
 * Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
 * Licensed under the Apache License, Version 2.0
 */

function setupAppsMenu() {
  const appsMenu = document.querySelector('#editor-toolbar .apps-menu');
  if (!appsMenu) {
    return;
  }

  const appsBtn = appsMenu.querySelector('#apps-btn');
  const dropdown = appsMenu.querySelector('.apps-dropdown');
  if (!appsBtn || !dropdown) {
    return;
  }

  let isOpen = false;

  function closeMenu() {
    if (!isOpen) {
      return;
    }
    isOpen = false;
    appsMenu.classList.remove('open');
    document.removeEventListener('click', handleOutsideClick);
  }

  function handleOutsideClick(event) {
    if (!appsMenu.contains(event.target)) {
      closeMenu();
    }
  }

  appsBtn.addEventListener('click', function(event) {
    event.preventDefault();
    event.stopPropagation();
    isOpen = !isOpen;
    appsMenu.classList.toggle('open', isOpen);
    if (isOpen) {
      document.addEventListener('click', handleOutsideClick);
    } else {
      document.removeEventListener('click', handleOutsideClick);
    }
  });

  dropdown.addEventListener('click', function(event) {
    if (event.target.closest('a')) {
      closeMenu();
    }
  });
}
