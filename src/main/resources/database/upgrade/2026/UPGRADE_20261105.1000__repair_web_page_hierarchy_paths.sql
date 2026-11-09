WITH RECURSIVE hierarchy_paths AS (
  SELECT hierarchy.web_page_id, ('/' || hierarchy.web_page_id || '/')::text AS path, 0 AS depth
  FROM web_page_hierarchy hierarchy
  WHERE hierarchy.parent_page_id IS NULL
     OR NOT EXISTS (SELECT 1 FROM web_page_hierarchy parent WHERE parent.web_page_id = hierarchy.parent_page_id)
  UNION ALL
  SELECT child.web_page_id, (parent.path || child.web_page_id || '/')::text, parent.depth + 1
  FROM web_page_hierarchy child
  JOIN hierarchy_paths parent ON child.parent_page_id = parent.web_page_id
)
UPDATE web_page_hierarchy hierarchy
SET path = hierarchy_paths.path, depth = hierarchy_paths.depth
FROM hierarchy_paths
WHERE hierarchy.web_page_id = hierarchy_paths.web_page_id
  AND (hierarchy.path IS DISTINCT FROM hierarchy_paths.path OR hierarchy.depth IS DISTINCT FROM hierarchy_paths.depth);