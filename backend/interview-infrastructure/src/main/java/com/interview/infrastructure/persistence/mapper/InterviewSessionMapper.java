package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.InterviewSession;
import com.interview.infrastructure.persistence.entity.InterviewSessionPO;
import org.apache.ibatis.annotations.Param;

public interface InterviewSessionMapper {
    int insert(InterviewSessionPO po);
    int updateById(InterviewSessionPO po);
    InterviewSession selectById(Long id);
    InterviewSession selectActiveByUserId(@Param("userId") Long userId);
    int updateStatusAndSnapshot(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("contextSnapshot") String contextSnapshot
    );
}
