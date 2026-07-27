CREATE TABLE web_page_files (
  web_page_file_id BIGSERIAL PRIMARY KEY,
  web_page_id BIGINT REFERENCES web_pages(web_page_id),
  file_id BIGINT REFERENCES files(file_id),
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  created_by BIGINT REFERENCES users(user_id) NOT NULL
);

CREATE INDEX web_page_files_web_page_idx ON web_page_files(web_page_id);
CREATE INDEX web_page_files_file_idx ON web_page_files(file_id);
