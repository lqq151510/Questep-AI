package com.interview.domain.model;

import java.math.BigDecimal;
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
        String sourceUrl,
        String sourceVersion,
        LocalDateTime lastVerifiedAt,
        BigDecimal confidenceScore,
        LocalDateTime expiresAt,
        String reviewStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
