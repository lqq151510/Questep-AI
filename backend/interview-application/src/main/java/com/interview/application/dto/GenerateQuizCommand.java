package com.interview.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GenerateQuizCommand(
        @NotEmpty List<Long> materialIds,
        @NotBlank String questionType,
        @Min(1) @Max(5) Integer difficulty,
        @Min(1) @Max(10) Integer count,
        Boolean interviewMode
) {
}
