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
 'Generate {{count}} {{questionType}} questions. Materials={{materialNames}}, difficulty={{difficulty}}, interviewMode={{interviewMode}}. {{questionGuidance}} Return ONLY strict JSON with schema: {"summary":"string","questions":[{"stem":"string","referenceAnswer":"string","analysis":"string"}]}. Do not use markdown and do not output any extra keys.',
 '["count","questionType","materialNames","difficulty","interviewMode","questionGuidance"]', 'ACTIVE');
