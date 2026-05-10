SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'progress'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN progress INT UNSIGNED NOT NULL DEFAULT 0 AFTER status',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
