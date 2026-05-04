ALTER TABLE materials
    ADD COLUMN IF NOT EXISTS analysis_text LONGTEXT NULL AFTER parse_error_msg;
