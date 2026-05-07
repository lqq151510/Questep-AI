package com.interview.application.service.quiz;

import com.interview.domain.model.Material;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuizPromptBuilder {
    private final String questionGuidance;

    public QuizPromptBuilder(
            @Value("${app.quiz.question-guidance:Keep each question grounded in backend project experience.}") String questionGuidance
    ) {
        this.questionGuidance = questionGuidance;
    }

    public String build(List<Material> materials, String questionType, int difficulty, int count, boolean interviewMode) {
        String materialNames = materials.stream().map(Material::name).toList().toString();
        return "Generate " + count + " " + questionType + " questions. Materials=" + materialNames
                + ", difficulty=" + difficulty + ", interviewMode=" + interviewMode
                + ". " + questionGuidance + " "
                + "Return ONLY strict JSON with schema: "
                + "{\"summary\":\"string\",\"questions\":[{\"stem\":\"string\",\"referenceAnswer\":\"string\",\"analysis\":\"string\"}]}. "
                + "Do not use markdown and do not output any extra keys.";
    }
}
