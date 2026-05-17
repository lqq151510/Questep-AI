package com.interview.application.service.quiz;

import com.interview.application.service.PromptTemplateService;
import com.interview.domain.model.Material;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuizPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(QuizPromptBuilder.class);
    private final String questionGuidance;
    private final PromptTemplateService promptTemplateService;

    public QuizPromptBuilder(
            @Value("${app.quiz.question-guidance:Keep each question grounded in backend project experience.}") String questionGuidance,
            PromptTemplateService promptTemplateService
    ) {
        this.questionGuidance = questionGuidance;
        this.promptTemplateService = promptTemplateService;
    }

    public String build(
            List<Material> materials,
            String questionType,
            int difficulty,
            int count,
            boolean interviewMode,
            String searchQuery,
            String webContext
    ) {
        String materialNames = materials.stream().map(Material::name).toList().toString();
        String materialInsights = buildMaterialInsights(materials);
        String demand = searchQuery == null ? "" : searchQuery.trim();

        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("count", String.valueOf(count));
            vars.put("questionType", questionType);
            vars.put("materialNames", materialNames);
            vars.put("difficulty", String.valueOf(difficulty));
            vars.put("interviewMode", String.valueOf(interviewMode));
            vars.put("questionGuidance", questionGuidance);
            vars.put("searchQuery", demand);
            vars.put("materialInsights", materialInsights);
            vars.put("webContext", webContext == null ? "" : webContext);
            String fromTemplate = promptTemplateService.resolveTemplate("quiz_generation", vars);
            if (fromTemplate != null && !fromTemplate.isEmpty()) {
                return fromTemplate;
            }
        } catch (Exception e) {
            log.debug("Falling back to hardcoded quiz prompt: {}", e.getMessage());
        }

        return "请用中文生成 " + count + " 道 " + questionType + " 题。"
                + "材料=" + materialNames
                + "，难度=" + difficulty
                + "，interviewMode=" + interviewMode
                + "。"
                + questionGuidance + " "
                + (demand.isBlank() ? "" : "用户需求：" + demand + "。 ")
                + "材料摘要：" + materialInsights + "。 "
                + ((webContext == null || webContext.isBlank()) ? "" : "联网补充：" + webContext + "。 ")
                + "仅返回严格 JSON，不要使用 Markdown，不要输出额外字段。"
                + "JSON 结构必须为："
                + "{\"summary\":\"string\",\"questions\":[{\"stem\":\"string\",\"optionsJson\":{\"A\":\"string\",\"B\":\"string\",\"C\":\"string\",\"D\":\"string\"},\"referenceAnswer\":\"string\",\"analysis\":\"string\"}]}. "
                + "如果题型是 SINGLE_CHOICE，每道题必须提供 4 个互斥选项，且 optionsJson 中 A-D 不能缺失。";
    }

    private String buildMaterialInsights(List<Material> materials) {
        return materials.stream()
                .map(material -> {
                    String analysis = material.analysisText();
                    if (analysis == null || analysis.isBlank()) {
                        return material.name() + ": 无结构化解析摘要";
                    }
                    String compact = analysis.replaceAll("\\s+", " ").trim();
                    if (compact.length() > 220) {
                        compact = compact.substring(0, 220);
                    }
                    return material.name() + ": " + compact;
                })
                .collect(Collectors.joining(" | "));
    }
}
