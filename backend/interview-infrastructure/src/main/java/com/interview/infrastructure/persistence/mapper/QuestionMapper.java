package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.Question;
import com.interview.infrastructure.persistence.entity.QuestionPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QuestionMapper {

    int insert(QuestionPO questionPO);

    Question selectById(Long id);

    List<Question> selectRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);
}
