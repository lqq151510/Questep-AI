package com.interview.domain.model;

import java.time.LocalDateTime;

public record MaterialChunk(
        Long id,
        Long materialId,
        Integer chunkNo,
        String chunkText,
        Integer tokenCount,
        String embeddingModel,
        String vectorId,
        String metadataJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
