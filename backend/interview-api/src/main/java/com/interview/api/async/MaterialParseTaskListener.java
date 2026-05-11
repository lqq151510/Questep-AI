package com.interview.api.async;

import com.interview.application.service.MaterialParseTaskProcessor;
import com.interview.common.constant.TaskConstants;
import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.async.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class MaterialParseTaskListener {
    private static final Logger logger = LoggerFactory.getLogger(MaterialParseTaskListener.class);

    private final AsyncTaskRecordRepository asyncTaskRecordRepository;
    private final MaterialParseTaskProcessor materialParseTaskProcessor;

    public MaterialParseTaskListener(
            AsyncTaskRecordRepository asyncTaskRecordRepository,
            MaterialParseTaskProcessor materialParseTaskProcessor
    ) {
        this.asyncTaskRecordRepository = asyncTaskRecordRepository;
        this.materialParseTaskProcessor = materialParseTaskProcessor;
    }

    @RabbitListener(queues = "${app.async.rabbitmq.queue:material.parse.queue}")
    public void consumeMaterialParseTask(String taskNo) {
        AsyncTaskRecord task = asyncTaskRecordRepository.findByTaskNo(taskNo).orElse(null);
        if (task == null) {
            logger.warn("Skip message, task not found: taskNo={}", taskNo);
            return;
        }

        if (!TaskConstants.STATUS_PENDING.equals(task.status())) {
            logger.info("Skip message, task status is not pending: taskNo={}, status={}", task.taskNo(), task.status());
            return;
        }

        logger.info("Consuming material parse task: taskNo={}", task.taskNo());
        materialParseTaskProcessor.processTask(task);
    }
}
