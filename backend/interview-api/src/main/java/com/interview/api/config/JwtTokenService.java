package com.interview.api.config;

import com.interview.application.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService implements TokenService {
    private final SecretKey key;
    private final long expiryMs;
    public JwtTokenService(@Value("${app.jwt.secret:}") String secret,
                           @Value("${app.jwt.expire-ms:86400000}") long expiryMs) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }
        if ("change-this-secret-change-this-secret".equals(secret)) {
            throw new IllegalStateException("app.jwt.secret must not use the sample value");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryMs;
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
}
