package com.interview.application.dto;

public record LoginResult(
        String token,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
}
