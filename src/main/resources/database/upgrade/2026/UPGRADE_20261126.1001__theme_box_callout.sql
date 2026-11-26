INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type)
SELECT v.o, v.l, v.n, v.v, 'color'
FROM (VALUES
  (97, 'Box Callout Text Color', 'theme.callout.box.text.color', '#0a0a0a'),
  (297, 'Dark Box Callout Text Color', 'theme.callout.dark.box.text.color', '#e8edf2')
) AS v(o, l, n, v)
WHERE NOT EXISTS (SELECT 1 FROM site_properties WHERE property_name = v.n);
