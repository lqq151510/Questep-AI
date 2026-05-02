package com.interview.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
        @NotBlank String username,
        @NotBlank String password
) {
}
