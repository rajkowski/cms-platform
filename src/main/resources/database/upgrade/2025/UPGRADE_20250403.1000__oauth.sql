CREATE TABLE oauth_state_values (
  state_id BIGSERIAL PRIMARY KEY,
  state VARCHAR(50) NOT NULL,
  resource VARCHAR(512) NOT NULL,
  created TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX oauth_state_sta_idx ON oauth_state_values(state);
CREATE INDEX oauth_state_cre_idx ON oauth_state_values(created);
