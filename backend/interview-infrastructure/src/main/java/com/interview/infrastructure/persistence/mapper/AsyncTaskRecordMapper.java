package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.infrastructure.persistence.entity.AsyncTaskRecordPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AsyncTaskRecordMapper {
    int insert(AsyncTaskRecordPO po);

    int updateById(AsyncTaskRecordPO po);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("progress") Integer progress);

    int updateError(@Param("id") Long id, @Param("errorMsg") String errorMsg, @Param("errorCode") String errorCode, @Param("stage") String stage, @Param("retryable") Boolean retryable);

    AsyncTaskRecord selectById(Long id);

    AsyncTaskRecord selectByTaskNo(String taskNo);

    List<AsyncTaskRecord> selectByStatus(String status);

    List<AsyncTaskRecord> selectPendingForUpdate(@Param("limit") int limit);

    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status, @Param("progress") Integer progress);

    List<AsyncTaskRecord> selectByCondition(AsyncTaskRecordPO condition);

    List<AsyncTaskRecord> selectByIds(@Param("ids") List<Long> ids);

    int deleteById(Long id);
}
