package com.interview.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final long tokenExpireMs;

    @Autowired
    public TokenBlacklistService(
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.expire-ms:86400000}") long tokenExpireMs
    ) {
        this.redisTemplate = redisTemplate;
        this.tokenExpireMs = tokenExpireMs;
    }

    public void addToBlacklist(String token, long expireMs) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", expireMs, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
