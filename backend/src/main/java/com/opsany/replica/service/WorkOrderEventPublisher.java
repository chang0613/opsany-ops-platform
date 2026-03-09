package com.opsany.replica.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.opsany.replica.config.AppProperties;
import com.opsany.replica.config.RabbitMqConfig;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkOrderEventPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final AppProperties appProperties;

    public void publish(WorkOrderCreatedEvent event) {
        if (!appProperties.getMessaging().isPublishEnabled()) {
            throw new IllegalStateException("RabbitMQ publishing is disabled for the current profile");
        }

        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            throw new IllegalStateException("RabbitTemplate is not available");
        }

        LOGGER.info("Publishing work order event {}", event.getOrderNo());
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.WORKBENCH_EXCHANGE,
            RabbitMqConfig.WORK_ORDER_CREATED_ROUTING_KEY,
            event
        );
    }
}
