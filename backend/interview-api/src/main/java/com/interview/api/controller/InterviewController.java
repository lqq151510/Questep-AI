package com.interview.api.controller;

import com.interview.api.support.CurrentUser;
import com.interview.application.dto.CreateSessionRequest;
import com.interview.common.api.ApiResponse;
import com.interview.common.exception.ResourceNotFoundException;
import com.interview.domain.model.InterviewSession;
import com.interview.domain.repository.InterviewSessionRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {

    private final InterviewSessionRepository interviewSessionRepository;

    public InterviewController(InterviewSessionRepository interviewSessionRepository) {
        this.interviewSessionRepository = interviewSessionRepository;
    }

    @PostMapping("/sessions")
    public ApiResponse<InterviewSession> createOrGetSession(@Valid @RequestBody CreateSessionRequest request) {
        Long userId = CurrentUser.id();
        Optional<InterviewSession> active = interviewSessionRepository.findActiveByUserId(userId);
        if (active.isPresent()) {
            return ApiResponse.ok(active.get());
        }
        InterviewSession session = new InterviewSession(
                null,
                userId,
                request.position(),
                request.difficulty(),
                "ACTIVE",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        InterviewSession saved = interviewSessionRepository.save(session);
        return ApiResponse.ok(saved);
    }

    @GetMapping("/sessions/active")
    public ApiResponse<InterviewSession> getActiveSession() {
        Long userId = CurrentUser.id();
        return interviewSessionRepository.findActiveByUserId(userId)
                .map(ApiResponse::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No active interview session found"));
    }

    @PostMapping("/{id}/resume")
    public ApiResponse<InterviewSession> resumeSession(@PathVariable Long id) {
        Long userId = CurrentUser.id();
        InterviewSession session = interviewSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found: " + id));
        if (!session.userId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to the current user");
        }
        if ("COMPLETED".equals(session.status())) {
            throw new IllegalArgumentException("Cannot resume a completed session");
        }
        String newStatus = "PAUSED".equals(session.status()) ? "ACTIVE" : session.status();
        interviewSessionRepository.updateStatus(session.id(), newStatus, session.contextSnapshot());
        InterviewSession resumed = interviewSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Interview session not found after resume: " + id));
        return ApiResponse.ok(resumed);
    }
}
