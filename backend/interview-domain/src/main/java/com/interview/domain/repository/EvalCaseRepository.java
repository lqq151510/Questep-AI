package com.interview.domain.repository;

import com.interview.domain.model.EvalCase;

import java.util.List;
import java.util.Optional;

public interface EvalCaseRepository {

    List<EvalCase> findActive();

    Optional<EvalCase> findById(Long id);
}
