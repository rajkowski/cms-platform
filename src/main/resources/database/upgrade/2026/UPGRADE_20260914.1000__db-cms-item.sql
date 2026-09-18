-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Copyright 2026 SimIS Inc.
-- Licensed under the Apache License, Version 2.0 (the "License").

ALTER TABLE users ADD COLUMN account_token_expires TIMESTAMP(3);
ALTER TABLE users ADD COLUMN mfa_secret VARCHAR(64);
ALTER TABLE users ADD COLUMN mfa_enabled BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN failed_attempt_count INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP(3);
ALTER TABLE users ADD COLUMN last_password_changed_at TIMESTAMP(3);
ALTER TABLE users ADD COLUMN suspension_reason VARCHAR(255);

ALTER TABLE sessions ADD COLUMN host VARCHAR(255);

CREATE TABLE audit_log (
  audit_id BIGSERIAL PRIMARY KEY,
  occurred TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  event_category VARCHAR(50) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  outcome VARCHAR(20) NOT NULL,
  actor_user_id BIGINT,
  actor_username VARCHAR(255),
  source_ip VARCHAR(200),
  target_type VARCHAR(50),
  target_id VARCHAR(255),
  target_label VARCHAR(255),
  details TEXT,
  session_id VARCHAR(255),
  schema_version INTEGER DEFAULT 1 NOT NULL,
  previous_hash VARCHAR(64),
  record_hash VARCHAR(64)
);
CREATE INDEX audit_log_occurred_idx ON audit_log(occurred);
CREATE INDEX audit_log_actor_idx ON audit_log(actor_user_id);
CREATE INDEX audit_log_category_type_idx ON audit_log(event_category, event_type);
CREATE INDEX audit_log_target_idx ON audit_log(target_type, target_label);

CREATE TABLE audit_log_archive (
  audit_id BIGINT PRIMARY KEY,
  occurred TIMESTAMP(3) NOT NULL,
  event_category VARCHAR(50) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  outcome VARCHAR(20) NOT NULL,
  actor_user_id BIGINT,
  actor_username VARCHAR(255),
  source_ip VARCHAR(200),
  target_type VARCHAR(50),
  target_id VARCHAR(255),
  target_label VARCHAR(255),
  details TEXT,
  session_id VARCHAR(255),
  schema_version INTEGER NOT NULL,
  previous_hash VARCHAR(64),
  record_hash VARCHAR(64),
  archived TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE audit_log_watermark (
  id INTEGER PRIMARY KEY DEFAULT 1,
  lowest_hashed_audit_id BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE web_pages ADD COLUMN submitted_by BIGINT REFERENCES users(user_id);
ALTER TABLE web_pages ADD COLUMN approved_by BIGINT REFERENCES users(user_id);
ALTER TABLE web_pages ADD COLUMN publish_at TIMESTAMP(3);
ALTER TABLE web_pages ADD COLUMN expires_at TIMESTAMP(3);
ALTER TABLE web_pages ADD COLUMN locale VARCHAR(35) NOT NULL DEFAULT 'en';

ALTER TABLE content ADD COLUMN content_format INTEGER NOT NULL DEFAULT 0;
ALTER TABLE content ADD COLUMN draft_content_format INTEGER NOT NULL DEFAULT 0;
ALTER TABLE content ADD COLUMN submitted_by BIGINT REFERENCES users(user_id);
ALTER TABLE content ADD COLUMN approved_by BIGINT REFERENCES users(user_id);
ALTER TABLE content ADD COLUMN locale VARCHAR(35) NOT NULL DEFAULT 'en';

CREATE TABLE form_definitions (
  form_definition_id BIGSERIAL PRIMARY KEY,
  unique_id VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255) NOT NULL,
  title VARCHAR(255),
  subtitle VARCHAR(255),
  button_name VARCHAR(100),
  success_title VARCHAR(255),
  success_message TEXT,
  email_to VARCHAR(512),
  use_captcha BOOLEAN DEFAULT FALSE,
  check_for_spam BOOLEAN DEFAULT TRUE,
  enabled BOOLEAN DEFAULT TRUE,
  show_privacy_notice BOOLEAN DEFAULT FALSE,
  send_confirmation_to_submitter BOOLEAN DEFAULT FALSE,
  notification_subject VARCHAR(255),
  confirmation_subject VARCHAR(255),
  confirmation_message TEXT,
  created_by BIGINT REFERENCES users(user_id),
  modified_by BIGINT REFERENCES users(user_id),
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX form_definitions_unique_id_idx ON form_definitions(unique_id);
CREATE INDEX form_definitions_enabled_idx ON form_definitions(enabled);

CREATE TABLE form_fields (
  form_field_id BIGSERIAL PRIMARY KEY,
  form_definition_id BIGINT NOT NULL REFERENCES form_definitions(form_definition_id),
  field_order INTEGER DEFAULT 100,
  name VARCHAR(255) NOT NULL,
  label VARCHAR(255) NOT NULL,
  field_type VARCHAR(30) DEFAULT 'text',
  required BOOLEAN DEFAULT FALSE,
  placeholder VARCHAR(255),
  default_value VARCHAR(255),
  options TEXT,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX form_fields_form_definition_idx ON form_fields(form_definition_id);
CREATE INDEX form_fields_order_idx ON form_fields(field_order);

CREATE TABLE form_submission_failures (
  failure_id BIGSERIAL PRIMARY KEY,
  form_unique_id VARCHAR(255),
  occurred TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  reason VARCHAR(30) NOT NULL,
  ip_address VARCHAR(200),
  url VARCHAR(512)
);
CREATE INDEX form_sub_fail_form_idx ON form_submission_failures(form_unique_id);
CREATE INDEX form_sub_fail_occurred_idx ON form_submission_failures(occurred);
CREATE INDEX form_sub_fail_reason_idx ON form_submission_failures(reason);

CREATE INDEX web_pg_hits_wpid_idx ON web_page_hits(web_page_id);

ALTER TABLE web_searches ADD COLUMN created_by BIGINT REFERENCES users(user_id);

CREATE TABLE search_analytics (
  search_analytics_id BIGSERIAL PRIMARY KEY,
  query VARCHAR(255) NOT NULL,
  search_type VARCHAR(50) NOT NULL,
  result_count INTEGER NOT NULL DEFAULT 0,
  page_path VARCHAR(255),
  facet_key VARCHAR(100),
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX search_analytics_created_idx ON search_analytics(created);
CREATE INDEX search_analytics_query_idx ON search_analytics(query);

ALTER TABLE blog_posts ADD COLUMN source_url VARCHAR(512);
ALTER TABLE blog_posts ADD COLUMN submitted_by BIGINT REFERENCES users(user_id);
ALTER TABLE blog_posts ADD COLUMN approved_by BIGINT REFERENCES users(user_id);
ALTER TABLE blog_posts ADD COLUMN locale VARCHAR(35) NOT NULL DEFAULT 'en';

ALTER TABLE calendar_events ADD COLUMN organizer_name VARCHAR(255);
ALTER TABLE calendar_events ADD COLUMN organizer_url VARCHAR(255);
ALTER TABLE calendar_events ADD COLUMN performer_name VARCHAR(255);
ALTER TABLE calendar_events ADD COLUMN performer_url VARCHAR(255);

ALTER TABLE items ADD COLUMN summary_text TEXT;

CREATE OR REPLACE FUNCTION items_tsv_trigger() RETURNS trigger AS $$
begin
  new.tsv :=
          setweight(to_tsvector('title_stem', new.name), 'A') ||
          setweight(to_tsvector(coalesce(new.keywords,'')), 'B') ||
          setweight(to_tsvector('title_stem', coalesce(new.summary_text,'')), 'C') ||
          setweight(to_tsvector('title_stem', coalesce(new.description_text,'')), 'D');
  return new;
end
$$ LANGUAGE plpgsql;

UPDATE items SET summary_text = summary;

ALTER TABLE emails ADD COLUMN title VARCHAR(150);
ALTER TABLE emails ADD COLUMN phone VARCHAR(50);
ALTER TABLE emails ADD COLUMN validated_at TIMESTAMP(3);

ALTER TABLE mailing_lists ADD COLUMN unique_id VARCHAR(255);

DO $$
DECLARE
  list RECORD;
  base TEXT;
  candidate TEXT;
  suffix INT;
BEGIN
  FOR list IN SELECT list_id, name FROM mailing_lists ORDER BY list_id LOOP
    base := COALESCE(NULLIF(
        TRIM(BOTH '-' FROM
          REGEXP_REPLACE(
            REGEXP_REPLACE(
              REGEXP_REPLACE(REPLACE(LOWER(list.name), '&', 'and'), '[ /]+', '-', 'g'),
              '[^a-z0-9-]', '', 'g'),
            '-+', '-', 'g')), ''), 'list');
    candidate := base;
    suffix := 1;
    WHILE EXISTS (SELECT 1 FROM mailing_lists WHERE unique_id = candidate) LOOP
      suffix := suffix + 1;
      candidate := base || '-' || suffix;
    END LOOP;
    UPDATE mailing_lists SET unique_id = candidate WHERE list_id = list.list_id;
  END LOOP;
END $$;

ALTER TABLE mailing_lists ADD CONSTRAINT mailing_lists_unique_id_key UNIQUE (unique_id);
ALTER TABLE mailing_lists ALTER COLUMN unique_id SET NOT NULL;

ALTER TABLE mailing_list_members ADD COLUMN unsubscribe_token VARCHAR(255);
ALTER TABLE mailing_list_members ADD COLUMN confirmed TIMESTAMP(3);
ALTER TABLE mailing_list_members ADD COLUMN confirm_token VARCHAR(255);
ALTER TABLE mailing_list_members ADD COLUMN confirm_token_expires TIMESTAMP(3);

CREATE UNIQUE INDEX mail_lis_mem_unsub_tok_idx ON mailing_list_members(unsubscribe_token);
CREATE UNIQUE INDEX mail_lis_mem_confirm_tok_idx ON mailing_list_members(confirm_token);

ALTER TABLE mailing_list_history ADD COLUMN subject VARCHAR(255);
ALTER TABLE mailing_list_history ADD COLUMN blog_post_id BIGINT REFERENCES blog_posts(post_id);
ALTER TABLE mailing_list_history ADD COLUMN mailchimp_campaign_id VARCHAR(50);
