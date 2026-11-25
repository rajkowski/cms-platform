INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type)
SELECT v.o, v.l, v.n,
  CASE
    WHEN v.n LIKE 'theme.button.%' THEN COALESCE(
      (SELECT NULLIF(property_value, '') FROM site_properties WHERE property_name = 'theme.button.text.color'),
      v.v)
    ELSE v.v
  END,
  'color'
FROM (VALUES
  (16, 'Web Page Link Color', 'theme.body.link.color', '#1779ba'),
  (51, 'Default Button Text', 'theme.button.default.text.color', '#FFFFFF'),
  (55, 'Primary Button Text', 'theme.button.primary.text.color', '#FFFFFF'),
  (59, 'Secondary Button Text', 'theme.button.secondary.text.color', '#FFFFFF'),
  (63, 'Success Button Text', 'theme.button.success.text.color', '#FFFFFF'),
  (67, 'Warning Button Text', 'theme.button.warning.text.color', '#FFFFFF'),
  (71, 'Alert Button Text', 'theme.button.alert.text.color', '#FFFFFF')
) AS v(o, l, n, v)
WHERE NOT EXISTS (SELECT 1 FROM site_properties WHERE property_name = v.n);
