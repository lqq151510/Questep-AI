package com.interview.domain.model;

import java.time.LocalDateTime;

public record AsyncTaskRecord(
        Long id,
        String taskNo,
        String taskType,
        String bizType,
        Long bizId,
        String status,
        Integer progress,
        String errorMsg,
        Long createdBy,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String errorCode,
        String stage,
        Boolean retryable
) {
}
