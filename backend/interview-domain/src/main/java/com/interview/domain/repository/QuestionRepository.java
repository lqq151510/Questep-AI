package com.interview.domain.repository;

import com.interview.domain.model.Question;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository {

    Question save(
            Long materialId,
            Long creatorUserId,
            String questionType,
            String stemText,
            String referenceAnswer,
            String analysisText,
            Integer difficulty,
            String sourceType,
            String modelName
    );

    Optional<Question> findById(Long id);

    List<Question> findRecentByUser(Long userId, int offset, int limit);

    List<Question> selectByIds(List<Long> ids);

    int markExpiredForReview(LocalDateTime reviewedAt, String reviewStatus);

    List<Question> findPendingRefreshCandidates(int limit, String reviewStatus);

    int archiveQuestion(Long questionId, LocalDateTime archivedAt);
}
