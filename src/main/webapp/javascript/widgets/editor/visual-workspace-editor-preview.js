(function () {
  const preview = document.getElementById("workspace-preview-page");
  if (!preview) {
    return;
  }

  const selectionButtons = Array.from(document.querySelectorAll("[data-preview-select]"));
  const modeButtons = Array.from(document.querySelectorAll("[data-preview-mode-select]"));
  const themeControls = Array.from(document.querySelectorAll("[data-theme-color-item]"));
  const selectedLabel = document.getElementById("workspace-editor-selected-component");
  const emptyMessage = document.getElementById("workspace-theme-empty");
  const topbarColors = { light: {}, dark: {} };
  const topbarMenuProperties = {
    "menu.text.color": "--workspace-preview-menu-text-color",
    "menu.arrow.color": "--workspace-preview-menu-arrow-color",
    "menu.text.hoverbackgroundcolor": "--workspace-preview-menu-hover-background-color",
    "menu.hovertextcolor": "--workspace-preview-menu-hover-text-color",
    "menu.dropdown.backgroundcolor": "--workspace-preview-menu-dropdown-background-color",
    "menu.dropdown.text.color": "--workspace-preview-menu-dropdown-text-color",
    "menu.activebackgroundcolor": "--workspace-preview-menu-active-background-color",
    "menu.activetextcolor": "--workspace-preview-menu-active-text-color"
  };
  const componentLabels = {
    utilitybar: "Info Bar",
    body: "Body",
    topbar: "Navigation",
    button: "Buttons",
    callout: "Callouts",
    footer: "Footer"
  };

  function isColor(value) {
    return /^#[0-9a-f]{3}(?:[0-9a-f]{3})?$/i.test(value);
  }

  function targetRoot(target) {
    if (preview.dataset.previewArea === target) {
      return preview;
    }
    return preview.querySelector(`[data-preview-area="${target}"]`);
  }

  function getColorControl(name) {
    return Array.from(document.querySelectorAll("[data-theme-property]"))
      .find((control) => control.name === name);
  }

  function setColors(root, selector, properties, value) {
    root.querySelectorAll(selector).forEach((element) => {
      properties.forEach((property) => element.style.setProperty(property, value));
    });
  }

  function applyTopbarColors() {
    const root = targetRoot("topbar");
    const mode = preview.dataset.previewMode === "dark" ? "dark" : "light";
    const colors = topbarColors[mode];
    const fallbackColors = mode === "dark" ? topbarColors.light : {};
    const colorFor = (name) => colors[name] || fallbackColors[name] || "";
    const backgroundColor = colorFor("backgroundcolor");
    const textColor = colorFor("text.color");
    if (backgroundColor) {
      root.style.backgroundColor = backgroundColor;
    } else {
      root.style.removeProperty("background-color");
    }
    if (textColor) {
      root.style.color = textColor;
    } else {
      root.style.removeProperty("color");
    }
    Object.entries(topbarMenuProperties).forEach(([name, property]) => {
      const value = colorFor(name);
      if (value) {
        root.style.setProperty(property, value);
      } else {
        root.style.removeProperty(property);
      }
    });
    const tocMenuColors = {
      "--workspace-preview-toc-active-background-color": colorFor("menu.activebackgroundcolor"),
      "--workspace-preview-toc-active-text-color": colorFor("menu.activetextcolor"),
      "--workspace-preview-toc-hover-background-color": colorFor("menu.text.hoverbackgroundcolor"),
      "--workspace-preview-toc-hover-text-color": colorFor("menu.hovertextcolor")
    };
    Object.entries(tocMenuColors).forEach(([property, value]) => {
      if (value) {
        preview.style.setProperty(property, value);
        preview.querySelectorAll(".workspace-preview-toc, .workspace-preview-tabs").forEach((component) => {
          component.style.setProperty(property, value);
        });
      } else {
        preview.style.removeProperty(property);
        preview.querySelectorAll(".workspace-preview-toc, .workspace-preview-tabs").forEach((component) => {
          component.style.removeProperty(property);
        });
      }
    });
  }

  function applyColor(name, value) {
    if (!isColor(value)) {
      return;
    }
    const control = getColorControl(name);
    const target = control?.closest("[data-theme-color-item]")?.dataset.previewTarget;
    if (!target) {
      return;
    }
    const root = targetRoot(target);
    if (!root) {
      return;
    }

    const lowerName = name.toLowerCase();
    if (target === "button") {
      if (lowerName === "theme.button.text.color") {
        root.style.setProperty("--workspace-preview-button-text-color", value);
        preview.style.setProperty("--workspace-preview-button-text-color", value);
        return;
      }
      const textColorVariant = name.match(/^theme\.button\.(default|primary|secondary|success|warning|alert)\.text\.color$/i);
      if (textColorVariant) {
        setColors(root, `[data-button-style="${textColorVariant[1].toLowerCase()}"]`,
          ["--workspace-preview-button-text-color"], value);
        return;
      }
      const property = lowerName.endsWith(".hoverbackgroundcolor")
        ? "--workspace-preview-button-hover-background-color"
        : "--workspace-preview-button-background-color";
      const variant = name.match(/^theme\.button\.(default|primary|secondary|success|warning|alert)\./i);
      if (variant) {
        setColors(root, `[data-button-style="${variant[1].toLowerCase()}"]`, [property], value);
      }
      return;
    }

    if (target === "callout") {
      const property = lowerName.endsWith(".text.color")
        ? "--workspace-preview-callout-text-color"
        : "--workspace-preview-callout-background-color";
      const variant = name.match(/^theme\.callout\.(primary|secondary|success|warning|alert)\./i);
      const style = variant ? variant[1].toLowerCase() : "base";
      setColors(root, `[data-callout-style="${style}"]`, [property], value);
      if (lowerName === "theme.callout.secondary.backgroundcolor") {
        preview.style.setProperty("--workspace-preview-toc-background-color", value);
        preview.querySelectorAll(".workspace-preview-toc, .workspace-preview-tabs").forEach((component) => {
          component.style.setProperty("--workspace-preview-toc-background-color", value);
        });
      } else if (lowerName === "theme.callout.secondary.text.color") {
        preview.style.setProperty("--workspace-preview-toc-text-color", value);
        preview.querySelectorAll(".workspace-preview-toc, .workspace-preview-tabs").forEach((component) => {
          component.style.setProperty("--workspace-preview-toc-text-color", value);
        });
      }
      return;
    }

    if (target === "utilitybar") {
      if (lowerName === "theme.utilitybar.link.color") {
        setColors(root, "a", ["color"], value);
      } else if (lowerName.endsWith(".backgroundcolor")) {
        root.style.backgroundColor = value;
      } else {
        root.style.color = value;
      }
      return;
    }

    if (target === "topbar") {
      const darkModeColor = lowerName.startsWith("theme.topbar.dark.");
      const prefix = darkModeColor ? "theme.topbar.dark." : "theme.topbar.";
      topbarColors[darkModeColor ? "dark" : "light"][lowerName.substring(prefix.length)] = value;
      applyTopbarColors();
      return;
    }

    if (target === "footer" && lowerName === "theme.footer.links.color") {
      setColors(root, "a", ["color"], value);
    } else if (target === "body" && lowerName === "theme.body.link.color") {
      preview.style.setProperty("--workspace-preview-link-color", value);
    } else if (lowerName.endsWith(".backgroundcolor")) {
      root.style.backgroundColor = value;
    } else {
      root.style.color = value;
    }
  }

  function selectTarget(target) {
    selectionButtons.forEach((button) => {
      const selected = button.dataset.previewSelect === target;
      button.classList.toggle("active", selected);
      button.setAttribute("aria-pressed", selected ? "true" : "false");
    });
    themeControls.forEach((control) => {
      control.hidden = control.dataset.previewTarget !== target;
    });
    preview.classList.toggle("is-selected", preview.dataset.previewArea === target);
    preview.querySelectorAll("[data-preview-area]").forEach((area) => {
      area.classList.toggle("is-selected", area.dataset.previewArea === target);
    });
    selectedLabel.textContent = componentLabels[target] || "Component";
    if (emptyMessage) {
      emptyMessage.hidden = themeControls.some((control) => control.dataset.previewTarget === target);
    }
  }

  function selectMode(mode) {
    preview.dataset.previewMode = mode;
    applyTopbarColors();
    modeButtons.forEach((button) => {
      const selected = button.dataset.previewModeSelect === mode;
      button.classList.toggle("active", selected);
      button.setAttribute("aria-pressed", selected ? "true" : "false");
    });
  }

  selectionButtons.forEach((button) => {
    button.addEventListener("click", () => selectTarget(button.dataset.previewSelect));
  });

  modeButtons.forEach((button) => {
    button.addEventListener("click", () => selectMode(button.dataset.previewModeSelect));
  });

  document.addEventListener("workspace-editor:value-change", (event) => {
    if (event.detail.name.startsWith("theme.")) {
      applyColor(event.detail.name, event.detail.value);
    }
  });

  document.querySelectorAll("[data-theme-property]").forEach((control) => {
    applyColor(control.name, control.value);
  });

  selectTarget("utilitybar");
  selectMode(preview.dataset.previewMode || "light");
})();
