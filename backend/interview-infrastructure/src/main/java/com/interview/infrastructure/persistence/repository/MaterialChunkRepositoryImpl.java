package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.MaterialChunk;
import com.interview.domain.model.MaterialChunkDraft;
import com.interview.domain.repository.MaterialChunkRepository;
import com.interview.infrastructure.persistence.entity.MaterialChunkPO;
import com.interview.infrastructure.persistence.mapper.MaterialChunkMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MaterialChunkRepositoryImpl implements MaterialChunkRepository {

    private final MaterialChunkMapper materialChunkMapper;

    public MaterialChunkRepositoryImpl(MaterialChunkMapper materialChunkMapper) {
        this.materialChunkMapper = materialChunkMapper;
    }

    @Override
    @Transactional
    public void replaceChunks(Long materialId, List<MaterialChunkDraft> chunks) {
        materialChunkMapper.deleteByMaterialId(materialId);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        List<MaterialChunkPO> batch = new ArrayList<>(chunks.size());
        for (MaterialChunkDraft draft : chunks) {
            MaterialChunkPO po = new MaterialChunkPO();
            po.setMaterialId(materialId);
            po.setChunkNo(draft.chunkNo());
            po.setChunkText(draft.chunkText());
            po.setTokenCount(draft.tokenCount());
            po.setEmbeddingModel(draft.embeddingModel());
            po.setVectorId(draft.vectorId());
            po.setMetadataJson(draft.metadataJson());
            batch.add(po);
        }
        materialChunkMapper.batchInsert(batch);
    }

    @Override
    public List<MaterialChunk> findByVectorIds(List<String> vectorIds) {
        if (vectorIds == null || vectorIds.isEmpty()) {
            return List.of();
        }
        return materialChunkMapper.selectByVectorIds(vectorIds);
    }

    @Override
    public List<MaterialChunk> findRecentByUserId(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return List.of();
        }
        return materialChunkMapper.selectRecentByUserId(userId, limit);
    }
}
