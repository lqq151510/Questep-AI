package com.interview.api.async;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.async.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncTaskRabbitMqConfig {

    @Bean
    public Queue materialParseQueue(@Value("${app.async.rabbitmq.queue:material.parse.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public DirectExchange materialParseExchange(@Value("${app.async.rabbitmq.exchange:material.parse.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Binding materialParseBinding(
            Queue materialParseQueue,
            DirectExchange materialParseExchange,
            @Value("${app.async.rabbitmq.routing-key:material.parse}") String routingKey
    ) {
        return BindingBuilder.bind(materialParseQueue).to(materialParseExchange).with(routingKey);
    }
}
