CREATE TABLE prompt_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_key VARCHAR(64) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    name VARCHAR(128),
    system_prompt TEXT,
    user_template TEXT NOT NULL,
    variables JSON,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_key_version (template_key, version)
);

INSERT INTO prompt_templates (template_key, version, name, user_template, variables, status) VALUES
('chat_default', 1, 'Chat Default v1',
 '<retrieved_context>\n{{#retrievedContexts}}\n  <chunk index="{{index}}">{{text}}</chunk>\n{{/retrievedContexts}}\n</retrieved_context>\n\n{{#hasHistory}}\n<conversation_history>\n{{#history}}\n  <{{role}}>\n    {{content}}\n  </{{role}}>\n{{/history}}\n</conversation_history>\n\n{{/hasHistory}}\n<user_query>\n{{message}}\n</user_query>',
 '["retrievedContexts","hasHistory","history","message"]', 'ACTIVE');

INSERT INTO prompt_templates (template_key, version, name, user_template, variables, status) VALUES
('quiz_generation', 1, 'Quiz Generation v1',
 '请用中文生成 {{count}} 道 {{questionType}} 题。材料={{materialNames}}，难度={{difficulty}}，interviewMode={{interviewMode}}。{{questionGuidance}} 仅返回严格 JSON，不要使用 Markdown，不要输出额外字段。JSON 结构必须为：{"summary":"string","questions":[{"stem":"string","optionsJson":{"A":"string","B":"string","C":"string","D":"string"},"referenceAnswer":"string","analysis":"string"}]}。如果题型是 SINGLE_CHOICE，每道题必须提供 4 个互斥选项，且 optionsJson 中 A-D 不能缺失。',
 '["count","questionType","materialNames","difficulty","interviewMode","questionGuidance"]', 'ACTIVE');
