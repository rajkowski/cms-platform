CREATE TABLE IF NOT EXISTS workspaces (
  workspace_id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  canonical_domain VARCHAR(255) NOT NULL UNIQUE,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS workspace_domains (
  workspace_domain_id BIGSERIAL PRIMARY KEY,
  workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  host_pattern VARCHAR(255) NOT NULL,
  wildcard BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  UNIQUE (host_pattern, wildcard)
);

CREATE TABLE IF NOT EXISTS workspace_access_grants (
  workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (workspace_id, user_id)
);

CREATE INDEX IF NOT EXISTS workspace_domains_active_idx ON workspace_domains (host_pattern, wildcard) WHERE active;
CREATE INDEX IF NOT EXISTS workspace_access_grants_user_idx ON workspace_access_grants (user_id) WHERE active;
