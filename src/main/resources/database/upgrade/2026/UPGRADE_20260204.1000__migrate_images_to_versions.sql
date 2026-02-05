-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0
-- Migrate all existing images to image_versions table

-- Insert all existing images into image_versions as version 1
INSERT INTO image_versions (
  image_id,
  version_number,
  filename,
  path,
  file_length,
  file_type,
  width,
  height,
  is_current,
  created_by,
  created,
  notes
)
SELECT
  image_id,
  COALESCE(version_number, 1) AS version_number,
  filename,
  path,
  file_length,
  file_type,
  width,
  height,
  true AS is_current, -- Mark all as current since they're the only version
  created_by,
  created,
  'Initial version migrated from images table' AS notes
FROM images
WHERE NOT EXISTS (
  SELECT 1
  FROM image_versions iv
  WHERE iv.image_id = images.image_id
    AND iv.version_number = COALESCE(images.version_number, 1)
);

-- Create indexes to support search functionality
CREATE INDEX images_filename_lower_idx ON images(LOWER(filename) text_pattern_ops);
CREATE INDEX images_title_lower_idx ON images(LOWER(title) text_pattern_ops);
CREATE INDEX images_alt_text_lower_idx ON images(LOWER(alt_text) text_pattern_ops);
CREATE INDEX images_description_lower_idx ON images(LOWER(description) text_pattern_ops);
