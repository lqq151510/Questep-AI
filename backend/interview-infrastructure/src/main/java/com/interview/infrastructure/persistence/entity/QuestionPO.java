package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public record QuestionPO(
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
    Integer status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
