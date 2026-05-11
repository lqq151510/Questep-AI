package com.interview.api.async;

import com.interview.application.port.AsyncTaskDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.async.rabbitmq.enabled", havingValue = "false")
public class NoopAsyncTaskDispatcher implements AsyncTaskDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(NoopAsyncTaskDispatcher.class);

    @Override
    public void dispatchMaterialParseTask(String taskNo) {
        logger.warn("RabbitMQ dispatch is disabled, task remains pending: taskNo={}", taskNo);
    }
}
