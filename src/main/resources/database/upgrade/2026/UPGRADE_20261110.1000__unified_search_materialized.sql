-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski), Licensed under the Apache License, Version 2.0 (the "License").
-- One searchable row per page, blog post, calendar event, attachment and item, ranked together using each table's tsv
-- Materialized so the combined tsv is indexed; unified_search stays the read name and is refreshed by RefreshUnifiedSearchJob

DROP VIEW IF EXISTS unified_search;
DROP MATERIALIZED VIEW IF EXISTS unified_search_mv;

CREATE MATERIALIZED VIEW unified_search_mv AS
SELECT
  'pages'::text AS result_type,
  web_pages.web_page_id AS result_id,
  web_pages.page_title::text AS title,
  web_pages.page_description::text AS description,
  web_pages.link::text AS link,
  web_pages.tags AS tags,
  web_pages.created AS created,
  web_pages.modified AS modified,
  web_pages.modified_by AS modified_by,
  NULL::text AS file_name,
  NULL::text AS file_type,
  NULL::text AS mime_type,
  NULL::text AS extension,
  left(concat_ws(' ', COALESCE(NULLIF(web_pages.page_text, ''), web_pages.page_description), page_file_text.content), 20000) AS headline_source,
  web_pages.tsv || setweight(to_tsvector('web_page_stem', COALESCE(page_file_text.content, '')), 'D') AS tsv,
  (COALESCE(web_pages.enabled, false) AND NOT COALESCE(web_pages.has_redirect, false)) AS is_listed,
  (COALESCE(web_pages.searchable, false) AND NOT COALESCE(web_pages.draft, false)) AS is_public,
  NULL::bigint AS folder_id,
  NULL::bigint AS collection_id,
  NULL::timestamp(3) AS start_date,
  NULL::bigint AS related_category_id,
  NULL::text AS category,
  NULL::jsonb AS custom_field_values,
  concat_ws(' ', web_pages.page_text, page_file_text.content) AS text_content
FROM web_pages
LEFT JOIN LATERAL (
  SELECT string_agg(concat_ws(' ', files.filename, files.title, files.summary, files.document_text), ' ') AS content
  FROM web_page_files
  JOIN files ON web_page_files.file_id = files.file_id
  WHERE web_page_files.web_page_id = web_pages.web_page_id
) page_file_text ON true
UNION ALL
SELECT
  'posts'::text,
  blog_posts.post_id,
  blog_posts.title::text,
  blog_posts.summary::text,
  ('/' || blogs.blog_unique_id || '/' || blog_posts.post_unique_id)::text,
  NULL::jsonb,
  blog_posts.created,
  blog_posts.modified,
  blog_posts.modified_by,
  NULL::text,
  NULL::text,
  NULL::text,
  NULL::text,
  left(blog_posts.body_text, 20000),
  blog_posts.tsv,
  (blog_posts.published IS NOT NULL),
  COALESCE(blogs.enabled, true),
  NULL::bigint,
  NULL::bigint,
  NULL::timestamp(3),
  blogs.blog_id::bigint,
  blogs.name::text,
  NULL::jsonb,
  blog_posts.body_text
FROM blog_posts
JOIN blogs ON blog_posts.blog_id = blogs.blog_id
UNION ALL
SELECT
  'calendar'::text,
  calendar_events.event_id,
  calendar_events.title::text,
  calendar_events.summary::text,
  ('/calendar-event/' || calendar_events.event_unique_id)::text,
  NULL::jsonb,
  calendar_events.created,
  calendar_events.modified,
  calendar_events.modified_by,
  NULL::text,
  NULL::text,
  NULL::text,
  NULL::text,
  left(calendar_events.summary, 20000),
  calendar_events.tsv,
  true,
  true,
  NULL::bigint,
  NULL::bigint,
  calendar_events.start_date,
  calendars.calendar_id::bigint,
  calendars.name::text,
  NULL::jsonb,
  calendars.description::text
FROM calendar_events
JOIN calendars ON calendar_events.calendar_id = calendars.calendar_id
UNION ALL
SELECT
  'attachments'::text,
  files.file_id,
  COALESCE(NULLIF(files.title, ''), files.filename)::text,
  files.summary::text,
  (files.web_path || '-' || files.file_id)::text,
  files.tags,
  files.created,
  files.modified,
  files.modified_by,
  files.filename::text,
  files.file_type::text,
  files.mime_type::text,
  files.extension::text,
  left(COALESCE(NULLIF(files.document_text, ''), files.summary), 20000),
  files.tsv,
  true,
  true,
  files.folder_id,
  NULL::bigint,
  NULL::timestamp(3),
  NULL::bigint,
  NULL::text,
  NULL::jsonb,
  files.document_text
FROM files
UNION ALL
SELECT
  'resources'::text,
  items.item_id,
  items.name::text,
  items.summary_text::text,
  ('/show/' || items.unique_id)::text,
  items.tags,
  items.created,
  items.modified,
  items.modified_by,
  NULL::text,
  NULL::text,
  NULL::text,
  NULL::text,
  left(concat_ws(' ', items.name, items.keywords, items.summary_text, items.description_text, item_file_text.content), 20000),
  items.tsv || setweight(to_tsvector('title_stem', COALESCE(item_file_text.content, '')), 'D'),
  true,
  (items.approved IS NOT NULL),
  NULL::bigint,
  items.collection_id,
  NULL::timestamp(3),
  collections.collection_id::bigint,
  collections.name::text,
  items.field_values::jsonb,
  concat_ws(' ', items.description_text, item_file_text.content)
FROM items
JOIN collections ON items.collection_id = collections.collection_id
LEFT JOIN LATERAL (
  SELECT string_agg(concat_ws(' ', item_files.filename, item_files.title, item_files.summary, item_files.document_text), ' ') AS content
  FROM item_files
  WHERE item_files.item_id = items.item_id
) item_file_text ON true
UNION ALL
SELECT
  'resourceFiles'::text,
  item_files.file_id,
  COALESCE(NULLIF(item_files.title, ''), item_files.filename)::text,
  item_files.summary::text,
  ('/show/' || items.unique_id || '/assets/' || item_files.web_path || '-' || item_files.file_id)::text,
  item_files.tags,
  item_files.created,
  item_files.modified,
  item_files.modified_by,
  item_files.filename::text,
  item_files.file_type::text,
  item_files.mime_type::text,
  item_files.extension::text,
  left(COALESCE(NULLIF(item_files.document_text, ''), item_files.summary), 20000),
  item_files.tsv,
  true,
  (items.approved IS NOT NULL),
  item_files.folder_id,
  items.collection_id,
  NULL::timestamp(3),
  collections.collection_id::bigint,
  collections.name::text,
  NULL::jsonb,
  item_files.document_text
FROM item_files
JOIN items ON item_files.item_id = items.item_id
JOIN collections ON items.collection_id = collections.collection_id
WITH DATA;

-- Required for REFRESH MATERIALIZED VIEW CONCURRENTLY
CREATE UNIQUE INDEX unified_search_mv_key_idx ON unified_search_mv (result_type, result_id);
CREATE INDEX unified_search_mv_tsv_idx ON unified_search_mv USING gin(tsv);
CREATE INDEX unified_search_mv_type_modified_idx ON unified_search_mv (result_type, modified DESC);

CREATE VIEW unified_search AS SELECT * FROM unified_search_mv;
