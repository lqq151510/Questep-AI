package com.interview.application.service;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.RegisterCommand;
import com.interview.common.exception.UnauthorizedException;
import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private CaptchaService captchaService;

    @InjectMocks
    private AuthApplicationService authApplicationService;

    private static final String CAPTCHA_ID = "captcha-id";
    private static final String CAPTCHA_CODE = "1234";
    private User testUser;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testPassword = "testPassword123";
        LocalDateTime now = LocalDateTime.now();
        testUser = new User(1L, "testUser", "test@example.com", "encodedPasswordHash", 1, now, now);
    }

    @Test
    @DisplayName("Test successful login with valid credentials")
    void testLoginSuccess() {
        LoginCommand command = new LoginCommand("testUser", testPassword);

        when(loginAttemptService.isBlocked("testUser")).thenReturn(false);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, "encodedPasswordHash")).thenReturn(true);
        when(tokenService.generateToken(1L, "testUser")).thenReturn("testToken123");
        when(tokenService.generateRefreshToken(1L, "testUser")).thenReturn("refreshToken123");
        when(tokenService.accessTokenExpireMs()).thenReturn(3_600_000L);

        LoginResult result = authApplicationService.login(command);

        assertNotNull(result);
        assertEquals("testToken123", result.token());
        assertEquals("refreshToken123", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(3600L, result.expiresInSeconds());
        verify(loginAttemptService, times(1)).isBlocked("testUser");
        verify(userRepository, times(1)).findByUsername("testUser");
        verify(passwordEncoder, times(1)).matches(testPassword, "encodedPasswordHash");
        verify(tokenService, times(1)).generateToken(1L, "testUser");
        verify(tokenService, times(1)).generateRefreshToken(1L, "testUser");
        verify(loginAttemptService, times(1)).recordSuccessfulAttempt("testUser");
    }

    @Test
    @DisplayName("Test login fails when user is blocked")
    void testLoginBlocked() {
        LoginCommand command = new LoginCommand("testUser", testPassword);

        when(loginAttemptService.isBlocked("testUser")).thenReturn(true);
        when(loginAttemptService.getRemainingLockoutMinutes("testUser")).thenReturn(15L);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("账户已锁定，请15分钟后重试", exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Test login fails when user not found")
    void testLoginUserNotFound() {
        LoginCommand command = new LoginCommand("nonexistent", testPassword);

        when(loginAttemptService.isBlocked("nonexistent")).thenReturn(false);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(loginAttemptService, times(1)).recordFailedAttempt("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Test login fails with invalid password")
    void testLoginInvalidPassword() {
        LoginCommand command = new LoginCommand("testUser", "wrongPassword");

        when(loginAttemptService.isBlocked("testUser")).thenReturn(false);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPasswordHash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(loginAttemptService, times(1)).recordFailedAttempt("testUser");
    }

    @Test
    @DisplayName("Test login fails when user is disabled")
    void testLoginUserDisabled() {
        LocalDateTime now = LocalDateTime.now();
        User disabledUser = new User(2L, "disabledUser", "disabled@example.com", "encodedPassword", 0, now, now);
        LoginCommand command = new LoginCommand("disabledUser", testPassword);

        when(loginAttemptService.isBlocked("disabledUser")).thenReturn(false);
        when(userRepository.findByUsername("disabledUser")).thenReturn(Optional.of(disabledUser));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(loginAttemptService, times(1)).recordFailedAttempt("disabledUser");
    }

    @Test
    @DisplayName("Test successful registration with new user")
    void testRegisterSuccess() {
        RegisterCommand command = new RegisterCommand("newUser", "new@example.com", "Password123!", CAPTCHA_ID, CAPTCHA_CODE);

        when(captchaService.validate(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedNewPassword");
        when(userRepository.save("newUser", "new@example.com", "encodedNewPassword"))
                .thenReturn(new User(3L, "newUser", "new@example.com", "encodedNewPassword", 1,
                        LocalDateTime.now(), LocalDateTime.now()));
        when(tokenService.generateToken(3L, "newUser")).thenReturn("newUserToken");
        when(tokenService.generateRefreshToken(3L, "newUser")).thenReturn("newUserRefreshToken");
        when(tokenService.accessTokenExpireMs()).thenReturn(3_600_000L);

        LoginResult result = authApplicationService.register(command);

        assertNotNull(result);
        assertEquals("newUserToken", result.token());
        assertEquals("newUserRefreshToken", result.refreshToken());
        verify(userRepository, times(1)).findByUsername("newUser");
        verify(userRepository, times(1)).findByEmail("new@example.com");
        verify(userRepository, times(1)).save("newUser", "new@example.com", "encodedNewPassword");
    }

    @Test
    @DisplayName("Test registration fails when username already exists")
    void testRegisterUsernameExists() {
        RegisterCommand command = new RegisterCommand("existingUser", "new@example.com", "password123", "cid", "1234");

        when(captchaService.validate(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsername("existingUser")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authApplicationService.register(command)
        );

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Test registration fails when email already exists")
    void testRegisterEmailExists() {
        RegisterCommand command = new RegisterCommand("newUser", "existing@example.com", "password123", "cid", "1234");

        when(captchaService.validate(anyString(), anyString())).thenReturn(true);
        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authApplicationService.register(command)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Test refresh returns new tokens for active user")
    void testRefreshSuccess() {
        when(tokenService.parseRefreshUserId("refreshToken")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tokenService.generateToken(1L, "testUser")).thenReturn("newAccessToken");
        when(tokenService.generateRefreshToken(1L, "testUser")).thenReturn("newRefreshToken");
        when(tokenService.accessTokenExpireMs()).thenReturn(3_600_000L);

        LoginResult result = authApplicationService.refresh("refreshToken");

        assertEquals("newAccessToken", result.token());
        assertEquals("newRefreshToken", result.refreshToken());
        assertEquals("Bearer", result.tokenType());
    }

    @Test
    @DisplayName("Test refresh fails when token is blank")
    void testRefreshBlankToken() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> authApplicationService.refresh("  ")
        );
        assertEquals("Refresh token is required", exception.getMessage());
    }
}
