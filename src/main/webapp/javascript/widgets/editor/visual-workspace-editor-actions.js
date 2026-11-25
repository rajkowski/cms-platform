(function () {
  const form = document.getElementById("workspace-editor-form");
  const state = window.VisualWorkspaceEditorState;
  if (!form || !state) {
    return;
  }

  document.getElementById("workspace-editor-discard").addEventListener("click", () => {
    state.discard();
  });

  const includedThemeSelect = document.getElementById("workspace-editor-included-theme");
  const historySelect = document.getElementById("workspace-editor-theme-history");
  const snapshotName = document.getElementById("workspace-editor-theme-snapshot-name");

  function applyTheme(select, otherSelect) {
    const value = select.value;
    if (!value) {
      return;
    }
    const template = document.getElementById(`workspace-theme-${value}`);
    if (!(template instanceof HTMLTemplateElement)) {
      return;
    }
    template.content.querySelectorAll("[data-theme-property-name]").forEach((property) => {
      const input = form.elements.namedItem(property.dataset.themePropertyName);
      if (input) {
        input.value = property.dataset.themePropertyValue;
        input.dispatchEvent(new Event("input", { bubbles: true }));
      }
    });
    if (otherSelect) {
      otherSelect.value = "";
    }
    if (snapshotName) {
      snapshotName.value = select.options[select.selectedIndex].text;
    }
    state.updateDirtyState();
  }

  includedThemeSelect?.addEventListener("change", () => applyTheme(includedThemeSelect, historySelect));
  historySelect?.addEventListener("change", () => applyTheme(historySelect, includedThemeSelect));
})();
