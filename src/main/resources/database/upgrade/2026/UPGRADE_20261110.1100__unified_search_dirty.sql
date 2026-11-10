-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski), Licensed under the Apache License, Version 2.0 (the "License").
-- Flags unified_search_mv as stale when any source table changes; RefreshUnifiedSearchJob refreshes it when flagged

CREATE TABLE IF NOT EXISTS unified_search_refresh (
  dirty BOOLEAN NOT NULL DEFAULT true
);
INSERT INTO unified_search_refresh (dirty) SELECT true WHERE NOT EXISTS (SELECT 1 FROM unified_search_refresh);

CREATE OR REPLACE FUNCTION unified_search_mark_dirty() RETURNS trigger AS $$
BEGIN
  -- Matches no row (and takes no lock) when already flagged
  UPDATE unified_search_refresh SET dirty = true WHERE NOT dirty;
  RETURN NULL;
END
$$ LANGUAGE plpgsql;

DO $$
DECLARE
  source_table text;
BEGIN
  FOREACH source_table IN ARRAY ARRAY['web_pages', 'web_page_files', 'blogs', 'blog_posts', 'calendars', 'calendar_events', 'files', 'collections', 'items', 'item_files']
  LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS unified_search_dirty ON %I', source_table);
    EXECUTE format('CREATE TRIGGER unified_search_dirty AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH STATEMENT EXECUTE FUNCTION unified_search_mark_dirty()', source_table);
  END LOOP;
END
$$;
