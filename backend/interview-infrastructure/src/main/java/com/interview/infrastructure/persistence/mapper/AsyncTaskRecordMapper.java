package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.infrastructure.persistence.entity.AsyncTaskRecordPO;

import java.util.List;

public interface AsyncTaskRecordMapper {
    int insert(AsyncTaskRecordPO po);
    AsyncTaskRecord selectById(Long id);
    AsyncTaskRecord selectByTaskNo(String taskNo);
    List<AsyncTaskRecord> selectByStatus(String status);
    int updateStatus(Long id, String status, Integer progress);
    int updateError(Long id, String errorMsg);
}
