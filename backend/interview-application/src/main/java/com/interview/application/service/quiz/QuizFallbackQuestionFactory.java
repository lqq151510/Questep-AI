package com.interview.application.service.quiz;

import com.interview.domain.model.Material;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizFallbackQuestionFactory {
    public QuestionDraft selectDraft(
            List<QuestionDraft> structuredQuestions,
            Material material,
            String questionType,
            int difficulty,
            int index,
            boolean interviewMode
    ) {
        if (index < structuredQuestions.size()) {
            QuestionDraft structured = structuredQuestions.get(index);
            String normalizedStem = prefixIndexIfMissing(structured.stemText(), index + 1);
            return new QuestionDraft(normalizedStem, structured.referenceAnswer(), structured.analysisText());
        }
        return draftQuestion(material, questionType, difficulty, index + 1, interviewMode);
    }

    public QuestionDraft draftQuestion(Material material, String questionType, int difficulty, int index, boolean interviewMode) {
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

    private String prefixIndexIfMissing(String stemText, int index) {
        String trimmed = stemText == null ? "" : stemText.trim();
        if (trimmed.isEmpty()) {
            return index + ". 题干缺失，已由系统补全。";
        }
        String prefix = index + ".";
        return trimmed.startsWith(prefix) ? trimmed : prefix + " " + trimmed;
    }
}
