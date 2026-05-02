package com.interview.api.controller;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
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

    @Operation(summary = "User logout", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Parameter(hidden = true) Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String auth
    ) {
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            try {
                long expireTime = tokenService.getExpireTime(token);
                if (expireTime > 0) {
                    tokenBlacklistService.addToBlacklist(token, expireTime);
                }
            } catch (Exception ignored) {
                // If token is invalid, just ignore and return success
            }
        }
        return ApiResponse.ok(null);
    }
}
