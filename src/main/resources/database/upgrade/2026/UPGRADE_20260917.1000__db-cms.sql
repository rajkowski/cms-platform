-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Copyright 2026 SimIS Inc.
-- Licensed under the Apache License, Version 2.0 (the "License").

CREATE TABLE redirects (
  redirect_id BIGSERIAL PRIMARY KEY,
  redirect_from VARCHAR(500) NOT NULL,
  redirect_to VARCHAR(2000) NOT NULL,
  status_code INTEGER NOT NULL DEFAULT 301,
  enabled BOOLEAN DEFAULT true,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT REFERENCES users(user_id),
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified_by BIGINT REFERENCES users(user_id),
  CONSTRAINT redirects_status_code_check CHECK (status_code IN (301, 302))
);
CREATE UNIQUE INDEX redirects_from_idx ON redirects(redirect_from);
CREATE INDEX redirects_enabled_idx ON redirects(enabled);

CREATE TABLE file_downloads (
  id BIGSERIAL PRIMARY KEY,
  file_id BIGINT NOT NULL,
  version_id BIGINT,
  download_by BIGINT REFERENCES users(user_id),
  download_date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  ip_address VARCHAR(200),
  session_id VARCHAR(255),
  is_logged_in BOOLEAN DEFAULT FALSE
);
CREATE INDEX file_downloads_fid_idx ON file_downloads(file_id);
CREATE INDEX file_downloads_dt_idx ON file_downloads(download_date);

CREATE TABLE file_download_snapshots (
  snapshot_id BIGSERIAL PRIMARY KEY,
  snapshot_date TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  date_value VARCHAR(10) NOT NULL,
  file_id BIGINT,
  downloads BIGINT DEFAULT 0
);
CREATE INDEX file_dl_snp_dt_idx ON file_download_snapshots(snapshot_date);
CREATE INDEX file_dl_snp_fid_idx ON file_download_snapshots(file_id);

-- Multi-factor authentication recovery codes: one-time backup codes, stored as SHA-256 hashes
CREATE TABLE user_mfa_recovery_codes (
  recovery_code_id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(user_id) NOT NULL,
  code_hash VARCHAR(64) NOT NULL,
  used BOOLEAN DEFAULT false,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX user_mfa_recovery_codes_user_idx ON user_mfa_recovery_codes(user_id);
