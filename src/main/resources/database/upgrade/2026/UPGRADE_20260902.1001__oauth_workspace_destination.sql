ALTER TABLE oauth_state_values ADD COLUMN IF NOT EXISTS workspace_id BIGINT;
ALTER TABLE oauth_state_values ADD COLUMN IF NOT EXISTS destination_domain VARCHAR(255);