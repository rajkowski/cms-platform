// Permission policy for the Visual Data Editor feature group.
// Covers collections, datasets, and data management operations.
// Grants the "access" action to any principal whose roles contain "admin"
// or "data-manager" for any component that is a member of this group.

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-data-editor"
)
when {
  "admin" in principal.roles ||
  "data-manager" in principal.roles
};
