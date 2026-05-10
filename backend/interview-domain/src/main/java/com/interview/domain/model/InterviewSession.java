package com.interview.domain.model;

import java.time.LocalDateTime;

public record InterviewSession(
        Long id,
        Long userId,
        String position,
        Integer difficulty,
        String status,
        String contextSnapshot,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
