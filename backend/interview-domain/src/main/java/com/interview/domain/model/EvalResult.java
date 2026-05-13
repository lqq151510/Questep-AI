package com.interview.domain.model;

import java.time.LocalDateTime;

public record EvalResult(
        Long id,
        Long runId,
        Long caseId,
        String actualOutput,
        Double score,
        String keywordHits,
        Boolean structureValid,
        Long durationMs,
        String errorMsg,
        LocalDateTime createdAt
) {
}
