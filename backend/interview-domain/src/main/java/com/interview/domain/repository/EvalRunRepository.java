package com.interview.domain.repository;

import com.interview.domain.model.EvalRun;

import java.util.List;
import java.util.Optional;

public interface EvalRunRepository {

    EvalRun save(EvalRun run);

    Optional<EvalRun> findByRunKey(String runKey);

    List<EvalRun> findAll();

    void updateFinished(Long id, int totalCases, int passedCases, double avgScore, String status);
}
