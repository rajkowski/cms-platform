
ALTER TABLE workspaces ADD workspace_unique_id VARCHAR(255);
UPDATE workspaces SET workspace_unique_id = workspace_id::text;
ALTER TABLE workspaces ALTER COLUMN workspace_unique_id SET NOT NULL;
CREATE UNIQUE INDEX workspace_unique_id_idx ON workspaces(workspace_unique_id);
