CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  email VARCHAR(128) NOT NULL UNIQUE,
  phone VARCHAR(32) NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(100) NULL,
  avatar_url VARCHAR(512) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL UNIQUE,
  role_name VARCHAR(100) NOT NULL,
  description VARCHAR(255) NULL,
  is_system TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- Seed roles
INSERT INTO roles (role_code, role_name, description, is_system)
VALUES ('ADMIN', 'Administrator', 'System administrator', 1);

INSERT INTO roles (role_code, role_name, description, is_system)
VALUES ('USER', 'User', 'Default application user', 1);

-- Seed demo user (password: demo123456)
INSERT INTO users (username, email, password_hash, display_name, status)
VALUES ('demo_user', 'demo@example.com', '$2a$10$cWtOxIZDLnCzXZbQnLZAXuCGEt5FRu.1ibT1SeKWTrdCcxkzfsrIu', 'Demo User', 1);

-- Bind default USER role for demo user
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'demo_user' AND r.role_code = 'USER';
