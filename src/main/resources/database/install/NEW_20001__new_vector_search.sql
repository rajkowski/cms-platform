-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski), Licensed under the Apache License, Version 2.0 (the "License").
-- Chunked content from unified_search with 384-dimension embeddings (pgvector)

-- Exactly one source reference is set; chunks are deleted with their source row
CREATE TABLE IF NOT EXISTS search_chunks (
  chunk_id BIGSERIAL PRIMARY KEY,
  web_page_id BIGINT REFERENCES web_pages(web_page_id) ON DELETE CASCADE,
  post_id BIGINT REFERENCES blog_posts(post_id) ON DELETE CASCADE,
  event_id BIGINT REFERENCES calendar_events(event_id) ON DELETE CASCADE,
  file_id BIGINT REFERENCES files(file_id) ON DELETE CASCADE,
  item_id BIGINT REFERENCES items(item_id) ON DELETE CASCADE,
  item_file_id BIGINT REFERENCES item_files(file_id) ON DELETE CASCADE,
  chunk_index INTEGER NOT NULL,
  chunk_text TEXT NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  embedding vector(384),
  embedding_model VARCHAR(255),
  embedded TIMESTAMP(3),
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  modified TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CHECK (num_nonnulls(web_page_id, post_id, event_id, file_id, item_id, item_file_id) = 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS search_chunks_web_page_idx ON search_chunks (web_page_id, chunk_index) WHERE web_page_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS search_chunks_post_idx ON search_chunks (post_id, chunk_index) WHERE post_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS search_chunks_event_idx ON search_chunks (event_id, chunk_index) WHERE event_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS search_chunks_file_idx ON search_chunks (file_id, chunk_index) WHERE file_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS search_chunks_item_idx ON search_chunks (item_id, chunk_index) WHERE item_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS search_chunks_item_file_idx ON search_chunks (item_file_id, chunk_index) WHERE item_file_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS search_chunks_pending_idx ON search_chunks (chunk_id) WHERE embedding IS NULL;
CREATE INDEX IF NOT EXISTS search_chunks_embedding_idx ON search_chunks USING hnsw (embedding vector_cosine_ops);

