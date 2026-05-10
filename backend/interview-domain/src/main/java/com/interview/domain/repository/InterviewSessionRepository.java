package com.interview.domain.repository;

import com.interview.domain.model.InterviewSession;
import java.util.Optional;

public interface InterviewSessionRepository {
    InterviewSession save(InterviewSession session);
    Optional<InterviewSession> findById(Long id);
    Optional<InterviewSession> findActiveByUserId(Long userId);
    void updateStatus(Long id, String status, String contextSnapshot);
}
