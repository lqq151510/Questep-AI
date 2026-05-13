package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.EvalResult;
import com.interview.domain.repository.EvalResultRepository;
import com.interview.infrastructure.persistence.mapper.EvalResultMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EvalResultRepositoryImpl implements EvalResultRepository {

    private final EvalResultMapper evalResultMapper;

    public EvalResultRepositoryImpl(EvalResultMapper evalResultMapper) {
        this.evalResultMapper = evalResultMapper;
    }

    @Override
    public void save(EvalResult result) {
        evalResultMapper.insert(result);
    }

    @Override
    public List<EvalResult> findByRunId(Long runId) {
        return evalResultMapper.selectByRunId(runId);
    }
}
