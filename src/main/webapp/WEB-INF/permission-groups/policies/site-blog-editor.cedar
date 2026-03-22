// Permission policy for the Site Blog Editor feature group.
// Covers the blog post widget (viewing drafts, deleting posts).
// Grants the "access" action to any principal whose roles contain "admin"
// or "content-manager".

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"site-blog-editor"
)
when {
  "admin" in principal.roles ||
  "content-manager" in principal.roles
};
