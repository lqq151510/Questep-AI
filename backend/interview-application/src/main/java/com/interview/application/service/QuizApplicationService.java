package com.interview.application.service;

import com.interview.application.dto.GenerateQuizCommand;
import com.interview.application.dto.GeneratedQuizResult;
import com.interview.application.port.LlmGateway;
import com.interview.application.service.quiz.QuestionDraft;
import com.interview.application.service.quiz.QuizFallbackQuestionFactory;
import com.interview.application.service.quiz.QuizGenerationPolicy;
import com.interview.application.service.quiz.QuizPromptBuilder;
import com.interview.application.service.quiz.StructuredQuizPayload;
import com.interview.application.service.quiz.StructuredQuizPayloadParser;
import com.interview.application.service.quiz.WebSearchService;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.Material;
import com.interview.domain.model.Question;
import com.interview.domain.repository.MaterialRepository;
import com.interview.domain.repository.QuestionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuizApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(QuizApplicationService.class);
    private static final int MAX_BRIEF_LENGTH = 500;
    private static final String MODEL_NAME_STRUCTURED = "llm-structured";
    private static final String MODEL_NAME_FALLBACK = "fallback-local";
    private static final int DEFAULT_REFRESH_BATCH = 20;
    private static final int DEFAULT_PREWARM_MAX_USERS = 20;
    private static final int DEFAULT_PREWARM_MATERIALS_PER_USER = 3;
    private static final int DEFAULT_PREWARM_FRESH_THRESHOLD = 30;
    private static final int MAX_PREWARM_MAX_USERS = 100;
    private static final int MAX_PREWARM_MATERIALS_PER_USER = 10;
    private static final int MAX_PREWARM_FRESH_THRESHOLD = 500;

    private final MaterialRepository materialRepository;
    private final QuestionRepository questionRepository;
    private final LlmGateway llmGateway;
    private final QuizGenerationPolicy generationPolicy;
    private final QuizPromptBuilder promptBuilder;
    private final StructuredQuizPayloadParser payloadParser;
    private final QuizFallbackQuestionFactory questionFactory;
    private final TransactionTemplate txTemplate;
    private final WebSearchService webSearchService;
    private final boolean webSearchEnabledDefault;

    public QuizApplicationService(
            MaterialRepository materialRepository,
            QuestionRepository questionRepository,
            LlmGateway llmGateway,
            QuizGenerationPolicy generationPolicy,
            QuizPromptBuilder promptBuilder,
            StructuredQuizPayloadParser payloadParser,
            QuizFallbackQuestionFactory questionFactory,
            TransactionTemplate txTemplate,
            WebSearchService webSearchService,
            @Value("${app.quiz.web-search.enabled:true}") boolean webSearchEnabledDefault
    ) {
        this.materialRepository = materialRepository;
        this.questionRepository = questionRepository;
        this.llmGateway = llmGateway;
        this.generationPolicy = generationPolicy;
        this.promptBuilder = promptBuilder;
        this.payloadParser = payloadParser;
        this.questionFactory = questionFactory;
        this.txTemplate = txTemplate;
        this.webSearchService = webSearchService;
        this.webSearchEnabledDefault = webSearchEnabledDefault;
    }

    @CircuitBreaker(name = "llmGateway", fallbackMethod = "generateQuizFallback")
    @RateLimiter(name = "quizGeneration")
    public GeneratedQuizResult generate(Long userId, GenerateQuizCommand command) {
        try {
            validateGenerateInput(userId, command);

            List<Material> materials = loadMaterials(userId, command.materialIds());
            boolean searchOnlyMode = materials.isEmpty() && hasSearchDemand(command);
            String questionType = generationPolicy.normalizeQuestionType(command.questionType());
            int difficulty = generationPolicy.normalizeDifficulty(command.difficulty());
            int count = generationPolicy.normalizeCount(command.count());
            boolean interviewMode = Boolean.TRUE.equals(command.interviewMode());
            String searchQuery = normalizeSearchQuery(command.searchQuery());
            boolean enableWebSearch = command.enableWebSearch() == null
                    ? webSearchEnabledDefault
                    : command.enableWebSearch();
            String webContext = enableWebSearch ? webSearchService.searchContext(searchQuery) : "";

            String prompt = promptBuilder.build(
                    materials,
                    questionType,
                    difficulty,
                    count,
                    interviewMode,
                    searchQuery,
                    webContext
            );
            String rawLlmResponse = llmGateway.chat(userId, prompt);
            StructuredQuizPayload payload = payloadParser.parse(rawLlmResponse, count);
            boolean fallbackUsed = payload == null || payload.questions().size() < count;
            int invalidCount = payload == null ? count : count - payload.questions().size();

            List<String> warnings = new ArrayList<>();
            if (payload == null) {
                warnings.add("LLM response schema invalid, all questions use deterministic fallback templates.");
            } else if (invalidCount > 0) {
                warnings.add("部分题目无法从 LLM 响应中解析，已用模板题补充 (" + invalidCount + "/" + count + ")");
            }

            List<Question> savedQuestions = txTemplate.execute(status ->
                    persistQuestions(userId, materials, questionType, difficulty, count, interviewMode, payload, searchOnlyMode));
            return new GeneratedQuizResult(
                    "quiz-" + shortUuid(),
                    buildModelBrief(payload, fallbackUsed),
                    savedQuestions,
                    fallbackUsed,
                    invalidCount,
                    warnings
            );
        } catch (Exception ex) {
            logger.warn("Quiz generation failed, falling back to deterministic templates: {}", ex.getMessage());
            return generateDeterministicFallback(userId, command, ex);
        }
    }

    public List<Question> recent(Long userId, int page, int pageSize) {
        int offset = page * pageSize;
        return questionRepository.findRecentByUser(userId, offset, pageSize);
    }

    public int refreshPendingQuestions(int limit) {
        int batchSize = limit <= 0 ? DEFAULT_REFRESH_BATCH : Math.min(limit, DEFAULT_REFRESH_BATCH);
        List<Question> candidates = questionRepository.findPendingRefreshCandidates(
                batchSize,
                TaskConstants.QUESTION_REVIEW_STATUS_PENDING
        );
        if (candidates.isEmpty()) {
            return 0;
        }

        int refreshed = 0;
        for (Question candidate : candidates) {
            try {
                if (candidate.creatorUserId() == null || candidate.materialId() == null) {
                    continue;
                }
                GenerateQuizCommand command = new GenerateQuizCommand(
                        List.of(candidate.materialId()),
                        toCommandQuestionType(candidate.questionType()),
                        candidate.difficulty(),
                        1,
                        "INTERVIEW".equalsIgnoreCase(candidate.questionType()),
                        null,
                        null
                );
                generate(candidate.creatorUserId(), command);
                questionRepository.archiveQuestion(candidate.id(), java.time.LocalDateTime.now());
                refreshed++;
            } catch (Exception ex) {
                logger.warn("Failed to refresh pending question id={}: {}", candidate.id(), ex.getMessage());
            }
        }
        return refreshed;
    }

    public int prewarmQuestionBank(
            int maxUsers,
            int materialsPerUser,
            int freshThreshold,
            int generateCount,
            int difficulty
    ) {
        int safeMaxUsers = clamp(maxUsers, DEFAULT_PREWARM_MAX_USERS, 1, MAX_PREWARM_MAX_USERS);
        int safeMaterialsPerUser = clamp(materialsPerUser, DEFAULT_PREWARM_MATERIALS_PER_USER, 1, MAX_PREWARM_MATERIALS_PER_USER);
        int safeFreshThreshold = clamp(freshThreshold, DEFAULT_PREWARM_FRESH_THRESHOLD, 1, MAX_PREWARM_FRESH_THRESHOLD);
        int safeGenerateCount = generationPolicy.normalizeCount(generateCount);
        int safeDifficulty = generationPolicy.normalizeDifficulty(difficulty);

        List<Long> userIds = materialRepository.findUsersWithParsedMaterials(safeMaxUsers);
        if (userIds.isEmpty()) {
            return 0;
        }

        int generatedQuestions = 0;
        for (Long userId : userIds) {
            int freshCount = questionRepository.countFreshApprovedByUser(userId, java.time.LocalDateTime.now());
            if (freshCount >= safeFreshThreshold) {
                continue;
            }

            List<Material> materials = materialRepository.findParsedMaterialsByUser(userId, safeMaterialsPerUser);
            if (materials.isEmpty()) {
                continue;
            }

            GenerateQuizCommand command = new GenerateQuizCommand(
                    materials.stream().map(Material::id).toList(),
                    "choice",
                    safeDifficulty,
                    safeGenerateCount,
                    false,
                    null,
                    null
            );
            try {
                GeneratedQuizResult result = generate(userId, command);
                generatedQuestions += result.questions() == null ? 0 : result.questions().size();
            } catch (Exception ex) {
                logger.warn("Question prewarm failed for userId={}: {}", userId, ex.getMessage());
            }
        }
        return generatedQuestions;
    }

    private GeneratedQuizResult generateQuizFallback(Long userId, GenerateQuizCommand command, Throwable throwable) {
        logger.error("LLM Gateway circuit breaker triggered, using deterministic fallback: {}", throwable.getMessage());

        return generateDeterministicFallback(userId, command, throwable);
    }

    private GeneratedQuizResult generateDeterministicFallback(Long userId, GenerateQuizCommand command, Throwable throwable) {
        validateGenerateInput(userId, command);
        List<Material> materials = loadMaterials(userId, command.materialIds());
        String questionType = generationPolicy.normalizeQuestionType(command.questionType());
        int difficulty = generationPolicy.normalizeDifficulty(command.difficulty());
        int count = generationPolicy.normalizeCount(command.count());
        boolean interviewMode = Boolean.TRUE.equals(command.interviewMode());
        boolean searchOnlyMode = materials.isEmpty() && hasSearchDemand(command);

        List<Question> questions = txTemplate.execute(status ->
                persistQuestions(userId, materials, questionType, difficulty, count, interviewMode, null, searchOnlyMode));
        String reason = throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? "unknown error"
                : throwable.getMessage();
        return new GeneratedQuizResult(
                "quiz-fallback-" + shortUuid(),
                "LLM unavailable or generation failed, generated questions using deterministic fallback templates.",
                questions,
                true,
                count,
                List.of("生成链路异常，已降级为确定性模板题：" + reason)
        );
    }

    private void validateGenerateInput(Long userId, GenerateQuizCommand command) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (command == null) {
            throw new IllegalArgumentException("command cannot be null");
        }
        boolean hasMaterials = command.materialIds() != null && !command.materialIds().isEmpty();
        boolean hasSearchDemand = hasSearchDemand(command);
        if (!hasMaterials && !hasSearchDemand) {
            throw new IllegalArgumentException("materialIds cannot be empty unless searchQuery or web search is enabled");
        }
    }

    private List<Material> loadMaterials(Long userId, List<Long> materialIds) {
        if (materialIds == null || materialIds.isEmpty()) {
            return List.of();
        }
        List<Material> materials = materialRepository.findByUserIdAndIds(userId, materialIds);
        return materials;
    }

    private List<Question> persistQuestions(
            Long userId,
            List<Material> materials,
            String questionType,
            int difficulty,
            int count,
            boolean interviewMode,
            StructuredQuizPayload payload,
            boolean searchOnlyMode
    ) {
        List<QuestionDraft> structuredQuestions = payload == null ? List.of() : payload.questions();
        String modelName = payload == null ? MODEL_NAME_FALLBACK : MODEL_NAME_STRUCTURED;
        List<Question> questions = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            Material material = materials.isEmpty() ? null : materials.get(index % materials.size());
            QuestionDraft draft = questionFactory.selectDraft(
                    structuredQuestions,
                    material,
                    questionType,
                    difficulty,
                    index,
                    interviewMode
            );
            questions.add(questionRepository.save(
                    material == null ? null : material.id(),
                    userId,
                    questionType,
                    draft.stemText(),
                    draft.optionsJson(),
                    draft.referenceAnswer(),
                    draft.analysisText(),
                    difficulty,
                    TaskConstants.SOURCE_TYPE_AI,
                    modelName
            ));
        }

        return questions;
    }

    private String buildModelBrief(StructuredQuizPayload payload, boolean fallbackUsed) {
        if (payload == null) {
            return "LLM response schema invalid, used deterministic fallback question templates.";
        }
        if (fallbackUsed) {
            return limitLength(payload.summary() + " (partially valid schema, fallback questions supplemented)");
        }
        return limitLength(payload.summary());
    }

    private String limitLength(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_BRIEF_LENGTH ? text.substring(0, MAX_BRIEF_LENGTH) : text;
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private int clamp(int value, int defaultValue, int min, int max) {
        int resolved = value <= 0 ? defaultValue : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private String toCommandQuestionType(String storedType) {
        if (storedType == null) {
            return "short";
        }
        return switch (storedType) {
            case "SINGLE_CHOICE" -> "choice";
            case "SHORT_ANSWER" -> "short";
            case "CODING" -> "coding";
            case "INTERVIEW" -> "interview";
            default -> "short";
        };
    }

    private String normalizeSearchQuery(String query) {
        if (query == null) {
            return "";
        }
        String normalized = query.trim();
        if (normalized.length() > 200) {
            return normalized.substring(0, 200);
        }
        return normalized;
    }

    private boolean hasSearchDemand(GenerateQuizCommand command) {
        String searchQuery = normalizeSearchQuery(command.searchQuery());
        boolean enableWebSearch = command.enableWebSearch() == null
                ? webSearchEnabledDefault
                : command.enableWebSearch();
        return !searchQuery.isBlank() || enableWebSearch;
    }
}
