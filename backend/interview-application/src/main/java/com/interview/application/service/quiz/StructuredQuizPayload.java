package com.interview.application.service.quiz;

import java.util.List;

public record StructuredQuizPayload(String summary, List<QuestionDraft> questions) {
}
