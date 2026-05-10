package com.interview.application.dto;

import com.interview.domain.model.Question;

import java.util.List;

public record GeneratedQuizResult(
        String traceId,
        String modelBrief,
        List<Question> questions,
        boolean fallbackUsed,
        int invalidCount,
        List<String> warnings
) {
    public GeneratedQuizResult {
        if (warnings == null) {
            warnings = List.of();
        }
    }
}
