package com.interview.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final long lockoutDurationMinutes;
    private final long attemptWindowMinutes;

    private final Map<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lockout-minutes:30}") long lockoutDurationMinutes,
            @Value("${app.security.login.attempt-window-minutes:10}") long attemptWindowMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
        this.attemptWindowMinutes = attemptWindowMinutes;
    }

    public boolean isBlocked(String username) {
        LoginAttempt attempt = attempts.get(username.toLowerCase());
        if (attempt == null) {
            return false;
        }

        if (attempt.isLocked()) {
            if (attempt.isLockExpired(lockoutDurationMinutes)) {
                attempts.remove(username.toLowerCase());
                return false;
            }
            return true;
        }

        return false;
    }

    public void recordFailedAttempt(String username) {
        String key = username.toLowerCase();
        LoginAttempt attempt = attempts.computeIfAbsent(key, k -> new LoginAttempt());
        attempt.recordFailure(attemptWindowMinutes);

        if (attempt.getFailureCount() >= maxAttempts) {
            attempt.lock();
        }
    }

    public void recordSuccessfulAttempt(String username) {
        attempts.remove(username.toLowerCase());
    }

    public long getRemainingLockoutMinutes(String username) {
        LoginAttempt attempt = attempts.get(username.toLowerCase());
        if (attempt != null && attempt.isLocked() && !attempt.isLockExpired(lockoutDurationMinutes)) {
            return attempt.getRemainingLockoutMinutes(lockoutDurationMinutes);
        }
        return 0;
    }

    private static class LoginAttempt {
        private int failureCount = 0;
        private Instant firstAttemptTime = Instant.now();
        private Instant lockTime;

        void recordFailure(long attemptWindowMinutes) {
            Instant now = Instant.now();
            if (Duration.between(firstAttemptTime, now).toMinutes() > attemptWindowMinutes) {
                failureCount = 0;
                firstAttemptTime = now;
            }
            failureCount++;
        }

        int getFailureCount() {
            return failureCount;
        }

        void lock() {
            this.lockTime = Instant.now();
        }

        boolean isLocked() {
            return lockTime != null;
        }

        boolean isLockExpired(long lockoutDurationMinutes) {
            if (lockTime == null) {
                return true;
            }
            return Duration.between(lockTime, Instant.now()).toMinutes() >= lockoutDurationMinutes;
        }

        long getRemainingLockoutMinutes(long lockoutDurationMinutes) {
            if (lockTime == null) {
                return 0;
            }
            long elapsed = Duration.between(lockTime, Instant.now()).toMinutes();
            return Math.max(0, lockoutDurationMinutes - elapsed);
        }
    }
}
