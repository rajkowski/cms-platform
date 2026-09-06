-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Licensed under the Apache License, Version 2.0 (the "License").

CREATE TABLE IF NOT EXISTS workspaces (
  workspace_id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  canonical_domain VARCHAR(255) NOT NULL UNIQUE,
  file_root VARCHAR(1024) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  show_in_catalog BOOLEAN NOT NULL DEFAULT TRUE,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS workspace_domains (
  workspace_domain_id BIGSERIAL PRIMARY KEY,
  workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  host_pattern VARCHAR(255) NOT NULL,
  wildcard BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (host_pattern, wildcard)
);

CREATE INDEX IF NOT EXISTS workspace_domains_active_idx ON workspace_domains (host_pattern, wildcard) WHERE active;

CREATE TABLE IF NOT EXISTS workspace_data_sources (
  workspace_id BIGINT PRIMARY KEY REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  jdbc_url VARCHAR(2048) NOT NULL,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(2048),
  driver_class_name VARCHAR(255) NOT NULL,
  pool_group VARCHAR(255),
  auth_method VARCHAR(50),
  azure_tenant_id VARCHAR(255),
  azure_client_id VARCHAR(255),
  azure_client_secret VARCHAR(2048)
);

CREATE TABLE IF NOT EXISTS workspace_access_grants (
  workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (workspace_id, user_id)
);

CREATE INDEX IF NOT EXISTS workspace_access_grants_user_idx ON workspace_access_grants (user_id) WHERE active;
