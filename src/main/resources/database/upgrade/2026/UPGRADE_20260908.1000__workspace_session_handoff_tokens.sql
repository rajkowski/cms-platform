-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Licensed under the Apache License, Version 2.0 (the "License").

CREATE TABLE IF NOT EXISTS workspace_session_handoff_tokens (
  workspace_session_handoff_token_id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  resource VARCHAR(512) NOT NULL,
  token VARCHAR(100) NOT NULL UNIQUE,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  consumed_at TIMESTAMP(3)
);

CREATE INDEX IF NOT EXISTS workspace_handoff_tok_idx ON workspace_session_handoff_tokens (token);
CREATE INDEX IF NOT EXISTS workspace_handoff_cre_idx ON workspace_session_handoff_tokens (created);
