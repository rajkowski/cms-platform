-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

ALTER TABLE content ADD COLUMN IF NOT EXISTS tags JSONB;
CREATE INDEX IF NOT EXISTS content_tags_idx ON content USING gin(tags);

CREATE TABLE content_versions (
  version_id BIGSERIAL PRIMARY KEY,
  content_id BIGINT REFERENCES content(content_id) NOT NULL,
  content TEXT,
  created_by BIGINT REFERENCES users(user_id) NOT NULL,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  notes TEXT
);
CREATE INDEX content_ver_cont_idx ON content_versions(content_id);
CREATE INDEX content_ver_creat_idx ON content_versions(created);
