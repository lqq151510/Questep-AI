package com.interview.api.scheduler;

import com.interview.application.service.QuizApplicationService;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class QuestionReviewScheduler {
    private static final Logger logger = LoggerFactory.getLogger(QuestionReviewScheduler.class);

    private final QuestionRepository questionRepository;
    private final QuizApplicationService quizApplicationService;
    private final int refreshBatchSize;

    public QuestionReviewScheduler(
            QuestionRepository questionRepository,
            QuizApplicationService quizApplicationService,
            @Value("${app.question-review.refresh-batch-size:10}") int refreshBatchSize
    ) {
        this.questionRepository = questionRepository;
        this.quizApplicationService = quizApplicationService;
        this.refreshBatchSize = Math.max(1, Math.min(20, refreshBatchSize));
    }

    @Scheduled(cron = "${app.question-review.expire-scan-cron:0 0 3 * * *}")
    public void markExpiredQuestionsForReview() {
        int updated = questionRepository.markExpiredForReview(
                LocalDateTime.now(),
                TaskConstants.QUESTION_REVIEW_STATUS_PENDING
        );
        if (updated > 0) {
            logger.info("Marked {} expired questions as {}", updated, TaskConstants.QUESTION_REVIEW_STATUS_PENDING);
        } else {
            logger.debug("No expired questions need review update.");
        }
    }

    @Scheduled(cron = "${app.question-review.auto-refresh-cron:0 15 3 * * *}")
    public void refreshPendingQuestions() {
        int refreshed = quizApplicationService.refreshPendingQuestions(refreshBatchSize);
        if (refreshed > 0) {
            logger.info("Auto refreshed {} pending questions.", refreshed);
        } else {
            logger.debug("No pending questions refreshed.");
        }
    }
}
