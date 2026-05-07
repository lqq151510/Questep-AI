package com.interview.application.service.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class QuizGenerationPolicy {
    private static final Logger logger = LoggerFactory.getLogger(QuizGenerationPolicy.class);
    private static final int DEFAULT_QUESTIONS_PER_QUIZ = 3;
    private static final int MAX_QUESTIONS_PER_QUIZ = 10;
    private static final int MIN_QUESTIONS_PER_QUIZ = 1;
    private static final int DEFAULT_DIFFICULTY = 3;
    private static final int MAX_DIFFICULTY = 5;
    private static final int MIN_DIFFICULTY = 1;

    private static final Map<String, String> QUESTION_TYPE_MAP = Map.ofEntries(
            Map.entry("choice", "SINGLE_CHOICE"),
            Map.entry("single_choice", "SINGLE_CHOICE"),
            Map.entry("short", "SHORT_ANSWER"),
            Map.entry("short_answer", "SHORT_ANSWER"),
            Map.entry("code", "CODING"),
            Map.entry("coding", "CODING"),
            Map.entry("interview", "INTERVIEW")
    );

    public String normalizeQuestionType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("questionType cannot be blank");
        }
        String normalized = QUESTION_TYPE_MAP.get(value.trim().toLowerCase(Locale.ROOT));
        if (normalized == null) {
            throw new IllegalArgumentException("Unsupported question type: " + value);
        }
        return normalized;
    }

    public int normalizeDifficulty(Integer difficulty) {
        if (difficulty == null) {
            return DEFAULT_DIFFICULTY;
        }
        if (difficulty < MIN_DIFFICULTY) {
            logger.warn("Difficulty {} below minimum {}, using minimum", difficulty, MIN_DIFFICULTY);
            return MIN_DIFFICULTY;
        }
        if (difficulty > MAX_DIFFICULTY) {
            logger.warn("Difficulty {} above maximum {}, using maximum", difficulty, MAX_DIFFICULTY);
            return MAX_DIFFICULTY;
        }
        return difficulty;
    }

    public int normalizeCount(Integer count) {
        if (count == null) {
            return DEFAULT_QUESTIONS_PER_QUIZ;
        }
        if (count < MIN_QUESTIONS_PER_QUIZ) {
            logger.warn("Quiz count {} below minimum {}, using minimum", count, MIN_QUESTIONS_PER_QUIZ);
            return MIN_QUESTIONS_PER_QUIZ;
        }
        if (count > MAX_QUESTIONS_PER_QUIZ) {
            logger.warn("Quiz count {} above maximum {}, using maximum", count, MAX_QUESTIONS_PER_QUIZ);
            return MAX_QUESTIONS_PER_QUIZ;
        }
        return count;
    }
}
