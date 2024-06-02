UPDATE site_properties SET property_order = 10 WHERE property_name = 'mail.from_address';
UPDATE site_properties SET property_order = 20 WHERE property_name = 'mail.from_name';
UPDATE site_properties SET property_order = 30 WHERE property_name = 'mail.host_name';
UPDATE site_properties SET property_order = 40 WHERE property_name = 'mail.port';
UPDATE site_properties SET property_order = 50 WHERE property_name = 'mail.username';
UPDATE site_properties SET property_order = 60 WHERE property_name = 'mail.password';
UPDATE site_properties SET property_order = 70 WHERE property_name = 'mail.ssl';

INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type) VALUES (80, 'SMTP TLS', 'mail.tls', 'false', 'boolean');
