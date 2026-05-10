package com.interview.aigateway.impl;

import com.interview.application.port.LlmGateway;
import com.interview.aigateway.exception.OpenAiGatewayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.llm.legacy-enabled", havingValue = "true")
public class OpenAiLlmGateway implements LlmGateway {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final int maxAttempts;
    private final long retryBackoffMs;

    public OpenAiLlmGateway(
            RestClient.Builder builder,
            @Value("${app.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.model:gpt-4o-mini}") String model,
            @Value("${app.llm.system-prompt:You generate concise interview practice material grounded in backend project experience.}") String systemPrompt,
            @Value("${app.llm.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.llm.retry.backoff-ms:500}") long retryBackoffMs
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.llm.api-key is required for provider=openai");
        }
        this.restClient = builder.baseUrl(normalizeBaseUrl(baseUrl)).build();
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    @Override
    public String chat(Long userId, String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new OpenAiGatewayException("app.llm.api-key is not configured for provider=openai");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", prompt)
                ),
                0.4
        );

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ChatCompletionResponse response = restClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(apiKey))
                        .body(request)
                        .retrieve()
                        .body(ChatCompletionResponse.class);
                return extractContent(response);
            } catch (RestClientException ex) {
                if (attempt == maxAttempts) {
                    throw new OpenAiGatewayException("OpenAI request failed after " + maxAttempts + " attempts: " + ex.getMessage(), ex);
                }
                sleepBeforeRetry();
            }
        }
        throw new OpenAiGatewayException("OpenAI request failed unexpectedly");
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new OpenAiGatewayException("OpenAI returned an empty response");
        }
        ChatMessage message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new OpenAiGatewayException("OpenAI returned an empty message");
        }
        return message.content();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private void sleepBeforeRetry() {
        if (retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OpenAiGatewayException("Retry interrupted", ex);
        }
    }

    private record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }

    private record ChatMessage(String role, String content) {
    }
}
