-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

CREATE TABLE site_exports (
  export_id BIGSERIAL PRIMARY KEY,
  export_date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  export_by BIGINT REFERENCES users(user_id),
  export_path VARCHAR(255) NOT NULL,
  export_size BIGINT DEFAULT 0,
  notes TEXT
);
CREATE INDEX site_exports_dt_idx ON site_exports(export_date);
