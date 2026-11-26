INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type)
SELECT v.o, v.l, v.n, v.v, 'color'
FROM (VALUES
  (97, 'Box Callout Background', 'theme.callout.box.backgroundColor', '#ffffff'),
  (297, 'Dark Box Callout Background', 'theme.callout.dark.box.backgroundColor', '#252d38')
) AS v(o, l, n, v)
WHERE NOT EXISTS (SELECT 1 FROM site_properties WHERE property_name = v.n);
