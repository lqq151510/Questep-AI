SET @error_code_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'error_code'
);

SET @error_code_ddl := IF(
    @error_code_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN error_code VARCHAR(50) NULL COMMENT ''machine-readable error code e.g. LLM_TIMEOUT, FILE_NOT_FOUND'' AFTER error_msg',
    'SELECT 1'
);

PREPARE stmt FROM @error_code_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @stage_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'stage'
);

SET @stage_ddl := IF(
    @stage_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN stage VARCHAR(50) NULL COMMENT ''which stage failed: UPLOAD, PARSE, LLM, STORAGE'' AFTER error_code',
    'SELECT 1'
);

PREPARE stmt FROM @stage_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @retryable_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'retryable'
);

SET @retryable_ddl := IF(
    @retryable_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN retryable TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''whether this task can be retried'' AFTER stage',
    'SELECT 1'
);

PREPARE stmt FROM @retryable_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
