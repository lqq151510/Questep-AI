package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.PromptTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PromptTemplateMapper {

    int insert(PromptTemplate template);

    PromptTemplate selectById(Long id);

    PromptTemplate selectActiveByKey(@Param("templateKey") String templateKey);

    List<PromptTemplate> selectByKey(@Param("templateKey") String templateKey);

    int updateStatusByKeyAndVersion(@Param("templateKey") String templateKey,
                                     @Param("version") int version,
                                     @Param("status") String status);
}
