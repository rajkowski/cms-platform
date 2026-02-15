-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

-- Add request configuration for dataset synchronization (headers, method, OAuth)
ALTER TABLE datasets ADD COLUMN IF NOT EXISTS request_config JSONB;
