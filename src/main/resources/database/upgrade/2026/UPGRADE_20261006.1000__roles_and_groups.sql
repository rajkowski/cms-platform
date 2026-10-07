ALTER TABLE menu_tabs DROP COLUMN role_id_list;
ALTER TABLE menu_tabs ADD roles JSONB;
ALTER TABLE menu_tabs ADD groups JSONB;

ALTER TABLE menu_items DROP COLUMN role_id_list;
ALTER TABLE menu_items ADD roles JSONB;
ALTER TABLE menu_items ADD groups JSONB;

ALTER TABLE web_pages DROP COLUMN role_id_list;
ALTER TABLE web_pages ADD roles JSONB;
ALTER TABLE web_pages ADD groups JSONB;

ALTER TABLE collection_tabs DROP COLUMN role_id_list;
ALTER TABLE collection_tabs ADD roles JSONB;
ALTER TABLE collection_tabs ADD groups JSONB;

DROP INDEX IF EXISTS col_tabs_col_idx;
CREATE INDEX col_tabs_col_idx ON collection_tabs(collection_id);

CREATE INDEX col_tab_group_tab_idx ON collection_tab_groups(tab_id);
