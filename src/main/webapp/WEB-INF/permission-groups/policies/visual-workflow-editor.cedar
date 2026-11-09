// Permission policy for the Visual Workflow Editor feature group.
// Covers event workflow management.
// Grants the "access" action only to principals whose roles contain "admin".

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-workflow-editor"
)
when {
  "admin" in principal.roles
};
