
ALTER TABLE files ADD COLUMN IF NOT EXISTS tags JSONB;
CREATE INDEX IF NOT EXISTS files_tags_idx ON files USING gin(tags);

ALTER TABLE item_files ADD COLUMN IF NOT EXISTS tags JSONB;
CREATE INDEX IF NOT EXISTS i_files_tags_idx ON item_files USING gin(tags);
