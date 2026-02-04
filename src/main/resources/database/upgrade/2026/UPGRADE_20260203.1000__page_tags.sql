-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0
-- Add page tagging support

ALTER TABLE web_pages ADD COLUMN IF NOT EXISTS tags JSONB;
CREATE INDEX IF NOT EXISTS web_pages_tags_idx ON web_pages USING GIN (tags);
