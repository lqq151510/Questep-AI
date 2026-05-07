package com.interview.application.service;

public interface TokenService {
    String generateToken(Long userId, String username);
    String generateRefreshToken(Long userId, String username);
    Long parseUserId(String token);
    Long parseRefreshUserId(String token);
    long getExpireTime(String token);
    long accessTokenExpireMs();
}
