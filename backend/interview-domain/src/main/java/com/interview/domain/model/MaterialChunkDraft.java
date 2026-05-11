package com.interview.domain.model;

public record MaterialChunkDraft(
        Integer chunkNo,
        String chunkText,
        Integer tokenCount,
        String embeddingModel,
        String vectorId,
        String metadataJson
) {
}
