package com.interview.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCommand(
        @NotBlank @Size(min = 1, max = 64) String username,
        @NotBlank @Size(min = 1, max = 72) String password
) {
}
