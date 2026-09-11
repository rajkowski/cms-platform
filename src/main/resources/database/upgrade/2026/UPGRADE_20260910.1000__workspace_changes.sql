-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Licensed under the Apache License, Version 2.0 (the "License").

ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(user_id);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS modified_by BIGINT REFERENCES users(user_id);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS owner BIGINT REFERENCES users(user_id);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS department VARCHAR(255);
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS description TEXT;

ALTER TABLE workspace_access_grants ADD COLUMN IF NOT EXISTS created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE workspace_access_grants ADD COLUMN IF NOT EXISTS modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE workspace_access_grants ADD COLUMN IF NOT EXISTS roles JSONB;
ALTER TABLE workspace_access_grants ADD COLUMN IF NOT EXISTS groups JSONB;
