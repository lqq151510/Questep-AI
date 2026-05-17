package com.interview.application.service.quiz;

public record QuestionDraft(String stemText, String optionsJson, String referenceAnswer, String analysisText) {
}
