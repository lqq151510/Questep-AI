package com.interview.application.dto;

import com.interview.domain.model.Question;
import com.interview.domain.model.WrongBook;

public record WrongBookItem(
        Long id,
        Long questionId,
        String question,
        String questionType,
        String referenceAnswer,
        String analysisText,
        Integer difficulty,
        String masteryStatus,
        Integer wrongCount,
        Integer reviewCount,
        String notes
) {
    public static WrongBookItem from(WrongBook wrongBook, Question question) {
        return new WrongBookItem(
                wrongBook.id(),
                wrongBook.questionId(),
                question != null ? question.stemText() : "",
                question != null ? question.questionType() : "",
                question != null ? question.referenceAnswer() : "",
                question != null ? question.analysisText() : "",
                question != null ? question.difficulty() : null,
                wrongBook.masteryStatus(),
                wrongBook.wrongCount(),
                wrongBook.wrongCount(),
                wrongBook.notes()
        );
    }
}
