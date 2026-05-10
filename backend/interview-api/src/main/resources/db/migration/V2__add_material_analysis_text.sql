ALTER TABLE materials
    ADD COLUMN analysis_text LONGTEXT NULL AFTER parse_error_msg;
