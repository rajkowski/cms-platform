
INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type) VALUES (147, 'Use regions?', 'site.regions.enabled', 'false', 'boolean');

CREATE TABLE IF NOT EXISTS regions (
  region_id BIGSERIAL PRIMARY KEY,
  level INTEGER NOT NULL DEFAULT 0,
  code VARCHAR(20) UNIQUE NOT NULL,
  name VARCHAR(100),
  values JSONB
);
