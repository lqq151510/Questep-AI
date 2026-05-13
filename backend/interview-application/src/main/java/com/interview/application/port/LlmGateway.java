package com.interview.application.port;

import java.util.function.Consumer;

public interface LlmGateway {

    String chat(Long userId, String prompt);

    default String chat(String prompt) {
        return chat(null, prompt);
    }

    default void chatStream(Long userId, String prompt, Consumer<String> tokenConsumer) {
        String result = chat(userId, prompt);
        if (result != null && !result.isEmpty()) {
            tokenConsumer.accept(result);
        }
    }
}
