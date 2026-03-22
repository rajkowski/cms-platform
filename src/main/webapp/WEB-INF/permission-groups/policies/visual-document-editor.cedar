// Permission policy for the Visual Document Editor feature group.
// Covers document library, folder management, file operations, and analytics.
// Grants the "access" action to any principal whose roles contain "admin"
// or "content-manager" for any component that is a member of this group.

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-document-editor"
)
when {
  "admin" in principal.roles ||
  "content-manager" in principal.roles
};
