SET @created_by_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'created_by'
);

SET @created_by_ddl := IF(
    @created_by_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN created_by BIGINT UNSIGNED NULL AFTER status',
    'SELECT 1'
);

PREPARE stmt FROM @created_by_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @error_msg_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'error_msg'
);

SET @error_msg_ddl := IF(
    @error_msg_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN error_msg VARCHAR(1000) NULL AFTER error_code',
    'SELECT 1'
);

PREPARE stmt FROM @error_msg_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
