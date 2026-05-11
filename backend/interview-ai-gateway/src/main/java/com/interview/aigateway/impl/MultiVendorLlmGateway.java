package com.interview.aigateway.impl;

import com.interview.aigateway.exception.OpenAiGatewayException;
import com.interview.application.port.LlmGateway;
import com.interview.common.util.LlmProviderNormalizer;
import com.interview.domain.model.UserLlmSettings;
import com.interview.domain.repository.UserLlmSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Primary
@Component
public class MultiVendorLlmGateway implements LlmGateway {
    private static final Pattern JSON_STRING_PATTERN_TEMPLATE = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final int DEFAULT_MAX_TOKENS = 1024;

    private final RestClient.Builder restClientBuilder;
    private final UserLlmSettingsRepository userLlmSettingsRepository;
    private final String defaultProvider;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private final String systemPrompt;
    private final int maxAttempts;
    private final long retryBackoffMs;
    private final boolean enableLocalClaudeSettingsFallback;
    private final Path claudeSettingsPath;

    public MultiVendorLlmGateway(
            RestClient.Builder restClientBuilder,
            UserLlmSettingsRepository userLlmSettingsRepository,
            @Value("${app.llm.provider:anthropic}") String defaultProvider,
            @Value("${app.llm.base-url:https://api.anthropic.com}") String defaultBaseUrl,
            @Value("${app.llm.api-key:}") String defaultApiKey,
            @Value("${app.llm.model:claude-3-5-sonnet-latest}") String defaultModel,
            @Value("${app.llm.system-prompt:You generate concise interview practice material grounded in backend project experience.}") String systemPrompt,
            @Value("${app.llm.retry.max-attempts:3}") int maxAttempts,
            @Value("${app.llm.retry.backoff-ms:500}") long retryBackoffMs,
            @Value("${app.llm.enable-local-claude-settings-fallback:true}") boolean enableLocalClaudeSettingsFallback,
            @Value("${app.llm.claude-code-settings-path:${user.home}/.claude/settings.json}") String claudeSettingsPath
    ) {
        this.restClientBuilder = restClientBuilder;
        this.userLlmSettingsRepository = userLlmSettingsRepository;
        this.defaultProvider = LlmProviderNormalizer.normalize(defaultProvider);
        this.defaultBaseUrl = trimToNull(defaultBaseUrl);
        this.defaultApiKey = trimToNull(defaultApiKey);
        this.defaultModel = trimToNull(defaultModel);
        this.systemPrompt = systemPrompt;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
        this.enableLocalClaudeSettingsFallback = enableLocalClaudeSettingsFallback;
        this.claudeSettingsPath = Path.of(claudeSettingsPath);
    }

    @Override
    public String chat(Long userId, String prompt) {
        ResolvedLlmConfig config = resolveConfig(userId);
        if ("noop".equals(config.provider())) {
            return "[stub] LLM gateway is connected. Prompt length=" + (prompt == null ? 0 : prompt.length());
        }
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            throw new OpenAiGatewayException("LLM api key is not configured for provider=" + config.provider());
        }
        if (config.model() == null || config.model().isBlank()) {
            throw new OpenAiGatewayException("LLM model is not configured for provider=" + config.provider());
        }

        return switch (config.provider()) {
            case "anthropic", "claude" -> callAnthropicApi(config, prompt);
            case "deepseek", "openai", "openai-compatible", "openai_compatible", "qwen", "moonshot", "zhipu", "gemini", "custom" ->
                    callOpenAiCompatibleApi(config, prompt);
            default -> callOpenAiCompatibleApi(config, prompt);
        };
    }

    private String callOpenAiCompatibleApi(ResolvedLlmConfig config, String prompt) {
        RestClient client = restClientBuilder.baseUrl(normalizeBaseUrl(config.baseUrl(), "https://api.openai.com/v1")).build();
        OpenAiChatRequest request = new OpenAiChatRequest(
                config.model(),
                List.of(
                        new OpenAiMessage("system", systemPrompt),
                        new OpenAiMessage("user", prompt)
                ),
                0.4
        );

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                OpenAiChatResponse response = client.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(config.apiKey()))
                        .body(request)
                        .retrieve()
                        .body(OpenAiChatResponse.class);
                return extractOpenAiContent(response);
            } catch (RestClientException ex) {
                if (attempt == maxAttempts) {
                    throw new OpenAiGatewayException("OpenAI-compatible request failed after " + maxAttempts + " attempts: " + ex.getMessage(), ex);
                }
                sleepBeforeRetry();
            }
        }
        throw new OpenAiGatewayException("OpenAI-compatible request failed unexpectedly");
    }

    private String callAnthropicApi(ResolvedLlmConfig config, String prompt) {
        RestClient client = restClientBuilder.baseUrl(normalizeBaseUrl(config.baseUrl(), "https://api.anthropic.com")).build();
        AnthropicRequest request = new AnthropicRequest(
                config.model(),
                DEFAULT_MAX_TOKENS,
                systemPrompt,
                List.of(new AnthropicMessage("user", prompt))
        );
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                AnthropicResponse response = client.post()
                        .uri("/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-api-key", config.apiKey())
                        .header("anthropic-version", "2023-06-01")
                        .body(request)
                        .retrieve()
                        .body(AnthropicResponse.class);
                return extractAnthropicContent(response);
            } catch (RestClientException ex) {
                if (attempt == maxAttempts) {
                    throw new OpenAiGatewayException("Anthropic request failed after " + maxAttempts + " attempts: " + ex.getMessage(), ex);
                }
                sleepBeforeRetry();
            }
        }
        throw new OpenAiGatewayException("Anthropic request failed unexpectedly");
    }

    private ResolvedLlmConfig resolveConfig(Long userId) {
        Optional<UserLlmSettings> userSettings = Optional.ofNullable(userId)
                .flatMap(userLlmSettingsRepository::findByUserId)
                .filter(settings -> settings.enabled() == null || settings.enabled() == 1);

        String provider = LlmProviderNormalizer.normalize(userSettings.map(UserLlmSettings::providerName).orElse(defaultProvider));
        String baseUrl = trimToNull(userSettings.map(UserLlmSettings::baseUrl).orElse(defaultBaseUrl));
        String apiKey = trimToNull(userSettings.map(UserLlmSettings::apiKey).orElse(defaultApiKey));
        String model = trimToNull(userSettings.map(UserLlmSettings::modelName).orElse(defaultModel));

        if (enableLocalClaudeSettingsFallback && (apiKey == null || apiKey.isBlank())) {
            LocalClaudeSettings localSettings = readLocalClaudeSettings();
            if (localSettings != null) {
                if (apiKey == null) {
                    apiKey = localSettings.authToken();
                }
                if (baseUrl == null) {
                    baseUrl = localSettings.baseUrl();
                }
                if (model == null) {
                    model = localSettings.model();
                }
                if ("anthropic".equals(defaultProvider) && provider != null && provider.equals("anthropic")) {
                    provider = "anthropic";
                }
            }
        }
        return new ResolvedLlmConfig(provider, baseUrl, apiKey, model);
    }

    private LocalClaudeSettings readLocalClaudeSettings() {
        try {
            if (!Files.exists(claudeSettingsPath)) {
                return null;
            }
            String content = Files.readString(claudeSettingsPath);
            String token = extractJsonString(content, "ANTHROPIC_AUTH_TOKEN");
            String baseUrl = extractJsonString(content, "ANTHROPIC_BASE_URL");
            String model = extractJsonString(content, "ANTHROPIC_MODEL");
            if ((token == null || token.isBlank()) && (baseUrl == null || baseUrl.isBlank()) && (model == null || model.isBlank())) {
                return null;
            }
            return new LocalClaudeSettings(trimToNull(token), trimToNull(baseUrl), trimToNull(model));
        } catch (Exception ex) {
            return null;
        }
    }

    private String extractJsonString(String content, String key) {
        Pattern pattern = Pattern.compile(String.format(JSON_STRING_PATTERN_TEMPLATE.pattern(), Pattern.quote(key)));
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractOpenAiContent(OpenAiChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new OpenAiGatewayException("OpenAI-compatible provider returned an empty response");
        }
        OpenAiMessage message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new OpenAiGatewayException("OpenAI-compatible provider returned an empty message");
        }
        return message.content();
    }

    private String extractAnthropicContent(AnthropicResponse response) {
        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new OpenAiGatewayException("Anthropic returned an empty response");
        }
        String text = response.content().stream()
                .map(AnthropicContentBlock::text)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
        if (text.isBlank()) {
            throw new OpenAiGatewayException("Anthropic returned empty text content");
        }
        return text;
    }

    private String normalizeBaseUrl(String value, String fallback) {
        String normalized = value == null || value.isBlank() ? fallback : value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record ResolvedLlmConfig(String provider, String baseUrl, String apiKey, String model) {
    }

    private record LocalClaudeSettings(String authToken, String baseUrl, String model) {
    }

    private record OpenAiChatRequest(String model, List<OpenAiMessage> messages, double temperature) {
    }

    private record OpenAiChatResponse(List<OpenAiChoice> choices) {
    }

    private record OpenAiChoice(OpenAiMessage message) {
    }

    private record OpenAiMessage(String role, String content) {
    }

    private record AnthropicRequest(String model, int max_tokens, String system, List<AnthropicMessage> messages) {
    }

    private record AnthropicMessage(String role, String content) {
    }

    private record AnthropicResponse(List<AnthropicContentBlock> content) {
    }

    private record AnthropicContentBlock(String type, String text) {
    }
}
