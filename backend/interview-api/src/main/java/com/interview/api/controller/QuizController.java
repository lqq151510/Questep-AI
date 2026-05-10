package com.interview.api.controller;

import com.interview.api.support.CurrentUser;
import com.interview.application.dto.GenerateQuizCommand;
import com.interview.application.dto.GeneratedQuizResult;
import com.interview.application.service.QuizApplicationService;
import com.interview.common.api.ApiResponse;
import com.interview.domain.model.Question;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quizzes")
public class QuizController {

    private final QuizApplicationService quizApplicationService;

    public QuizController(QuizApplicationService quizApplicationService) {
        this.quizApplicationService = quizApplicationService;
    }

    @PostMapping("/generate")
    public ApiResponse<GeneratedQuizResult> generate(@Valid @RequestBody GenerateQuizCommand command) {
        return ApiResponse.ok(quizApplicationService.generate(CurrentUser.id(), command));
    }

    @GetMapping("/questions")
    public ApiResponse<List<Question>> recent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (pageSize < 1 || pageSize > 50) {
            throw new IllegalArgumentException("pageSize must be between 1 and 50");
        }
        return ApiResponse.ok(quizApplicationService.recent(CurrentUser.id(), page, pageSize));
    }
}
