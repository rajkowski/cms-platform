// Permission policy for the Visual Content Editor feature group.
// Covers content blocks, page editor, sitemap, blogs, and calendars.
// Grants the "access" action to any principal whose roles contain "admin"
// or "content-manager" for any component that is a member of this group.
// The PageContentBlocksJsonService also extends access to "content-editor".

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-content-editor"
)
when {
  "admin" in principal.roles ||
  "content-manager" in principal.roles ||
  "content-editor" in principal.roles
};
