package com.opsany.replica.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String WORKBENCH_EXCHANGE = "opsany.workbench.exchange";
    public static final String WORK_ORDER_CREATED_QUEUE = "opsany.workorder.created.queue";
    public static final String WORK_ORDER_CREATED_ROUTING_KEY = "opsany.workorder.created";

    @Bean
    DirectExchange workbenchExchange() {
        return new DirectExchange(WORKBENCH_EXCHANGE, true, false);
    }

    @Bean
    Queue workOrderCreatedQueue() {
        return QueueBuilder.durable(WORK_ORDER_CREATED_QUEUE).build();
    }

    @Bean
    Binding workOrderCreatedBinding(Queue workOrderCreatedQueue, DirectExchange workbenchExchange) {
        return BindingBuilder.bind(workOrderCreatedQueue)
            .to(workbenchExchange)
            .with(WORK_ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }
}
