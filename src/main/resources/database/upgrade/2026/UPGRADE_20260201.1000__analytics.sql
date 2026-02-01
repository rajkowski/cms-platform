-- Copyright 2026 Matt Rajkowski, Licensed under the Apache License, Version 2.0

-- CREATE INDEX IF NOT EXISTS web_page_hits_session_bot_idx ON web_page_hits(session_id, is_bot);
CREATE INDEX IF NOT EXISTS web_page_hits_page_path_idx ON web_page_hits(page_path);
CREATE INDEX IF NOT EXISTS web_page_hits_composite_analytics_idx ON web_page_hits(hit_date, page_path, session_id) INCLUDE (ip_address, method);
CREATE INDEX IF NOT EXISTS web_page_hits_hit_date_method_idx ON web_page_hits(hit_date, method);

CREATE INDEX IF NOT EXISTS sessions_bot_created_idx ON sessions(is_bot, created);
CREATE INDEX IF NOT EXISTS sessions_referer_created_idx ON sessions(referer, created);

CREATE INDEX IF NOT EXISTS user_logins_user_id_idx ON user_logins(user_id);
CREATE INDEX IF NOT EXISTS user_logins_user_id_created_idx ON user_logins(user_id, created DESC);
