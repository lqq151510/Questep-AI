package com.interview.application.service;

public interface TokenService {
    String generateToken(Long userId, String username);
    Long parseUserId(String token);
}
