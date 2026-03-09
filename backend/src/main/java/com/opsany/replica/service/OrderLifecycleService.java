package com.opsany.replica.service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.opsany.replica.config.AppProperties;
import com.opsany.replica.domain.WorkOrder;
import com.opsany.replica.dto.CreateWorkOrderRequest;
import com.opsany.replica.messaging.WorkOrderCreatedEvent;
import com.opsany.replica.repository.WorkOrderRepository;
import com.opsany.replica.security.SessionUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderEventPublisher workOrderEventPublisher;
    private final WorkOrderProjectionService workOrderProjectionService;
    private final PlatformBootstrapService platformBootstrapService;
    private final AppProperties appProperties;

    @Transactional
    public WorkOrder createOrder(CreateWorkOrderRequest request, SessionUser sessionUser) {
        LocalDateTime now = LocalDateTime.now();
        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
            .orderNo(nextOrderNo(now))
            .title(request.getTitle())
            .type(request.getType())
            .creatorUsername(sessionUser.getUsername())
            .creatorDisplayName(sessionUser.getDisplayName())
            .progress("<2> 工程师处理")
            .status("正在进行")
            .priority(defaultIfBlank(request.getPriority(), "中"))
            .serviceName(defaultIfBlank(request.getServiceName(), request.getTitle()))
            .description(defaultIfBlank(request.getDescription(), "通过 Vue3 + Spring Boot 重建的模拟工单"))
            .estimatedAt("--")
            .createdAt(now)
            .build());

        WorkOrderCreatedEvent event = new WorkOrderCreatedEvent(
            workOrder.getId(),
            workOrder.getOrderNo(),
            workOrder.getTitle(),
            workOrder.getType(),
            workOrder.getCreatorUsername(),
            workOrder.getCreatorDisplayName(),
            workOrder.getPriority(),
            workOrder.getCreatedAt()
        );

        try {
            workOrderEventPublisher.publish(event);
        } catch (Exception exception) {
            if (appProperties.getMessaging().isFallbackOnPublishFailure()) {
                workOrderProjectionService.handleWorkOrderCreated(event);
            } else {
                throw exception;
            }
        }

        platformBootstrapService.evictBootstrapCache(sessionUser.getUserId());
        return workOrder;
    }

    private String nextOrderNo(LocalDateTime now) {
        return now.format(DateFormats.SECOND_PRECISION).replace("-", "").replace(" ", "").replace(":", "")
            + ThreadLocalRandom.current().nextInt(10, 99);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
