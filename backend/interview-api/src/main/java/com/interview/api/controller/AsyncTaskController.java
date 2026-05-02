package com.interview.api.controller;

import com.interview.application.service.AsyncTaskApplicationService;
import com.interview.common.api.ApiResponse;
import com.interview.domain.model.AsyncTaskRecord;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/async-tasks")
public class AsyncTaskController {
    private final AsyncTaskApplicationService service;
    public AsyncTaskController(AsyncTaskApplicationService service) { this.service = service; }
    @GetMapping("/{taskNo}")
    public ApiResponse<AsyncTaskRecord> get(@PathVariable String taskNo) { return ApiResponse.ok(service.getByTaskNo(taskNo)); }
}
