package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.EvalCase;
import com.interview.domain.repository.EvalCaseRepository;
import com.interview.infrastructure.persistence.mapper.EvalCaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EvalCaseRepositoryImpl implements EvalCaseRepository {

    private final EvalCaseMapper evalCaseMapper;

    public EvalCaseRepositoryImpl(EvalCaseMapper evalCaseMapper) {
        this.evalCaseMapper = evalCaseMapper;
    }

    @Override
    public List<EvalCase> findActive() {
        return evalCaseMapper.selectActive();
    }

    @Override
    public Optional<EvalCase> findById(Long id) {
        return Optional.ofNullable(evalCaseMapper.selectById(id));
    }
}
