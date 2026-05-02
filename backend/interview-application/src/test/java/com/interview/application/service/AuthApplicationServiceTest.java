package com.interview.application.service;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.RegisterCommand;
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

    @InjectMocks
    private AuthApplicationService authApplicationService;

    private User testUser;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testPassword = "testPassword123";
        testUser = new User(1L, "testUser", "encodedPasswordHash", 1);
    }

    @Test
    @DisplayName("Test successful login with valid credentials")
    void testLoginSuccess() {
        LoginCommand command = new LoginCommand("testUser", testPassword);

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testPassword, "encodedPasswordHash")).thenReturn(true);
        when(tokenService.generateToken(1L, "testUser")).thenReturn("testToken123");

        LoginResult result = authApplicationService.login(command);

        assertNotNull(result);
        assertEquals("testToken123", result.token());
        assertEquals("Bearer", result.tokenType());
        verify(userRepository, times(1)).findByUsername("testUser");
        verify(passwordEncoder, times(1)).matches(testPassword, "encodedPasswordHash");
        verify(tokenService, times(1)).generateToken(1L, "testUser");
    }

    @Test
    @DisplayName("Test login fails when user not found")
    void testLoginUserNotFound() {
        LoginCommand command = new LoginCommand("nonexistent", testPassword);

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Test login fails with invalid password")
    void testLoginInvalidPassword() {
        LoginCommand command = new LoginCommand("testUser", "wrongPassword");

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPasswordHash")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    @DisplayName("Test login fails when user is disabled")
    void testLoginUserDisabled() {
        User disabledUser = new User(2L, "disabledUser", "encodedPassword", 0);
        LoginCommand command = new LoginCommand("disabledUser", testPassword);

        when(userRepository.findByUsername("disabledUser")).thenReturn(Optional.of(disabledUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authApplicationService.login(command)
        );

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    @DisplayName("Test successful registration with new user")
    void testRegisterSuccess() {
        RegisterCommand command = new RegisterCommand("newUser", "new@example.com", "password123");

        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encodedNewPassword");
        when(userRepository.save("newUser", "new@example.com", "encodedNewPassword"))
                .thenReturn(new User(3L, "newUser", "encodedNewPassword", 1));
        when(tokenService.generateToken(3L, "newUser")).thenReturn("newUserToken");

        LoginResult result = authApplicationService.register(command);

        assertNotNull(result);
        assertEquals("newUserToken", result.token());
        verify(userRepository, times(1)).findByUsername("newUser");
        verify(userRepository, times(1)).findByEmail("new@example.com");
        verify(userRepository, times(1)).save("newUser", "new@example.com", "encodedNewPassword");
    }

    @Test
    @DisplayName("Test registration fails when username already exists")
    void testRegisterUsernameExists() {
        RegisterCommand command = new RegisterCommand("existingUser", "new@example.com", "password123");

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
        RegisterCommand command = new RegisterCommand("newUser", "existing@example.com", "password123");

        when(userRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authApplicationService.register(command)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(anyString(), anyString(), anyString());
    }
}
