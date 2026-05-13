package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.EvalRun;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EvalRunMapper {

    int insert(EvalRun run);

    EvalRun selectByRunKey(@Param("runKey") String runKey);

    List<EvalRun> selectAll();

    EvalRun selectById(Long id);

    int updateFinished(@Param("id") Long id,
                       @Param("totalCases") int totalCases,
                       @Param("passedCases") int passedCases,
                       @Param("avgScore") double avgScore,
                       @Param("status") String status);
}
