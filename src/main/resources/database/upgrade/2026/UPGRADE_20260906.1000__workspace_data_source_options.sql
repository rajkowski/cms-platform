-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Licensed under the Apache License, Version 2.0 (the "License").

ALTER TABLE workspace_data_sources ADD COLUMN IF NOT EXISTS pool_group VARCHAR(255);
ALTER TABLE workspace_data_sources ADD COLUMN IF NOT EXISTS auth_method VARCHAR(50);
ALTER TABLE workspace_data_sources ADD COLUMN IF NOT EXISTS azure_tenant_id VARCHAR(255);
ALTER TABLE workspace_data_sources ADD COLUMN IF NOT EXISTS azure_client_id VARCHAR(255);
ALTER TABLE workspace_data_sources ADD COLUMN IF NOT EXISTS azure_client_secret VARCHAR(2048);