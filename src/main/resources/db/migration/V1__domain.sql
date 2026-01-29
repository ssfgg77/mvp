-- Domain schema (MVP)
CREATE TABLE app_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_app_user_username ON app_user(username);
CREATE UNIQUE INDEX uk_app_user_email ON app_user(email);

CREATE TABLE app_user_role (
  user_id BIGINT NOT NULL,
  role VARCHAR(32) NOT NULL,
  PRIMARY KEY (user_id, role),
  CONSTRAINT fk_app_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE schwab_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  schwab_account_ref VARCHAR(128) NOT NULL,
  account_type VARCHAR(64),
  nickname VARCHAR(128),
  cash_balance DECIMAL(19,4),
  equity_value DECIMAL(19,4),
  last_refreshed_at TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_schwab_account_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_schwab_account_user_ref ON schwab_account(user_id, schwab_account_ref);

CREATE TABLE user_settings (
  user_id BIGINT NOT NULL,
  default_schwab_account_id BIGINT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_user_settings_default_account FOREIGN KEY (default_schwab_account_id) REFERENCES schwab_account(id)
);

CREATE TABLE trade_order (
  id BINARY(16) NOT NULL,
  user_id BIGINT NOT NULL,
  schwab_account_id BIGINT NOT NULL,
  client_order_id VARCHAR(64) NOT NULL,
  schwab_order_id VARCHAR(128),
  side VARCHAR(16) NOT NULL,
  order_type VARCHAR(16) NOT NULL,
  symbol VARCHAR(32) NOT NULL,
  quantity INT NOT NULL,
  limit_price DECIMAL(19,4),
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_trade_order_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
  CONSTRAINT fk_trade_order_account FOREIGN KEY (schwab_account_id) REFERENCES schwab_account(id)
);

CREATE INDEX ix_trade_order_user_status_created ON trade_order(user_id, status, created_at);

CREATE TABLE execution_fill (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BINARY(16) NOT NULL,
  external_exec_id VARCHAR(128),
  price DECIMAL(19,4) NOT NULL,
  quantity INT NOT NULL,
  exec_time TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_execution_fill_order FOREIGN KEY (order_id) REFERENCES trade_order(id) ON DELETE CASCADE
);

CREATE INDEX ix_execution_fill_order_time ON execution_fill(order_id, exec_time);

CREATE TABLE audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64),
  entity_id VARCHAR(64),
  detail VARCHAR(1024),
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE INDEX ix_audit_user_time ON audit_log(user_id, created_at);

CREATE TABLE idempotency_key (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  key_hash VARCHAR(64) NOT NULL,
  request_fingerprint VARCHAR(128) NOT NULL,
  order_id BINARY(16) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  CONSTRAINT fk_idem_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_idem_user_keyhash ON idempotency_key(user_id, key_hash);
