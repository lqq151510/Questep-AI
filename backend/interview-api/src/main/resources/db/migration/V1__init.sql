-- AI Interview Platform - MySQL 8 Initialization Script
-- Charset/Collation: utf8mb4 / utf8mb4_0900_ai_ci

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS interview_ai
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE interview_ai;

-- 1) users
CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  email VARCHAR(128) NOT NULL,
  phone VARCHAR(32) NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NULL,
  avatar_url VARCHAR(512) NULL,
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=active,0=disabled',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_phone (phone),
  KEY idx_users_status (status),
  KEY idx_users_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2) roles
CREATE TABLE IF NOT EXISTS roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  is_system TINYINT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_role_code (role_code),
  KEY idx_roles_is_system (is_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3) user_roles
CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_roles_user_role (user_id, role_id),
  KEY idx_user_roles_role_id (role_id),
  CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_user_roles_role_id FOREIGN KEY (role_id) REFERENCES roles (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4) materials
CREATE TABLE IF NOT EXISTS materials (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  material_name VARCHAR(255) NOT NULL,
  material_type VARCHAR(32) NOT NULL COMMENT 'PDF/WORD/MD/IMAGE/TEXT',
  source_type VARCHAR(32) NOT NULL DEFAULT 'UPLOAD' COMMENT 'UPLOAD/URL/IMPORT',
  storage_url VARCHAR(1024) NULL,
  content_hash CHAR(64) NULL,
  parse_status ENUM('PENDING','PROCESSING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  parse_error_msg VARCHAR(500) NULL,
  parsed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_materials_user_content_hash (user_id, content_hash),
  KEY idx_materials_user_status (user_id, parse_status),
  KEY idx_materials_created_at (created_at),
  CONSTRAINT fk_materials_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 5) material_chunks
CREATE TABLE IF NOT EXISTS material_chunks (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_id BIGINT UNSIGNED NOT NULL,
  chunk_no INT UNSIGNED NOT NULL,
  chunk_text LONGTEXT NOT NULL,
  token_count INT UNSIGNED NULL,
  embedding_model VARCHAR(128) NULL,
  vector_id VARCHAR(128) NULL COMMENT 'Milvus vector primary id',
  metadata_json JSON NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_chunks_material_chunk_no (material_id, chunk_no),
  KEY idx_material_chunks_vector_id (vector_id),
  KEY idx_material_chunks_created_at (created_at),
  CONSTRAINT fk_material_chunks_material_id FOREIGN KEY (material_id) REFERENCES materials (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 6) questions
CREATE TABLE IF NOT EXISTS questions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  material_id BIGINT UNSIGNED NULL,
  creator_user_id BIGINT UNSIGNED NULL,
  question_type ENUM('SINGLE_CHOICE','MULTIPLE_CHOICE','SHORT_ANSWER','CODING','INTERVIEW') NOT NULL,
  stem_text TEXT NOT NULL,
  options_json JSON NULL,
  reference_answer TEXT NULL,
  analysis_text TEXT NULL,
  difficulty TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '1-5',
  source_type ENUM('AI','MANUAL','IMPORT') NOT NULL DEFAULT 'AI',
  model_name VARCHAR(128) NULL,
  status TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_questions_material_id (material_id),
  KEY idx_questions_creator_user_id (creator_user_id),
  KEY idx_questions_type_difficulty (question_type, difficulty),
  KEY idx_questions_created_at (created_at),
  CONSTRAINT fk_questions_material_id FOREIGN KEY (material_id) REFERENCES materials (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_questions_creator_user_id FOREIGN KEY (creator_user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 7) quiz_records
CREATE TABLE IF NOT EXISTS quiz_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  material_id BIGINT UNSIGNED NULL,
  quiz_batch_no VARCHAR(64) NULL COMMENT 'group one quiz submission set',
  user_answer_json JSON NULL,
  is_correct TINYINT(1) NULL,
  score DECIMAL(6,2) NOT NULL DEFAULT 0.00,
  max_score DECIMAL(6,2) NOT NULL DEFAULT 100.00,
  grader_model VARCHAR(128) NULL,
  grade_detail_json JSON NULL,
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_quiz_records_user_submitted (user_id, submitted_at),
  KEY idx_quiz_records_question_id (question_id),
  KEY idx_quiz_records_batch_no (quiz_batch_no),
  KEY idx_quiz_records_material_id (material_id),
  CONSTRAINT fk_quiz_records_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_quiz_records_question_id FOREIGN KEY (question_id) REFERENCES questions (id)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_quiz_records_material_id FOREIGN KEY (material_id) REFERENCES materials (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 8) wrong_book
CREATE TABLE IF NOT EXISTS wrong_book (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  question_id BIGINT UNSIGNED NOT NULL,
  first_wrong_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_wrong_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  wrong_count INT UNSIGNED NOT NULL DEFAULT 1,
  mastery_status ENUM('UNMASTERED','IN_PROGRESS','MASTERED') NOT NULL DEFAULT 'UNMASTERED',
  last_review_at DATETIME NULL,
  notes VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wrong_book_user_question (user_id, question_id),
  KEY idx_wrong_book_user_mastery (user_id, mastery_status),
  KEY idx_wrong_book_last_wrong_at (last_wrong_at),
  CONSTRAINT fk_wrong_book_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_wrong_book_question_id FOREIGN KEY (question_id) REFERENCES questions (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9) knowledge_points
CREATE TABLE IF NOT EXISTS knowledge_points (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  kp_code VARCHAR(64) NULL,
  kp_name VARCHAR(128) NOT NULL,
  parent_id BIGINT UNSIGNED NULL,
  kp_level TINYINT UNSIGNED NOT NULL DEFAULT 1,
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_points_kp_code (kp_code),
  UNIQUE KEY uk_knowledge_points_name_parent (kp_name, parent_id),
  KEY idx_knowledge_points_parent_id (parent_id),
  CONSTRAINT fk_knowledge_points_parent_id FOREIGN KEY (parent_id) REFERENCES knowledge_points (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 10) question_kp_rel
CREATE TABLE IF NOT EXISTS question_kp_rel (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  question_id BIGINT UNSIGNED NOT NULL,
  knowledge_point_id BIGINT UNSIGNED NOT NULL,
  weight DECIMAL(6,2) NOT NULL DEFAULT 1.00,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_question_kp_rel_question_kp (question_id, knowledge_point_id),
  KEY idx_question_kp_rel_knowledge_point_id (knowledge_point_id),
  CONSTRAINT fk_question_kp_rel_question_id FOREIGN KEY (question_id) REFERENCES questions (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_question_kp_rel_knowledge_point_id FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_points (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 11) interview_sessions
CREATE TABLE IF NOT EXISTS interview_sessions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  session_no VARCHAR(64) NOT NULL,
  position_name VARCHAR(128) NULL,
  tech_stack VARCHAR(255) NULL,
  difficulty TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '1-5',
  status ENUM('CREATED','IN_PROGRESS','FINISHED','ABORTED') NOT NULL DEFAULT 'CREATED',
  started_at DATETIME NULL,
  ended_at DATETIME NULL,
  summary_text TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_interview_sessions_session_no (session_no),
  KEY idx_interview_sessions_user_status (user_id, status),
  KEY idx_interview_sessions_created_at (created_at),
  CONSTRAINT fk_interview_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 12) interview_messages
CREATE TABLE IF NOT EXISTS interview_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  role_type ENUM('SYSTEM','INTERVIEWER','CANDIDATE','TOOL') NOT NULL,
  message_text LONGTEXT NOT NULL,
  message_order INT UNSIGNED NOT NULL,
  token_count INT UNSIGNED NULL,
  audio_url VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_interview_messages_session_order (session_id, message_order),
  KEY idx_interview_messages_role_type (role_type),
  KEY idx_interview_messages_created_at (created_at),
  CONSTRAINT fk_interview_messages_session_id FOREIGN KEY (session_id) REFERENCES interview_sessions (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 13) learning_reports
CREATE TABLE IF NOT EXISTS learning_reports (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  report_type ENUM('DAILY','WEEKLY','MONTHLY','SESSION','CUSTOM') NOT NULL,
  report_date DATE NOT NULL,
  related_session_id BIGINT UNSIGNED NULL,
  report_data_json JSON NOT NULL,
  report_summary TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_reports_user_type_date_session (user_id, report_type, report_date, related_session_id),
  KEY idx_learning_reports_user_date (user_id, report_date),
  CONSTRAINT fk_learning_reports_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT fk_learning_reports_related_session_id FOREIGN KEY (related_session_id) REFERENCES interview_sessions (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 14) async_task_records
CREATE TABLE IF NOT EXISTS async_task_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  task_type VARCHAR(64) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  status ENUM('PENDING','PROCESSING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
  progress INT UNSIGNED NOT NULL DEFAULT 0,
  error_msg VARCHAR(500) NULL,
  created_by BIGINT UNSIGNED NOT NULL,
  started_at DATETIME NULL,
  finished_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_async_task_records_task_no (task_no),
  KEY idx_async_task_records_type_biz (task_type, biz_id),
  KEY idx_async_task_records_status_created_at (status, created_at),
  CONSTRAINT fk_async_task_records_created_by FOREIGN KEY (created_by) REFERENCES users (id)
    ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 15) llm_call_logs
CREATE TABLE IF NOT EXISTS llm_call_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  trace_id VARCHAR(64) NOT NULL,
  task_id BIGINT UNSIGNED NULL,
  user_id BIGINT UNSIGNED NULL,
  session_id BIGINT UNSIGNED NULL COMMENT 'interview_sessions.id',
  biz_type VARCHAR(64) NOT NULL COMMENT 'QUIZ_GEN/QUIZ_GRADE/INTERVIEW/REPORT/...',
  biz_id BIGINT UNSIGNED NULL,
  provider_name VARCHAR(64) NOT NULL COMMENT 'OPENAI/DEEPSEEK/QWEN_VL/...',
  model_name VARCHAR(128) NOT NULL,
  request_tokens INT UNSIGNED NOT NULL DEFAULT 0,
  response_tokens INT UNSIGNED NOT NULL DEFAULT 0,
  total_tokens INT UNSIGNED AS (request_tokens + response_tokens) STORED,
  latency_ms INT UNSIGNED NOT NULL DEFAULT 0,
  retry_count INT UNSIGNED NOT NULL DEFAULT 0,
  is_success TINYINT(1) NOT NULL DEFAULT 1,
  error_code VARCHAR(64) NULL,
  error_message VARCHAR(1000) NULL,
  request_payload_json JSON NULL,
  response_payload_json JSON NULL,
  called_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_llm_call_logs_trace_id (trace_id),
  KEY idx_llm_call_logs_user_called_at (user_id, called_at),
  KEY idx_llm_call_logs_model_called_at (model_name, called_at),
  KEY idx_llm_call_logs_biz (biz_type, biz_id),
  KEY idx_llm_call_logs_task_id (task_id),
  KEY idx_llm_call_logs_session_id (session_id),
  CONSTRAINT fk_llm_call_logs_task_id FOREIGN KEY (task_id) REFERENCES async_task_records (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_llm_call_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT fk_llm_call_logs_session_id FOREIGN KEY (session_id) REFERENCES interview_sessions (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 16) idempotency_records
CREATE TABLE IF NOT EXISTS idempotency_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  idempotency_key VARCHAR(128) NOT NULL,
  request_path VARCHAR(255) NOT NULL,
  request_method VARCHAR(16) NOT NULL,
  user_id BIGINT UNSIGNED NULL,
  request_hash CHAR(64) NULL,
  status ENUM('PROCESSING','SUCCESS','FAILED') NOT NULL DEFAULT 'PROCESSING',
  response_code INT NULL,
  response_body_json JSON NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_idempotency_records_key (idempotency_key),
  KEY idx_idempotency_records_user_id (user_id),
  KEY idx_idempotency_records_expires_at (expires_at),
  KEY idx_idempotency_records_path_method (request_path, request_method),
  CONSTRAINT fk_idempotency_records_user_id FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed roles
INSERT INTO roles (role_code, role_name, description, is_system)
VALUES
  ('ADMIN', 'Administrator', 'System administrator', 1),
  ('USER', 'User', 'Default application user', 1)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  description = VALUES(description),
  is_system = VALUES(is_system),
  updated_at = CURRENT_TIMESTAMP;

-- Seed demo user (for local API testing)
INSERT INTO users (username, email, password_hash, display_name, status)
VALUES ('demo_user', 'demo@example.com', '$2a$10$cWtOxIZDLnCzXZbQnLZAXuCGEt5FRu.1ibT1SeKWTrdCcxkzfsrIu', 'Demo User', 1)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash),
  display_name = VALUES(display_name),
  status = VALUES(status),
  updated_at = CURRENT_TIMESTAMP;

-- Bind default USER role for demo user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.role_code = 'USER'
WHERE u.username = 'demo_user'
ON DUPLICATE KEY UPDATE
  updated_at = CURRENT_TIMESTAMP;

SET FOREIGN_KEY_CHECKS = 1;
