package com.interview.infrastructure.persistence.mapper;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.infrastructure.persistence.entity.AsyncTaskRecordPO;

public interface AsyncTaskRecordMapper {
    int insert(AsyncTaskRecordPO po);
    AsyncTaskRecord selectById(Long id);
    AsyncTaskRecord selectByTaskNo(String taskNo);
}
