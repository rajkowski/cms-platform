INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type)
SELECT v.o, v.l, v.n, v.v, 'color'
FROM (VALUES
  (214, 'Dark Web Page Background', 'theme.body.dark.backgroundColor', '#10151d'),
  (215, 'Dark Web Page Text Color', 'theme.body.dark.text.color', '#e8edf2'),
  (216, 'Dark Web Page Link Color', 'theme.body.dark.link.color', '#60a5fa'),
  (217, 'Dark Bar Background Color', 'theme.utilitybar.dark.backgroundColor', '#17212c'),
  (218, 'Dark Bar Text Color', 'theme.utilitybar.dark.text.color', '#f3f4f6'),
  (219, 'Dark Bar Link Color', 'theme.utilitybar.dark.link.color', '#f3f4f6'),
  (280, 'Dark Callout Background', 'theme.callout.dark.backgroundColor', '#252d38'),
  (281, 'Dark Callout Text Color', 'theme.callout.dark.text.color', '#e8edf2'),
  (283, 'Dark Primary Callout Background', 'theme.callout.dark.primary.backgroundColor', '#17283a'),
  (284, 'Dark Primary Callout Text Color', 'theme.callout.dark.primary.text.color', '#e8edf2'),
  (286, 'Dark Secondary Callout Background', 'theme.callout.dark.secondary.backgroundColor', '#252d38'),
  (287, 'Dark Secondary Callout Text Color', 'theme.callout.dark.secondary.text.color', '#e8edf2'),
  (289, 'Dark Success Callout Background', 'theme.callout.dark.success.backgroundColor', '#173d2b'),
  (290, 'Dark Success Callout Text Color', 'theme.callout.dark.success.text.color', '#e8edf2'),
  (292, 'Dark Warning Callout Background', 'theme.callout.dark.warning.backgroundColor', '#4a3513'),
  (293, 'Dark Warning Callout Text Color', 'theme.callout.dark.warning.text.color', '#fff4d6'),
  (295, 'Dark Alert Callout Background', 'theme.callout.dark.alert.backgroundColor', '#492423'),
  (296, 'Dark Alert Callout Text Color', 'theme.callout.dark.alert.text.color', '#fde8e7'),
  (298, 'Dark Pagination Border Color', 'theme.pagination.dark.border.color', '#46505d'),
  (299, 'Dark Pagination Link Color', 'theme.pagination.dark.link.color', '#60a5fa'),
  (300, 'Dark Current Page Background', 'theme.pagination.dark.current.backgroundColor', '#60a5fa'),
  (301, 'Dark Current Page Text', 'theme.pagination.dark.current.text.color', '#ffffff'),
  (352, 'Dark Badge Background', 'theme.badge.dark.backgroundColor', '#3b4654'),
  (353, 'Dark Badge Text', 'theme.badge.dark.text.color', '#f3f4f6'),
  (364, 'Dark Progress Track', 'theme.progress.dark.backgroundColor', '#343d49'),
  (365, 'Dark Progress Text', 'theme.progress.dark.text.color', '#ffffff'),
  (412, 'Dark Footer Background', 'theme.footer.dark.backgroundColor', '#0b1118'),
  (414, 'Dark Footer Text Color', 'theme.footer.dark.text.color', '#dce2e8'),
  (416, 'Dark Footer Links Color', 'theme.footer.dark.links.color', '#60a5fa')
) AS v(o, l, n, v)
WHERE NOT EXISTS (SELECT 1 FROM site_properties WHERE property_name = v.n);
