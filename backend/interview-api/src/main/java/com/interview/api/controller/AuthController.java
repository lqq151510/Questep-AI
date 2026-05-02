package com.interview.api.controller;

import com.interview.application.dto.LoginCommand;
import com.interview.application.dto.LoginResult;
import com.interview.application.dto.RegisterCommand;
import com.interview.application.service.AuthApplicationService;
import com.interview.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthApplicationService authApplicationService;
    public AuthController(AuthApplicationService authApplicationService) { this.authApplicationService = authApplicationService; }
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginCommand command) { return ApiResponse.ok(authApplicationService.login(command)); }

    @PostMapping("/register")
    public ApiResponse<LoginResult> register(@Valid @RequestBody RegisterCommand command) {
        return ApiResponse.ok(authApplicationService.register(command));
    }
}
