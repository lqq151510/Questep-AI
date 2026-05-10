package com.interview.domain.model;

import java.time.LocalDateTime;

public record WrongBook(
        Long id,
        Long userId,
        Long questionId,
        LocalDateTime firstWrongAt,
        LocalDateTime lastWrongAt,
        Integer wrongCount,
        String masteryStatus,
        LocalDateTime lastReviewAt,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
