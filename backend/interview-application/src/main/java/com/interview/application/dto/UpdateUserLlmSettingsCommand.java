package com.interview.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserLlmSettingsCommand(
        @NotBlank @Size(max = 32) String providerName,
        @NotBlank @Size(max = 128) String modelName,
        @Size(max = 512) String baseUrl,
        @Size(max = 255) String apiKey,
        Boolean enabled
) {
}
