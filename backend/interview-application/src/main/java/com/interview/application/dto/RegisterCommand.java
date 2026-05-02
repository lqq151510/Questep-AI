package com.interview.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCommand(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Email @Size(max = 128) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
