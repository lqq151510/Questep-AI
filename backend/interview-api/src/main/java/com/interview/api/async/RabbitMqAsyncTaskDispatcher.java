package com.interview.api.async;

import com.interview.application.port.AsyncTaskDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.async.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqAsyncTaskDispatcher implements AsyncTaskDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMqAsyncTaskDispatcher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitMqAsyncTaskDispatcher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.async.rabbitmq.exchange:material.parse.exchange}") String exchange,
            @Value("${app.async.rabbitmq.routing-key:material.parse}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void dispatchMaterialParseTask(String taskNo) {
        rabbitTemplate.convertAndSend(exchange, routingKey, taskNo);
        logger.info("Published material parse task: taskNo={}", taskNo);
    }
}
