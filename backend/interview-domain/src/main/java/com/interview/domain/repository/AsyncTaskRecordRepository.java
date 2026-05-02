package com.interview.domain.repository;

import com.interview.domain.model.AsyncTaskRecord;

import java.util.Optional;

public interface AsyncTaskRecordRepository {

    AsyncTaskRecord create(String taskNo, String taskType, Long bizId, Long createdBy);

    Optional<AsyncTaskRecord> findByTaskNo(String taskNo);
}
