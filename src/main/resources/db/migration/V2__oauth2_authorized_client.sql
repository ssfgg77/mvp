-- Spring Security OAuth2 Authorized Client schema
-- Compatible with JdbcOAuth2AuthorizedClientService primary key (client_registration_id, principal_name).

CREATE TABLE oauth2_authorized_client (
  client_registration_id VARCHAR(100) NOT NULL,
  principal_name VARCHAR(200) NOT NULL,
  access_token_type VARCHAR(100) NOT NULL,
  access_token_value BLOB NOT NULL,
  access_token_issued_at TIMESTAMP(6) NOT NULL,
  access_token_expires_at TIMESTAMP(6) NOT NULL,
  access_token_scopes VARCHAR(1000) DEFAULT NULL,
  refresh_token_value BLOB DEFAULT NULL,
  refresh_token_issued_at TIMESTAMP(6) DEFAULT NULL,
  created_at TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (client_registration_id, principal_name)
);
