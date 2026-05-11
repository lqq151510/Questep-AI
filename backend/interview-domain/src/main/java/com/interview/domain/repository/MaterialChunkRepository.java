package com.interview.domain.repository;

import com.interview.domain.model.MaterialChunk;
import com.interview.domain.model.MaterialChunkDraft;

import java.util.List;

public interface MaterialChunkRepository {

    void replaceChunks(Long materialId, List<MaterialChunkDraft> chunks);

    List<MaterialChunk> findByVectorIds(List<String> vectorIds);

    List<MaterialChunk> findRecentByUserId(Long userId, int limit);
}
