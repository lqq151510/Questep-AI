SET @source_url_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'source_url'
);

SET @source_url_ddl := IF(
    @source_url_exists = 0,
    'ALTER TABLE questions ADD COLUMN source_url VARCHAR(512) NULL COMMENT ''evidence source URL or internal material URI'' AFTER model_name',
    'SELECT 1'
);

PREPARE stmt FROM @source_url_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @source_version_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'source_version'
);

SET @source_version_ddl := IF(
    @source_version_exists = 0,
    'ALTER TABLE questions ADD COLUMN source_version VARCHAR(64) NULL COMMENT ''evidence/version marker'' AFTER source_url',
    'SELECT 1'
);

PREPARE stmt FROM @source_version_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @last_verified_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'last_verified_at'
);

SET @last_verified_ddl := IF(
    @last_verified_exists = 0,
    'ALTER TABLE questions ADD COLUMN last_verified_at DATETIME NULL COMMENT ''last verification timestamp'' AFTER source_version',
    'SELECT 1'
);

PREPARE stmt FROM @last_verified_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @confidence_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'confidence_score'
);

SET @confidence_ddl := IF(
    @confidence_exists = 0,
    'ALTER TABLE questions ADD COLUMN confidence_score DECIMAL(4,3) NOT NULL DEFAULT 0.800 COMMENT ''0.000-1.000 quality confidence'' AFTER last_verified_at',
    'SELECT 1'
);

PREPARE stmt FROM @confidence_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @expires_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'expires_at'
);

SET @expires_ddl := IF(
    @expires_exists = 0,
    'ALTER TABLE questions ADD COLUMN expires_at DATETIME NULL COMMENT ''question review expiration time'' AFTER confidence_score',
    'SELECT 1'
);

PREPARE stmt FROM @expires_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @review_status_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND COLUMN_NAME = 'review_status'
);

SET @review_status_ddl := IF(
    @review_status_exists = 0,
    'ALTER TABLE questions ADD COLUMN review_status VARCHAR(32) NOT NULL DEFAULT ''APPROVED'' COMMENT ''APPROVED/PENDING_REVIEW'' AFTER expires_at',
    'SELECT 1'
);

PREPARE stmt FROM @review_status_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_review_expire_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'questions'
      AND INDEX_NAME = 'idx_questions_review_expire'
);

SET @idx_review_expire_ddl := IF(
    @idx_review_expire_exists = 0,
    'CREATE INDEX idx_questions_review_expire ON questions (review_status, expires_at)',
    'SELECT 1'
);

PREPARE stmt FROM @idx_review_expire_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE questions
SET source_url = IF(source_url IS NULL AND material_id IS NOT NULL, CONCAT('material://', material_id), source_url),
    source_version = IFNULL(source_version, 'material-v1'),
    last_verified_at = IFNULL(last_verified_at, created_at),
    confidence_score = IFNULL(confidence_score, 0.800),
    expires_at = IFNULL(expires_at, DATE_ADD(created_at, INTERVAL 30 DAY)),
    review_status = IFNULL(review_status, 'APPROVED')
WHERE source_url IS NULL
   OR source_version IS NULL
   OR last_verified_at IS NULL
   OR expires_at IS NULL
   OR review_status IS NULL;
