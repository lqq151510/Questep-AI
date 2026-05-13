package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.EvalCase;

import java.util.List;

public interface EvalCaseMapper {

    List<EvalCase> selectActive();

    EvalCase selectById(Long id);
}
