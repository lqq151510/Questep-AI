package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.infrastructure.persistence.entity.AsyncTaskRecordPO;
import com.interview.infrastructure.persistence.mapper.AsyncTaskRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AsyncTaskRecordRepositoryImpl implements AsyncTaskRecordRepository {
    private final AsyncTaskRecordMapper mapper;
    public AsyncTaskRecordRepositoryImpl(AsyncTaskRecordMapper mapper) { this.mapper = mapper; }
    public AsyncTaskRecord create(String taskNo, String taskType, Long bizId, Long createdBy) {
        AsyncTaskRecordPO po = new AsyncTaskRecordPO();
        po.setTaskNo(taskNo);
        po.setTaskType(taskType);
        po.setBizId(bizId);
        po.setStatus("PENDING");
        po.setProgress(0);
        po.setCreatedBy(createdBy);
        mapper.insert(po);
        return mapper.selectById(po.getId());
    }
    public Optional<AsyncTaskRecord> findByTaskNo(String taskNo) { return Optional.ofNullable(mapper.selectByTaskNo(taskNo)); }
}
