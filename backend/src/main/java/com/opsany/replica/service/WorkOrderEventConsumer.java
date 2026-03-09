package com.opsany.replica.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.opsany.replica.config.RabbitMqConfig;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkOrderEventConsumer {

    private final WorkOrderProjectionService workOrderProjectionService;

    @RabbitListener(queues = RabbitMqConfig.WORK_ORDER_CREATED_QUEUE, autoStartup = "${app.messaging.consumer-enabled:true}")
    public void onWorkOrderCreated(WorkOrderCreatedEvent event) {
        workOrderProjectionService.handleWorkOrderCreated(event);
    }
}
