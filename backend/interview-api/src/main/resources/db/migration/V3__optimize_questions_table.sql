-- V3: 优化 questions 表
-- 1. 为 stem_text 添加全文索引，支持全文搜索
-- 2. 为 options_json 中的高频查询字段添加生成列和索引（如果未来需要）

-- 添加全文索引
ALTER TABLE questions ADD FULLTEXT INDEX ft_idx_questions_stem_text (stem_text);

-- 为未来可能的高频查询预留生成列（目前代码中还没有直接查询 JSON 字段）
-- 例如：如果需要频繁查询 options 数量，可以添加如下生成列
-- ALTER TABLE questions 
--     ADD COLUMN IF NOT EXISTS option_count INT UNSIGNED AS (JSON_LENGTH(options_json)) STORED,
--     ADD INDEX idx_questions_option_count (option_count);
