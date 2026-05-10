package com.interview.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank @Size(max = 4000) String message,
        @Size(max = 20) List<ChatMessage> context
) {
}
