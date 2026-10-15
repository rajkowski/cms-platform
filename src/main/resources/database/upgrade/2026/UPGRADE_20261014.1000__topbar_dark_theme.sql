-- Light mode defaults, only where the value is still the original install default
UPDATE site_properties SET property_value = v.new_value
FROM (VALUES
  ('theme.topbar.backgroundColor', '#353535', '#ffffff'),
  ('theme.topbar.text.color', '#ffffff', '#333333'),
  ('theme.topbar.menu.text.color', '#ffffff', '#333333'),
  ('theme.topbar.menu.arrow.color', '#ffffff', '#333333'),
  ('theme.topbar.menu.hoverTextColor', '#ffffff', '#2c3e50'),
  ('theme.topbar.menu.text.hoverBackgroundColor', '#4d4d4d', '#f5f5f5'),
  ('theme.topbar.menu.activeTextColor', '#ffffff', '#0d6efd'),
  ('theme.topbar.menu.activeBackgroundColor', '#4d4d4d', '#f5f5f5'),
  ('theme.topbar.menu.dropdown.backgroundColor', '#2e2e2e', '#ffffff'),
  ('theme.topbar.menu.dropdown.text.color', '#ffffff', '#333333')
) AS v(name, old_value, new_value)
WHERE site_properties.property_name = v.name AND LOWER(site_properties.property_value) = v.old_value;

-- Dark mode colors for the modern header (theme.topbar.*)
INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type)
SELECT v.o, v.l, v.n, v.v, 'color' FROM (VALUES
  (38, 'Dark Top Bar', 'theme.topbar.dark.backgroundColor', '#1a1a1a'),
  (39, 'Dark Top Bar Text Color', 'theme.topbar.dark.text.color', '#e0e0e0'),
  (40, 'Dark Menu Tab Text', 'theme.topbar.dark.menu.text.color', '#e0e0e0'),
  (41, 'Dark Menu Tab Arrow', 'theme.topbar.dark.menu.arrow.color', '#e0e0e0'),
  (42, 'Dark Menu Tab Hover Text', 'theme.topbar.dark.menu.hoverTextColor', '#ffffff'),
  (43, 'Dark Menu Tab Hover Bg', 'theme.topbar.dark.menu.text.hoverBackgroundColor', '#3a3a3a'),
  (44, 'Dark Menu Tab Active Text', 'theme.topbar.dark.menu.activeTextColor', '#ffffff'),
  (45, 'Dark Menu Tab Active Bg', 'theme.topbar.dark.menu.activeBackgroundColor', '#3a3a3a'),
  (46, 'Dark Drop Down Menu', 'theme.topbar.dark.menu.dropdown.backgroundColor', '#2a2a2a'),
  (47, 'Dark Drop Down Menu Text', 'theme.topbar.dark.menu.dropdown.text.color', '#e0e0e0')
) AS v(o, l, n, v)
WHERE NOT EXISTS (SELECT 1 FROM site_properties WHERE property_name = v.n);
