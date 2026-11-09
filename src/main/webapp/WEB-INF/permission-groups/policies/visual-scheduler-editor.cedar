// Permission policy for the Visual Scheduler Editor feature group.
// Grants access only to principals whose roles contain "admin".

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-scheduler-editor"
)
when {
  "admin" in principal.roles
};