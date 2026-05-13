package com.interview.api.controller;

import com.interview.application.service.EvalService;
import com.interview.common.api.ApiResponse;
import com.interview.domain.model.EvalCase;
import com.interview.domain.model.EvalRun;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eval")
public class EvalController {

    private final EvalService evalService;

    public EvalController(EvalService evalService) {
        this.evalService = evalService;
    }

    @PostMapping("/runs")
    public ApiResponse<EvalRun> triggerEval() {
        String runKey = "eval-" + UUID.randomUUID().toString().substring(0, 8);
        return ApiResponse.ok(evalService.runEval(runKey));
    }

    @GetMapping("/runs")
    public ApiResponse<List<EvalRun>> listRuns() {
        return ApiResponse.ok(evalService.listRuns());
    }

    @GetMapping("/runs/{runKey}")
    public ApiResponse<EvalRun> getRun(@PathVariable String runKey) {
        EvalRun run = evalService.getRun(runKey);
        if (run == null) {
            return ApiResponse.fail("Run not found: " + runKey);
        }
        return ApiResponse.ok(run);
    }

    @GetMapping("/cases")
    public ApiResponse<List<EvalCase>> listCases() {
        return ApiResponse.ok(evalService.listCases());
    }
}
