package com.interview.application.service;

import com.interview.aigateway.client.LlmGateway;
import com.interview.application.dto.GenerateQuizCommand;
import com.interview.application.dto.GeneratedQuizResult;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.Material;
import com.interview.domain.model.Question;
import com.interview.domain.repository.MaterialRepository;
import com.interview.domain.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuizApplicationService {

    private static final Map<String, String> QUESTION_TYPE_MAP = new LinkedHashMap<>();

    static {
        QUESTION_TYPE_MAP.put("choice", "SINGLE_CHOICE");
        QUESTION_TYPE_MAP.put("single_choice", "SINGLE_CHOICE");
        QUESTION_TYPE_MAP.put("SINGLE_CHOICE", "SINGLE_CHOICE");
        QUESTION_TYPE_MAP.put("short", "SHORT_ANSWER");
        QUESTION_TYPE_MAP.put("short_answer", "SHORT_ANSWER");
        QUESTION_TYPE_MAP.put("SHORT_ANSWER", "SHORT_ANSWER");
        QUESTION_TYPE_MAP.put("code", "CODING");
        QUESTION_TYPE_MAP.put("coding", "CODING");
        QUESTION_TYPE_MAP.put("CODING", "CODING");
        QUESTION_TYPE_MAP.put("interview", "INTERVIEW");
        QUESTION_TYPE_MAP.put("INTERVIEW", "INTERVIEW");
    }

    private final MaterialRepository materialRepository;
    private final QuestionRepository questionRepository;
    private final LlmGateway llmGateway;

    public QuizApplicationService(
            MaterialRepository materialRepository,
            QuestionRepository questionRepository,
            LlmGateway llmGateway
    ) {
        this.materialRepository = materialRepository;
        this.questionRepository = questionRepository;
        this.llmGateway = llmGateway;
    }

    @Transactional
    public GeneratedQuizResult generate(Long userId, GenerateQuizCommand command) {
        List<Material> materials = materialRepository.findByUserIdAndIds(userId, command.materialIds());
        if (materials.isEmpty()) {
            throw new IllegalArgumentException("No materials found for quiz generation");
        }

        String questionType = normalizeQuestionType(command.questionType());
        int difficulty = command.difficulty() == null ? 3 : command.difficulty();
        int count = command.count() == null ? 3 : command.count();
        boolean interviewMode = Boolean.TRUE.equals(command.interviewMode());
        String traceId = "quiz-" + UUID.randomUUID().toString().substring(0, 8);
        String prompt = buildPrompt(materials, questionType, difficulty, count, interviewMode);
        String modelBrief = llmGateway.chat(prompt);

        List<Question> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Material material = materials.get(index % materials.size());
            QuestionDraft draft = draftQuestion(material, questionType, difficulty, index + 1, interviewMode);
            questions.add(questionRepository.save(
                    material.id(),
                    userId,
                    questionType,
                    draft.stemText(),
                    draft.referenceAnswer(),
                    draft.analysisText(),
                    difficulty,
                    TaskConstants.SOURCE_TYPE_AI,
                    TaskConstants.GATEWAY_NOOP
            ));
        }

        return new GeneratedQuizResult(traceId, modelBrief, questions);
    }

    public List<Question> recent(Long userId, int limit) {
        return questionRepository.findRecentByUser(userId, Math.max(1, Math.min(limit, 50)));
    }

    private String normalizeQuestionType(String value) {
        String normalized = QUESTION_TYPE_MAP.get(value);
        if (normalized == null) {
            normalized = QUESTION_TYPE_MAP.get(value.toLowerCase());
        }
        if (normalized == null) {
            throw new IllegalArgumentException("Unsupported question type: " + value);
        }
        return normalized;
    }

    private String buildPrompt(List<Material> materials, String questionType, int difficulty, int count, boolean interviewMode) {
        String materialNames = materials.stream().map(Material::name).toList().toString();
        return "Generate " + count + " " + questionType + " questions. Materials=" + materialNames
                + ", difficulty=" + difficulty + ", interviewMode=" + interviewMode
                + ". Keep each question grounded in backend project experience.";
    }

    private QuestionDraft draftQuestion(Material material, String questionType, int difficulty, int index, boolean interviewMode) {
        String depth = switch (difficulty) {
            case 1 -> "基础概念";
            case 2 -> "常见场景";
            case 3 -> "项目实践";
            case 4 -> "故障排查";
            default -> "架构取舍";
        };
        String stem = switch (questionType) {
            case "SINGLE_CHOICE" -> material.name() + "：围绕" + depth + "设计一道单选题，指出最合理的方案并说明其他选项的问题。";
            case "CODING" -> material.name() + "：写一个可落地的代码设计题，要求体现边界条件、异常处理和复杂度分析。";
            case "INTERVIEW" -> material.name() + "：请用面试追问方式考察候选人对" + depth + "的理解，并要求结合真实项目回答。";
            default -> material.name() + "：请回答一个" + depth + "问题，并给出原理、项目落点和排查路径。";
        };
        if (interviewMode) {
            stem = stem + " 追问候选人的技术取舍、失败案例和可观测性方案。";
        }
        return new QuestionDraft(
                index + ". " + stem,
                "参考答案应覆盖核心概念、项目实践、风险边界和验证方法。",
                "评分重点：答案是否贴合资料主题，是否能解释取舍，并能给出可执行排查步骤。"
        );
    }

    private record QuestionDraft(String stemText, String referenceAnswer, String analysisText) {
    }
}
