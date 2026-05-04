package com.interview.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenCommand(
        @NotBlank @Size(min = 1, max = 4096) String refreshToken
) {
}
