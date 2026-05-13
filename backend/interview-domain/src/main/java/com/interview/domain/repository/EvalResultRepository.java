package com.interview.domain.repository;

import com.interview.domain.model.EvalResult;

import java.util.List;

public interface EvalResultRepository {

    void save(EvalResult result);

    List<EvalResult> findByRunId(Long runId);
}
