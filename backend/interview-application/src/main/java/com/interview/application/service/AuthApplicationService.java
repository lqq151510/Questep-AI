package com.interview.application.service;

import com.interview.application.dto.CaptchaResponse;
import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.RegisterCommand;
import com.interview.common.exception.UnauthorizedException;
import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final CaptchaService captchaService;

    public AuthApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService, LoginAttemptService loginAttemptService, CaptchaService captchaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.captchaService = captchaService;
    }

    public LoginResult login(LoginCommand command) {
        if (loginAttemptService.isBlocked(command.username())) {
            long remainingMinutes = loginAttemptService.getRemainingLockoutMinutes(command.username());
            throw new UnauthorizedException("账户已锁定，请" + remainingMinutes + "分钟后重试");
        }

        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> {
                    loginAttemptService.recordFailedAttempt(command.username());
                    return new UnauthorizedException("用户名或密码错误");
                });

        if (user.status() == null || user.status() != User.STATUS_ACTIVE || !passwordEncoder.matches(command.password(), user.passwordHash())) {
            loginAttemptService.recordFailedAttempt(command.username());
            throw new UnauthorizedException("用户名或密码错误");
        }

        loginAttemptService.recordSuccessfulAttempt(command.username());
        return issueTokens(user);
    }

    @Transactional
    public LoginResult register(RegisterCommand command) {
        if (!captchaService.validate(command.captchaId(), command.captchaCode())) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        if (userRepository.findByUsername(command.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.findByEmail(command.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

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
}
