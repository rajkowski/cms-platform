-- Adjust terminology
UPDATE site_properties SET property_order = 200, property_label = 'Standard Logo' WHERE property_name = 'site.logo';
UPDATE site_properties SET property_order = 210, property_label = 'Dark Mode Logo' WHERE property_name = 'site.logo.mixed';
UPDATE site_properties SET property_order = 220, property_label = 'All White Logo' WHERE property_name = 'site.logo.white';
