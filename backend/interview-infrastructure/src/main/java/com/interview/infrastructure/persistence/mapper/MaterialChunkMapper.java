package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.MaterialChunk;
import com.interview.infrastructure.persistence.entity.MaterialChunkPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MaterialChunkMapper {

    int deleteByMaterialId(@Param("materialId") Long materialId);

    int batchInsert(@Param("chunks") List<MaterialChunkPO> chunks);

    List<MaterialChunk> selectByVectorIds(@Param("vectorIds") List<String> vectorIds);

    List<MaterialChunk> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
