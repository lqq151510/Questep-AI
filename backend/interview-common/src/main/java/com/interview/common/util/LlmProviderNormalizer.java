package com.interview.common.util;

import java.util.Locale;

public final class LlmProviderNormalizer {

    private LlmProviderNormalizer() {
    }

    public static String normalize(String provider) {
        if (provider == null || provider.isBlank()) {
            return "openai";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "openai-compatible", "openai_compatible", "openai-format", "openai_format", "compatible" -> "openai-compatible";
            case "anthropic", "claude" -> "anthropic";
            default -> normalized;
        };
    }
}
