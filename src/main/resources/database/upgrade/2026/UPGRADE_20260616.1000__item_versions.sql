
CREATE TABLE item_versions (
  item_version_id BIGSERIAL PRIMARY KEY,
  item_id BIGINT REFERENCES items(item_id) NOT NULL,
  collection_id BIGINT REFERENCES collections(collection_id) NOT NULL,
  unique_id VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  created_by BIGINT REFERENCES users(user_id) NOT NULL,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP,
  version_data JSONB NOT NULL  
);

-- INSERT INTO item_versions (item_id, collection_id, unique_id, name, created_by, version_data)
-- SELECT item_id, collection_id, unique_id, name, created_by, to_jsonb(items.*) - 'tsv' AS version_data
-- FROM items;
