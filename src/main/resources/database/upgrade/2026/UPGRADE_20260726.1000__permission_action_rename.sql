-- Permission Engine: switch component identification from class names to Actions.
-- The PermissionEngine now keys permission checks by a stable action string
-- (e.g. "cms.content.save-draft") instead of a Java class name, so the
-- component-groups.xml and permission_group_members rows now store action
-- identifiers instead of fully-qualified class names.

ALTER TABLE permission_group_members RENAME COLUMN class_name TO action_name;
