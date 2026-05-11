package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.Question;
import com.interview.infrastructure.persistence.entity.QuestionPO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionMapper {

    int insert(QuestionPO questionPO);

    int updateById(QuestionPO questionPO);

    Question selectById(Long id);

    List<Question> selectRecentByUser(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    int countFreshApprovedByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<Question> selectByCondition(QuestionPO condition);

    List<Question> selectByIds(@Param("ids") List<Long> ids);

    int deleteById(Long id);

    int markExpiredForReview(@Param("reviewedAt") LocalDateTime reviewedAt, @Param("reviewStatus") String reviewStatus);

    List<Question> selectPendingRefreshCandidates(@Param("reviewStatus") String reviewStatus, @Param("limit") int limit);

    int archiveQuestion(@Param("questionId") Long questionId, @Param("archivedAt") LocalDateTime archivedAt);
}
