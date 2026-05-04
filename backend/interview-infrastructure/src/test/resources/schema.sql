CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  email VARCHAR(128) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(128),
  status TINYINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE materials (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  material_name VARCHAR(255) NOT NULL,
  material_type VARCHAR(64) NOT NULL,
  source_type VARCHAR(32),
  storage_url VARCHAR(512),
  content_hash VARCHAR(128),
  parse_status VARCHAR(32),
  parse_error_msg VARCHAR(500),
  analysis_text CLOB,
  parsed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE async_task_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_no VARCHAR(64) NOT NULL UNIQUE,
  task_type VARCHAR(64) NOT NULL,
  biz_id BIGINT,
  status VARCHAR(32) NOT NULL,
  progress INT,
  error_msg VARCHAR(500),
  created_by BIGINT,
  started_at TIMESTAMP NULL,
  finished_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  material_id BIGINT,
  creator_user_id BIGINT,
  question_type VARCHAR(32) NOT NULL,
  stem_text CLOB NOT NULL,
  reference_answer CLOB,
  analysis_text CLOB,
  difficulty INT,
  source_type VARCHAR(32),
  model_name VARCHAR(64),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
