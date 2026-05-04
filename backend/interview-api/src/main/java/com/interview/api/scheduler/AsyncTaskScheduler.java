package com.interview.api.scheduler;

import com.interview.application.service.MaterialParseTaskProcessor;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class AsyncTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskScheduler.class);

    private final AsyncTaskRecordRepository asyncTaskRecordRepository;
    private final MaterialParseTaskProcessor materialParseTaskProcessor;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final int claimBatchSize;

    public AsyncTaskScheduler(
            AsyncTaskRecordRepository asyncTaskRecordRepository,
            MaterialParseTaskProcessor materialParseTaskProcessor,
            @Value("${app.async.core-pool-size:2}") int corePoolSize,
            @Value("${app.async.max-pool-size:4}") int maxPoolSize,
            @Value("${app.async.queue-capacity:20}") int queueCapacity,
            @Value("${app.async.claim-batch-size:10}") int claimBatchSize
    ) {
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
        this.materialParseTaskProcessor = materialParseTaskProcessor;
        this.taskExecutor = buildExecutor(corePoolSize, maxPoolSize, queueCapacity);
        this.claimBatchSize = Math.max(1, claimBatchSize);
    }

    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        // Reliability first: when the pool is saturated, run on scheduler thread instead of dropping tasks.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Scheduled(fixedDelay = 10000)
    public void processPendingTasks() {
        logger.debug("Checking for pending tasks...");

        List<AsyncTaskRecord> pendingTasks = asyncTaskRecordRepository.claimPendingTasks(claimBatchSize);

        if (pendingTasks.isEmpty()) {
            logger.debug("No pending tasks to process.");
            return;
        }

        logger.info("Claimed {} pending tasks for processing.", pendingTasks.size());

        for (AsyncTaskRecord task : pendingTasks) {
            taskExecutor.execute(() -> {
                try {
                    logger.info("Processing task: taskNo={}, taskType={}", task.taskNo(), task.taskType());
                    materialParseTaskProcessor.processTask(task);
                } catch (Exception e) {
                    logger.error("Error processing task: taskNo={}", task.taskNo(), e);
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down async task executor...");
        taskExecutor.shutdown();
    }
}
