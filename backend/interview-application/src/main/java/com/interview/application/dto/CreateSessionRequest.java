package com.interview.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateSessionRequest(
        String position,
        @Min(1) @Max(5) Integer difficulty
) {
}
