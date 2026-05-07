package com.interview.application.dto;

import jakarta.validation.constraints.Size;

public record LogoutCommand(
        @Size(max = 4096) String refreshToken
) {
}
