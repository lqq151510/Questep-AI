package com.interview.api.scheduler;

import com.interview.application.service.MaterialParseTaskProcessor;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AsyncTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskScheduler.class);

    private final AsyncTaskRecordRepository asyncTaskRecordRepository;
    private final MaterialParseTaskProcessor materialParseTaskProcessor;

    public AsyncTaskScheduler(
            AsyncTaskRecordRepository asyncTaskRecordRepository,
            MaterialParseTaskProcessor materialParseTaskProcessor
    ) {
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
        this.materialParseTaskProcessor = materialParseTaskProcessor;
    }

    @Scheduled(fixedDelay = 10000)
    public void processPendingTasks() {
        logger.debug("Checking for pending tasks...");

        List<AsyncTaskRecord> pendingTasks = asyncTaskRecordRepository.findByStatus("PENDING");

        if (pendingTasks.isEmpty()) {
            logger.debug("No pending tasks to process.");
            return;
        }

        logger.info("Found {} pending tasks to process.", pendingTasks.size());

        for (AsyncTaskRecord task : pendingTasks) {
            try {
                logger.info("Processing task: taskNo={}, taskType={}", task.taskNo(), task.taskType());
                materialParseTaskProcessor.processTask(task);
            } catch (Exception e) {
                logger.error("Error processing task: taskNo={}", task.taskNo(), e);
            }
        }
    }
}
