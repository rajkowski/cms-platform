-- Add modified tracking columns to images table
ALTER TABLE images ADD COLUMN IF NOT EXISTS modified_by BIGINT REFERENCES users(user_id);
ALTER TABLE images ADD COLUMN IF NOT EXISTS modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP;
UPDATE images SET modified_by = created_by, modified = created;
-- Set NOT NULL constraints
ALTER TABLE images ALTER COLUMN modified_by SET NOT NULL;
