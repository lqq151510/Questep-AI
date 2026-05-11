package com.interview.api.scheduler;

import com.interview.application.service.QuizApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QuestionPrewarmScheduler {
    private static final Logger logger = LoggerFactory.getLogger(QuestionPrewarmScheduler.class);
    private static final int MAX_USERS_LIMIT = 100;
    private static final int MAX_MATERIALS_PER_USER_LIMIT = 10;
    private static final int MAX_FRESH_THRESHOLD_LIMIT = 500;
    private static final int MAX_GENERATE_COUNT_LIMIT = 20;
    private static final int MAX_DIFFICULTY_LIMIT = 5;

    private final QuizApplicationService quizApplicationService;
    private final boolean enabled;
    private final int maxUsers;
    private final int materialsPerUser;
    private final int freshThreshold;
    private final int generateCount;
    private final int difficulty;

    public QuestionPrewarmScheduler(
            QuizApplicationService quizApplicationService,
            @Value("${app.question-prewarm.enabled:false}") boolean enabled,
            @Value("${app.question-prewarm.max-users:20}") int maxUsers,
            @Value("${app.question-prewarm.materials-per-user:3}") int materialsPerUser,
            @Value("${app.question-prewarm.fresh-threshold:30}") int freshThreshold,
            @Value("${app.question-prewarm.generate-count:8}") int generateCount,
            @Value("${app.question-prewarm.difficulty:3}") int difficulty
    ) {
        this.quizApplicationService = quizApplicationService;
        this.enabled = enabled;
        this.maxUsers = normalize(maxUsers, 20, MAX_USERS_LIMIT);
        this.materialsPerUser = normalize(materialsPerUser, 3, MAX_MATERIALS_PER_USER_LIMIT);
        this.freshThreshold = normalize(freshThreshold, 30, MAX_FRESH_THRESHOLD_LIMIT);
        this.generateCount = normalize(generateCount, 8, MAX_GENERATE_COUNT_LIMIT);
        this.difficulty = normalize(difficulty, 3, MAX_DIFFICULTY_LIMIT);
    }

    @Scheduled(cron = "${app.question-prewarm.cron:0 30 3 * * *}")
    public void prewarmQuestionBank() {
        if (!enabled) {
            logger.debug("Question prewarm disabled, skip this schedule.");
            return;
        }
        int generated = quizApplicationService.prewarmQuestionBank(
                maxUsers,
                materialsPerUser,
                freshThreshold,
                generateCount,
                difficulty
        );
        if (generated > 0) {
            logger.info("Question prewarm generated {} questions.", generated);
        } else {
            logger.debug("Question prewarm completed with no new questions.");
        }
    }

    private int normalize(int value, int defaultValue, int max) {
        int resolved = value <= 0 ? defaultValue : value;
        return Math.min(resolved, max);
    }
}
