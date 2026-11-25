(function () {
  const categoryButtons = Array.from(document.querySelectorAll("[data-property-category-select]"));
  const categoryPanels = Array.from(document.querySelectorAll("[data-property-category]"));

  function selectCategory(category) {
    categoryButtons.forEach((button) => {
      const selected = button.dataset.propertyCategorySelect === category;
      button.classList.toggle("active", selected);
      button.setAttribute("aria-pressed", selected ? "true" : "false");
    });
    categoryPanels.forEach((panel) => {
      panel.hidden = panel.dataset.propertyCategory !== category;
    });
  }

  categoryButtons.forEach((button) => {
    button.addEventListener("click", () => selectCategory(button.dataset.propertyCategorySelect));
  });
  if (categoryButtons.length > 0) {
    selectCategory(categoryButtons[0].dataset.propertyCategorySelect);
  }

  document.querySelectorAll("[data-spectrum-color-picker]").forEach((input) => {
    let colorWhenOpened = input.value;
    let colorCommitted = false;

    function normalizeColor(value) {
      if (/^#[0-9a-f]{6}$/i.test(value)) {
        return value;
      }
      if (/^#[0-9a-f]{3}$/i.test(value)) {
        return `#${value[1]}${value[1]}${value[2]}${value[2]}${value[3]}${value[3]}`;
      }
      return null;
    }

    function updateSpectrum() {
      const color = normalizeColor(input.value);
      if (color) {
        $(input).spectrum("set", color);
      }
    }

    function updateInput(color) {
      if (color) {
        input.value = color.toHexString();
        input.dispatchEvent(new Event("input", { bubbles: true }));
      }
    }

    $(input).spectrum({
      color: normalizeColor(input.value),
      flat: false,
      preferredFormat: "hex",
      chooseText: "Choose",
      cancelText: "Cancel",
      showPalette: true,
      palette: [
        ["#000", "#444", "#666", "#999", "#ccc", "#eee", "#f3f3f3", "#fff"],
        ["#f00", "#f90", "#ff0", "#0f0", "#0ff", "#00f", "#90f", "#f0f"],
        ["#f4cccc", "#fce5cd", "#fff2cc", "#d9ead3", "#d0e0e3", "#cfe2f3", "#d9d2e9", "#ead1dc"],
        ["#ea9999", "#f9cb9c", "#ffe599", "#b6d7a8", "#a2c4c9", "#9fc5e8", "#b4a7d6", "#d5a6bd"],
        ["#e06666", "#f6b26b", "#ffd966", "#93c47d", "#76a5af", "#6fa8dc", "#8e7cc3", "#c27ba0"],
        ["#c00", "#e69138", "#f1c232", "#6aa84f", "#45818e", "#3d85c6", "#674ea7", "#a64d79"],
        ["#900", "#b45f06", "#bf9000", "#38761d", "#134f5c", "#0b5394", "#351c75", "#741b47"],
        ["#600", "#783f04", "#7f6000", "#274e13", "#0c343d", "#073763", "#20124d", "#4c1130"]
      ],
      showSelectionPalette: true,
      localStorageKey: "visual.workspace.editor",
      showInput: true,
      showInitial: true,
      showAlpha: false,
      allowEmpty: false,
      show: () => {
        colorWhenOpened = input.value;
        colorCommitted = false;
      },
      move: updateInput,
      change: (color) => {
        colorCommitted = true;
        updateInput(color);
      },
      hide: () => {
        if (!colorCommitted) {
          input.value = colorWhenOpened;
          input.dispatchEvent(new Event("input", { bubbles: true }));
        }
      }
    });

    input.addEventListener("input", updateSpectrum);
    input.addEventListener("workspace-editor:value-change", updateSpectrum);
  });

  document.querySelectorAll("[data-color-picker-for]").forEach((picker) => {
    const input = document.getElementById(picker.dataset.colorPickerFor);
    if (!input) {
      return;
    }
    picker.addEventListener("input", () => {
      input.value = picker.value;
      input.dispatchEvent(new Event("input", { bubbles: true }));
    });
    input.addEventListener("input", () => {
      if (/^#[0-9a-f]{6}$/i.test(input.value)) {
        picker.value = input.value;
      }
    });
  });

  document.querySelectorAll("[data-image-preview-for]").forEach((image) => {
    const input = document.getElementById(image.dataset.imagePreviewFor);
    if (input) {
      input.addEventListener("input", () => {
        image.src = input.value;
      });
    }
  });
})();
