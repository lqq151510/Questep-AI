package com.interview.application.port;

public interface LlmGateway {

    String chat(Long userId, String prompt);

    default String chat(String prompt) {
        return chat(null, prompt);
    }
}
