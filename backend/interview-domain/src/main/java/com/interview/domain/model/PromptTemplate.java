package com.interview.domain.model;

import java.time.LocalDateTime;

public record PromptTemplate(
        Long id,
        String templateKey,
        Integer version,
        String name,
        String systemPrompt,
        String userTemplate,
        String variables,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
