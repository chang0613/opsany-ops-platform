package com.opsany.replica.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.opsany.replica.config.RabbitMqConfig;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(WorkOrderCreatedEvent event) {
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.WORKBENCH_EXCHANGE,
            RabbitMqConfig.WORK_ORDER_CREATED_ROUTING_KEY,
            event
        );
    }
}
