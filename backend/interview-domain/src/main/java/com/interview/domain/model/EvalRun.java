package com.interview.domain.model;

import java.time.LocalDateTime;

public record EvalRun(
        Long id,
        String runKey,
        Long promptVersionId,
        Integer totalCases,
        Integer passedCases,
        Double avgScore,
        String status,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
}
