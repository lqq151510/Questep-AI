package com.interview.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMaterialCommand(
        @NotNull Long userId,
        @NotBlank String name,
        @NotBlank String fileType
) {
}
