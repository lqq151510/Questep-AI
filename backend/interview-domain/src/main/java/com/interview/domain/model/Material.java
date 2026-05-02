package com.interview.domain.model;

import java.time.LocalDateTime;

public record Material(
        Long id,
        Long userId,
        String name,
        String fileType,
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
