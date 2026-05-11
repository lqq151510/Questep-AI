package com.interview.application.port;

import java.util.List;

public interface VectorStoreGateway {

    void upsert(List<VectorDocument> documents);

    List<VectorSearchHit> search(float[] queryVector, int topK, Long userId);

    record VectorDocument(
            String id,
            Long userId,
            Long materialId,
            Integer chunkNo,
            String text,
            float[] vector
    ) {
    }

    record VectorSearchHit(
            String id,
            double score,
            String text,
            Long materialId,
            Integer chunkNo
    ) {
    }
}
