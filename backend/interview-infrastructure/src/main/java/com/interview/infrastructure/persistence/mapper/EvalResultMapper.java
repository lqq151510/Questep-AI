package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.EvalResult;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EvalResultMapper {

    int insert(EvalResult result);

    List<EvalResult> selectByRunId(@Param("runId") Long runId);
}
