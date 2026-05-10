package com.interview.api.controller;

import com.interview.api.support.CurrentUser;
import com.interview.application.dto.UpdateUserLlmSettingsCommand;
import com.interview.application.dto.UserLlmSettingsView;
import com.interview.application.service.UserLlmSettingsApplicationService;
import com.interview.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/llm/settings")
public class UserLlmSettingsController {

    private final UserLlmSettingsApplicationService applicationService;

    public UserLlmSettingsController(UserLlmSettingsApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ApiResponse<UserLlmSettingsView> get() {
        return ApiResponse.ok(applicationService.get(CurrentUser.id()));
    }

    @PutMapping
    public ApiResponse<UserLlmSettingsView> update(@Valid @RequestBody UpdateUserLlmSettingsCommand command) {
        return ApiResponse.ok(applicationService.update(CurrentUser.id(), command));
    }
}
