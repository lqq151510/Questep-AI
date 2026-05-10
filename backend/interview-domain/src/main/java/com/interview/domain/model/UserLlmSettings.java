package com.interview.domain.model;

import java.time.LocalDateTime;

public record UserLlmSettings(
        Long id,
        Long userId,
        String providerName,
        String modelName,
        String baseUrl,
        String apiKey,
        Integer enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
