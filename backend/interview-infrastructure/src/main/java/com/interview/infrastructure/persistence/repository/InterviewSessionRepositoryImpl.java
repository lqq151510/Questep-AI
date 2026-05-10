package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.InterviewSession;
import com.interview.domain.repository.InterviewSessionRepository;
import com.interview.infrastructure.persistence.entity.InterviewSessionPO;
import com.interview.infrastructure.persistence.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class InterviewSessionRepositoryImpl implements InterviewSessionRepository {

    private final InterviewSessionMapper mapper;

    public InterviewSessionRepositoryImpl(InterviewSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InterviewSession save(InterviewSession session) {
        InterviewSessionPO po = new InterviewSessionPO();
        po.setUserId(session.userId());
        po.setPosition(session.position());
        po.setDifficulty(session.difficulty());
        po.setStatus(session.status());
        po.setContextSnapshot(session.contextSnapshot());
        mapper.insert(po);
        return mapper.selectById(po.getId());
    }

    @Override
    public Optional<InterviewSession> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<InterviewSession> findActiveByUserId(Long userId) {
        return Optional.ofNullable(mapper.selectActiveByUserId(userId));
    }

    @Override
    public void updateStatus(Long id, String status, String contextSnapshot) {
        mapper.updateStatusAndSnapshot(id, status, contextSnapshot);
    }
}
