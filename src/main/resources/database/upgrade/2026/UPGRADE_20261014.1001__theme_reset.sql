
-- Reset theme header and footer names to default
DELETE FROM site_properties WHERE property_name = 'theme.header.name';
DELETE FROM site_properties WHERE property_name = 'theme.footer.name';
INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type) VALUES (130, 'Header XML Name', 'theme.header.name', '', 'text');
INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type) VALUES (132, 'Footer XML Name', 'theme.footer.name', '', 'text');

-- Backup legacy containers
UPDATE web_containers SET container_name = 'header.app.backup' WHERE container_name = 'header.default';
UPDATE web_containers SET container_name = 'footer.app.backup' WHERE container_name = 'footer.default';

-- Remove legacy header and footer options
DELETE FROM site_properties WHERE property_name = 'theme.menu.location';
DELETE FROM site_properties WHERE property_name = 'theme.footer.style';
DELETE FROM site_properties WHERE property_name = 'theme.footer.logo.color';
DELETE FROM site_properties WHERE property_name = 'theme.fonts.headlines';
DELETE FROM site_properties WHERE property_name = 'theme.fonts.body';
DELETE FROM site_properties WHERE property_name = 'theme.logo.color';

-- Reset footer theme colors to default if they match legacy values
UPDATE site_properties SET property_value = '' WHERE property_name = 'theme.footer.backgroundColor' AND property_value = '#353535';
UPDATE site_properties SET property_value = '' WHERE property_name = 'theme.footer.text.color' AND property_value = '#acacac';
UPDATE site_properties SET property_value = '' WHERE property_name = 'theme.footer.links.color' AND property_value = '#cdcdcd';
