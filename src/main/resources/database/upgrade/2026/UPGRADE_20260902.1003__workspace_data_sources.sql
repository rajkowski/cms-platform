CREATE TABLE IF NOT EXISTS workspace_data_sources (
  workspace_id BIGINT PRIMARY KEY REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  jdbc_url VARCHAR(2048) NOT NULL,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(2048),
  driver_class_name VARCHAR(255) NOT NULL
);