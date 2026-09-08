-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Licensed under the Apache License, Version 2.0 (the "License").

ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS site_url VARCHAR(512);
