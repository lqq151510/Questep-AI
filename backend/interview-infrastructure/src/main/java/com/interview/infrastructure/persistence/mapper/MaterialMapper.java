package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.Material;
import com.interview.infrastructure.persistence.entity.MaterialPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MaterialMapper {

    int insert(MaterialPO materialPO);

    Material selectById(Long id);

    List<Material> selectByUserIdAndIds(@Param("userId") Long userId, @Param("ids") List<Long> ids);

    List<Material> selectByUserId(Long userId);
}
