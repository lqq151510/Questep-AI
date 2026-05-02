package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.Question;
import com.interview.domain.repository.QuestionRepository;
import com.interview.infrastructure.persistence.entity.QuestionPO;
import com.interview.infrastructure.persistence.mapper.QuestionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {

    private final QuestionMapper questionMapper;

    public QuestionRepositoryImpl(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Override
    public Question save(
            Long materialId,
            Long creatorUserId,
            String questionType,
            String stemText,
            String referenceAnswer,
            String analysisText,
            Integer difficulty,
            String sourceType,
            String modelName
    ) {
        QuestionPO po = new QuestionPO();
        po.setMaterialId(materialId);
        po.setCreatorUserId(creatorUserId);
        po.setQuestionType(questionType);
        po.setStemText(stemText);
        po.setReferenceAnswer(referenceAnswer);
        po.setAnalysisText(analysisText);
        po.setDifficulty(difficulty);
        po.setSourceType(sourceType);
        po.setModelName(modelName);
        questionMapper.insert(po);
        return questionMapper.selectById(po.getId());
    }

    @Override
    public List<Question> findRecentByUser(Long userId, int limit) {
        return questionMapper.selectRecentByUser(userId, limit);
    }
}
