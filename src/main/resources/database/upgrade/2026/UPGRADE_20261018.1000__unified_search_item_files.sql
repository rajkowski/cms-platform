-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski), Licensed under the Apache License, Version 2.0 (the "License").
-- One searchable row per page, blog post, calendar event, attachment and item, ranked together using each table's tsv

CREATE OR REPLACE VIEW unified_search AS
SELECT
  'pages'::text AS result_type,
  web_pages.web_page_id AS result_id,
  web_pages.page_title::text AS title,
  web_pages.page_description::text AS description,
  web_pages.link::text AS link,
  web_pages.tags AS tags,
  web_pages.modified AS modified,
  web_pages.modified_by AS modified_by,
  NULL::text AS file_name,
  NULL::text AS file_type,
  NULL::text AS mime_type,
  NULL::text AS extension,
  left(COALESCE(NULLIF(web_pages.page_text, ''), web_pages.page_description), 20000) AS headline_source,
  web_pages.tsv AS tsv,
  (COALESCE(web_pages.enabled, false) AND NOT COALESCE(web_pages.has_redirect, false)) AS is_listed,
  (COALESCE(web_pages.searchable, false) AND NOT COALESCE(web_pages.draft, false)) AS is_public,
  NULL::bigint AS folder_id,
  NULL::bigint AS collection_id,
  NULL::timestamp(3) AS start_date
FROM web_pages
UNION ALL
SELECT
  'posts'::text,
  blog_posts.post_id,
  blog_posts.title::text,
  blog_posts.summary::text,
  ('/' || blogs.blog_unique_id || '/' || blog_posts.post_unique_id)::text,
  NULL::jsonb,
  blog_posts.modified,
  blog_posts.modified_by,
  NULL::text,
  NULL::text,
  NULL::text,
  NULL::text,
  left(blog_posts.body_text, 20000),
  blog_posts.tsv,
  (blog_posts.published IS NOT NULL),
  true,
  NULL::bigint,
  NULL::bigint,
  NULL::timestamp(3)
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
  calendar_events.start_date
FROM calendar_events
UNION ALL
SELECT
  'attachments'::text,
  files.file_id,
  COALESCE(NULLIF(files.title, ''), files.filename)::text,
  files.summary::text,
  (files.web_path || '-' || files.file_id)::text,
  files.tags,
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
  NULL::timestamp(3)
FROM files
UNION ALL
SELECT
  'resources'::text,
  items.item_id,
  items.name::text,
  items.summary_text::text,
  ('/show/' || items.unique_id)::text,
  items.tags,
  items.modified,
  items.modified_by,
  NULL::text,
  NULL::text,
  NULL::text,
  NULL::text,
  left(items.name || ' ' || COALESCE(items.keywords, '') || ' ' || COALESCE(items.summary_text, '') || ' ' || COALESCE(items.description_text, ''), 20000),
  items.tsv,
  true,
  (items.approved IS NOT NULL),
  NULL::bigint,
  items.collection_id,
  NULL::timestamp(3)
FROM items
UNION ALL
SELECT
  'resourceFiles'::text,
  item_files.file_id,
  COALESCE(NULLIF(item_files.title, ''), item_files.filename)::text,
  item_files.summary::text,
  ('/show/' || items.unique_id || '/assets/' || item_files.web_path || '-' || item_files.file_id)::text,
  item_files.tags,
  item_files.modified,
  item_files.modified_by,
  item_files.filename::text,
  item_files.file_type::text,
  item_files.mime_type::text,
  item_files.extension::text,
  left(COALESCE(NULLIF(item_files.document_text, ''), item_files.summary), 20000),
  item_files.tsv,
  true,
  true,
  item_files.folder_id,
  items.collection_id,
  NULL::timestamp(3)
FROM item_files
JOIN items ON item_files.item_id = items.item_id;
