package com.interview.domain.model;

import java.time.LocalDateTime;

public record Material(
        Long id,
        Long userId,
        String name,
        String fileType,
        String parseStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
