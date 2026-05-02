package com.interview.infrastructure.persistence.entity;

import java.time.LocalDateTime;

public record MaterialPO(
    Long id,
    Long userId,
    String materialName,
    String materialType,
    String sourceType,
    String storageUrl,
    String contentHash,
    String parseStatus,
    String parseErrorMsg,
    LocalDateTime parsedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
