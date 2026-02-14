-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0
-- Git publish settings for static site export

CREATE TABLE git_publish_settings (
  settings_id BIGSERIAL PRIMARY KEY,
  enabled BOOLEAN DEFAULT false,
  git_provider VARCHAR(50) NOT NULL,
  repository_url VARCHAR(500) NOT NULL,
  branch_name VARCHAR(255) DEFAULT 'main',
  base_branch VARCHAR(255) DEFAULT 'main',
  access_token TEXT,
  username VARCHAR(255),
  email VARCHAR(255),
  commit_message_template VARCHAR(500) DEFAULT 'Static site update: ${timestamp}',
  auto_create_pr BOOLEAN DEFAULT true,
  pr_title_template VARCHAR(255) DEFAULT 'Static site update: ${timestamp}',
  pr_description_template TEXT DEFAULT 'Automated static site export',
  target_directory VARCHAR(500) DEFAULT '/',
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT REFERENCES users(user_id),
  modified_by BIGINT REFERENCES users(user_id)
);
