-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

-- Add version_number column to track sequential version numbers per content item
-- Example: Content 1 has versions 1,2,3... Content 2 has versions 1,2,3...
ALTER TABLE content_versions ADD COLUMN IF NOT EXISTS version_number INTEGER;

-- Create index for efficient querying by content_id and version_number
CREATE INDEX IF NOT EXISTS content_ver_num_idx ON content_versions(content_id, version_number);

-- Backfill version_number for existing records
-- Uses ROW_NUMBER() to assign sequential numbers based on created timestamp
WITH numbered_versions AS (
  SELECT 
    version_id,
    ROW_NUMBER() OVER (PARTITION BY content_id ORDER BY created ASC) AS version_num
  FROM content_versions
)
UPDATE content_versions cv
SET version_number = nv.version_num
FROM numbered_versions nv
WHERE cv.version_id = nv.version_id
  AND cv.version_number IS NULL;

-- Add NOT NULL constraint after backfilling data
ALTER TABLE content_versions ALTER COLUMN version_number SET NOT NULL;

-- Add unique constraint to ensure no duplicate version numbers per content
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'content_ver_unique_num'
  ) THEN
    ALTER TABLE content_versions ADD CONSTRAINT content_ver_unique_num UNIQUE (content_id, version_number);
  END IF;
END $$;
