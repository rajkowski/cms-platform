
CREATE TABLE IF NOT EXISTS data_source_pools (
  pool_id BIGSERIAL PRIMARY KEY,
  pool_unique_id VARCHAR(255) UNIQUE NOT NULL,  
  ds_class_name VARCHAR(255),
  driver_class_name VARCHAR(255),
  server_name VARCHAR(255) NOT NULL,
  port_number INT DEFAULT 5432,
  ssl BOOLEAN NOT NULL DEFAULT FALSE,
  username VARCHAR(255) NOT NULL,
  password VARCHAR(2048),
  auth_method VARCHAR(50),
  azure_tenant_id VARCHAR(255),
  azure_client_id VARCHAR(255),
  azure_client_secret VARCHAR(2048),
  max_connections INT NOT NULL DEFAULT 8
);

CREATE TABLE IF NOT EXISTS pool_databases (
  pool_database_id BIGSERIAL PRIMARY KEY,
  pool_id BIGINT NOT NULL REFERENCES data_source_pools(pool_id) ON DELETE CASCADE,
  database_name VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT FALSE,
  available BOOLEAN NOT NULL DEFAULT TRUE,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);

-- Migrate workspace_data_sources to the new data_source_pools structure

INSERT INTO data_source_pools (pool_unique_id, ds_class_name, driver_class_name, server_name, username, password, port_number, ssl, auth_method, azure_tenant_id, azure_client_id, azure_client_secret, max_connections)
SELECT DISTINCT wds.pool_group, 'org.postgresql.ds.PGSimpleDataSource', null, 'localhost', wds.username, wds.password, 5432, false, null, null, null, null, 8
FROM workspace_data_sources wds;

-- Extract the last part of the jdbc_url (after /) for the database_name
INSERT INTO pool_databases (pool_id, database_name, active, available, created, modified)
SELECT 1, split_part(wds.jdbc_url, '/', array_length(string_to_array(wds.jdbc_url, '/'), 1)), TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM workspace_data_sources wds
ORDER BY wds.workspace_id;

-- Rebuild the workspace_data_sources from the pool_databases

DROP TABLE workspace_data_sources;

CREATE TABLE workspace_data_sources (
  workspace_id BIGINT PRIMARY KEY REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
  pool_database_id BIGINT REFERENCES pool_databases(pool_database_id) ON DELETE CASCADE,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO workspace_data_sources (workspace_id, pool_database_id)
SELECT pd.pool_database_id, pd.pool_database_id
FROM pool_databases pd
ORDER BY pd.pool_database_id;

-- A workspace can only have one pool_database associated with it

ALTER TABLE workspace_data_sources
ADD CONSTRAINT unique_workspace_pool_database UNIQUE (workspace_id, pool_database_id);

-- Update grants too

ALTER TABLE workspace_access_grants
ADD CONSTRAINT unique_workspace_user UNIQUE (workspace_id, user_id);
