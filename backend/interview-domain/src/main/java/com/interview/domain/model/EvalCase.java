package com.interview.domain.model;

import java.time.LocalDateTime;

public record EvalCase(
        Long id,
        String caseKey,
        String category,
        String description,
        String input,
        String expectedKeywords,
        String expectedStructure,
        Double minScore,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
