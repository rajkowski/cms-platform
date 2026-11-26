(function () {
  const preview = document.getElementById("workspace-preview-page");
  if (!preview) {
    return;
  }

  const selectionButtons = Array.from(document.querySelectorAll("[data-preview-select]"));
  const modeButtons = Array.from(document.querySelectorAll("[data-preview-mode-select]"));
  const themeControls = Array.from(document.querySelectorAll("[data-theme-color-item]"));
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

  function isControlVisibleInMode(control, mode) {
    const colorMode = control.dataset.previewColorMode;
    if (mode === "light") {
      return colorMode !== "dark";
    }
    if (colorMode === "dark") {
      return true;
    }
    const propertyInput = control.querySelector("[data-theme-property]");
    const darkName = propertyInput?.name.replace(
      /^(theme\.(?:body|utilitybar|topbar|callout|pagination|badge|progress|footer))\./,
      "$1.dark."
    );
    return !darkName || !getColorControl(darkName);
  }

  function setColors(root, selector, properties, value) {
    root.querySelectorAll(selector).forEach((element) => {
      properties.forEach((property) => element.style.setProperty(property, value));
    });
  }

  function setButtonTextColors(root, selector, value) {
    root.querySelectorAll(selector).forEach((element) => {
      element.style.setProperty("--workspace-preview-button-text-color", value);
      element.style.setProperty("color", value, "important");
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
      "--workspace-preview-toc-text-color": colorFor("menu.text.color"),
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
    const colorMode = control?.closest("[data-theme-color-item]")?.dataset.previewColorMode;
    if (!target) {
      return;
    }
    if (colorMode === "dark" && preview.dataset.previewMode !== "dark") {
      return;
    }
    const root = targetRoot(target);
    if (!root) {
      return;
    }

    const lowerName = name.toLowerCase().replace(
      /^(theme\.(?:body|utilitybar|callout|pagination|badge|progress|footer))\.dark\./,
      "$1."
    );
    if (target === "button") {
      if (lowerName === "theme.button.text.color") {
        root.style.setProperty("--workspace-preview-button-text-color", value);
        preview.style.setProperty("--workspace-preview-button-text-color", value);
        root.querySelectorAll(".workspace-preview-button").forEach((element) => {
          const style = element.dataset.buttonStyle;
          const variantControl = style ? getColorControl(`theme.button.${style}.text.color`) : null;
          const variantValue = variantControl?.value;
          const buttonTextColor = isColor(variantValue) ? variantValue : value;
          element.style.setProperty("color", buttonTextColor, "important");
        });
        return;
      }
      const textColorVariant = name.match(/^theme\.button\.(default|primary|secondary|success|warning|alert)\.text\.color$/i);
      if (textColorVariant) {
        setButtonTextColors(root, `[data-button-style="${textColorVariant[1].toLowerCase()}"]`, value);
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
      const variant = lowerName.match(/^theme\.callout\.(primary|secondary|success|warning|alert|box)\./i);
      const style = variant ? variant[1].toLowerCase() : "base";
      setColors(root, `[data-callout-style="${style}"]`, [property], value);
      if (lowerName === "theme.callout.box.backgroundcolor") {
        preview.style.setProperty("--workspace-preview-toc-background-color", value);
        preview.querySelectorAll(".workspace-preview-toc, .workspace-preview-tabs").forEach((component) => {
          component.style.setProperty("--workspace-preview-toc-background-color", value);
        });
      } else if (lowerName === "theme.callout.box.text.color") {
        preview.style.setProperty("--workspace-preview-box-text-color", value);
        preview.querySelectorAll(".workspace-preview-toc, .workspace-preview-tabs").forEach((component) => {
          component.style.setProperty("--workspace-preview-box-text-color", value);
        });
      }
      return;
    }

    if (target === "pagination") {
      if (lowerName === "theme.pagination.border.color") {
        preview.style.setProperty("--workspace-preview-pagination-border-color", value);
      } else if (lowerName === "theme.pagination.link.color") {
        preview.style.setProperty("--workspace-preview-pagination-link-color", value);
      } else if (lowerName === "theme.pagination.current.backgroundcolor") {
        preview.style.setProperty("--workspace-preview-pagination-current-background-color", value);
      } else if (lowerName === "theme.pagination.current.text.color") {
        preview.style.setProperty("--workspace-preview-pagination-current-text-color", value);
      }
      return;
    }

    if (target === "label" || target === "badge") {
      const variant = name.match(new RegExp(`^theme\\.${target}\\.(primary|secondary|success|warning|alert)\\.`, "i"));
      const style = variant ? variant[1].toLowerCase() : "base";
      const property = lowerName.endsWith(".text.color")
        ? `--workspace-preview-${target}-text-color`
        : `--workspace-preview-${target}-background-color`;
      setColors(root, `[data-${target}-style="${style}"]`, [property], value);
      return;
    }

    if (target === "progress") {
      if (lowerName === "theme.progress.backgroundcolor") {
        setColors(root, ".workspace-preview-progress", ["--workspace-preview-progress-background-color"], value);
      } else if (lowerName === "theme.progress.text.color") {
        setColors(root, ".workspace-preview-progress", ["--workspace-preview-progress-text-color"], value);
      } else {
        const variant = name.match(/^theme\.progress\.meter\.(primary|secondary|success|warning|alert)\.backgroundcolor$/i);
        const style = variant ? variant[1].toLowerCase() : "base";
        setColors(root, `[data-progress-style="${style}"]`, ["--workspace-preview-progress-meter-background-color"], value);
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
      const mode = preview.dataset.previewMode || "light";
      const modeMatches = isControlVisibleInMode(control, mode);
      control.hidden = control.dataset.previewTarget !== target || !modeMatches;
    });
    if (emptyMessage) {
      emptyMessage.hidden = themeControls.some((control) =>
        control.dataset.previewTarget === target && !control.hidden);
    }
  }

  function selectMode(mode) {
    preview.dataset.previewMode = mode;
    modeButtons.forEach((button) => {
      const selected = button.dataset.previewModeSelect === mode;
      button.classList.toggle("active", selected);
      button.setAttribute("aria-pressed", selected ? "true" : "false");
    });
    document.querySelectorAll("[data-theme-property]").forEach((control) => {
      const controlMode = control.closest("[data-theme-color-item]")?.dataset.previewColorMode;
      if (controlMode === "light" || controlMode === mode) {
        applyColor(control.name, control.value);
      }
    });
    applyTopbarColors();
    const activeTarget = selectionButtons.find((button) => button.classList.contains("active"))?.dataset.previewSelect
      || "utilitybar";
    selectTarget(activeTarget);
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

  selectMode(preview.dataset.previewMode || "light");
})();
