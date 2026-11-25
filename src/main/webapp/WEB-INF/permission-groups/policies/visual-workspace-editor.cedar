// Permission policy for the Visual Workspace Editor.
// Only administrators and content managers may edit workspace site settings.

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-workspace-editor"
)
when {
  "admin" in principal.roles ||
  "content-manager" in principal.roles
};
