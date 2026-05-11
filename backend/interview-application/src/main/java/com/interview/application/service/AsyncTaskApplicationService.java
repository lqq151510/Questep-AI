package com.interview.application.service;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AsyncTaskApplicationService {

    private final AsyncTaskRecordRepository asyncTaskRecordRepository;

    public AsyncTaskApplicationService(AsyncTaskRecordRepository asyncTaskRecordRepository) {
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
    }

    public AsyncTaskRecord getByTaskNo(Long userId, String taskNo) {
        AsyncTaskRecord task = asyncTaskRecordRepository.findByTaskNo(taskNo)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (userId == null || task.createdBy() == null || !task.createdBy().equals(userId)) {
            throw new ResourceNotFoundException("Task not found");
        }
        return task;
    }
}
