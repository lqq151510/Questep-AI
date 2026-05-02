package com.interview.domain.model;

import java.time.LocalDateTime;

public record Question(
        Long id,
        Long materialId,
        Long creatorUserId,
        String questionType,
        String stemText,
        String referenceAnswer,
        String analysisText,
        Integer difficulty,
        String sourceType,
        String modelName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
