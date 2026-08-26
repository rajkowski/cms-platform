
INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type) VALUES (147, 'Use regions?', 'site.regions.enabled', 'false', 'boolean');

CREATE TABLE IF NOT EXISTS regions (
  region_id BIGSERIAL PRIMARY KEY,
  level INTEGER NOT NULL DEFAULT 0,
  code VARCHAR(20) UNIQUE NOT NULL,
  name VARCHAR(100),
  values JSONB
);

-- Example
-- INSERT INTO regions (level, code, name, values) VALUES (10, 'apac', 'Asia Pacific', '["apac"]');
-- INSERT INTO regions (level, code, name, values) VALUES (20, 'emea', 'Europe, Middle East, and Africa', '["emea"]');
-- INSERT INTO regions (level, code, name, values) VALUES (30, 'latam', 'Latin America', '["latam"]');
-- INSERT INTO regions (level, code, name, values) VALUES (40, 'na', 'North America', '["na", "na-ca", "na-us"]');
-- INSERT INTO regions (level, code, name, values) VALUES (60, 'global', 'Global', '["na", "na-us", "na-ca", "apac", "emea", "latam"]');
-- UPDATE site_properties SET property_value = 'true' WHERE property_name = 'site.regions.enabled';
