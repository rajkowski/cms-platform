// Permission policy for the Site Content Editor feature group.
// Covers the content widget (inline HTML/content-block editing),
// the menu widget (TOC editor access), and the sub-folder form widget.
// Grants the "access" action to any principal whose roles contain "admin"
// or "content-manager".

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"site-content-editor"
)
when {
  "admin" in principal.roles ||
  "content-manager" in principal.roles
};
