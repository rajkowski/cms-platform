-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

ALTER TABLE items ADD COLUMN IF NOT EXISTS tags JSONB;
CREATE INDEX IF NOT EXISTS items_tags_idx ON items USING gin(tags);
