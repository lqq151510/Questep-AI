package com.interview.domain.repository;

import com.interview.domain.model.Question;

import java.util.List;

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

    List<Question> findRecentByUser(Long userId, int limit);
}
