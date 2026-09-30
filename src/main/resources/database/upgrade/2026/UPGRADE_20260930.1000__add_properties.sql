-- Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
-- Licensed under the Apache License, Version 2.0 (the "License").

INSERT INTO site_properties (property_order, property_label, property_name, property_value, property_type) VALUES (21, 'OpenAuth Allow Public Access?', 'oauth.allowPublicAccess', 'false', 'boolean');

INSERT INTO lookup_role (level, code, title) VALUES (70, 'content-editor', 'Content Editor');
INSERT INTO lookup_role (level, code, title) VALUES (92, 'data-editor', 'Data Editor');
