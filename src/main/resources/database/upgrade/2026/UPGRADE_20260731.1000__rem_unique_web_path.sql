
-- web_path is not always unique

ALTER TABLE images DROP CONSTRAINT IF EXISTS images_web_path_key; 
ALTER TABLE files DROP CONSTRAINT IF EXISTS files_web_path_key; 
ALTER TABLE item_files DROP CONSTRAINT IF EXISTS item_files_web_path_key;

DROP INDEX IF EXISTS images_web_path_key;
DROP INDEX IF EXISTS files_web_path_key;
DROP INDEX IF EXISTS item_files_web_path_key;
