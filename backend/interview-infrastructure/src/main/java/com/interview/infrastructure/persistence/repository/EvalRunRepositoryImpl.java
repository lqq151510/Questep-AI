package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.EvalRun;
import com.interview.domain.repository.EvalRunRepository;
import com.interview.infrastructure.persistence.mapper.EvalRunMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EvalRunRepositoryImpl implements EvalRunRepository {

    private final EvalRunMapper evalRunMapper;

    public EvalRunRepositoryImpl(EvalRunMapper evalRunMapper) {
        this.evalRunMapper = evalRunMapper;
    }

    @Override
    public EvalRun save(EvalRun run) {
        evalRunMapper.insert(run);
        return evalRunMapper.selectById(run.id());
    }

    @Override
    public Optional<EvalRun> findByRunKey(String runKey) {
        return Optional.ofNullable(evalRunMapper.selectByRunKey(runKey));
    }

    @Override
    public List<EvalRun> findAll() {
        return evalRunMapper.selectAll();
    }

    @Override
    public void updateFinished(Long id, int totalCases, int passedCases, double avgScore, String status) {
        evalRunMapper.updateFinished(id, totalCases, passedCases, avgScore, status);
    }
}
