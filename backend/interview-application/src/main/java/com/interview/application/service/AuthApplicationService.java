package com.interview.application.service;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.RegisterCommand;
import com.interview.common.exception.UnauthorizedException;
import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
public class AuthApplicationService {

    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWER_CASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR = Pattern.compile("[^A-Za-z0-9]");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
        if (user.status() == null || user.status() != User.STATUS_ACTIVE || !passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public LoginResult register(RegisterCommand command) {
        if (userRepository.findByUsername(command.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        validatePasswordStrength(command.password());

        User user = userRepository.save(
                command.username(),
                command.email(),
                passwordEncoder.encode(command.password())
        );
        return issueTokens(user);
    }

    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }
        Long userId = tokenService.parseRefreshUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (user.status() == null || user.status() != User.STATUS_ACTIVE) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        return issueTokens(user);
    }

    private LoginResult issueTokens(User user) {
        String accessToken = tokenService.generateToken(user.id(), user.username());
        String refreshToken = tokenService.generateRefreshToken(user.id(), user.username());
        long expiresInSeconds = Math.max(1L, tokenService.accessTokenExpireMs() / 1000L);
        return new LoginResult(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }

    private void validatePasswordStrength(String password) {
        int categories = 0;
        if (UPPER_CASE.matcher(password).find()) categories++;
        if (LOWER_CASE.matcher(password).find()) categories++;
        if (DIGIT.matcher(password).find()) categories++;
        if (SPECIAL_CHAR.matcher(password).find()) categories++;

        if (categories < 3) {
            throw new IllegalArgumentException(
                    "Password must contain at least 3 of the following: uppercase letters, lowercase letters, digits, special characters");
        }
    }
}
