// Permission policy for the Visual CRM Editor feature group.
// Covers forms, mailing lists, customers, orders, product catalog,
// pricing rules, sales tax, shipping rates, users, groups, and roles.
// Grants "access" to admins, content-managers, and ecommerce-managers.
// User/group/role management (CRMUsers, CRMUserGroups, CRMUserRoles)
// requires admin only — enforced within those service implementations.

permit (
  principal,
  action == CMS::Action::"access",
  resource in CMS::PermissionGroup::"visual-crm-editor"
)
when {
  "admin" in principal.roles ||
  "content-manager" in principal.roles ||
  "ecommerce-manager" in principal.roles
};
