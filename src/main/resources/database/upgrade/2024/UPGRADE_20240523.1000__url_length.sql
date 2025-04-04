ALTER TABLE users ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE users ALTER COLUMN video_url TYPE VARCHAR(512);

ALTER TABLE collections ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE categories ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE collection_tabs ALTER COLUMN page_image_url TYPE VARCHAR(512);

ALTER TABLE items ALTER COLUMN url TYPE VARCHAR(512);
ALTER TABLE items ALTER COLUMN image_url TYPE VARCHAR(512);

ALTER TABLE blog_posts ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE blog_posts ALTER COLUMN video_url TYPE VARCHAR(512);

ALTER TABLE calendar_events ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE calendar_events ALTER COLUMN video_url TYPE VARCHAR(512);
ALTER TABLE calendar_events ALTER COLUMN details_url TYPE VARCHAR(512);
ALTER TABLE calendar_events ALTER COLUMN sign_up_url TYPE VARCHAR(512);

ALTER TABLE products ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE products ALTER COLUMN product_url TYPE VARCHAR(512);
ALTER TABLE product_skus ALTER COLUMN image_url TYPE VARCHAR(512);
ALTER TABLE order_payments ALTER COLUMN receipt_url TYPE VARCHAR(512);

ALTER TABLE web_pages ALTER COLUMN redirect_url TYPE VARCHAR(512);
ALTER TABLE web_pages ALTER COLUMN page_image_url TYPE VARCHAR(512);
