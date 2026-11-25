(function () {
  const form = document.getElementById("workspace-editor-form");
  if (!form) {
    return;
  }

  const controls = Array.from(form.querySelectorAll("[data-property-input]"));
  const savedValues = new Map(controls.map((control) => [control.name, control.dataset.savedValue ?? control.value]));
  const saveButton = document.getElementById("workspace-editor-save");
  const discardButton = document.getElementById("workspace-editor-discard");
  const status = document.getElementById("workspace-editor-dirty-status");

  function isDirty() {
    return controls.some((control) => control.value !== savedValues.get(control.name));
  }

  function notifyChange(control) {
    control.dispatchEvent(new CustomEvent("workspace-editor:value-change", {
      bubbles: true,
      detail: { name: control.name, value: control.value }
    }));
  }

  function updateDirtyState() {
    const dirty = isDirty();
    saveButton.disabled = !dirty;
    discardButton.disabled = !dirty;
    status.textContent = dirty ? "Unsaved changes" : "All changes saved";
    status.classList.toggle("is-unsaved", dirty);
  }

  controls.forEach((control) => {
    control.addEventListener("input", () => {
      notifyChange(control);
      updateDirtyState();
    });
    control.addEventListener("change", () => {
      notifyChange(control);
      updateDirtyState();
    });
  });

  function discard() {
    controls.forEach((control) => {
      control.value = savedValues.get(control.name);
      notifyChange(control);
    });
    updateDirtyState();
  }

  form.addEventListener("submit", (event) => {
    if (!isDirty()) {
      event.preventDefault();
      return;
    }
    const themeNameInput = document.getElementById("workspace-editor-theme-snapshot-name");
    if (themeNameInput) {
      const themeName = window.prompt("Name this theme:", themeNameInput.value);
      if (themeName === null) {
        event.preventDefault();
        updateDirtyState();
        return;
      }
      if (!themeName.trim()) {
        window.alert("Enter a name for the theme.");
        event.preventDefault();
        updateDirtyState();
        return;
      }
      themeNameInput.value = themeName.trim();
    }
    saveButton.disabled = true;
    status.textContent = "Saving changes";
  });

  updateDirtyState();

  window.VisualWorkspaceEditorState = {
    discard,
    isDirty,
    updateDirtyState
  };
})();
