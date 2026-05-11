CREATE TABLE IF NOT EXISTS user_llm_settings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  provider_name VARCHAR(32) NOT NULL DEFAULT 'anthropic',
  model_name VARCHAR(128) NOT NULL,
  base_url VARCHAR(512) NULL,
  api_key VARCHAR(1024) NULL,
  enabled TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_llm_settings_user_id (user_id),
  KEY idx_user_llm_settings_provider (provider_name),
  CONSTRAINT fk_user_llm_settings_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
