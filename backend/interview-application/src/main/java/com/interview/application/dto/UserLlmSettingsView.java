package com.interview.application.dto;

public record UserLlmSettingsView(
        String providerName,
        String modelName,
        String baseUrl,
        boolean hasApiKey,
        boolean enabled,
        String source
) {
}
