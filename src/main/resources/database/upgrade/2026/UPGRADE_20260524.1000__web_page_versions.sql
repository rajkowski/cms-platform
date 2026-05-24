CREATE TABLE web_page_versions (
  version_id BIGSERIAL PRIMARY KEY,
  web_page_id BIGINT REFERENCES web_pages(web_page_id) NOT NULL,
  page_xml TEXT,
  created_by BIGINT REFERENCES users(user_id) NOT NULL,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  notes TEXT
);
CREATE INDEX web_page_ver_pg_idx ON web_page_versions(web_page_id);
CREATE INDEX web_page_ver_creat_idx ON web_page_versions(created);
