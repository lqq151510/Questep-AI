SET @biz_type_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'biz_type'
);

SET @biz_type_ddl := IF(
    @biz_type_exists = 0,
    'ALTER TABLE async_task_records ADD COLUMN biz_type VARCHAR(64) NULL COMMENT ''business type e.g. MATERIAL_PARSE, QUIZ_GEN'' AFTER task_type',
    'SELECT 1'
);

PREPARE stmt FROM @biz_type_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
