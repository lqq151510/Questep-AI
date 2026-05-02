package com.interview.application.dto;

import com.interview.domain.model.Question;

import java.util.List;

public record GeneratedQuizResult(
        String traceId,
        String modelBrief,
        List<Question> questions
) {
}
