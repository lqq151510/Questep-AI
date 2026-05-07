package com.interview.domain.repository;

import com.interview.domain.model.AsyncTaskRecord;

import java.util.List;
import java.util.Optional;

public interface AsyncTaskRecordRepository {

    AsyncTaskRecord create(String taskNo, String taskType, Long bizId, Long createdBy);

    Optional<AsyncTaskRecord> findByTaskNo(String taskNo);

    List<AsyncTaskRecord> findByStatus(String status);

    List<AsyncTaskRecord> claimPendingTasks(int limit);

    AsyncTaskRecord updateStatus(Long id, String status, Integer progress);

    AsyncTaskRecord updateError(Long id, String errorMsg);
}
