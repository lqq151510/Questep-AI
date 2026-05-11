package com.interview.application.service;

import com.interview.application.dto.UpdateUserLlmSettingsCommand;
import com.interview.application.dto.UserLlmSettingsView;
import com.interview.domain.model.UserLlmSettings;
import com.interview.domain.repository.UserLlmSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserLlmSettingsApplicationService {

    private final UserLlmSettingsRepository repository;
    private final String defaultProvider;
    private final String defaultModel;
    private final String defaultBaseUrl;
    private final String defaultApiKey;

    public UserLlmSettingsApplicationService(
            UserLlmSettingsRepository repository,
            @Value("${app.llm.provider:anthropic}") String defaultProvider,
            @Value("${app.llm.model:claude-3-5-sonnet-latest}") String defaultModel,
            @Value("${app.llm.base-url:https://api.anthropic.com}") String defaultBaseUrl,
            @Value("${app.llm.api-key:}") String defaultApiKey
    ) {
        this.repository = repository;
        this.defaultProvider = defaultProvider;
        this.defaultModel = defaultModel;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultApiKey = defaultApiKey;
    }

    public UserLlmSettingsView get(Long userId) {
        return repository.findByUserId(userId)
                .filter(setting -> setting.enabled() == null || setting.enabled() == 1)
                .map(this::toView)
                .orElseGet(this::defaultView);
    }

    public UserLlmSettingsView update(Long userId, UpdateUserLlmSettingsCommand command) {
        Optional<UserLlmSettings> existing = repository.findByUserId(userId);
        String apiKey = resolveApiKey(command.apiKey(), existing.map(UserLlmSettings::apiKey).orElse(null));
        String providerName = normalizeProvider(command.providerName());
        UserLlmSettings saved = repository.saveOrUpdate(
                userId,
                providerName,
                command.modelName().trim(),
                normalizeBaseUrl(providerName, command.baseUrl()),
                apiKey,
                Boolean.TRUE.equals(command.enabled()) ? 1 : 0
        );
        return toView(saved);
    }

    private UserLlmSettingsView toView(UserLlmSettings settings) {
        return new UserLlmSettingsView(
                settings.providerName(),
                settings.modelName(),
                settings.baseUrl(),
                settings.apiKey() != null && !settings.apiKey().isBlank(),
                settings.enabled() == null || settings.enabled() == 1,
                "user"
        );
    }

    private UserLlmSettingsView defaultView() {
        return new UserLlmSettingsView(
                defaultProvider,
                defaultModel,
                defaultBaseUrl,
                defaultApiKey != null && !defaultApiKey.isBlank(),
                true,
                "default"
        );
    }

    private String resolveApiKey(String incoming, String existing) {
        String normalized = trimToNull(incoming);
        if (normalized != null) {
            return normalized;
        }
        return trimToNull(existing);
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "openai";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "openai-compatible", "openai_compatible", "openai-format", "openai_format", "compatible" -> "openai-compatible";
            case "anthropic", "claude" -> "anthropic";
            default -> normalized;
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeBaseUrl(String providerName, String baseUrl) {
        String normalized = trimToNull(baseUrl);
        if (normalized != null && normalized.matches("(?i)^https://api\\.deepseek\\.co(/.*)?$")) {
            if (!"deepseek".equals(providerName)) {
                return normalized.replaceFirst("(?i)^https://api\\.deepseek\\.co", "https://api.deepseek.com");
            }
            return "https://api.deepseek.com";
        }
        return normalized;
    }
}
