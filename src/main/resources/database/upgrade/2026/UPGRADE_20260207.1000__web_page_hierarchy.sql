-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0
-- Migrate all existing images to image_versions table

CREATE TABLE web_page_hierarchy (
  page_hierarchy_id BIGSERIAL PRIMARY KEY,
  web_page_id BIGINT UNIQUE NOT NULL REFERENCES web_pages(web_page_id) ON DELETE CASCADE,
  parent_page_id BIGINT REFERENCES web_pages(web_page_id) ON DELETE SET NULL,
  sort_order INTEGER DEFAULT 100 NOT NULL,
  depth INTEGER DEFAULT 0 NOT NULL,
  path TEXT DEFAULT '/' NOT NULL,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX web_page_hierarchy_parent_idx ON web_page_hierarchy(parent_page_id, sort_order);
CREATE INDEX web_page_hierarchy_path_idx ON web_page_hierarchy(path);
CREATE INDEX web_page_hierarchy_depth_idx ON web_page_hierarchy(depth);
