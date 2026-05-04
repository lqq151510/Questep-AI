package com.interview.api.controller;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.LogoutCommand;
import com.interview.application.dto.RefreshTokenCommand;
import com.interview.application.dto.RegisterCommand;
import com.interview.application.service.AuthApplicationService;
import com.interview.application.service.TokenBlacklistService;
import com.interview.application.service.TokenService;
import com.interview.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthApplicationService authApplicationService;
    private final TokenBlacklistService tokenBlacklistService;
    private final TokenService tokenService;

    public AuthController(
            AuthApplicationService authApplicationService,
            TokenBlacklistService tokenBlacklistService,
            TokenService tokenService
    ) {
        this.authApplicationService = authApplicationService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.tokenService = tokenService;
    }

    @Operation(summary = "User login")
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginCommand command) {
        return ApiResponse.ok(authApplicationService.login(command));
    }

    @Operation(summary = "User registration")
    @PostMapping("/register")
    public ApiResponse<LoginResult> register(@Valid @RequestBody RegisterCommand command) {
        return ApiResponse.ok(authApplicationService.register(command));
    }

    @Operation(summary = "Refresh JWT tokens")
    @PostMapping("/refresh")
    public ApiResponse<LoginResult> refresh(@Valid @RequestBody RefreshTokenCommand command) {
        return ApiResponse.ok(authApplicationService.refresh(command.refreshToken()));
    }

    @Operation(summary = "User logout", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String auth,
            @RequestBody(required = false) @Valid LogoutCommand command
    ) {
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            blacklistToken(token);
        }
        if (command != null && StringUtils.hasText(command.refreshToken())) {
            blacklistToken(command.refreshToken().trim());
        }
        return ApiResponse.ok(null);
    }

    private void blacklistToken(String token) {
        try {
            long expireTime = tokenService.getExpireTime(token);
            if (expireTime > 0) {
                tokenBlacklistService.addToBlacklist(token, expireTime);
            }
        } catch (Exception ignored) {
            // If token is invalid, just ignore and return success
        }
    }
}
