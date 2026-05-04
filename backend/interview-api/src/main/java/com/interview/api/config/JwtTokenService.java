package com.interview.api.config;

import com.interview.application.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;

@Service
public class JwtTokenService implements TokenService {
    private static final int MIN_SECRET_LENGTH = 32;
    private static final String SAMPLE_SECRET = "change-this-secret-change-this-secret";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryMs;
    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.expire-ms:86400000}") long accessExpiryMs,
                           @Value("${app.jwt.refresh-expire-ms:604800000}") long refreshExpiryMs,
                           Environment environment) {
        validateSecret(secret, environment);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryMs = refreshExpiryMs;
    }

    private void validateSecret(String secret, Environment environment) {
        boolean isDevProfile = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (secret == null || secret.isBlank()) {
            if (isDevProfile) {
                throw new IllegalStateException("app.jwt.secret must be configured. Set JWT_SECRET environment variable.");
            }
            throw new IllegalStateException("FATAL: app.jwt.secret is not configured. Application cannot start without JWT_SECRET.");
        }
        if (SAMPLE_SECRET.equals(secret)) {
            if (isDevProfile) {
                throw new IllegalStateException("app.jwt.secret must not use the sample value even in dev profile.");
            }
            throw new IllegalStateException("FATAL: app.jwt.secret uses the default sample value. Generate a secure secret and set JWT_SECRET.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " bytes for HS256, current: "
                            + secret.getBytes(StandardCharsets.UTF_8).length + " bytes");
        }
    }
    @Override
    public String generateToken(Long userId, String username) {
        return buildToken(userId, username, ACCESS_TOKEN_TYPE, accessExpiryMs);
    }

    @Override
    public String generateRefreshToken(Long userId, String username) {
        return buildToken(userId, username, REFRESH_TOKEN_TYPE, refreshExpiryMs);
    }

    private String buildToken(Long userId, String username, String tokenType, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    @Override
    public Long parseUserId(String token) {
        return parseUserIdByType(token, ACCESS_TOKEN_TYPE);
    }

    @Override
    public Long parseRefreshUserId(String token) {
        return parseUserIdByType(token, REFRESH_TOKEN_TYPE);
    }

    private Long parseUserIdByType(String token, String expectedType) {
        Claims claims = parseClaims(token);
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedType.equals(tokenType)) {
            throw new IllegalArgumentException("Unexpected token type");
        }
        return Long.valueOf(claims.getSubject());
    }

    @Override
    public long getExpireTime(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }

    @Override
    public long accessTokenExpireMs() {
        return accessExpiryMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
