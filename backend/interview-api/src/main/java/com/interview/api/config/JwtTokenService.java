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

    private final SecretKey key;
    private final long expiryMs;
    public JwtTokenService(@Value("${app.jwt.secret}") String secret,
                           @Value("${app.jwt.expire-ms:86400000}") long expiryMs,
                           Environment environment) {
        validateSecret(secret, environment);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryMs;
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
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(userId)).claim("username", username)
                .issuedAt(now).expiration(new Date(now.getTime() + expiryMs)).signWith(key).compact();
    }
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(claims.getSubject());
    }
    public long getExpireTime(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}
