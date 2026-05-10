package com.interview.aigateway.impl;

import com.interview.application.port.LlmGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.llm.legacy-enabled", havingValue = "true")
public class NoopLlmGateway implements LlmGateway {

    @Override
    public String chat(Long userId, String prompt) {
        return "[stub] LLM gateway is connected. Prompt length=" + (prompt == null ? 0 : prompt.length());
    }
}
