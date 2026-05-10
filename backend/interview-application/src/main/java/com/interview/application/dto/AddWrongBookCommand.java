package com.interview.application.dto;

import jakarta.validation.constraints.NotNull;

public record AddWrongBookCommand(
        @NotNull(message = "questionId is required")
        Long questionId
) {
}
