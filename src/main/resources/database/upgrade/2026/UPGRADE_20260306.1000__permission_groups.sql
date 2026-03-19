-- Permission Engine tables
-- Stores runtime overrides for Cedar permission policies.
-- Rows with matching group_code OVERRIDE the .cedar file;
-- rows with new group_codes ADD new policies to the engine.

CREATE TABLE IF NOT EXISTS permission_policies (
  policy_id BIGSERIAL PRIMARY KEY,
  group_code VARCHAR(100) UNIQUE NOT NULL,
  group_name VARCHAR(200),
  policy_text TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS permission_group_members (
  member_id BIGSERIAL PRIMARY KEY,
  group_code VARCHAR(100) NOT NULL REFERENCES permission_policies(group_code) ON DELETE CASCADE,
  class_name VARCHAR(500) NOT NULL,
  member_type VARCHAR(20) NOT NULL DEFAULT 'WIDGET'
);

CREATE INDEX IF NOT EXISTS idx_permission_group_members_group_code ON permission_group_members(group_code);
