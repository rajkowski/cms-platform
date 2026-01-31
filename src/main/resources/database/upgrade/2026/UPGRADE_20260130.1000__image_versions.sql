-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0
-- Add image versioning support

-- Create image_versions table to track all versions of an image
CREATE TABLE image_versions (
  version_id BIGSERIAL PRIMARY KEY,
  image_id BIGINT REFERENCES images(image_id) NOT NULL,
  version_number INTEGER NOT NULL,
  filename VARCHAR(255) NOT NULL,
  path VARCHAR(255) NOT NULL,
  file_length BIGINT DEFAULT 0,
  file_type VARCHAR(20),
  width INTEGER NOT NULL,
  height INTEGER NOT NULL,
  is_current BOOLEAN DEFAULT false,
  created_by BIGINT REFERENCES users(user_id) NOT NULL,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  notes TEXT
);

CREATE INDEX IF NOT EXISTS image_versions_img_idx ON image_versions(image_id);
CREATE INDEX IF NOT EXISTS image_versions_current_idx ON image_versions(image_id, is_current);
CREATE INDEX IF NOT EXISTS image_versions_created_idx ON image_versions(created);

-- Add metadata fields to images table for title, alt text, and description
ALTER TABLE images ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE images ADD COLUMN IF NOT EXISTS alt_text VARCHAR(500);
ALTER TABLE images ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE images ADD COLUMN IF NOT EXISTS version_number INTEGER DEFAULT 1;

-- Create index on images for metadata searches
CREATE INDEX IF NOT EXISTS images_title_idx ON images(title);
