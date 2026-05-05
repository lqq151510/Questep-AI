package com.interview.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService(3, 30, 10);
    }

    @Test
    @DisplayName("Test new user is not blocked")
    void testNewUserNotBlocked() {
        assertFalse(loginAttemptService.isBlocked("newUser"));
    }

    @Test
    @DisplayName("Test user is blocked after max failed attempts")
    void testUserBlockedAfterMaxAttempts() {
        String username = "testUser";

        assertFalse(loginAttemptService.isBlocked(username));

        loginAttemptService.recordFailedAttempt(username);
        assertFalse(loginAttemptService.isBlocked(username));

        loginAttemptService.recordFailedAttempt(username);
        assertFalse(loginAttemptService.isBlocked(username));

        loginAttemptService.recordFailedAttempt(username);
        assertTrue(loginAttemptService.isBlocked(username));
    }

    @Test
    @DisplayName("Test successful login resets failed attempts")
    void testSuccessfulLoginResetsAttempts() {
        String username = "testUser";

        loginAttemptService.recordFailedAttempt(username);
        loginAttemptService.recordFailedAttempt(username);

        loginAttemptService.recordSuccessfulAttempt(username);

        assertFalse(loginAttemptService.isBlocked(username));

        loginAttemptService.recordFailedAttempt(username);
        assertFalse(loginAttemptService.isBlocked(username));
    }

    @Test
    @DisplayName("Test remaining lockout minutes when blocked")
    void testRemainingLockoutMinutes() {
        String username = "testUser";

        loginAttemptService.recordFailedAttempt(username);
        loginAttemptService.recordFailedAttempt(username);
        loginAttemptService.recordFailedAttempt(username);

        assertTrue(loginAttemptService.isBlocked(username));
        assertTrue(loginAttemptService.getRemainingLockoutMinutes(username) > 0);
        assertTrue(loginAttemptService.getRemainingLockoutMinutes(username) <= 30);
    }

    @Test
    @DisplayName("Test username is case-insensitive")
    void testUsernameCaseInsensitive() {
        loginAttemptService.recordFailedAttempt("TestUser");
        loginAttemptService.recordFailedAttempt("testuser");
        loginAttemptService.recordFailedAttempt("TESTUSER");

        assertTrue(loginAttemptService.isBlocked("TestUser"));
        assertTrue(loginAttemptService.isBlocked("testuser"));
        assertTrue(loginAttemptService.isBlocked("TESTUSER"));
    }

    @Test
    @DisplayName("Test remaining minutes returns 0 when not blocked")
    void testRemainingMinutesWhenNotBlocked() {
        assertEquals(0, loginAttemptService.getRemainingLockoutMinutes("anyUser"));
    }
}
