package com.interview.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMasteryCommand(
        @NotNull(message = "masteryStatus is required")
        String masteryStatus
) {
}
