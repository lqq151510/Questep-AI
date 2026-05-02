package com.interview.api.scheduler;

import com.interview.application.service.MaterialParseTaskProcessor;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

@Component
public class AsyncTaskScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskScheduler.class);

    private final AsyncTaskRecordRepository asyncTaskRecordRepository;
    private final MaterialParseTaskProcessor materialParseTaskProcessor;
    private final Executor taskExecutor;

    public AsyncTaskScheduler(
            AsyncTaskRecordRepository asyncTaskRecordRepository,
            MaterialParseTaskProcessor materialParseTaskProcessor,
            @Value("${app.async.core-pool-size:2}") int corePoolSize,
            @Value("${app.async.max-pool-size:4}") int maxPoolSize,
            @Value("${app.async.queue-capacity:20}") int queueCapacity
    ) {
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
        this.materialParseTaskProcessor = materialParseTaskProcessor;
        this.taskExecutor = buildExecutor(corePoolSize, maxPoolSize, queueCapacity);
    }

    private Executor buildExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler((r, e) ->
                logger.warn("Async task rejected: queue full (size={})", e.getQueue().size()));
        executor.initialize();
        return executor;
    }

    @Scheduled(fixedDelay = 10000)
    public void processPendingTasks() {
        logger.debug("Checking for pending tasks...");

        List<AsyncTaskRecord> pendingTasks = asyncTaskRecordRepository.findByStatus(TaskConstants.STATUS_PENDING);

        if (pendingTasks.isEmpty()) {
            logger.debug("No pending tasks to process.");
            return;
        }

        logger.info("Found {} pending tasks to process.", pendingTasks.size());

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
}
