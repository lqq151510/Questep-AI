package com.interview.infrastructure.persistence.repository;

import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.infrastructure.persistence.entity.AsyncTaskRecordPO;
import com.interview.infrastructure.persistence.mapper.AsyncTaskRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class AsyncTaskRecordRepositoryImpl implements AsyncTaskRecordRepository {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskRecordRepositoryImpl.class);

    private final AsyncTaskRecordMapper mapper;
    public AsyncTaskRecordRepositoryImpl(AsyncTaskRecordMapper mapper) { this.mapper = mapper; }
    public AsyncTaskRecord create(String taskNo, String taskType, String bizType, Long bizId, Long createdBy) {
        AsyncTaskRecordPO po = new AsyncTaskRecordPO();
        po.setTaskNo(taskNo);
        po.setTaskType(taskType);
        po.setBizType(bizType);
        po.setBizId(bizId);
        po.setStatus("PENDING");
        po.setProgress(0);
        po.setCreatedBy(createdBy);
        mapper.insert(po);
        return mapper.selectById(po.getId());
    }
    public Optional<AsyncTaskRecord> findByTaskNo(String taskNo) { return Optional.ofNullable(mapper.selectByTaskNo(taskNo)); }
    public List<AsyncTaskRecord> findByStatus(String status) { return mapper.selectByStatus(normalizeStatus(status)); }

    @Override
    @Transactional
    public List<AsyncTaskRecord> claimPendingTasks(int limit) {
        int safeLimit = Math.max(1, limit);
        List<AsyncTaskRecord> tasks = mapper.selectPendingForUpdate(safeLimit);
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<Long> ids = tasks.stream().map(AsyncTaskRecord::id).toList();
        mapper.batchUpdateStatus(ids, normalizeStatus(TaskConstants.STATUS_PROCESSING), 0);
        return tasks;
    }

    @Override
    public AsyncTaskRecord updateStatus(Long id, String status, Integer progress) {
        String normalizedStatus = normalizeStatus(status);
        try {
            mapper.updateStatus(id, normalizedStatus, progress);
        } catch (RuntimeException ex) {
            if (isStatusEnumMismatch(ex)) {
                logger.error(
                        "async_task_records status enum mismatch detected: rawStatus={}, normalizedStatus={}, message={}",
                        status,
                        normalizedStatus,
                        ex.getMessage()
                );
            }
            throw ex;
        }
        return mapper.selectById(id);
    }

    @Override
    public AsyncTaskRecord updateError(Long id, String errorMsg, String errorCode, String stage, Boolean retryable) {
        mapper.updateError(id, errorMsg, errorCode, stage, retryable);
        return mapper.selectById(id);
    }

    static String normalizeStatus(String status) {
        if (TaskConstants.STATUS_PROCESSING.equals(status)) {
            return TaskConstants.STATUS_RUNNING;
        }
        return status;
    }

    private boolean isStatusEnumMismatch(RuntimeException ex) {
        String message = ex.getMessage();
        return message != null
                && message.contains("Data truncated for column 'status'");
    }
}
