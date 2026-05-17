SET @status_column_type := (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'async_task_records'
      AND COLUMN_NAME = 'status'
);

SET @needs_align := IF(
    @status_column_type IS NULL,
    0,
    IF(
        @status_column_type LIKE 'enum(%'
        AND (
            @status_column_type NOT LIKE '%''PENDING''%'
            OR @status_column_type NOT LIKE '%''RUNNING''%'
            OR @status_column_type NOT LIKE '%''PROCESSING''%'
            OR @status_column_type NOT LIKE '%''SUCCESS''%'
            OR @status_column_type NOT LIKE '%''FAILED''%'
            OR @status_column_type NOT LIKE '%''CANCELED''%'
        ),
        1,
        0
    )
);

SET @align_status_enum_ddl := IF(
    @needs_align = 1,
    'ALTER TABLE async_task_records MODIFY COLUMN status ENUM(''PENDING'',''RUNNING'',''PROCESSING'',''SUCCESS'',''FAILED'',''CANCELED'') NOT NULL DEFAULT ''PENDING''',
    'SELECT 1'
);

PREPARE stmt FROM @align_status_enum_ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
