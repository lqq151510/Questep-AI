package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.Question;
import com.interview.domain.repository.QuestionRepository;
import com.interview.infrastructure.persistence.entity.QuestionPO;
import com.interview.infrastructure.persistence.mapper.QuestionMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class QuestionRepositoryImpl implements QuestionRepository {
    private static final String REVIEW_STATUS_APPROVED = "APPROVED";
    private static final String SOURCE_TYPE_AI = "AI";

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
        LocalDateTime now = LocalDateTime.now();
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
        po.setSourceUrl(materialId == null ? null : "material://" + materialId);
        po.setSourceVersion("material-v1");
        po.setLastVerifiedAt(now);
        po.setConfidenceScore(resolveConfidenceScore(modelName, sourceType));
        po.setExpiresAt(now.plusDays(30));
        po.setReviewStatus(REVIEW_STATUS_APPROVED);
        po.setStatus(1);
        questionMapper.insert(po);
        return questionMapper.selectById(po.getId());
    }

    @Override
    public Optional<Question> findById(Long id) {
        return Optional.ofNullable(questionMapper.selectById(id));
    }

    @Override
    public List<Question> findRecentByUser(Long userId, int offset, int limit) {
        return questionMapper.selectRecentByUser(userId, offset, limit);
    }

    @Override
    public List<Question> selectByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return questionMapper.selectByIds(ids);
    }

    @Override
    public int markExpiredForReview(LocalDateTime reviewedAt, String reviewStatus) {
        if (reviewedAt == null || reviewStatus == null || reviewStatus.isBlank()) {
            return 0;
        }
        return questionMapper.markExpiredForReview(reviewedAt, reviewStatus);
    }

    @Override
    public List<Question> findPendingRefreshCandidates(int limit, String reviewStatus) {
        if (limit <= 0 || reviewStatus == null || reviewStatus.isBlank()) {
            return List.of();
        }
        return questionMapper.selectPendingRefreshCandidates(reviewStatus, limit);
    }

    @Override
    public int archiveQuestion(Long questionId, LocalDateTime archivedAt) {
        if (questionId == null || archivedAt == null) {
            return 0;
        }
        return questionMapper.archiveQuestion(questionId, archivedAt);
    }

    private BigDecimal resolveConfidenceScore(String modelName, String sourceType) {
        if (SOURCE_TYPE_AI.equalsIgnoreCase(sourceType)) {
            if ("fallback-local".equalsIgnoreCase(modelName)) {
                return new BigDecimal("0.650");
            }
            return new BigDecimal("0.820");
        }
        return new BigDecimal("0.900");
    }
}
