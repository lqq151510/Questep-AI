CREATE TABLE eval_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_key VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(32) NOT NULL,
    description VARCHAR(256),
    input JSON NOT NULL,
    expected_keywords JSON,
    expected_structure JSON,
    min_score DOUBLE DEFAULT 0.6,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE eval_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_key VARCHAR(64) NOT NULL UNIQUE,
    prompt_version_id BIGINT,
    total_cases INT DEFAULT 0,
    passed_cases INT DEFAULT 0,
    avg_score DOUBLE DEFAULT 0.0,
    status VARCHAR(16) DEFAULT 'PENDING',
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE eval_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    case_id BIGINT NOT NULL,
    actual_output TEXT,
    score DOUBLE DEFAULT 0.0,
    keyword_hits JSON,
    structure_valid TINYINT,
    duration_ms BIGINT,
    error_msg VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eval_results_run_id (run_id)
);

INSERT INTO eval_cases (case_key, category, description, input, expected_keywords, min_score, status) VALUES
('chat_knowledge_response', 'chat', '知识库对话应包含相关知识点', '{"message":"什么是Spring Boot自动配置","context":[]}', '["Spring","Boot","自动配置","AutoConfiguration","条件注解"]', 0.4, 'ACTIVE'),
('chat_empty_context', 'chat', '无上下文时应给出合理回复', '{"message":"你好，能帮我准备面试吗","context":[]}', '["面试","帮助","准备","问题"]', 0.25, 'ACTIVE'),
('quiz_gen_java_backend', 'quiz', 'Java后端题目生成应包含关键技术词', '{"questionType":"INTERVIEW","difficulty":3,"count":3,"interviewMode":false,"materialNames":["Java核心","Spring Boot实战"]}', '["Java","Spring","后端","面试"]', 0.3, 'ACTIVE'),
('quiz_json_format', 'quiz', '生成的JSON应符合schema', '{"questionType":"SHORT_ANSWER","difficulty":2,"count":2,"interviewMode":false,"materialNames":["数据库原理"]}', '["summary","questions","stem","referenceAnswer"]', 0.5, 'ACTIVE'),
('chat_code_question', 'chat', '代码问题应提及具体技术', '{"message":"如何处理Java内存泄漏","context":[]}', '["内存","GC","引用","泄漏"]', 0.3, 'ACTIVE');
