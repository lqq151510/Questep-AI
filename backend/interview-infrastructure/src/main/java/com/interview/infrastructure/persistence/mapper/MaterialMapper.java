package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.Material;
import com.interview.infrastructure.persistence.entity.MaterialPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MaterialMapper {

    int insert(MaterialPO materialPO);

    int updateById(MaterialPO materialPO);

    Material selectById(Long id);

    List<Material> selectByUserIdAndIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    List<Material> selectByUserId(@Param("userId") Long userId, @Param("parseStatus") String parseStatus);

    List<Material> selectByCondition(MaterialPO condition);

    int updateParseSuccess(
            @Param("id") Long id,
            @Param("contentHash") String contentHash,
            @Param("analysisText") String analysisText
    );

    int updateParseFailure(@Param("id") Long id, @Param("parseErrorMsg") String parseErrorMsg);

    int updateParsePending(@Param("id") Long id);

    int deleteById(Long id);

    int deleteByIds(@Param("ids") List<Long> ids);
}
