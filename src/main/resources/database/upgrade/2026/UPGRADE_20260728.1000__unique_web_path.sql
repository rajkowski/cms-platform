
ALTER TABLE images ADD CONSTRAINT images_web_path_key UNIQUE (web_path);
ALTER TABLE files ADD CONSTRAINT files_web_path_key UNIQUE (web_path);
ALTER TABLE item_files ADD CONSTRAINT item_files_web_path_key UNIQUE (web_path);
