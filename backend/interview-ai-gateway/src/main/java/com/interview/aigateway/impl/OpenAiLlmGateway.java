package com.interview.aigateway.impl;

import com.interview.aigateway.client.LlmGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai")
public class OpenAiLlmGateway implements LlmGateway {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiLlmGateway(
            RestClient.Builder builder,
            @Value("${app.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.model:gpt-4o-mini}") String model
    ) {
        this.restClient = builder.baseUrl(normalizeBaseUrl(baseUrl)).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String chat(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[openai disabled] app.llm.api-key is not configured";
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new ChatMessage("system", "You generate concise interview practice material."),
                        new ChatMessage("user", prompt)
                ),
                0.4
        );

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
            return "[openai error] " + ex.getMessage();
        }
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return "[openai empty response]";
        }
        ChatMessage message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            return "[openai empty response]";
        }
        return message.content();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
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
