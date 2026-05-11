package com.interview.api.controller;

import com.interview.api.support.CurrentUser;
import com.interview.application.service.AsyncTaskApplicationService;
import com.interview.common.api.ApiResponse;
import com.interview.domain.model.AsyncTaskRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/async-tasks")
public class AsyncTaskController {
    private static final Pattern TASK_NO_PATTERN = Pattern.compile("^PARSE-[a-f0-9]+$");

    private final AsyncTaskApplicationService service;
    public AsyncTaskController(AsyncTaskApplicationService service) { this.service = service; }
    @GetMapping("/{taskNo}")
    public ApiResponse<AsyncTaskRecord> get(@PathVariable String taskNo) {
        if (!TASK_NO_PATTERN.matcher(taskNo).matches()) {
            throw new IllegalArgumentException("Invalid task number format");
        }
        return ApiResponse.ok(service.getByTaskNo(CurrentUser.id(), taskNo));
    }
}
