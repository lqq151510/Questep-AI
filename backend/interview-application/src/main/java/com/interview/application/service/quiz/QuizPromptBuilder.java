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

    public String build(List<Material> materials, String questionType, int difficulty, int count, boolean interviewMode) {
        String materialNames = materials.stream().map(Material::name).toList().toString();

        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("count", String.valueOf(count));
            vars.put("questionType", questionType);
            vars.put("materialNames", materialNames);
            vars.put("difficulty", String.valueOf(difficulty));
            vars.put("interviewMode", String.valueOf(interviewMode));
            vars.put("questionGuidance", questionGuidance);
            String fromTemplate = promptTemplateService.resolveTemplate("quiz_generation", vars);
            if (fromTemplate != null && !fromTemplate.isEmpty()) {
                return fromTemplate;
            }
        } catch (Exception e) {
            log.debug("Falling back to hardcoded quiz prompt: {}", e.getMessage());
        }

        return "Generate " + count + " " + questionType + " questions. Materials=" + materialNames
                + ", difficulty=" + difficulty + ", interviewMode=" + interviewMode
                + ". " + questionGuidance + " "
                + "Return ONLY strict JSON with schema: "
                + "{\"summary\":\"string\",\"questions\":[{\"stem\":\"string\",\"referenceAnswer\":\"string\",\"analysis\":\"string\"}]}. "
                + "Do not use markdown and do not output any extra keys.";
    }
}
